package studio.mevera.imperat;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.util.StringUtils;

import java.util.List;

final class InternalVelocityCommand implements SimpleCommand {

    private final VelocityImperat imperat;
    private final Command<VelocitySource> command;

    private final CommandMeta meta;

    InternalVelocityCommand(VelocityImperat imperat, Command<VelocitySource> command, CommandManager commandManager) {
        this.imperat = imperat;
        this.command = command;
        this.meta = createMeta(commandManager);
    }

    private CommandMeta createMeta(CommandManager commandManager) {
        var builder = commandManager.metaBuilder(command.name())
            .plugin(imperat.plugin);
        if (command.aliases().isEmpty()) {
            return builder.build();
        }
        return builder.aliases(command.aliases().toArray(new String[0]))
            .build();
    }

    public CommandMeta getMeta() {
        return meta;
    }

    @Override
    public void execute(Invocation invocation) {
        String label = invocation.alias();
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        imperat.executeSafely(imperat.wrapSender(source), StringUtils.stripNamespace(label), args);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        StringBuilder builder = new StringBuilder(invocation.alias()).append(" ");
        for(String arg : invocation.arguments()) {
            builder.append(arg).append(" ");
        }
        return imperat.autoComplete(
                    imperat.wrapSender(invocation.source()), builder.toString()
                ).join();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return imperat.config().getPermissionChecker().hasPermission(
            imperat.wrapSender(invocation.source()),
            imperat.config().isAutoPermissionAssignMode()
                    ? imperat.config().getPermissionLoader().load(command)
                    : command.getMainPermission()
        );
    }

}
