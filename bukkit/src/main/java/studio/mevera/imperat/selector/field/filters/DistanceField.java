package studio.mevera.imperat.selector.field.filters;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.selector.field.NumericField;
import studio.mevera.imperat.selector.field.Range;
import studio.mevera.imperat.selector.field.RangedNumericField;
import studio.mevera.imperat.util.TypeWrap;

final class DistanceField extends PredicateField<Range<Double>> {

    private final RangedNumericField<Double> rangedNumericField;

    /**
     * Constructs an AbstractField instance with the specified name and type.
     *
     * @param name The name of the selection field.
     */
    DistanceField(String name) {
        super(name, new TypeWrap<>() {
        });
        this.rangedNumericField = RangedNumericField.of(NumericField.doubleField(name));
    }

    /**
     * This method generates an EntityCondition based on the specified range.
     * It ensures that only players (not console) can use the 'distance' field.
     * The condition checks if the distance between the player and the entity
     * falls within the given range.
     *
     * @param value              the range of acceptable distances
     * @param commandInputStream the input stream associated with the command
     * @return an EntityCondition that checks if the entity meets the distance condition
     */
    @Override
    protected @NotNull EntityCondition getCondition(Range<Double> value, CommandInputStream<BukkitSource> commandInputStream) {
        return ((sender, entity) -> {
            if (sender.isConsole()) {
                throw new SourceException("Only players can use the field=`distance`");
            }
            Player commandSource = sender.asPlayer();
            double diffInDistance = commandSource.getLocation().distance(entity.getLocation());

            return value.isInRange(diffInDistance);
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
    public Range<Double> parseFieldValue(String value) throws ImperatException {
        return rangedNumericField.parseFieldValue(value);
    }
}
