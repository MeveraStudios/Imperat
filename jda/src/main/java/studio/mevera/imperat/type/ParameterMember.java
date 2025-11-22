package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.NoDMSExceptionn;
import studio.mevera.imperat.exception.UnknownMemberException;

public final class ParameterMember extends BaseParameterType<JdaSource, Member> {

    @Override
    public @NotNull Member resolve(@NotNull ExecutionContext<JdaSource> context, @NotNull CommandInputStream<JdaSource> inputStream, @NotNull String input) throws ImperatException {
        if (context.source().isConsole()) {
            throw new NoDMSExceptionn(context);
        }
        var guild = context.source().origin().getGuild();
        String memberId = input.replaceAll("\\D", "");
        Member member = guild != null ? guild.getMemberById(memberId.isEmpty() ? input : memberId) : null;
        if (member == null) {
            throw new UnknownMemberException(input, context);
        }
        return member;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<JdaSource> context, CommandParameter<JdaSource> parameter) {
        String arg = context.arguments().get(rawPosition);
        return arg != null;
    }
}
