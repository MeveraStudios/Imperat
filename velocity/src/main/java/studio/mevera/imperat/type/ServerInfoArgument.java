package studio.mevera.imperat.type;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.VelocityResponseKey;

public final class ServerInfoArgument extends ArgumentType<VelocitySource, ServerInfo> {

    private final ProxyServer server;

    public ServerInfoArgument(ProxyServer server) {
        this.server = server;
    }

    @Override
    public @NotNull ServerInfo parse(
            @NotNull ExecutionContext<VelocitySource> context,
            @NotNull Cursor<VelocitySource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        return server.getServer(correspondingInput)
                       .map(RegisteredServer::getServerInfo)
                       .orElseThrow(() -> new CommandException(VelocityResponseKey.UNKNOWN_SERVER)
                                                  .withPlaceholder("input", correspondingInput));
    }
}
