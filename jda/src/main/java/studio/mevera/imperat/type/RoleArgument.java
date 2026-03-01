package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.responses.JdaResponseKey;

public final class RoleArgument extends ArgumentType<JdaSource, Role> {

    @Override
    public @NotNull Role parse(@NotNull ExecutionContext<JdaSource> context, @NotNull Cursor<JdaSource> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        var guild = context.source().origin().getGuild();
        if (guild == null) {
            throw new NoDMSException();
        }

        String userId = correspondingInput.replaceAll("\\D", "");
        String lookupId = userId.isEmpty() ? correspondingInput : userId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_ROLE, correspondingInput);
        }

        final Role role = guild.getRoleById(lookupId);
        if (role == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_ROLE, correspondingInput);
        }
        return role;
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<JdaSource> context, Argument<JdaSource> parameter) {
        String arg = context.arguments().get(rawPosition);
        return arg != null;
    }
}
