package studio.mevera.imperat;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.mevera.imperat.command.Command;


final class InternalHytaleCommand extends CommandBase {

    private final HytaleImperat imperat;
    InternalHytaleCommand( HytaleImperat imperat, Command<HytaleSource> imperatCmd) {
        super(imperatCmd.name(), imperatCmd.description().toString());
        this.imperat = imperat;
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        imperat.execute(
                imperat.wrapSender(commandContext.sender()),
                commandContext.getInputString()
        );
    }
}
