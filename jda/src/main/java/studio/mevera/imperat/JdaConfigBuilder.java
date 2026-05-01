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
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.JdaResponseKey;
import studio.mevera.imperat.type.MemberArgument;
import studio.mevera.imperat.type.RoleArgument;
import studio.mevera.imperat.type.UserArgument;
import studio.mevera.imperat.util.TypeWrap;

public class JdaConfigBuilder<S extends JdaCommandSource>
        extends ConfigBuilder<S, JdaImperat<S>, JdaConfigBuilder<S>> {

    private final JDA jda;

    JdaConfigBuilder(@NotNull JDA jda, Class<S> sourceClass, CommandSourceMapper<JdaCommandSource, S> mapper) {
        super(sourceClass);
        this.jda = jda;
        config.setSourceMapper(mapper);
        registerContextResolvers();
        registerSourceResolvers();
        registerArgumentTypes();
        registerThrowableResolvers();
        registerDefaultResponses();
        config.registerDependencyResolver(JDA.class, () -> jda);
    }

    private void registerContextResolvers() {
        deferredDefaults.add(cfg -> {
            cfg.registerContextArgumentProvider(
                    TypeWrap.ofParameterized(ExecutionContext.class, sourceClass).getType(),
                    (ctx, param) -> ctx
            );
            cfg.registerContextArgumentProvider(
                    TypeWrap.ofParameterized(CommandHelp.class, sourceClass).getType(),
                    (ctx, param) -> CommandHelp.create(ctx)
            );
        });
        config.registerContextArgumentProvider(SlashCommandInteractionEvent.class, (ctx, param) -> ctx.source().origin());
        config.registerContextArgumentProvider(JDA.class, (ctx, param) -> jda);
        config.registerContextArgumentProvider(Guild.class, (ctx, param) -> ctx.source().origin().getGuild());
    }

    private void registerSourceResolvers() {
        // v4: SourceProviderRegistry deleted. Member / User come from
        // ContextArgumentProvider so DM-only / member-only gating still
        // works.
        config.registerContextArgumentProvider(Member.class, (ctx, p) -> {
            Member member = ctx.source().member();
            if (member == null) {
                throw new NoDMSException();
            }
            return member;
        });
        config.registerContextArgumentProvider(User.class, (ctx, p) -> ctx.source().user());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerArgumentTypes() {
        config.registerArgType(Member.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new RoleArgument());
        config.registerArgType(User.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new UserArgument(jda));
        config.registerArgType(Member.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new MemberArgument());
    }

    private void registerThrowableResolvers() {
        config.setErrorHandler(NoDMSException.class, (ex, ctx) ->
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
    public @NotNull JdaImperat<S> build() {
        materializeDeferredDefaults();
        return new JdaImperat<>(jda, config);
    }
}
