package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.StringReader;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;

public class BukkitStringReader extends StringReader {

    public BukkitStringReader(ExecutionContext<BukkitSource> ctx, Cursor<BukkitSource> inputStream) {
        super(loadString(ctx, inputStream));
        setCursor(inputStream.currentRawPosition());
    }

    private static String loadString(ExecutionContext<BukkitSource> ctx, Cursor<BukkitSource> inputStream) {
        StringBuilder builder = new StringBuilder();
        builder.append(ctx.label());
        for (var arg : inputStream.getRawQueue()) {
            builder.append(' ').append(arg);
        }
        return builder.toString();
    }
}
