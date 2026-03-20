package studio.mevera.imperat.type;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaCommandSource;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.responses.JdaResponseKey;

public final class UserArgument extends ArgumentType<JdaCommandSource, User> {

    private final JDA jda;

    public UserArgument(JDA jda) {
        this.jda = jda;
    }

    @Override
    public @NotNull User parse(@NotNull CommandContext<JdaCommandSource> context, @NotNull String input) throws CommandException {
        String userId = input.replaceAll("\\D", "");
        String lookupId = userId.isEmpty() ? input : userId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_USER, input);
        }
        User user = jda.getUserById(lookupId);
        if (user == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_USER, input);
        }
        return user;
    }
}
