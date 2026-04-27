package studio.mevera.imperat.config;

import studio.mevera.imperat.CommandParsingMode;
import studio.mevera.imperat.context.CommandSource;

/**
 * Mutable bag of scalar runtime flags previously scattered across
 * {@code ImperatConfigImpl} as loose fields. Grouping them avoids polluting
 * the impl with five unrelated booleans/strings and gives callers a single
 * object to capture when a strategy needs the flags.
 *
 * @param <S> the command-source type
 */
public final class BehaviorSettings<S extends CommandSource> {

    private CommandParsingMode parsingMode = CommandParsingMode.JAVA;
    private boolean overlapOptionalParameterSuggestions = false;
    private boolean handleExecutionMiddleOptionalSkipping = false;
    private String commandPrefix = "/";

    public CommandParsingMode parsingMode() {
        return parsingMode;
    }

    public void setParsingMode(CommandParsingMode parsingMode) {
        this.parsingMode = parsingMode;
    }

    public boolean isOverlapOptionalParameterSuggestions() {
        return overlapOptionalParameterSuggestions;
    }

    public void setOverlapOptionalParameterSuggestions(boolean enabled) {
        this.overlapOptionalParameterSuggestions = enabled;
    }

    public boolean handleExecutionMiddleOptionalSkipping() {
        return handleExecutionMiddleOptionalSkipping;
    }

    public void setHandleExecutionMiddleOptionalSkipping(boolean enabled) {
        this.handleExecutionMiddleOptionalSkipping = enabled;
    }

    public String commandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String prefix) {
        this.commandPrefix = prefix;
    }
}
