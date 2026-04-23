package studio.mevera.imperat.annotations.base.parsers;

import static studio.mevera.imperat.annotations.base.AnnotationHelper.isAbnormalClass;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.base.AnnotationParser;
import studio.mevera.imperat.annotations.base.ExecutorServiceProvider;
import studio.mevera.imperat.annotations.base.MethodCommandExecutor;
import studio.mevera.imperat.annotations.base.element.ClassElement;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.annotations.base.element.selector.ElementSelector;
import studio.mevera.imperat.annotations.types.Async;
import studio.mevera.imperat.annotations.types.Cooldown;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.ExceptionHandler;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.PathwayCommand;
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.Processor;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Secret;
import studio.mevera.imperat.annotations.types.Shortcut;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandCoordinator;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.processors.CommandPostProcessor;
import studio.mevera.imperat.command.processors.CommandPreProcessor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandExceptionHandler;
import studio.mevera.imperat.exception.InvalidSourceException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.util.asm.MethodCaller;
import studio.mevera.imperat.util.asm.MethodCallerFactory;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandElementParser<S extends CommandSource> extends CommandClassParser<S, Set<Command<S>>> {

    private final ImperatConfig<S> config;
    private final ElementSelector<MethodElement> methodSelector;
    private final ParameterParser<S> parameterParser;
    private final PathwaySyntaxParser<S> pathwaySyntaxParser;
    CommandElementParser(Imperat<S> imperat, AnnotationParser<S> parser, ElementSelector<MethodElement> methodSelector) {
        super(imperat, parser, methodSelector);
        this.config = imperat.config();
        this.methodSelector = methodSelector;
        this.parameterParser = new ParameterParser<>(config);
        this.pathwaySyntaxParser = PathwaySyntaxParser.of(imperat, this, parameterParser);
    }

    @Override
    public Set<Command<S>> visitCommandClass(@NotNull ClassElement clazz) {
        Set<Command<S>> commands = new LinkedHashSet<>();
        var parsedSpecificCommand = parseSpecificClass(null, clazz);
        if (parsedSpecificCommand != null) {
            commands.add(parsedSpecificCommand);
        }
        commands.addAll(parseEmbeddedRoots(clazz)
                                .stream().filter(Objects::nonNull)
                                .collect(Collectors.toSet()));

        commands.addAll(pathwaySyntaxParser.getParsedPathwayCommands());
        return commands;
    }

    private Command<S> parseMetaDataForCmd(Command<S> command, ParseElement<?> element) {
        Permission[] permAnnotations = element.getAnnotationsByType(Permission.class);
        PermissionsData permMeta = null;
        for (Permission permissionAnn : permAnnotations) {
            String permLine = config.replacePlaceholders(permissionAnn.value());
            if (permMeta == null) {
                permMeta = PermissionsData.fromText(permLine);
            } else {
                permMeta.append(PermissionsData.fromText(permLine));
            }
        }
        if (permMeta != null) {
            command.setPermissionData(permMeta);
        }

        Description descriptionAnn = element.getAnnotation(Description.class);
        if (descriptionAnn != null) {
            command.describe(descriptionAnn.value());
        }

        return command;
    }

    @SuppressWarnings("unchecked")
    protected <E extends Throwable> Command<S> parseSpecificClass(@Nullable Command<S> parent, ClassElement clazz) {
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

        currentCommand = parseMetaDataForCmd(currentCommand, clazz);

        if (clazz.isAnnotationPresent(Secret.class)) {
            currentCommand.setSecret(true);
        }

        //we process methods first, THEN we process classes inside of this class
        var methods = clazz.getChildren()
                              .stream()
                              .filter((e) -> e instanceof MethodElement)
                              .map((e) -> (MethodElement) e)
                              .filter((m) -> {
                                  return methodSelector.canBeSelected(imperat, imperat.getAnnotationParser(), m, false);
                              })
                              .sorted((m1, m2) -> {
                                  // Order: @Processor first, then @Execute, then @SubCommand, then the rest
                                  int rank1 = methodSortRank(m1);
                                  int rank2 = methodSortRank(m2);
                                  return Integer.compare(rank1, rank2);
                              })
                              .toList();

        for (MethodElement method : methods) {
            //either its @Execute, or its @Subcommand method or BOTH (which is weird but why not)
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                //we load it as a throwable resolver instead of a processor, and we don't care about the parent command
                ExceptionHandler ann = method.getAnnotation(ExceptionHandler.class);
                assert ann != null;

                var firstParam = method.getParameterAt(0);
                if (firstParam == null) {
                    throw new IllegalStateException("Method '" + method.getName()
                                                            + "' is annotated with @ExceptionHandler but has no parameters. Exception handler "
                                                            + "methods must have at"
                                                            + " least one parameter of type Throwable.");
                }

                if (!Throwable.class.isAssignableFrom(firstParam.getElement().getType())) {
                    throw new IllegalStateException("Method '" + method.getName()
                                                            + "' is annotated with @ExceptionHandler but its first parameter is of type '"
                                                            + firstParam.getElement().getType().getTypeName()
                                                            + "'. Exception handler methods must have a first parameter of type Throwable.");
                }
                if (ann.value() != firstParam.getElement().getType()) {
                    throw new IllegalStateException("Method '" + method.getName()
                                                            + "' is annotated with @ExceptionHandler for error type '" + ann.value().getName()
                                                            + "' but its first parameter is of type '" + firstParam.getElement().getType()
                                                                                                                 .getTypeName()
                                                            + "'. The first parameter of an exception handler method must be of the same type as "
                                                            + "the error type specified in the annotation.");
                }

                Class<E> errorType = (Class<E>) ann.value();
                CommandExceptionHandler<E, S> handler = ErrorHandlerParsingVisitor.loadErrorHandler(config, clazz, method);
                currentCommand.setErrorHandler(errorType, handler);
            }

            if (method.isAnnotationPresent(Processor.class)) {

                if (parent != null) {
                    throw new IllegalStateException(
                            "Method '" + method.getName() + "' in class '" + clazz.getElement().getName()
                                    + "' is annotated with @Processor but belongs to a subcommand."
                                    + " Processors can only be defined on root commands."
                    );
                }

                var firstParam = method.getParameterAt(0);
                if (firstParam == null) {
                    throw new IllegalStateException("Method '" + method.getName()
                                                            + "' is annotated with @Processor but has no parameters. Processor methods must have at"
                                                            + " least one parameter of type CommandContext or ExecutionContext.");
                }
                var rawType = firstParam.getElement().getType();
                if (!CommandContext.class.isAssignableFrom(rawType)) {
                    throw new IllegalStateException(
                            "Method '" + method.getName() + "' is annotated with @Processor but its first parameter is of type '"
                                    + firstParam.getElement().getType().getTypeName()
                                    + "'. Processor methods must have a first parameter of type CommandContext or ExecutionContext.");
                }
                Processor processorAnn = method.getAnnotation(Processor.class);
                assert processorAnn != null;

                if (ExecutionContext.class.isAssignableFrom(rawType)) {
                    //post processor
                    currentCommand.addPostProcessor(
                            this.loadPostProcessorMethod(method, processorAnn)
                    );
                } else {
                    //pre processor
                    currentCommand.addPreProcessor(
                            this.loadPreProcessorMethod(method, processorAnn)
                    );
                }
            }

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


            if (method.isAnnotationPresent(PathwayCommand.class)) {
                PathwayCommand ann = method.getAnnotation(PathwayCommand.class);
                assert ann != null;
                for (String syntax : ann.value()) {
                    String rootToken = syntax.substring(0, syntax.indexOf(' '));
                    String rootName = rootToken.split(PathwaySyntaxParser.LITERAL_SPLIT)[0];
                    Command<S> providedRoot = currentCommand.hasName(rootName) ? currentCommand : null;
                    pathwaySyntaxParser.loadCommand(providedRoot, syntax, method);
                }
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

    private @NotNull CommandPostProcessor<S> loadPostProcessorMethod(MethodElement method, Processor processorAnn) {
        return new MethodPostProcessor<>(method, processorAnn);
    }

    private @NotNull CommandPreProcessor<S> loadPreProcessorMethod(MethodElement method, Processor processorAnn) {
        return new MethodPreProcessor<>(method, processorAnn);
    }

    private int methodSortRank(MethodElement method) {
        if (method.isAnnotationPresent(Processor.class) || method.isAnnotationPresent(ExceptionHandler.class)) {
            return 0;
        }
        if (method.isAnnotationPresent(Execute.class)) {
            return 1;
        }
        if (method.isAnnotationPresent(SubCommand.class)) {
            return 2;
        }
        return 3;
    }

    static final class MethodPostProcessor<S extends CommandSource> implements CommandPostProcessor<S> {

        private final Priority priority;

        private final MethodCaller.BoundMethodCaller boundMethodCaller;

        MethodPostProcessor(MethodElement method, Processor annotation) {
            this.priority = Priority.of(annotation.priority());
            try {
                boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
                this.boundMethodCaller = MethodCallerFactory.methodHandles()
                                                 .createFor(method.getElement())
                                                 .bindTo(isStaticMethod ? null : method.getParent().getObjectInstance());
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to create method caller for processor method '" + method.getName() + "'", ex);
            }
        }

        @Override
        public void process(ExecutionContext<S> context) {
            boundMethodCaller.call(context);
        }

        @Override
        public @NotNull Priority getPriority() {
            return priority;
        }

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
        Command<S> command;
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
        } else if (method.isAnnotationPresent(SubCommand.class)) {
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
        } else if (method.isAnnotationPresent(PathwayCommand.class)) {
            PathwayCommand ann = method.getAnnotation(PathwayCommand.class);
            assert ann != null;

            boolean secret = method.isAnnotationPresent(Secret.class);
            for (String syntax : ann.value()) {
                String rootFormat = syntax.substring(0, syntax.indexOf(' '));

                List<String> cmdNames = new ArrayList<>(Arrays.asList(rootFormat.split(PathwaySyntaxParser.LITERAL_SPLIT)));

                Command<S> builtRoot = Command.create(imperat, cmdNames.get(0))
                                              .aliases(cmdNames.subList(1, cmdNames.size()))
                                              .supressPermissionsForAutoCompletion(ann.suppressPermissionCheckDuringAutoCompletion())
                                              .build();

                if (secret) {
                    builtRoot.setSecret(true);
                }
                pathwaySyntaxParser.loadCommand(builtRoot, syntax, method);
            }
            return null;
        } else {
            return null;
        }

        /*if (command == null) {
            throw new IllegalStateException("Pathway-method '" + method.getName()
                                                    + "' is neither annotated with @RootCommand nor @SubCommand nor @PathwayCommand. Pathway "
                                                    + "methods must be annotated "
                                                    + "with either of these annotations.");
        }*/

        command = parseMetaDataForCmd(command, method);

        if (method.isAnnotationPresent(Secret.class)) {
            command.setSecret(true);
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
        ParameterElement firstParam = method.getParameterAt(0);
        if (firstParam == null) {
            throw new IllegalStateException(
                    "Method '" + method.getElement().getName() + "', its first parameter is not a valid source type. The first parameter of a "
                            + "pathway-method must be a valid source type."
            );
        } else if (!isSenderParameter(firstParam)) {
            throw new InvalidSourceException(method, firstParam.getElement().getParameterizedType());
        }

        List<Argument<S>> personalParams = new ArrayList<>();
        List<ParameterElement> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ParameterElement param = parameters.get(i);
            if ((i == 0 && isSenderParameter(param)) || param.isAnnotationPresent(InheritedArg.class)) {
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
                        .arguments(personalParams)
                        .examples(
                                (method.isAnnotationPresent(Execute.class) ?
                                         config.replacePlaceholders(Objects.requireNonNull(method.getAnnotation(Execute.class)).examples()) :
                                         new String[0])
                        )
                        .execute(MethodCommandExecutor.of(imperat, method))
        );


    }

    CommandPathway.Builder<S> processedPathway(MethodElement method, CommandPathway.Builder<S> builder) {
        Permission[] permissionsAnnotations = method.getAnnotationsByType(Permission.class);

        PermissionsData data = null;
        for (Permission ann : permissionsAnnotations) {
            String permLine = config.replacePlaceholders(ann.value());
            if (data == null) {
                data = PermissionsData.fromText(permLine);
            } else {
                data.append(PermissionsData.fromText(permLine));
            }
        }

        if (data != null) {
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
            var ann = method.getAnnotation(Async.class);
            assert ann != null;
            ExecutorServiceProvider serviceProvider = config.getInstanceFactory().createInstance(config, ann.value());
            builder.coordinator(CommandCoordinator.async(serviceProvider.provideExecutorService()));
        }

        return builder;
    }

    CommandPathway.Builder<S> finalizedPathway(MethodElement method, Command<S> owningCommand, CommandPathway.Builder<S> builder) {

        List<Argument<S>> parsedMethodArgs = new ArrayList<>();
        List<ParameterElement> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            ParameterElement parameter = parameters.get(i);
            if (i == 0 && isSenderParameter(parameter)) {
                continue;
            }

            Argument<S> argument = parseMethodParameter(method, parameter);
            if (argument != null) {
                parsedMethodArgs.add(argument);
            }
        }

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

                if (methodChild.isAnnotationPresent(RootCommand.class) || methodChild.isAnnotationPresent(PathwayCommand.class)) {
                    var parsedCmdFromMethod = parseCommandMethod(null, methodChild);
                    commands.add(parsedCmdFromMethod);
                }

            }
        }
        return commands;
    }

    private boolean isSenderParameter(ParameterElement param) {
        Type type = param.getElement().getParameterizedType();
        return imperat.canBeSender(type) || config.hasSourceResolver(type);
    }

    private Command<S> loadPathwayShortcut(
            @NotNull MethodElement method,
            @NotNull List<Argument<S>> parseMethodParameters,
            @NotNull Command<S> originalCommand,
            @NotNull CommandPathway.Builder<S> originalPathway,
            @NotNull Shortcut shortcutAnn
    ) {

        String shortcutValue = config.replacePlaceholders(shortcutAnn.value());

        Command<S> shortcut = originalCommand.getShortcut(shortcutValue);
        if (shortcut == null) {
            shortcut = Command.create(imperat, shortcutValue, method)
                               .setMetaPropertiesFromOtherCommand(originalCommand)
                               .build();
        }

        CommandPathway<S> fabricated = CommandPathway.<S>builder()
                                               .arguments(parseMethodParameters)
                                               .execute(originalPathway.getExecution())
                                               .permission(originalPathway.getPermission())
                                               .description(originalPathway.getDescription())
                                               .build(shortcut);

        shortcut.addPathway(fabricated);
        return shortcut;
    }

    static final class MethodPreProcessor<S extends CommandSource> implements CommandPreProcessor<S> {

        private final Priority priority;
        private final MethodCaller.BoundMethodCaller boundMethodCaller;

        MethodPreProcessor(MethodElement method, Processor annotation) {
            this.priority = Priority.of(annotation.priority());
            try {
                boolean isStaticMethod = Modifier.isStatic(method.getModifiers());
                this.boundMethodCaller = MethodCallerFactory.methodHandles()
                                                 .createFor(method.getElement())
                                                 .bindTo(isStaticMethod ? null : method.getParent().getObjectInstance());
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to create method caller for processor method '" + method.getName() + "'", ex);
            }
        }

        @Override
        public void process(CommandContext<S> context) {
            boundMethodCaller.call(context);
        }

        @Override
        public @NotNull Priority getPriority() {
            return priority;
        }

    }

}
