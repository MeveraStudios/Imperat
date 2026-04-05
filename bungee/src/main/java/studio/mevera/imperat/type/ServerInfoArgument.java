package studio.mevera.imperat.type;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.BungeeCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.BungeeResponseKey;

public final class ServerInfoArgument extends ArgumentType<BungeeCommandSource, ServerInfo> {

    private final ProxyServer server;

    public ServerInfoArgument(ProxyServer server) {
        this.server = server;
    }

    public ServerInfoArgument() {
        this(ProxyServer.getInstance());
    }

    @Override
    public ServerInfo parse(@NotNull CommandContext<BungeeCommandSource> context, @NonNull Argument<BungeeCommandSource> argument,
            @NotNull String input) throws CommandException {
        ServerInfo serverInfo = server.getServerInfo(input);
        if (serverInfo == null) {
            throw new ArgumentParseException(BungeeResponseKey.UNKNOWN_SERVER, input);
        }
        return serverInfo;
    }

}
