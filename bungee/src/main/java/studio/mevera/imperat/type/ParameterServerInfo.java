package studio.mevera.imperat.type;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.UnknownServerException;

public final class ParameterServerInfo extends BaseParameterType<BungeeSource, ServerInfo> {
    
    private final ProxyServer server;
    
    public ParameterServerInfo(ProxyServer server) {
        this.server = server;
    }
    
    public ParameterServerInfo() {
        this(ProxyServer.getInstance());
    }
    
    @Override
    public @NotNull ServerInfo resolve(
            @NotNull ExecutionContext<BungeeSource> context,
            @NotNull CommandInputStream<BungeeSource> inputStream,
            @NotNull String input
    ) throws UnknownServerException {
        ServerInfo serverInfo = server.getServerInfo(input);
        if (serverInfo == null) {
            throw new UnknownServerException(input, context);
        }
        return serverInfo;
    }
}
