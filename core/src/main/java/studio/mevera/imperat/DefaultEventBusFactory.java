package studio.mevera.imperat;

import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.events.Event;
import studio.mevera.imperat.events.EventBus;
import studio.mevera.imperat.events.EventExceptionHandler;
import studio.mevera.imperat.events.EventSubscription;
import studio.mevera.imperat.events.exception.EventException;

import java.util.concurrent.ForkJoinPool;

final class DefaultEventBusFactory {

    private DefaultEventBusFactory() {
    }

    static <S extends CommandSource> EventBus create(Imperat<S> imperat, ImperatConfig<S> config) {
        return EventBus.builder()
                       .exceptionHandler(new EventExceptionHandler() {
                           @Override
                           public <E extends Event> void handle(
                                   E event,
                                   Throwable exception,
                                   EventSubscription<E> subscription
                           ) {
                               var ctxFactory = config.getContextFactory();
                               CommandContext<S> dummy = ctxFactory.createDummyContext(imperat);
                               config.handleExecutionError(
                                       new EventException(event, subscription, exception),
                                       dummy,
                                       EventBus.class,
                                       "handle(event, exception, subscription)"
                               );
                           }
                       })
                       .executorService(ForkJoinPool.commonPool())
                       .build();
    }
}
