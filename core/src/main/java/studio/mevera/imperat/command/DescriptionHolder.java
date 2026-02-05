package studio.mevera.imperat.command;

/**
 * Represents an entity that can have a description.
 * This interface provides methods to get and set a description.
 */
public interface DescriptionHolder {

    /**
     * Retrieves the current description associated with this entity.
     *
     * @return the current {@link Description}.
     */
    Description getDescription();

    /**
     * Sets the description for this entity.
     *
     * @param description the {@link Description} to set.
     */
    void describe(final Description description);

    /**
     * Sets the description for this entity using a string.
     *
     * @param description the string to create a {@link Description} from and set.
     */
    default void describe(final String description) {
        describe(Description.of(description));
    }

}
