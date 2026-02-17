package studio.mevera.imperat.events.exception;

import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventSubscription;
import studio.mevera.imperat.exception.CommandException;

public final class EventException extends CommandException {

    public <E extends Event> EventException(E event, EventSubscription<E> subscription, Throwable cause) {
        super("On event '" + event.getClass().getSimpleName() + "' , subscription-id='" + subscription.id().toString() + "', subscription-strategy"
                      + "='" + subscription.strategy().name() + "', failed due to:\n" + cause.getMessage(),
                cause);
    }
}
