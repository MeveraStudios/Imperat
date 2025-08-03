package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.arguments.*;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ArgumentTypeResolver;
import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.command.parameters.type.ParameterWord;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

class DefaultArgTypeResolvers {


    private static final ArgumentType<?> SINGLE_PLAYER = entity(true, true);
    private static final ArgumentType<?> MULTI_ENTITY = entity(false, false);

    public final static ArgumentTypeResolver STRING = (parameter -> {
        if (parameter.isGreedy()) return StringArgumentType.greedyString();
        if (parameter.type() instanceof ParameterWord<?>) return StringArgumentType.word();
        return StringArgumentType.string();
    });

    public final static ArgumentTypeResolver BOOLEAN = (parameter -> BoolArgumentType.bool());

    public final static ArgumentTypeResolver NUMERIC = (parameter) -> {

        if (parameter.isNumeric()) {
            NumericRange range = parameter.asNumeric().getRange();
            return numeric(parameter.valueType(), range);
        }

        return null;
    };

    public static final ArgumentTypeResolver PLAYER = parameter -> SINGLE_PLAYER;

    public static final ArgumentTypeResolver ENTITY_SELECTOR = parameter -> {

        if(TypeUtility.matches(parameter.valueType(), TargetSelector.class) || TypeWrap.of(Entity.class).isSupertypeOf(parameter.valueType()))
            return MULTI_ENTITY;
        return null;
    };

    private static ArgumentType<? extends Number> numeric(
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
        if (range == null)
            return Double.MIN_VALUE;
        else
            return range.getMin();
    }

    private static double getMax(@Nullable NumericRange range) {
        if (range == null)
            return Double.MAX_VALUE;
        else
            return range.getMax();
    }

    private static ArgumentType<?> entity(boolean single, boolean playerOnly) {
        return MinecraftArgumentType.ENTITY.create(single, playerOnly);
    }
}
