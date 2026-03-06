package studio.mevera.imperat.events.types;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.events.CancellableEvent;
import studio.mevera.imperat.events.CommandEvent;

public class CommandPostProcessEvent<S extends Source> extends CommandEvent<S> implements CancellableEvent {

    private final ExecutionContext<S> context;
    private boolean cancelled = false;

    /**
     * Constructs a new command event.
     *
     * @param command the command associated with this event
     */
    public CommandPostProcessEvent(Command<S> command, ExecutionContext<S> context) {
        super(command);
        this.context = context;
    }

    public ExecutionContext<S> getContext() {
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
