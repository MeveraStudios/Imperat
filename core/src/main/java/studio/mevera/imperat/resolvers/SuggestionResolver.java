package studio.mevera.imperat.resolvers;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a suggestion providing interface
 * for an argument/parameter
 *
 * @param <S> the command-sender valueType
 * @see Argument
 */
@ApiStatus.AvailableSince("1.0.0")
public interface SuggestionResolver<S extends Source> {

    static <S extends Source> SuggestionResolver<S> staticSuggestions(List<String> results) {
        return new StaticSuggestionResolver<>(results);
    }

    static <S extends Source> SuggestionResolver<S> staticSuggestions(String... results) {
        return staticSuggestions(Arrays.asList(results));
    }

    static <S extends Source> SuggestionResolver<S> forCommand(Command<S> command) {
        List<String> list = new ArrayList<>();
        list.add(command.name());
        list.addAll(command.aliases());
        return staticSuggestions(list);
    }


    /**
     * @param context   the context for suggestions
     * @param parameter the parameter of the value to complete
     * @return the auto-completed suggestions of the current argument
     */
    List<String> autoComplete(SuggestionContext<S> context, Argument<S> parameter);

    default CompletableFuture<List<String>> asyncAutoComplete(SuggestionContext<S> context, Argument<S> parameter) {
        return CompletableFuture.supplyAsync(() -> autoComplete(context, parameter));
    }
}
