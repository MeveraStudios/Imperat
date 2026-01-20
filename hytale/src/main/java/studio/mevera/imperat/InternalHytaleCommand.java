package studio.mevera.imperat;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.ParameterEnum;
import studio.mevera.imperat.type.HytaleParameterType;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;

final class InternalHytaleCommand extends CommandBase {

    private final HytaleImperat imperat;

    InternalHytaleCommand(HytaleImperat imperat, Command<HytaleSource> imperatCmd) {
        super(imperatCmd.name().toLowerCase(), imperatCmd.description().toString());
        this.imperat = imperat;
        setAllowsExtraArguments(true); //TODO IN THE FUTURE , WE MAY NOT ACTUALLY NEED THIS UNLESS THERE'S A GREEDY ARG IN ANY TYPE OF USAGE
        addAliases(imperatCmd.aliases().toArray(String[]::new));

        String cmdPerm;
        if((cmdPerm = imperatCmd.getMainPermission()) != null) {
            this.requirePermission(cmdPerm);
        }

        //add the required args
        this.hookRequiredArgs(imperatCmd);

        //add the sub commands
        this.hookSubcommands(imperatCmd);
    }

    private void hookRequiredArgs(Command<HytaleSource> imperatCmd) {
        CommandUsage<HytaleSource> mainUsage = imperatCmd.getMainUsage();
        for(CommandParameter<HytaleSource> parameter : mainUsage) {
            withRequiredArg(parameter.name(), parameter.description().toString(), loadArgType(parameter));
        }
    }

    private static ArgumentType<?> loadArgType(CommandParameter<HytaleSource> parameter) {

        Type type = parameter.valueType();
        if(parameter.type() instanceof ParameterEnum parameterEnum) {
            var typeStr = type.getTypeName();
            var enumTypeName = typeStr.substring(typeStr.lastIndexOf('.')+1);
            return ArgTypes.forEnum(enumTypeName, parameterEnum.wrappedType().getRawType());
        }

        if(parameter.isNumeric()) {
            if(TypeUtility.matches(type, Double.class)) {
                return ArgTypes.DOUBLE;
            }

            else if (TypeUtility.matches(type, Float.class)) {
                return ArgTypes.FLOAT;
            }
            else {
                return ArgTypes.INTEGER;
            }
        }else {

            //Genius solution
            if(parameter.type() instanceof HytaleParameterType<?> hytaleParameterType) {
                return hytaleParameterType.getHytaleArgType();
            }

        }

        return ArgTypes.STRING;
    }

    private void hookSubcommands(Command<HytaleSource> imperatCmd) {

        for (var sub : imperatCmd.getSubCommands()) {
            this.addSubCommand(new InternalHytaleCommand(imperat,  sub));
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
