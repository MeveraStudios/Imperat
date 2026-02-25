package studio.mevera.imperat;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.mevera.imperat.annotations.RequireConfirmation;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.EnumArgument;
import studio.mevera.imperat.type.HytaleArgumentType;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InternalHytaleCommand extends CommandBase {

    private final HytaleImperat imperat;

    InternalHytaleCommand(HytaleImperat imperat, List<Argument<HytaleSource>> variant) {
        super("");
        this.imperat = imperat;
        for (var p : variant) {
            withRequiredArg(p.getName(), p.getDescription().getValueOrElse(""), loadArgType(p));
        }
    }

    InternalHytaleCommand(HytaleImperat imperat, Command<HytaleSource> imperatCmd) {
        super(imperatCmd.getName().toLowerCase(), imperatCmd.getDescription().getValueOrElse(""), requiresConfirmation(imperatCmd));
        this.imperat = imperat;
        setAllowsExtraArguments(true); //TODO IN THE FUTURE , WE MAY NOT ACTUALLY NEED THIS UNLESS THERE'S A GREEDY ARG IN ANY TYPE OF USAGE
        addAliases(imperatCmd.aliases().toArray(String[]::new));

        String cmdPerm;
        if ((cmdPerm = imperatCmd.getPrimaryPermission()) != null) {
            this.requirePermission(cmdPerm);
        }

        //add the required args
        this.deduceVariants(imperatCmd);

        //add the sub commands
        this.hookSubcommands(imperatCmd);
    }

    /**
     * Determines whether the given command requires user confirmation before execution.
     * <p>
     * A command is considered to require confirmation if its underlying annotated element
     * is annotated with {@link RequireConfirmation}. If the command is not annotated at
     * all (i.e., {@code imperatCmd.isAnnotated()} returns {@code false}), this method
     * will return {@code false}, meaning no confirmation is required.
     *
     * @param imperatCmd the command definition whose confirmation requirement should be checked
     * @return {@code true} if the command's annotated element is annotated with
     *         {@link RequireConfirmation}, {@code false} otherwise
     */
    private static boolean requiresConfirmation(Command<HytaleSource> imperatCmd) {
        if (!imperatCmd.isAnnotated()) {
            return false;
        }
        ParseElement<?> annotatedElement = imperatCmd.getAnnotatedElement();
        assert annotatedElement != null;
        return annotatedElement.isAnnotationPresent(RequireConfirmation.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentType<?> loadArgType(Argument<HytaleSource> parameter) {
        final Type type = parameter.valueType();
        if (parameter.type() instanceof EnumArgument EnumArgument) {
            var typeStr = type.getTypeName();
            var enumTypeName = typeStr.substring(typeStr.lastIndexOf('.') + 1);
            return ArgTypes.forEnum(enumTypeName, EnumArgument.wrappedType().getRawType());
        }

        if (parameter.isNumeric()) {
            if (TypeUtility.matches(type, Double.class)) {
                return ArgTypes.DOUBLE;
            } else if (TypeUtility.matches(type, Float.class)) {
                return ArgTypes.FLOAT;
            } else {
                return ArgTypes.INTEGER;
            }
        } else {
            if (parameter.type() instanceof HytaleArgumentType<?> hytaleArgumentType) {
                return hytaleArgumentType.getHytaleArgType();
            }
        }
        return ArgTypes.STRING;
    }

    //we split each usage INTO variants
    //the main usage will be split into multiple usages
    private void deduceVariants(Command<HytaleSource> imperatCmd) {
        for (CommandPathway<HytaleSource> mainUsage : imperatCmd.getDedicatedPathways()) {
            Map<Integer, Argument<HytaleSource>> optionals = new HashMap<>();
            for (int i = 0; i < mainUsage.size(); i++) {
                var parameter = mainUsage.getArgumentAt(i);
                assert parameter != null;
                if (parameter.isOptional() && i != mainUsage.size() - 1) {
                    optionals.put(i, parameter);
                } else if (parameter.isOptional()) {
                    //last optional
                    withOptionalArg(parameter.getName(), parameter.getDescription().getValueOrElse(""), loadArgType(parameter));
                    break;
                }
                withRequiredArg(parameter.getName(), parameter.getDescription().getValueOrElse(""), loadArgType(parameter));
            }


            List<List<Argument<HytaleSource>>> parameterVariants = new ArrayList<>();
            for (int i = 0; i < mainUsage.size(); i++) {
                var parameter = mainUsage.getArgumentAt(i);
                assert parameter != null;
                if (optionals.get(i) != null) {
                    //add to a new variant , then remove and skip
                    List<Argument<HytaleSource>> variant = new ArrayList<>();
                    for (int j = 0; j < mainUsage.size(); j++) {
                        var p = mainUsage.getArgumentAt(j);
                        if (j != i) {
                            variant.add(p);
                        }
                    }
                    parameterVariants.add(variant);
                    optionals.remove(i);
                }
            }


            for (var variant : parameterVariants) {
                addUsageVariant(new InternalHytaleCommand(imperat, variant));
            }
        }
    }


    private void hookSubcommands(Command<HytaleSource> imperatCmd) {
        for (var sub : imperatCmd.getSubCommands()) {
            this.addSubCommand(new InternalHytaleCommand(imperat, sub));
        }
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        imperat.execute(
                imperat.wrapSender(commandContext.sender()),
                commandContext.getInputString()
        );
    }
}
