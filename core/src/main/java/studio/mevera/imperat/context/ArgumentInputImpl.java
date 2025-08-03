package studio.mevera.imperat.context;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.StringJoiner;

@ApiStatus.Internal
final class ArgumentInputImpl extends LinkedList<String> implements ArgumentInput {

    private final String originalRaw;
    ArgumentInputImpl(String originalRaw, @NotNull Collection<? extends String> input) {
        super(input);
        this.originalRaw = originalRaw;
    }


    ArgumentInputImpl(String originalRaw, @NotNull String... rawArgs) {
        this.originalRaw = originalRaw;
        Collections.addAll(this, rawArgs);
    }

    ArgumentInputImpl() {
        this.originalRaw = "";
    }

    @Override
    public String getOriginalRaw() {
        return originalRaw;
    }


    @Override
    public @NotNull String join(String delimiter) {
        return String.join(delimiter, this);
    }

    @Override
    public @NotNull String join(@NotNull String delimiter, int startIndex) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (int i = startIndex; i < this.size(); i++) {
            joiner.add(get(i));
        }
        return joiner.toString();
    }
    

    @Override
    public @NotNull ArgumentInput copy() {
        return new ArgumentInputImpl(originalRaw, this);
    }

}
