package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.responses.JdaResponseKey;

/** A server member, given as a mention ({@code <@id>}) or an ID. */
public final class MemberArgument extends SimpleArgumentType<JdaCommandSource, Member> {

    @Override
    public @NotNull Member parse(@NotNull CommandContext<JdaCommandSource> context, @NotNull Argument<JdaCommandSource> argument,
            @NotNull String correspondingInput) throws
            CommandException {
        JdaCommandSource source = context.source();
        if (source.guild() == null) {
            throw new NoDMSException();
        }
        long id = Snowflakes.parse(correspondingInput);
        Member member = id == 0 ? null : source.findMember(id);
        if (member == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_MEMBER, correspondingInput);
        }
        return member;
    }
}
