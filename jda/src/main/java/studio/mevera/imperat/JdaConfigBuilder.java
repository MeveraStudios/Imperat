package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.responses.JdaResponseKey;
import studio.mevera.imperat.type.MemberArgument;
import studio.mevera.imperat.type.RoleArgument;
import studio.mevera.imperat.type.UserArgument;
import studio.mevera.imperat.util.TypeWrap;

/**
 * Configuration builder for {@link JdaImperat}.
 */
public final class JdaConfigBuilder extends ConfigBuilder<JdaSource, JdaImperat, JdaConfigBuilder> {

    private final JDA jda;

    JdaConfigBuilder(@NotNull JDA jda) {
        this.jda = jda;
        registerContextResolvers();
        registerSourceResolvers();
        registerArgumentTypes();
        registerThrowableResolvers();
        registerDefaultResponses();
        config.registerDependencyResolver(JDA.class, () -> jda);
    }

    private void registerContextResolvers() {
        config.registerContextArgumentProvider(new TypeWrap<ExecutionContext<JdaSource>>() {
        }.getType(), (ctx, param) -> ctx);
        config.registerContextArgumentProvider(new TypeWrap<CommandHelp<JdaSource>>() {
        }.getType(), (ctx, param) -> CommandHelp.create(ctx));
        config.registerContextArgumentProvider(SlashCommandInteractionEvent.class, (ctx, param) -> ctx.source().origin());
        config.registerContextArgumentProvider(JDA.class, (ctx, param) -> jda);
        config.registerContextArgumentProvider(Guild.class, (ctx, param) -> ctx.source().origin().getGuild());
    }

    private void registerSourceResolvers() {
        config.registerSourceProvider(Member.class, (source, ctx) -> {
            Member member = source.member();
            if (member == null) {
                throw new NoDMSException();
            }
            return member;
        });
        config.registerSourceProvider(User.class, (source, ctx) -> source.user());
    }

    private void registerArgumentTypes() {
        config.registerArgType(Member.class, new RoleArgument());
        config.registerArgType(User.class, new UserArgument(jda));
        config.registerArgType(Member.class, new MemberArgument());
    }

    private void registerThrowableResolvers() {
        //        config.setThrowableResolver(UnknownUserException.class, (ex, ctx) ->
        //                                                                        ctx.source().error("User '" + ex.getIdentifier() + "' could not
        //                                                                        be found")
        //        );
        //
        //        config.setThrowableResolver(UnknownMemberException.class, (ex, ctx) ->
        //                                                                          ctx.source().error("Member '" + ex.getIdentifier() + "' could
        //                                                                          not be found")
        //        );

        config.setThrowableResolver(NoDMSException.class, (ex, ctx) ->
                                                                  ctx.source().error(ex.getMessage())
        );
    }

    private void registerDefaultResponses() {
        this.visit(ImperatConfig::getResponseRegistry, registry -> {
            registry.registerResponse(JdaResponseKey.UNKNOWN_USER, () -> "Unknown user: %input%", "input");
            registry.registerResponse(JdaResponseKey.UNKNOWN_MEMBER, () -> "Unknown member: %input%", "input");
            registry.registerResponse(JdaResponseKey.UNKNOWN_ROLE, () -> "Unknown role: %input%", "input");
        });
    }

    @Override
    public @NotNull JdaImperat build() {
        return new JdaImperat(jda, config);
    }
}
