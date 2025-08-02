package dev.velix.imperat;

import net.minestom.server.command.builder.Command;

import static dev.velix.imperat.SyntaxDataLoader.*;

final class InternalMinestomCommand extends Command {

    MinestomImperat imperat;
    dev.velix.imperat.command.Command<MinestomSource> imperatCommand;

    InternalMinestomCommand(MinestomImperat imperat, dev.velix.imperat.command.Command<MinestomSource> imperatCommand) {
        super(imperatCommand.name(), imperatCommand.aliases().toArray(new String[0]));
        this.imperat = imperat;
        this.imperatCommand = imperatCommand;

        this.setCondition(
            (sender, commandString) -> imperat.config().getPermissionResolver().hasPermission(
                imperat.wrapSender(sender), imperatCommand.permission()
            )
        );

        this.setDefaultExecutor(
            (commandSender, commandContext) ->
                imperat.executeSafely(imperat.wrapSender(commandSender),
                    commandContext.getCommandName(), commandContext.getInput())
        );

        for (var usage : imperatCommand.usages()) {
            addConditionalSyntax(
                loadCondition(imperat, usage),
                loadExecutor(imperat),
                loadArguments(imperat, imperatCommand, usage)
            );
        }

    }

}
