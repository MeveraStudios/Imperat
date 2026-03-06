package studio.mevera.imperat.command.processors;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.Priority;

public interface CommandProcessor extends Prioritizable {

    /**
     * Returns the priority of the command processor.
     * Processors with lower priority values are executed first.
     *
     * <p>
     * The default priority is 50. Implementations may override this
     * method to specify a custom priority value.
     * </p>
     *
     * @return the priority of this command processor
     */
    default @NotNull Priority getPriority() {
        return Priority.NORMAL;
    }

}
