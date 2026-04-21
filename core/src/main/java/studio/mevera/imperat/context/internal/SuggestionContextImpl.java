package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.HashSet;
import java.util.Set;

final class SuggestionContextImpl<S extends CommandSource> extends ContextImpl<S> implements SuggestionContext<S> {

    private final CompletionArg completionArg;
    private final static char FLAG_START = '-';
    private final Set<Integer> flagEnteredPositions = new HashSet<>();

    SuggestionContextImpl(
            Imperat<S> dispatcher,
            Command<S> command,
            S source,
            String label,
            ArgumentInput args,
            CompletionArg completionArg
    ) {
        super(dispatcher, command, source, label, args);
        this.completionArg = completionArg;


        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (quickIsArgFlag(arg)) {
                flagEnteredPositions.add(i);
            }
        }
    }

    private boolean quickIsArgFlag(String str) {
        if (str.isBlank()) {
            return false;
        }
        boolean singleStartFlag = str.charAt(0) == FLAG_START;
        boolean doubleStartFlag = str.length() > 1 && singleStartFlag && str.charAt(1) == FLAG_START;

        int maxLength = doubleStartFlag ? 2 : singleStartFlag ? 1 : -1;
        if (maxLength == -1) {
            return false;
        }
        return str.length() > maxLength;

    }

    @Override
    public @NotNull CompletionArg getArgToComplete() {
        return completionArg;
    }

    @Override
    public boolean isFlagPosition(int depth) {
        return flagEnteredPositions.contains(depth);
    }


}
