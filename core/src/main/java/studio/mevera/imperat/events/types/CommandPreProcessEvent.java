package studio.mevera.imperat.events.types;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.events.CancellableEvent;
import studio.mevera.imperat.events.CommandEvent;

public final class CommandPreProcessEvent<S extends Source> extends CommandEvent<S> implements CancellableEvent {

    private final CommandContext<S> context;

    private boolean cancelled = false;

    /**
     * Constructs a new command event.
     *
     * @param command the command associated with this event
     */
    public CommandPreProcessEvent(Command<S> command, CommandContext<S> context) {
        super(command);
        this.context = context;
    }

    public CommandContext<S> getContext() {
        return context;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
