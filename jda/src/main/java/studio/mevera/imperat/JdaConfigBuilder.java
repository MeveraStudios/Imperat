package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.NoDMSException;
import studio.mevera.imperat.exception.UnsupportedInvocationException;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.JdaResponseKey;
import studio.mevera.imperat.type.MemberArgument;
import studio.mevera.imperat.type.RoleArgument;
import studio.mevera.imperat.type.UserArgument;
import studio.mevera.imperat.util.TypeWrap;

public class JdaConfigBuilder<S extends JdaCommandSource>
        extends ConfigBuilder<S, JdaImperat<S>, JdaConfigBuilder<S>> {

    private final JDA jda;
    private JdaTextCommands textCommands;

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
        config.registerContextArgumentProvider(SlashCommandInteractionEvent.class, (ctx, param) -> {
            SlashCommandInteractionEvent event = ctx.source().slashEvent();
            if (event == null) {
                throw new UnsupportedInvocationException("This command only works as a slash command.");
            }
            return event;
        });
        config.registerContextArgumentProvider(MessageReceivedEvent.class, (ctx, param) -> {
            MessageReceivedEvent event = ctx.source().messageEvent();
            if (event == null) {
                throw new UnsupportedInvocationException("This command only works as a text command.");
            }
            return event;
        });
        config.registerContextArgumentProvider(JDA.class, (ctx, param) -> jda);
        config.registerContextArgumentProvider(Guild.class, (ctx, param) -> ctx.source().guild());
        config.registerContextArgumentProvider(MessageChannel.class, (ctx, param) -> ctx.source().channel());
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
        config.setErrorHandler(UnsupportedInvocationException.class, (ex, ctx) ->
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

    /**
     * Lets the commands also run from text messages ({@code !ban @user 1d}); off unless set.
     *
     * @see JdaTextCommands
     */
    public JdaConfigBuilder<S> textCommands(@NotNull JdaTextCommands textCommands) {
        this.textCommands = java.util.Objects.requireNonNull(textCommands, "textCommands");
        return this;
    }

    @Override
    public @NotNull JdaImperat<S> build() {
        materializeDeferredDefaults();
        return new JdaImperat<>(jda, config, textCommands);
    }
}
