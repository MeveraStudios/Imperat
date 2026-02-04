package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;

class NumericArgUtil {

    static ArgumentType<? extends Number> numeric(
            Type type,
            @Nullable NumericRange range) {
        if (TypeUtility.matches(type, int.class)) {
            return IntegerArgumentType.integer((int) getMin(range), (int) getMax(range));
        } else if (TypeUtility.matches(type, long.class)) {
            return LongArgumentType.longArg((long) getMin(range), (long) getMax(range));
        } else if (TypeUtility.matches(type, float.class)) {
            return FloatArgumentType.floatArg((float) getMin(range), (float) getMax(range));
        } else if (TypeUtility.matches(type, double.class)) {
            return DoubleArgumentType.doubleArg(getMin(range), getMax(range));
        } else {
            throw new IllegalArgumentException("Unsupported numeric valueType: " + type);
        }
    }

    private static double getMin(@Nullable NumericRange range) {
        if (range == null) {
            return Double.MIN_VALUE;
        } else {
            return range.getMin();
        }
    }

    private static double getMax(@Nullable NumericRange range) {
        if (range == null) {
            return Double.MAX_VALUE;
        } else {
            return range.getMax();
        }
    }
}
