package studio.mevera.imperat.command.tree;

import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;

import java.util.Iterator;

public final class RawInputStream<S extends CommandSource> implements Iterator<String> {

    private final CommandContext<S> context;
    private final ArgumentInput immutableInput;
    private int rawIndex = -1;

    private RawInputStream(CommandContext<S> context, ArgumentInput immutableInput) {
        this.context = context;
        this.immutableInput = immutableInput;
    }


    private RawInputStream(CommandContext<S> context, ArgumentInput immutableInput, int rawIndex) {
        this.immutableInput = immutableInput;
        this.rawIndex = rawIndex;
        this.context = context;
    }

    public static <S extends CommandSource> RawInputStream<S> newStream(CommandContext<S> context) {
        return new RawInputStream<>(context, context.arguments().copy());
    }

    public void setRawIndex(int rawIndex) {
        this.rawIndex = rawIndex;
    }

    @Override
    public boolean hasNext() {
        return rawIndex + 1 < immutableInput.size();
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new ArrayIndexOutOfBoundsException(
                    "There is no next index after current index='" + rawIndex + "', size of collection: '" + immutableInput.size() + "'");
        }
        return immutableInput.getOr(++rawIndex, null);
    }

    public void backward() {
        rawIndex--;
    }

    public RawInputStream<S> copy() {
        return new RawInputStream<>(context, immutableInput, rawIndex);
    }

    public RawInputStream<S> copyAndSetAtIndex(int index) {
        return new RawInputStream<>(context, immutableInput, index);
    }

    public CommandContext<S> getContext() {
        return context;
    }
}
