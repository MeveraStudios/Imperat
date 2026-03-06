package studio.mevera.imperat.events;

@FunctionalInterface
public interface EventListenerConsumer<T extends Event> {

    void accept(T event) throws Exception;
}
