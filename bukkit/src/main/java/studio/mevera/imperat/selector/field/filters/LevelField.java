package studio.mevera.imperat.selector.field.filters;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.selector.field.NumericField;
import studio.mevera.imperat.selector.field.Range;
import studio.mevera.imperat.selector.field.RangedNumericField;
import studio.mevera.imperat.util.TypeWrap;

final class LevelField extends PredicateField<Range<Integer>> {

    private final RangedNumericField<Integer> numericField;

    LevelField(String name) {
        super(name, new TypeWrap<>() {
        });
        this.numericField = RangedNumericField.of(NumericField.integerField(name));
    }


    @Override
    protected @NotNull EntityCondition getCondition(Range<Integer> value, CommandInputStream<BukkitSource> commandInputStream) {
        return ((sender, entity) -> {
            if (!(entity instanceof Player humanEntity)) return false;
            return value.isInRange(humanEntity.getLevel());
        });
    }

    /**
     * Parses the given string representation of the value and converts it into the field's value type.
     *
     * @param value the string representation of the value to be parsed
     * @return the parsed value of the field's type
     * @throws ImperatException if the parsing fails
     */
    @Override
    public Range<Integer> parseFieldValue(String value) throws ImperatException {
        return numericField.parseFieldValue(value);
    }
}
