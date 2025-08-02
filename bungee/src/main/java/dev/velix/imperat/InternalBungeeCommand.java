package dev.velix.imperat;

import dev.velix.imperat.command.Command;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;


final class InternalBungeeCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

    private final BungeeImperat bungeeCommandDispatcher;
    private final Command<BungeeSource> bungeeCommand;

    InternalBungeeCommand(
        BungeeImperat commandDispatcher,
        Command<BungeeSource> bungeeCommand
    ) {
        super(
            bungeeCommand.name(),
            bungeeCommand.permission(),
            bungeeCommand.aliases().toArray(new String[0])
        );
        this.bungeeCommandDispatcher = commandDispatcher;
        this.bungeeCommand = bungeeCommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        bungeeCommandDispatcher.dispatch(
            bungeeCommandDispatcher.wrapSender(sender),
            bungeeCommand.name(),
            args
        );
    }


    @Override
    public Iterable<String> onTabComplete(
        CommandSender sender,
        String[] args
    ) {
        StringBuilder builder = new StringBuilder(this.bungeeCommand.name()).append(" ");
        for(String arg : args) {
            builder.append(arg).append(" ");
        }
        return bungeeCommandDispatcher.autoComplete(
                bungeeCommandDispatcher.wrapSender(sender),
                builder.toString()
        ).join();
    }

}
