package studio.mevera.imperat;

import net.minestom.server.command.builder.Command;
import static studio.mevera.imperat.SyntaxDataLoader.*;

final class InternalMinestomCommand extends Command {

    MinestomImperat imperat;
    studio.mevera.imperat.command.Command<MinestomSource> imperatCommand;

    InternalMinestomCommand(MinestomImperat imperat, studio.mevera.imperat.command.Command<MinestomSource> imperatCommand) {
        super(imperatCommand.name(), imperatCommand.aliases().toArray(new String[0]));
        this.imperat = imperat;
        this.imperatCommand = imperatCommand;

        this.setCondition(
            (sender, commandString) -> imperat.config().getPermissionChecker().hasPermission(
                imperat.wrapSender(sender), imperat.config().isAutoPermissionAssignMode() ? imperat.config().getPermissionLoader().load(imperatCommand) : imperatCommand.getMainPermission()
            )
        );

        this.setDefaultExecutor(
            (commandSender, commandContext) -> {
                String input = commandContext.getInput();
                String commandName = commandContext.getCommandName();

                String argsOnly;
                if (input.startsWith(commandName)) {
                    argsOnly = input.substring(commandName.length()).trim();
                } else {
                    argsOnly = input;
                }

                imperat.execute(imperat.wrapSender(commandSender),
                        commandName, argsOnly);
            }
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
