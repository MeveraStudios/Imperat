package studio.mevera.imperat;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.mevera.imperat.annotations.RequireConfirmation;
import studio.mevera.imperat.annotations.base.element.ParseElement;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.ParameterEnum;
import studio.mevera.imperat.type.HytaleParameterType;
import studio.mevera.imperat.util.TypeUtility;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InternalHytaleCommand extends CommandBase {

    private final HytaleImperat imperat;

    InternalHytaleCommand(HytaleImperat imperat, List<CommandParameter<HytaleSource> > variant) {
        super("");
        this.imperat = imperat;
        for(var p : variant ) {
            withRequiredArg(p.name(), p.description().getValueOrElse(""), loadArgType(p));
        }
    }

    InternalHytaleCommand(HytaleImperat imperat, Command<HytaleSource> imperatCmd) {
        super(imperatCmd.name().toLowerCase(), imperatCmd.description().getValueOrElse(""), requiresConfirmation(imperatCmd));
        this.imperat = imperat;
        setAllowsExtraArguments(true); //TODO IN THE FUTURE , WE MAY NOT ACTUALLY NEED THIS UNLESS THERE'S A GREEDY ARG IN ANY TYPE OF USAGE
        addAliases(imperatCmd.aliases().toArray(String[]::new));

        String cmdPerm;
        if ((cmdPerm = imperatCmd.getMainPermission()) != null) {
            this.requirePermission(cmdPerm);
        }

        //add the required args
        this.deduceVariants(imperatCmd);

        //add the sub commands
        this.hookSubcommands(imperatCmd);
    }
    private static boolean requiresConfirmation(Command<HytaleSource> imperatCmd) {
        if(!imperatCmd.isAnnotated()) {
            return false;
        }
        ParseElement<?> annotatedElement = imperatCmd.getAnnotatedElement();
        assert annotatedElement != null;
        return annotatedElement.isAnnotationPresent(RequireConfirmation.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentType<?> loadArgType(CommandParameter<HytaleSource> parameter) {
        final Type type = parameter.valueType();
        if (parameter.type() instanceof ParameterEnum parameterEnum) {
            var typeStr = type.getTypeName();
            var enumTypeName = typeStr.substring(typeStr.lastIndexOf('.') + 1);
            return ArgTypes.forEnum(enumTypeName, parameterEnum.wrappedType().getRawType());
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
            if (parameter.type() instanceof HytaleParameterType<?> hytaleParameterType) {
                return hytaleParameterType.getHytaleArgType();
            }
        }
        return ArgTypes.STRING;
    }

    //we split each usage INTO variants
    //the main usage will be split into multiple usages
    private void deduceVariants(Command<HytaleSource> imperatCmd) {
        CommandUsage<HytaleSource> mainUsage = imperatCmd.getMainUsage();
        Map<Integer, CommandParameter<HytaleSource>> optionals = new HashMap<>();
        for (int i = 0; i < mainUsage.size(); i++) {
            var parameter = mainUsage.getParameter(i);
            assert parameter != null;
            if(parameter.isOptional() && i != mainUsage.size()-1) {
                optionals.put(i, parameter);
            }else if(parameter.isOptional()) {
                //last optional
                withOptionalArg(parameter.name(), parameter.description().getValueOrElse(""), loadArgType(parameter));
                break;
            }
            withRequiredArg(parameter.name(), parameter.description().getValueOrElse(""), loadArgType(parameter));
        }

        List<List<CommandParameter<HytaleSource>>> parameterVariants = new ArrayList<>();
        for (int i = 0; i < mainUsage.size(); i++) {
            var parameter = mainUsage.getParameter(i);
            assert parameter != null;
            if(optionals.get(i) != null) {
                //add to a new variant , then remove and skip
                List<CommandParameter<HytaleSource>> variant = new ArrayList<>();
                for (int j = 0; j < mainUsage.size(); j++) {
                    var p = mainUsage.getParameter(j);
                    if(j != i) {
                        variant.add(p);
                    }
                }
                parameterVariants.add(variant);
                optionals.remove(i);
            }
        }

        for(var variant : parameterVariants) {
            addUsageVariant(new InternalHytaleCommand(imperat, variant));
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
