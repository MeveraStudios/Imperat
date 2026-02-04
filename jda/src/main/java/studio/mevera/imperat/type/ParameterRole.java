package studio.mevera.imperat.type;

import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.exception.UnknownRoleException;

public final class ParameterRole extends BaseParameterType<JdaSource, Role> {

    @Override
    public @NotNull Role resolve(@NotNull ExecutionContext<JdaSource> context, @NotNull CommandInputStream<JdaSource> inputStream,
            @NotNull String input) throws
            CommandException {
        var guild = context.source().origin().getGuild();
        if (guild == null) {
            throw new NoDMSException();
        }

        String userId = input.replaceAll("\\D", "");
        String lookupId = userId.isEmpty() ? input : userId;
        if (!lookupId.matches("\\d{17,20}")) {
            throw new UnknownRoleException(input);
        }

        final Role role = guild.getRoleById(lookupId);
        if (role == null) {
            throw new UnknownRoleException(input);
        }
        return role;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<JdaSource> context, CommandParameter<JdaSource> parameter) {
        String arg = context.arguments().get(rawPosition);
        return arg != null;
    }
}
