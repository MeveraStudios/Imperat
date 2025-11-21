package studio.mevera.imperat.type;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.JdaSource;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.UnknownUserException;

public final class ParameterUser extends BaseParameterType<JdaSource, User> {

    private final JDA jda;

    public ParameterUser(JDA jda) {
        this.jda = jda;
    }

    @Override
    public @NotNull User resolve(@NotNull ExecutionContext<JdaSource> context, @NotNull CommandInputStream<JdaSource> inputStream, @NotNull String input) throws ImperatException {
        String userId = input.replaceAll("\\D", "");
        User user = jda.getUserById(userId.isEmpty() ? input : userId);
        if (user == null) {
            throw new UnknownUserException(input, context);
        }
        return user;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<JdaSource> context, CommandParameter<JdaSource> parameter) {
        String arg = context.arguments().get(rawPosition);
        return arg != null;
    }
}
