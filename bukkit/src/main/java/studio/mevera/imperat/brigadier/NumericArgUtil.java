package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.NumericRange;
import studio.mevera.imperat.util.TypeUtility;

import java.lang.reflect.Type;

class NumericArgUtil {

    static ArgumentType<? extends Number> numeric(
            Type type,
            @NotNull NumericRange range
    ) {
        if (TypeUtility.matches(type, int.class)) {
            if (range.isEmpty()) {
                return IntegerArgumentType.integer();
            } else if (range.hasMin() && range.hasMax()) {
                return IntegerArgumentType.integer((int) range.getMin(), (int) range.getMax());
            } else if (range.hasMin()) {
                return IntegerArgumentType.integer((int) range.getMin());
            } else {
                return IntegerArgumentType.integer(-Integer.MAX_VALUE, (int) range.getMax());
            }
        } else if (TypeUtility.matches(type, long.class)) {
            if (range.isEmpty()) {
                return LongArgumentType.longArg();
            } else if (range.hasMin() && range.hasMax()) {
                return LongArgumentType.longArg((long) range.getMin(), (long) range.getMax());
            } else if (range.hasMin()) {
                return LongArgumentType.longArg((long) range.getMin());
            } else {
                return LongArgumentType.longArg(-Long.MAX_VALUE, (long) range.getMax());
            }
        } else if (TypeUtility.matches(type, float.class)) {
            if (range.isEmpty()) {
                return FloatArgumentType.floatArg();
            } else if (range.hasMin() && range.hasMax()) {
                return FloatArgumentType.floatArg((float) range.getMin(), (float) range.getMax());
            } else if (range.hasMin()) {
                return FloatArgumentType.floatArg((float) range.getMin());
            } else {
                return FloatArgumentType.floatArg(-Float.MAX_VALUE, (float) range.getMax());
            }
        } else if (TypeUtility.matches(type, double.class)) {
            if (range.isEmpty()) {
                return DoubleArgumentType.doubleArg();
            } else if (range.hasMin() && range.hasMax()) {
                return DoubleArgumentType.doubleArg(range.getMin(), range.getMax());
            } else if (range.hasMin()) {
                return DoubleArgumentType.doubleArg(range.getMin());
            } else {
                return DoubleArgumentType.doubleArg(-Double.MAX_VALUE, range.getMax());
            }
        } else {
            throw new IllegalArgumentException("Unsupported numeric valueType: " + type);
        }
    }

    private static ArgumentType<? extends Number> numeric(Type type) {
        return numeric(type, NumericRange.empty());
    }

}
