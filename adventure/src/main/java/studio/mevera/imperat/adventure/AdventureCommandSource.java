package studio.mevera.imperat.adventure;

import net.kyori.adventure.text.ComponentLike;
import studio.mevera.imperat.context.CommandSource;

/**
 * The {@code AdventureCommandSource} interface represents a source capable of receiving
 * messages using the Adventure API. It provides a method to send messages
 * represented as {@link ComponentLike} instances to the source.
 */
public interface AdventureCommandSource extends CommandSource {

    /**
     * Sends a message to this command source using Adventure API components.
     *
     * @param component the component to send
     */
    void reply(ComponentLike component);

}
