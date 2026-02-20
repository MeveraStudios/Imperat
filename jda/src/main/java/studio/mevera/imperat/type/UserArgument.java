package studio.mevera.imperat.type;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.responses.JdaResponseKey;

public final class UserArgument extends ArgumentType<JdaSource, User> {

    private final JDA jda;

    public UserArgument(JDA jda) {
        this.jda = jda;
    }

    @Override
    public @NotNull User parse(@NotNull ExecutionContext<JdaSource> context, @NotNull Cursor<JdaSource> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        String userId = correspondingInput.replaceAll("\\D", "");
        String lookupId = userId.isEmpty() ? correspondingInput : userId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_USER, correspondingInput);
        }

        User user = jda.getUserById(lookupId);
        if (user == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_USER, correspondingInput);
        }
        return user;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<JdaSource> context, Argument<JdaSource> parameter) {
        String arg = context.arguments().get(rawPosition);
        return arg != null;
    }
}
