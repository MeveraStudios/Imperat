package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.responses.JdaResponseKey;

public final class RoleArgument extends SimpleArgumentType<JdaCommandSource, Role> {

    @Override
    public @NotNull Role parse(@NotNull CommandContext<JdaCommandSource> context, @NotNull Argument<JdaCommandSource> argument, @NotNull String input)
            throws CommandException {
        var guild = context.source().origin().getGuild();
        if (guild == null) {
            throw new NoDMSException();
        }
        String userId = input.replaceAll("\\D", "");
        String lookupId = userId.isEmpty() ? input : userId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_ROLE, input);
        }
        final Role role = guild.getRoleById(lookupId);
        if (role == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_ROLE, input);
        }
        return role;
    }

}
