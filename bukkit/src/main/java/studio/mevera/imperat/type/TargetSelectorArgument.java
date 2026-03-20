package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.selector.SelectionParameterInput;
import studio.mevera.imperat.selector.SelectionType;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.selector.field.filters.PredicateField;
import studio.mevera.imperat.selector.field.operators.OperatorField;

import java.util.ArrayList;
import java.util.List;

public final class TargetSelectorArgument extends ArgumentType<BukkitCommandSource, TargetSelector> {

    private final static char PARAMETER_START = '[';
    private final static char PARAMETER_END = ']';

    private final SuggestionProvider<BukkitCommandSource> suggestionProvider;

    public TargetSelectorArgument() {
        super();
        SelectionType.TYPES.stream()
                .filter(type -> type != SelectionType.UNKNOWN)
                .map(SelectionType::id)
                .forEach((id) -> suggestions.add(SelectionType.MENTION_CHARACTER + id));
        suggestionProvider = new TargetSelectorSuggestionProvider();
    }

    @SuppressWarnings("unchecked")
    private static @NotNull <V> EntityCondition getEntityPredicate(@NotNull Cursor<BukkitCommandSource> cursor,
            List<SelectionParameterInput<?>> inputParameters, CommandContext<BukkitCommandSource> ctx) {
        EntityCondition entityPredicted = (sender, entity) -> true;
        for (var input : inputParameters) {
            if (!(input.getField() instanceof PredicateField<?>)) {
                continue;
            }
            PredicateField<V> predicateField = (PredicateField<V>) input.getField();
            entityPredicted = entityPredicted.and(
                    (sender, entity) -> predicateField.isApplicable(sender, entity, (V) input.getValue(), cursor, ctx)
            );

        }
        return entityPredicted;
    }

    @SuppressWarnings("unchecked")
    private static <V> void operateFields(List<SelectionParameterInput<?>> inputParameters, List<Entity> selected) {
        for (var input : inputParameters) {
            if (input.getField() instanceof OperatorField<?>) {
                OperatorField<V> operatorField = (OperatorField<V>) input.getField();
                operatorField.operate((V) input.getValue(), selected);
            }
        }
    }

    @Override
    public TargetSelector parse(@NotNull CommandContext<BukkitCommandSource> context, @NotNull String input) throws CommandException {
        // Fallback: just return an empty TargetSelector for now
        return TargetSelector.empty();
    }

    @Override
    public TargetSelector parse(@NotNull ExecutionContext<BukkitCommandSource> context, @NotNull Cursor<BukkitCommandSource> cursor)
            throws CommandException {
        String raw = cursor.currentRaw().orElse(null);
        if (raw == null) {
            return TargetSelector.empty();
        }
        return parse(context, raw);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return suggestionProvider;
    }

    private final class TargetSelectorSuggestionProvider implements SuggestionProvider<BukkitCommandSource> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(
                SuggestionContext<BukkitCommandSource> context,
                Argument<BukkitCommandSource> argument
        ) {
            List<String> completions = new ArrayList<>(suggestions);
            Bukkit.getOnlinePlayers().stream().
                    map(Player::getName).forEach(completions::add);
            return completions;
        }
    }

}
