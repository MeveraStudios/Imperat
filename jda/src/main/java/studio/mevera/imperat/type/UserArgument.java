package studio.mevera.imperat.type;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.responses.JdaResponseKey;

/**
 * A Discord user, given as a mention ({@code <@id>}) or an ID. Users the command already carries
 * (a slash option, a mention in the message) are found even when they are not cached.
 */
public final class UserArgument extends SimpleArgumentType<JdaCommandSource, User> {

    private final JDA jda;

    public UserArgument(JDA jda) {
        this.jda = jda;
    }

    @Override
    public @NotNull User parse(@NotNull CommandContext<JdaCommandSource> context, @NotNull Argument<JdaCommandSource> argument, @NotNull String input)
            throws CommandException {
        long id = Snowflakes.parse(input);
        if (id == 0) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_USER, input);
        }
        JdaCommandSource source = context.source();
        User user = source.origin() == null ? jda.getUserById(id) : source.findUser(id);
        if (user == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_USER, input);
        }
        return user;
    }
}
