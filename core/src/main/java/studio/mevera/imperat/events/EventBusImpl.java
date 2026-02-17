package studio.mevera.imperat.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.PriorityList;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Package-private implementation of {@link EventBus}.
 * Instantiated exclusively via {@link EventBus#createDummy()} and {@link EventBus.Builder#build()}.
 */
final class EventBusImpl implements EventBus {

    private final Map<Class<? extends Event>, PriorityList<EventSubscription<?>>> subscriptions;
    private final Map<UUID, EventSubscription<?>> subscriptionIdMap;
    private final ExecutorService executorService;
    private final boolean hasDefaultExecutor;
    private final EventExceptionHandler exceptionHandler;

    EventBusImpl(
            @Nullable EventExceptionHandler exceptionHandler,
            @Nullable ExecutorService executorService
    ) {
        this.subscriptions = new ConcurrentHashMap<>();
        this.subscriptionIdMap = new ConcurrentHashMap<>();
        this.exceptionHandler = exceptionHandler;

        if (executorService != null) {
            this.executorService = executorService;
            this.hasDefaultExecutor = false;
        } else {
            this.executorService = Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "EventBus-Worker");
                thread.setDaemon(true);
                return thread;
            });
            this.hasDefaultExecutor = true;
        }
    }

    @Override
    public <T extends Event> EventSubscription<T> register(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    ) {
        return register(eventType, handler, Priority.NORMAL, ExecutionStrategy.SYNC);
    }

    @Override
    public <T extends Event> EventSubscription<T> register(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull Priority priority
    ) {
        return register(eventType, handler, priority, ExecutionStrategy.SYNC);
    }

    @Override
    public <T extends Event> EventSubscription<T> register(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull Priority priority,
            @NotNull ExecutionStrategy strategy
    ) {

        UUID id = UUID.randomUUID();
        EventSubscription<T> subscription = new EventSubscriptionImpl<>(id, handler, priority, strategy);

        subscriptions.computeIfAbsent(eventType, k -> new PriorityList<>())
                     .add(priority, subscription);

        subscriptionIdMap.put(id, subscription);
        return subscription;
    }

    @Override
    public boolean unregister(@NotNull UUID subscriptionId) {

        EventSubscription<?> subscription = subscriptionIdMap.remove(subscriptionId);
        if (subscription == null) return false;

        for (PriorityList<EventSubscription<?>> list : subscriptions.values()) {
            if (list.remove(subscription)) return true;
        }

        return false;
    }

    @Override
    public <T extends Event> void post(@NotNull T event) {

        @SuppressWarnings("unchecked")
        Class<T> eventType = (Class<T>) event.getClass();

        PriorityList<EventSubscription<?>> list = subscriptions.get(eventType);
        if (list == null || list.isEmpty()) return;

        for (EventSubscription<?> subscription : list) {
            @SuppressWarnings("unchecked")
            EventSubscription<T> typed = (EventSubscription<T>) subscription;

            if (typed.strategy() == ExecutionStrategy.SYNC) {
                executeSyncHandler(event, typed);
            } else {
                executeAsyncHandler(event, typed);
            }
        }
    }

    @Override
    public int getSubscriptionCount(@NotNull Class<? extends Event> eventType) {
        PriorityList<EventSubscription<?>> list = subscriptions.get(eventType);
        return list == null ? 0 : list.size();
    }

    @Override
    public int getTotalSubscriptionCount() {
        return subscriptionIdMap.size();
    }

    @Override
    public void shutdown() {
        if (hasDefaultExecutor) {
            executorService.shutdown();
        }
    }

    @Override
    public void shutdownAndWait() throws InterruptedException {
        if (hasDefaultExecutor) {
            executorService.shutdown();
            while (!executorService.isTerminated()) {
                Thread.sleep(100);
            }
        }
    }

    @Override
    public boolean isDummyBus() {
        return exceptionHandler == null && hasDefaultExecutor;
    }

    // -------------------------------------------------------------------------
    // Internal execution helpers
    // -------------------------------------------------------------------------

    private <T extends Event> void executeSyncHandler(
            T event,
            EventSubscription<T> subscription
    ) {
        try {
            subscription.handler().accept(event);
        } catch (Throwable throwable) {
            handleException(event, throwable, subscription);
        }
    }

    private <T extends Event> void executeAsyncHandler(
            T event,
            EventSubscription<T> subscription
    ) {
        executorService.submit(() -> {
            try {
                subscription.handler().accept(event);
            } catch (Throwable throwable) {
                handleException(event, throwable, subscription);
            }
        });
    }

    private <T extends Event> void handleException(T event, Throwable throwable, EventSubscription<T> subscription) {
        if (exceptionHandler == null) return;
        try {
            exceptionHandler.handle(event, throwable, subscription);
        } catch (Throwable exHandlerThrowable) {
            System.err.println("[EventBus] Exception handler itself threw an exception:");
            exHandlerThrowable.printStackTrace(System.err);
            System.err.println("[EventBus] Original exception:");
            throwable.printStackTrace(System.err);
        }
    }
}