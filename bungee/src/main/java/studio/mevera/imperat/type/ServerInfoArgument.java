package studio.mevera.imperat.type;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.UnknownServerException;

public final class ServerInfoArgument extends ArgumentType<BungeeSource, ServerInfo> {

    private final ProxyServer server;

    public ServerInfoArgument(ProxyServer server) {
        this.server = server;
    }

    public ServerInfoArgument() {
        this(ProxyServer.getInstance());
    }

    @Override
    public @NotNull ServerInfo resolve(
            @NotNull ExecutionContext<BungeeSource> context,
            @NotNull Cursor<BungeeSource> cursor,
            @NotNull String correspondingInput
    ) throws UnknownServerException {
        ServerInfo serverInfo = server.getServerInfo(correspondingInput);
        if (serverInfo == null) {
            throw new UnknownServerException(correspondingInput);
        }
        return serverInfo;
    }
}
