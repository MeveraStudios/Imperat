package studio.mevera.imperat.config;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.CommandParsingMode;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.Registry;

/**
 * Mutable bag of scalar runtime flags previously scattered across
 * {@code ImperatConfigImpl} as loose fields. Grouping them avoids polluting
 * the impl with five unrelated booleans/strings and gives callers a single
 * object to capture when a strategy needs the flags.
 *
 * @param <S> the command-source type
 */
public final class BehavioralOptionRegistry<S extends CommandSource> extends Registry<BehaviouralOptionKey, BehaviouralOption<?>> {

    public BehavioralOptionRegistry() {
        this.registerOption(BehaviouralOptionKey.PARSING_MODE, CommandParsingMode.class, CommandParsingMode.JAVA, CommandParsingMode.JAVA);
        this.registerOption(BehaviouralOptionKey.OVERLAP_OPTIONALS_ARGUMENTS_SUGGESTIONS, Boolean.class, false, false);
        this.registerOption(BehaviouralOptionKey.STRICT_AMBIGUITY_RESOLUTION, Boolean.class, true, true);
        this.registerOption(BehaviouralOptionKey.COMMAND_PREFIX, String.class, "/", "/");
    }

    public void registerOption(BehaviouralOption<?> value) {
        this.setData(value.key(), value);
    }

    public <T> void registerOption(BehaviouralOptionKey key, Class<T> valueType, T value, T defaultValue) {
        this.setData(key, new BehaviouralOption<>(key, valueType, value, defaultValue));
    }

    @SuppressWarnings("unchecked")
    public <T> void setOption(BehaviouralOptionKey key, T value) {
        this.updateIfPresent(key, (option) -> {
            if (value != null && !option.valueType().equals(value.getClass())) {
                throw new IllegalArgumentException("Value type mismatch for option " + key);
            }
            return ((BehaviouralOption<T>) option).setValue(value);
        });
    }

    public void resetOption(BehaviouralOptionKey key) {
        this.updateIfPresent(key, BehaviouralOption::reset);
    }

    public <T> @NotNull T getOptionValue(BehaviouralOptionKey key) {
        var option = getData(key);
        if (option.isEmpty()) {
            throw new IllegalArgumentException("Option " + key + " not found");
        }
        return (T) option.get().value();
    }
}
