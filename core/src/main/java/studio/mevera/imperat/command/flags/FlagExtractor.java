package studio.mevera.imperat.command.flags;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.FlagRegistrar;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;

import java.util.Set;

/**
 * A class that extracts flags from a single string/argument.
 * @param <S> the type of the command source
 */
@ApiStatus.AvailableSince("1.9.6")
public sealed interface FlagExtractor<S extends Source> extends FlagRegistrar<S> permits FlagExtractorImpl {

    static <S extends Source> FlagExtractor<S> createNative(CommandUsage<S> usage) {
        return new FlagExtractorImpl<>(usage);
    }

    /**
     * Inserts a flag during into the trie.
     * May be useful if it's necessary to insert a flag during runtime.
     * @param flagArgumentData the {@link FlagData} to insert
     */
    void insertFlag(FlagArgument<S> flagArgumentData);

    /**
     * Extracts all flags used from a single string with no spaces.
     *
     * @param rawInput the raw input of an argument
     * @return the extracted {@link FlagData} for flags.
     */
    Set<FlagArgument<S>> extract(String rawInput) throws CommandException;

}
