package studio.mevera.imperat;

import static studio.mevera.imperat.SyntaxDataLoader.loadArguments;
import static studio.mevera.imperat.SyntaxDataLoader.loadCondition;
import static studio.mevera.imperat.SyntaxDataLoader.loadExecutor;

import net.minestom.server.command.builder.Command;

final class InternalMinestomCommand extends Command {

    MinestomImperat imperat;
    studio.mevera.imperat.command.Command<MinestomSource> imperatCommand;

    InternalMinestomCommand(MinestomImperat imperat, studio.mevera.imperat.command.Command<MinestomSource> imperatCommand) {
        super(imperatCommand.name(), imperatCommand.aliases().toArray(new String[0]));
        this.imperat = imperat;
        this.imperatCommand = imperatCommand;

        this.setCondition(
                (sender, _) -> imperat.config().getPermissionChecker().hasPermission(
                        imperat.wrapSender(sender),
                        imperatCommand
                )
        );

        this.setDefaultExecutor(loadExecutor(imperat));

        for (var usage : imperatCommand.getAllPossiblePathways()) {
            addConditionalSyntax(
                    loadCondition(imperat, usage),
                    loadExecutor(imperat),
                    loadArguments(imperat, usage)
            );
        }

    }

}
