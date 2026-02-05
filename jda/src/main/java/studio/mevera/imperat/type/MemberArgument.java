package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.exception.UnknownMemberException;

public final class MemberArgument extends ArgumentType<JdaSource, Member> {

    @Override
    public @NotNull Member resolve(@NotNull ExecutionContext<JdaSource> context, @NotNull Cursor<JdaSource> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        var guild = context.source().origin().getGuild();
        if (guild == null) {
            throw new NoDMSException();
        }

        String memberId = correspondingInput.replaceAll("\\D", "");
        String lookupId = memberId.isEmpty() ? correspondingInput : memberId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new UnknownMemberException(correspondingInput);
        }

        Member member = guild.getMemberById(lookupId);
        if (member == null) {
            throw new UnknownMemberException(correspondingInput);
        }
        return member;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<JdaSource> context, Argument<JdaSource> parameter) {
        String arg = context.arguments().get(rawPosition);
        return arg != null;
    }
}
