package studio.mevera.imperat.command.suggestions;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents an argument that's being completed
 * or an argument requested by the command-sender
 * to be completed
 *
 * @param value the argument input half-complete or empty to be completed
 * @param index the index of this argument
 */
@ApiStatus.AvailableSince("1.0.0")
public record CompletionArg(String value, int index) {

    public final static CompletionArg EMPTY = new CompletionArg("", 0);
    
    public boolean isEmpty() {
        return value.isEmpty() || value.isBlank();
    }
}
