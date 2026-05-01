package studio.mevera.imperat;

import static studio.mevera.imperat.SyntaxDataLoader.loadArguments;
import static studio.mevera.imperat.SyntaxDataLoader.loadCondition;
import static studio.mevera.imperat.SyntaxDataLoader.loadExecutor;

import net.minestom.server.command.builder.Command;

final class InternalMinestomCommand<S extends MinestomCommandSource> extends Command {

    MinestomImperat<S> imperat;
    studio.mevera.imperat.command.Command<S> imperatCommand;

    InternalMinestomCommand(MinestomImperat<S> imperat, studio.mevera.imperat.command.Command<S> imperatCommand) {
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

        for (var usage : imperatCommand.getDedicatedPathways()) {
            if (usage.isDefault()) {
                continue;
            }
            addConditionalSyntax(
                    loadCondition(imperat, usage),
                    loadExecutor(imperat),
                    loadArguments(imperat, usage)
            );
        }

        for (var sub : imperatCommand.getSubCommands()) {
            addSubcommand(new InternalMinestomCommand<>(imperat, sub));
        }
    }

}
