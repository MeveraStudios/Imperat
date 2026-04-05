package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.JdaArgumentParseException;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.responses.JdaResponseKey;

public final class MemberArgument extends ArgumentType<JdaCommandSource, Member> {

    @Override
    public @NotNull Member parse(@NotNull CommandContext<JdaCommandSource> context, @NotNull Argument<JdaCommandSource> argument,
            @NotNull String correspondingInput) throws
            CommandException {
        var guild = context.source().origin().getGuild();
        if (guild == null) {
            throw new NoDMSException();
        }

        String memberId = correspondingInput.replaceAll("\\D", "");
        String lookupId = memberId.isEmpty() ? correspondingInput : memberId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_MEMBER, correspondingInput);
        }

        Member member = guild.getMemberById(lookupId);
        if (member == null) {
            throw new JdaArgumentParseException(JdaResponseKey.UNKNOWN_MEMBER, correspondingInput);
        }
        return member;
    }
}
