package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.suggestions.AutoCompleter;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class ImperatAutoCompleter<S extends CommandSource> {

    private final Imperat<S> imperat;
    private final ImperatConfig<S> config;

    ImperatAutoCompleter(Imperat<S> imperat, ImperatConfig<S> config) {
        this.imperat = imperat;
        this.config = config;
    }

    CompletableFuture<List<String>> autoComplete(@NotNull S source, @NotNull String fullCommandLine) {
        int firstSpace = fullCommandLine.indexOf(' ');
        if (firstSpace == -1) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String cmdName = fullCommandLine.substring(0, firstSpace);
        Command<S> command = imperat.getCommand(cmdName);
        if (command == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        boolean endsWithSpace = Character.isWhitespace(fullCommandLine.charAt(fullCommandLine.length() - 1));
        int argumentsStart = firstSpace + 1;
        int argumentsEnd = endsWithSpace ? fullCommandLine.length() - 1 : fullCommandLine.length();
        String argumentsSection = argumentsStart >= argumentsEnd
                                          ? ""
                                          : fullCommandLine.substring(argumentsStart, argumentsEnd);
        ArgumentInput argumentInput = ArgumentInput.parseAutoCompletion(
                argumentsSection,
                endsWithSpace
        );

        SuggestionContext<S> context = config.getContextFactory()
                                               .createSuggestionContext(
                                                       imperat, source, command, cmdName, argumentInput
                                               );
        return command.autoCompleter()
                       .autoComplete(context)
                       .exceptionally((ex) -> {
                           config.handleExecutionError(ex, context, AutoCompleter.class, "autoComplete(dispatcher, sender, args)");
                           return Collections.emptyList();
                       });
    }
}
