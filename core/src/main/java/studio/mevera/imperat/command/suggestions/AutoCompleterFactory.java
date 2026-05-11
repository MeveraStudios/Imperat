package studio.mevera.imperat.command.suggestions;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandSource;

/**
 * Factory responsible for creating an {@link AutoCompleter} for a given command.
 *
 * @param <S> the command-source type
 */
@FunctionalInterface
public interface AutoCompleterFactory<S extends CommandSource> {

    AutoCompleter<S> create(Command<S> command);

}
