package studio.mevera.imperat.command.processors;

import studio.mevera.imperat.context.Source;

public interface CommandProcessor<S extends Source> {

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
    default int priority() {
        return 50;
    }

}
