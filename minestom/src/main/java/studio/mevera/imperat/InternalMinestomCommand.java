package studio.mevera.imperat;

import static studio.mevera.imperat.SyntaxDataLoader.loadArguments;
import static studio.mevera.imperat.SyntaxDataLoader.loadCondition;
import static studio.mevera.imperat.SyntaxDataLoader.loadExecutor;

import net.minestom.server.command.builder.Command;

final class InternalMinestomCommand extends Command {

    MinestomImperat imperat;
    studio.mevera.imperat.command.Command<MinestomSource> imperatCommand;

    InternalMinestomCommand(MinestomImperat imperat, studio.mevera.imperat.command.Command<MinestomSource> imperatCommand) {
        super(imperatCommand.getName(), imperatCommand.aliases().toArray(new String[0]));
        this.imperat = imperat;
        this.imperatCommand = imperatCommand;

        this.setCondition(
                (sender, _) -> imperat.config().getPermissionChecker().hasPermission(
                        imperat.wrapSender(sender),
                        imperatCommand
                )
        );

        this.setDefaultExecutor(loadExecutor(imperat));

        // Register this command's own dedicated syntaxes (local pathways)
        for (var usage : imperatCommand.getDedicatedPathways()) {
            if (usage.isDefault()) {
                continue; // default is handled by setDefaultExecutor
            }
            addConditionalSyntax(
                    loadCondition(imperat, usage),
                    loadExecutor(imperat),
                    loadArguments(imperat, usage)
            );
        }

        // Recursively register subcommands as nested Minestom commands
        for (var sub : imperatCommand.getSubCommands()) {
            addSubcommand(new InternalMinestomCommand(imperat, sub));
        }
    }

}
