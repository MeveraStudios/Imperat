package studio.mevera.imperat.type;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocityCommandSource;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.VelocityResponseKey;

public final class ServerInfoArgument extends ArgumentType<VelocityCommandSource, ServerInfo> {

    private final ProxyServer server;

    public ServerInfoArgument(ProxyServer server) {
        this.server = server;
    }

    @Override
    public @NotNull ServerInfo parse(
            @NotNull ExecutionContext<VelocityCommandSource> context,
            @NotNull Cursor<VelocityCommandSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        return server.getServer(correspondingInput)
                       .map(RegisteredServer::getServerInfo)
                       .orElseThrow(() -> new ArgumentParseException(VelocityResponseKey.UNKNOWN_SERVER, correspondingInput));
    }
}
