package studio.mevera.imperat.type;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.UnknownServerException;

public final class ParameterServerInfo extends BaseParameterType<VelocitySource, ServerInfo> {

    private final ProxyServer server;

    public ParameterServerInfo(ProxyServer server) {
        this.server = server;
    }

    @Override
    public @NotNull ServerInfo resolve(
            @NotNull ExecutionContext<VelocitySource> context,
            @NotNull CommandInputStream<VelocitySource> inputStream,
            @NotNull String input
    ) throws UnknownServerException {
        return server.getServer(input)
                       .map(RegisteredServer::getServerInfo)
                       .orElseThrow(() -> new UnknownServerException(input));
    }
}
