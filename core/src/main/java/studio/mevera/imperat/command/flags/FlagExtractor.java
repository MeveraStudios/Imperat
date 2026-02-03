package studio.mevera.imperat.command.flags;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.FlagRegistrar;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.UnknownFlagException;

import java.util.Set;

/**
 * A class that extracts flags from a single string/argument.
 * @param <S> the type of the command source
 */
@ApiStatus.AvailableSince("1.9.6")
public sealed interface FlagExtractor<S extends Source> extends FlagRegistrar<S> permits FlagExtractorImpl {

    /**
     * Inserts a flag during into the trie.
     * May be useful if it's necessary to insert a flag during runtime.
     * @param flagData the {@link FlagData} to insert
     */
    void insertFlag(FlagParameter<S> flagData);

    /**
     * Extracts all flags used from a single string with no spaces.
     *
     * @param rawInput the raw input of an argument
     * @return the extracted {@link FlagData} for flags.
     */
    Set<FlagParameter<S>> extract(String rawInput) throws UnknownFlagException;

    static <S extends Source> FlagExtractor<S> createNative(CommandUsage<S> usage) {
        return new FlagExtractorImpl<>(usage);
    }

}
