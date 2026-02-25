package studio.mevera.imperat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.TabExecutor;
import studio.mevera.imperat.command.Command;


final class InternalBungeeCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor {

    private final BungeeImperat bungeeCommandDispatcher;
    private final Command<BungeeSource> bungeeCommand;

    InternalBungeeCommand(
            BungeeImperat commandDispatcher,
            Command<BungeeSource> bungeeCommand
    ) {
        super(
                bungeeCommand.getName(),
                bungeeCommand.getPrimaryPermission(),
                bungeeCommand.aliases().toArray(new String[0])
        );
        this.bungeeCommandDispatcher = commandDispatcher;
        this.bungeeCommand = bungeeCommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        bungeeCommandDispatcher.execute(
                bungeeCommandDispatcher.wrapSender(sender),
                bungeeCommand.getName(),
                args
        );
    }


    @Override
    public Iterable<String> onTabComplete(
            CommandSender sender,
            String[] args
    ) {
        StringBuilder builder = new StringBuilder(this.bungeeCommand.getName()).append(" ");
        for (String arg : args) {
            builder.append(arg).append(" ");
        }

        if (!builder.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return bungeeCommandDispatcher.autoComplete(
                bungeeCommandDispatcher.wrapSender(sender),
                builder.toString()
        ).join();
    }

}
