package studio.mevera.imperat.annotations.base.system.parsers;

import static studio.mevera.imperat.annotations.base.AnnotationHelper.isAbnormalClass;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.Async;
import studio.mevera.imperat.annotations.Cooldown;
import studio.mevera.imperat.annotations.Description;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.InheritedArg;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.Shortcut;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.MethodCommandExecutor;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CommandElementParser<S extends Source> extends CommandClassParser<S, Set<Command<S>>> {

    private final ImperatConfig<S> config;
    private final ElementSelector<MethodElement> methodSelector;
    private final ParameterParser<S> parameterParser;

    CommandElementParser(Imperat<S> imperat, AnnotationParser<S> parser, ElementSelector<MethodElement> methodSelector) {
        super(imperat, parser, methodSelector);
        this.config = imperat.config();
        this.methodSelector = methodSelector;
        this.parameterParser = new ParameterParser<>(config);
    }

    @Override
    public Set<Command<S>> visitCommandClass(@NotNull ClassElement clazz) throws Exception {
        Set<Command<S>> commands = new LinkedHashSet<>();
        commands.add(parseSpecificClass(null, clazz));
        commands.addAll(parseEmbeddedRoots(clazz));
        return commands;
    }

    protected Command<S> parseSpecificClass(@Nullable Command<S> parent, ClassElement clazz) {
        //core
        //two possibilities, either this class is a root command, or it's not. If it is, we parse it as a root command,
        // if it's not, we parse it as a regular class and look for methods and inner classes inside of it.
        Command<S> currentCommand;
        if (clazz.isAnnotationPresent(RootCommand.class)) {
            //umm
            RootCommand ann = clazz.getAnnotation(RootCommand.class);
            assert ann != null;

            if (parent != null) {
                throw new IllegalStateException(
                        "Class '" + clazz.getElement().getName() + "' is annotated with @RootCommand but has a parent command '"
                                + parent.getName() + "'. Root commands cannot have a parent."
                );
            }
            String[] values = config.replacePlaceholders(ann.value());

            currentCommand = Command.create(imperat, null, 0, values[0])
                                     .aliases(Arrays.asList(values).subList(1, values.length))
                                     .supressPermissionsForAutoCompletion(ann.skipSuggestionsChecks())
                                     .build();

        } else if (clazz.isAnnotationPresent(SubCommand.class)) {
            SubCommand ann = clazz.getAnnotation(SubCommand.class);
            assert ann != null;

            if (parent == null) {
                throw new IllegalStateException(
                        "Class '" + clazz.getElement().getName()
                                + "' is annotated with @SubCommand but does not have a parent command."
                                + " SubCommands must have a parent command."
                );
            }

            //verify that attachnode exists in parent command if it's not blank
            verifyAttachmentNodeExistsInParent(parent, ann.attachTo(), ann.value()[0]);

            String[] values = config.replacePlaceholders(ann.value());
            currentCommand = Command.create(imperat, parent, 0, values[0])
                                     .aliases(Arrays.asList(values).subList(1, values.length))
                                     .build();
        } else {
            return null;
        }

        //we process methods first, THEN we process classes inside of this class
        var methods = clazz.getChildren()
                              .stream()
                              .filter((e) -> e instanceof MethodElement)
                              .map((e) -> (MethodElement) e)
                              .filter((m) -> methodSelector.canBeSelected(imperat, imperat.getAnnotationParser(), m, false))
                              .sorted((m1, m2) -> {
                                  //we want to process @Execute methods first, then @SubCommand methods, then the rest
                                  boolean m1Execute = m1.isAnnotationPresent(Execute.class);
                                  boolean m2Execute = m2.isAnnotationPresent(Execute.class);
                                  if (m1Execute && !m2Execute) {
                                      return -1;
                                  } else if (!m1Execute && m2Execute) {
                                      return 1;
                                  }

                                  boolean m1Sub = m1.isAnnotationPresent(SubCommand.class);
                                  boolean m2Sub = m2.isAnnotationPresent(SubCommand.class);
                                  if (m1Sub && !m2Sub) {
                                      return -1;
                                  } else if (!m1Sub && m2Sub) {
                                      return 1;
                                  }

                                  //otherwise, we don't care about the order
                                  return 0;
                              }) // makes @Execute methods parsed before @Subcommand methods
                              .toList();

        for (MethodElement method : methods) {
            //either its @Execute, or its @Subcommand method or BOTH (which is weird but why not)
            if (method.isAnnotationPresent(Execute.class)) {
                CommandPathway<S> pathway = finalizedPathway(
                        method,
                        currentCommand,
                        parsePathwayMethod(currentCommand, method)
                ).build(currentCommand);
                currentCommand.addPathway(pathway);
            }

            if (method.isAnnotationPresent(SubCommand.class)) {
                var sub = parseCommandMethod(currentCommand, method);

                SubCommand ann = method.isAnnotationPresent(SubCommand.class) ? method.getAnnotation(SubCommand.class) :
                                         method.getParent().getAnnotation(SubCommand.class);
                assert ann != null;

                String attachmentNodeFormat = ann.attachTo();
                verifyAttachmentNodeExistsInParent(currentCommand, attachmentNodeFormat, sub.getName());
                currentCommand.addSubCommand(sub, attachmentNodeFormat);
            }
        }

        //now we look for inner classes and process them as well, they might be annotated with @SubCommand
        var innerClasses = clazz.getChildren()
                                   .stream()
                                   .filter((e) -> e instanceof ClassElement)
                                   .filter((e) -> e.isAnnotationPresent(SubCommand.class))
                                   .map((e) -> (ClassElement) e)
                                   .filter((e) -> !isAbnormalClass(e))
                                   .toList();

        for (ClassElement inner : innerClasses) {
            Command<S> subCommand = parseSpecificClass(currentCommand, inner);
            if (subCommand != null) {
                SubCommand ann = inner.getAnnotation(SubCommand.class);
                assert ann != null;
                String attachmentNodeFormat = ann.attachTo();
                currentCommand.addSubCommand(subCommand, attachmentNodeFormat);
            }
        }

        return currentCommand;
    }

    private void verifyAttachmentNodeExistsInParent(@Nullable Command<S> parent, String attachmentNode, String commandName) {
        if (attachmentNode.isBlank()) {
            return;
        }

        if (parent == null) {
            throw new IllegalStateException(
                    "Command '" + commandName + "' specifies an attachment node format of '" + attachmentNode
                            + "' but does not have a parent command. Only subcommands can specify an attachment node."
            );
        }

        boolean found = parent.getDedicatedPathways().stream()
                                .flatMap((path) -> path.getArguments().stream())
                                .anyMatch((arg) -> arg.format().equals(attachmentNode));
        if (!found) {
            throw new IllegalStateException(
                    "Command '" + commandName + "' specifies an attachment node format of '" + attachmentNode
                            + "' but no argument with that format was found"
                            + " in the parent command '" + parent.getName() + "'."
            );
        }
    }

    protected Command<S> parseCommandMethod(@Nullable Command<S> parent, MethodElement method) {
        Command<S> command = null;
        if (method.isAnnotationPresent(RootCommand.class)) {
            RootCommand ann = method.getAnnotation(RootCommand.class);
            assert ann != null;
            if (parent != null) {
                throw new IllegalStateException(
                        "Method '" + method.getElement().getName() + "' is annotated with @RootCommand but has a parent command '"
                                + parent.getName() + "'. Root commands cannot have a parent."
                );
            }
            String[] values = config.replacePlaceholders(ann.value());
            command = Command.create(imperat, null, 0, values[0])
                           .aliases(Arrays.asList(values).subList(1, values.length))
                           .supressPermissionsForAutoCompletion(ann.skipSuggestionsChecks())
                           .build();
        }

        if (method.isAnnotationPresent(SubCommand.class)) {
            SubCommand ann = method.getAnnotation(SubCommand.class);
            assert ann != null;
            if (parent == null) {
                throw new IllegalStateException(
                        "Method '" + method.getElement().getName()
                                + "' is annotated with @SubCommand but does not have a parent command."
                                + " SubCommands must have a parent command."
                );
            }
            String[] values = config.replacePlaceholders(ann.value());
            command = Command.create(imperat, parent, 0, values[0])
                              .aliases(Arrays.asList(values).subList(1, values.length))
                              .build();
        }

        if (command == null) {
            throw new IllegalStateException("Pathway-method '" + method.getName()
                                                    + "' is neither annotated with @RootCommand nor @SubCommand. Pathway methods must be annotated "
                                                    + "with either of these annotations.");
        }


        var pathway = finalizedPathway(
                method,
                command,
                parsePathwayMethod(command, method)
        ).build(command);


        command.addPathway(pathway);

        return command;
    }

    protected CommandPathway.Builder<S> parsePathwayMethod(Command<S> owningCommand, MethodElement method) {

        List<Argument<S>> personalParams = new ArrayList<>();
        for (ParameterElement param : method.getParameters()) {
            if (isSenderParameter(param) || param.isAnnotationPresent(InheritedArg.class)) {
                continue;
            }

            Argument<S> argument = parseMethodParameter(method, param);
            if (argument != null) {
                personalParams.add(argument);
            }
        }

        return processedPathway(
                method,
                CommandPathway.<S>builder(method)
                        .parameters(personalParams)
                        .examples(
                                (method.isAnnotationPresent(Execute.class) ?
                                         config.replacePlaceholders(Objects.requireNonNull(method.getAnnotation(Execute.class)).examples()) :
                                         new String[0])
                        )
                        .execute(MethodCommandExecutor.of(imperat, method))
        );


    }

    private CommandPathway.Builder<S> processedPathway(MethodElement method, CommandPathway.Builder<S> builder) {
        if (method.isAnnotationPresent(Permission.class)) {
            Permission ann = method.getAnnotation(Permission.class);
            assert ann != null;
            String permLine = config.replacePlaceholders(ann.value());
            PermissionsData data = PermissionsData.fromText(permLine);
            builder.permission(data);
        }

        if (method.isAnnotationPresent(Description.class)) {
            Description ann = method.getAnnotation(Description.class);
            assert ann != null;
            String description = config.replacePlaceholders(ann.value());
            builder.description(studio.mevera.imperat.command.Description.of(description));
        }

        if (method.isAnnotationPresent(Cooldown.class)) {
            Cooldown ann = method.getAnnotation(Cooldown.class);
            assert ann != null;
            String perm = ann.permission().isEmpty() ? null : ann.permission();
            builder.cooldown(ann.value(), ann.unit(), perm);
        }

        if (method.isAnnotationPresent(Async.class)) {
            builder.coordinator(CommandCoordinator.async());
        }

        return builder;
    }

    private CommandPathway.Builder<S> finalizedPathway(MethodElement method, Command<S> owningCommand, CommandPathway.Builder<S> builder) {

        List<Argument<S>> parsedMethodArgs = method.getParameters().stream()
                                                     .filter((p) -> !isSenderParameter(p))
                                                     .map((p) -> parseMethodParameter(method, p))
                                                     .filter(Objects::nonNull)
                                                     .toList();

        Shortcut shortcutAnn = method.getAnnotation(Shortcut.class);
        if (shortcutAnn != null) {
            if (shortcutAnn.value().isEmpty()) {
                throw new IllegalStateException("Shortcut value cannot be empty for method '" + method.getName() + "'");
            }

            if (shortcutAnn.value().contains(" ")) {
                throw new IllegalStateException("Shortcut value cannot contain spaces for method '" + method.getName() + "'");
            }

            var shortcut = loadPathwayShortcut(method, parsedMethodArgs, owningCommand, builder, shortcutAnn);
            owningCommand.addShortcut(shortcut);
        }

        return builder;
    }


    protected Argument<S> parseMethodParameter(MethodElement method, ParameterElement parameter) {
        return parameterParser.parseParameter(parameter);
    }

    private Set<Command<S>> parseEmbeddedRoots(ClassElement root) {
        Set<Command<S>> commands = new LinkedHashSet<>();
        for (ParseElement<?> child : root.getChildren()) {
            if (child instanceof ClassElement classChild) {
                if (classChild.getElement().isAnnotationPresent(RootCommand.class)) {
                    commands.add(parseSpecificClass(null, classChild));
                }
                commands.addAll(parseEmbeddedRoots(classChild));
            } else if (child instanceof MethodElement methodChild) {
                if (methodChild.getElement().isAnnotationPresent(RootCommand.class)) {
                    commands.add(parseCommandMethod(null, methodChild));
                }
            }
        }
        return commands;
    }

    private boolean isSenderParameter(ParameterElement param) {
        Type type = param.getElement().getParameterizedType();
        return imperat.canBeSender(type) || config.hasSourceResolver(type);
    }

    private studio.mevera.imperat.command.Command<S> loadPathwayShortcut(
            @NotNull MethodElement method,
            @NotNull List<Argument<S>> parseMethodParameters,
            @NotNull studio.mevera.imperat.command.Command<S> originalCommand,
            @NotNull CommandPathway.Builder<S> originalPathway,
            @NotNull Shortcut shortcutAnn
    ) {

        String shortcutValue = config.replacePlaceholders(shortcutAnn.value());

        studio.mevera.imperat.command.Command<S> shortcut = originalCommand.getShortcut(shortcutValue);
        if (shortcut == null) {
            shortcut = studio.mevera.imperat.command.Command.create(imperat, shortcutValue, method)
                               .setMetaPropertiesFromOtherCommand(originalCommand)
                               .build();
        }

        CommandPathway<S> fabricated = CommandPathway.<S>builder()
                                               .parameters(parseMethodParameters)
                                               .execute(originalPathway.getExecution())
                                               .permission(originalPathway.getPermission())
                                               .description(originalPathway.getDescription())
                                               .build(shortcut);

        shortcut.addPathway(fabricated);
        return shortcut;
    }


}
