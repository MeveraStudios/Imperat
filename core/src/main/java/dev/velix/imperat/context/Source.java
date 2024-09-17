package dev.velix.imperat.context;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents the sender/source
 * of a command being executed
 * may be a console, a player in a game, etc...
 */
@ApiStatus.AvailableSince("1.0.0")
public interface Source {
    
    /**
     * @return name of a command source
     */
    String name();
    
    /**
     * @return The original command sender type instance
     */
    Object origin();
    
    /**
     * Replies to the command sender with a string message
     *
     * @param message the message
     */
    void reply(String message);

    /**
     * Replies to the command sender with a string message
     * formatted specifically for error messages
     *
     * @param message the message
     */
    void warn(String message);

    /**
     * Replies to the command sender with a string message
     * formatted specifically for error messages
     *
     * @param message the message
     */
    void error(String message);

    /**
     * @return Whether the command source is from the console
     */
    boolean isConsole();
    
    @SuppressWarnings("unchecked")
    default <T> T as(Class<T> clazz) {
        return (T) this.origin();
    }
    
}
