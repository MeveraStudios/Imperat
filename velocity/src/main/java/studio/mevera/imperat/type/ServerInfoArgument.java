package studio.mevera.imperat.type;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.VelocityCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.VelocityResponseKey;

public final class ServerInfoArgument extends SimpleArgumentType<VelocityCommandSource, ServerInfo> {

    private final ProxyServer server;

    public ServerInfoArgument(ProxyServer server) {
        this.server = server;
    }

    @Override
    public @NotNull ServerInfo parse(
            @NotNull CommandContext<VelocityCommandSource> context,
            @NonNull Argument<VelocityCommandSource> argument,
            @NotNull String input
    ) throws CommandException {
        return server.getServer(input)
                       .map(RegisteredServer::getServerInfo)
                       .orElseThrow(() -> new ArgumentParseException(VelocityResponseKey.UNKNOWN_SERVER, input));
    }
}
