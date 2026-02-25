package studio.mevera.imperat.providers;

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
public interface SuggestionProvider<S extends Source> {

    static <S extends Source> SuggestionProvider<S> staticSuggestions(List<String> results) {
        return new StaticSuggestionProvider<>(results);
    }

    static <S extends Source> SuggestionProvider<S> staticSuggestions(String... results) {
        return staticSuggestions(Arrays.asList(results));
    }

    static <S extends Source> SuggestionProvider<S> forCommand(Command<S> command) {
        List<String> list = new ArrayList<>();
        list.add(command.getName());
        list.addAll(command.aliases());
        return staticSuggestions(list);
    }


    /**
     * @param context   the context for suggestions
     * @param parameter the parameter of the value to complete
     * @return the auto-completed suggestions of the current argument
     */
    List<String> provide(SuggestionContext<S> context, Argument<S> parameter);

    default CompletableFuture<List<String>> provideAsynchronously(SuggestionContext<S> context, Argument<S> parameter) {
        return CompletableFuture.supplyAsync(() -> provide(context, parameter));
    }
}
