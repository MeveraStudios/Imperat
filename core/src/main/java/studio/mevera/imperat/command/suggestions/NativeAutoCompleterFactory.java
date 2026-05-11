package studio.mevera.imperat.command.suggestions;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandSource;

/**
 * Default {@link AutoCompleterFactory} that produces either a native (prefix-based)
 * or fuzzy (substring-based) completer depending on the {@code useFuzzy} flag.
 *
 * @param <S> the command-source type
 */
public final class NativeAutoCompleterFactory<S extends CommandSource> implements AutoCompleterFactory<S> {

    private boolean useFuzzy;

    public NativeAutoCompleterFactory(boolean useFuzzy) {
        this.useFuzzy = useFuzzy;
    }

    public NativeAutoCompleterFactory<S> useFuzzy(boolean useFuzzy) {
        this.useFuzzy = useFuzzy;
        return this;
    }

    @Override
    public AutoCompleter<S> create(Command<S> command) {
        return useFuzzy ? new FuzzyAutoCompleter<>(command) : AutoCompleter.createNative(command);
    }
}
