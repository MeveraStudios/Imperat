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

/** A server role, given as a mention ({@code <@&id>}) or an ID. */
public final class RoleArgument extends SimpleArgumentType<JdaCommandSource, Role> {

    @Override
    public @NotNull Role parse(@NotNull CommandContext<JdaCommandSource> context, @NotNull Argument<JdaCommandSource> argument, @NotNull String input)
            throws CommandException {
        JdaCommandSource source = context.source();
        if (source.guild() == null) {
            throw new NoDMSException();
        }
        long id = Snowflakes.parse(input);
        Role role = id == 0 ? null : source.findRole(id);
        if (role == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_ROLE, input);
        }
        return role;
    }

}
