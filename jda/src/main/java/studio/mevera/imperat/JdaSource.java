package studio.mevera.imperat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;

import java.util.UUID;

/**
 * Discord-specific command source wrapper for Imperat when using JDA.
 * Handles sending replies back to the invoking user and exposes
 * the underlying {@link SlashCommandInteractionEvent} for advanced usage.
 */
public final class JdaSource implements Source {

    private final SlashCommandInteractionEvent event;
    private final InteractionHook hook;

    JdaSource(SlashCommandInteractionEvent event) {
        this.event = event;
        this.hook = event.getHook();
    }

    @Override
    public String name() {
        return event.getUser().getName();
    }

    @Override
    public SlashCommandInteractionEvent origin() {
        return event;
    }

    private void respond(String message, boolean ephemeral) {
        if (event.isAcknowledged()) {
            hook.sendMessage(message).setEphemeral(ephemeral).queue();
        } else {
            event.reply(message).setEphemeral(ephemeral).queue();
        }
    }

    @Override
    public void reply(String message) {
        respond(message, false);
    }

    @Override
    public void warn(String message) {
        respond("⚠️ " + message, true);
    }

    @Override
    public void error(String message) {
        respond("❌ " + message, true);
    }

    @Override
    public boolean isConsole() {
        return event.getGuild() == null;
    }

    @Override
    public UUID uuid() {
        return UUID.nameUUIDFromBytes(event.getUser().getId().getBytes());
    }

    public User user() {
        return event.getUser();
    }

    public @Nullable Member member() {
        return event.getMember();
    }

    public @NotNull JDA jda() {
        return event.getJDA();
    }
}
