package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.StringReader;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;

public class BukkitStringReader extends StringReader {

    public BukkitStringReader(ExecutionContext<BukkitCommandSource> ctx, Cursor<BukkitCommandSource> inputStream) {
        super(loadString(ctx, inputStream));
        setCursor(inputStream.currentRawPosition());
    }

    private static String loadString(ExecutionContext<BukkitCommandSource> ctx, Cursor<BukkitCommandSource> inputStream) {
        StringBuilder builder = new StringBuilder();
        builder.append(ctx.getRootCommandLabelUsed());
        for (var arg : inputStream.getRawQueue()) {
            builder.append(' ').append(arg);
        }
        return builder.toString();
    }
}
