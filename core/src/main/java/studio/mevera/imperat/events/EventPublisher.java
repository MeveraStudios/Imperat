package studio.mevera.imperat.events;

public interface EventPublisher {

    <E extends Event> void publishEvent(E event);

}
