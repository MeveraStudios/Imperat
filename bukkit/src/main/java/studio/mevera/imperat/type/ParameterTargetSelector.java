package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.Version;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.selector.UnknownEntitySelectionTypeException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.selector.EntityCondition;
import studio.mevera.imperat.selector.SelectionParameterInput;
import studio.mevera.imperat.selector.SelectionType;
import studio.mevera.imperat.selector.TargetSelector;
import studio.mevera.imperat.selector.field.filters.PredicateField;
import studio.mevera.imperat.selector.field.operators.OperatorField;

import java.util.ArrayList;
import java.util.List;

public final class ParameterTargetSelector extends BaseParameterType<BukkitSource, TargetSelector> {

    private final static char PARAMETER_START = '[';
    private final static char PARAMETER_END = ']';

    private final SuggestionResolver<BukkitSource> suggestionResolver;

    public ParameterTargetSelector() {
        super();
        SelectionType.TYPES.stream()
                .filter(type -> type != SelectionType.UNKNOWN)
                .map(SelectionType::id)
                .forEach((id) -> suggestions.add(SelectionType.MENTION_CHARACTER + id));
        suggestionResolver = new TargetSelectorSuggestionResolver();
    }

    @SuppressWarnings("unchecked")
    private static @NotNull <V> EntityCondition getEntityPredicate(@NotNull CommandInputStream<BukkitSource> commandInputStream,
            List<SelectionParameterInput<?>> inputParameters, Context<BukkitSource> ctx) {
        EntityCondition entityPredicted = (sender, entity) -> true;
        for (var input : inputParameters) {
            if (!(input.getField() instanceof PredicateField<?>)) {
                continue;
            }
            PredicateField<V> predicateField = (PredicateField<V>) input.getField();
            entityPredicted = entityPredicted.and(
                    (sender, entity) -> predicateField.isApplicable(sender, entity, (V) input.getValue(), commandInputStream, ctx)
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
    public @NotNull TargetSelector resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream,
            @NotNull String input) throws CommandException {

        String raw = commandInputStream.currentRaw().orElse(null);
        if (raw == null) {
            return TargetSelector.empty();
        }

        if (Version.isOrOver(1, 13, 0)) {
            SelectionType type = commandInputStream.popLetter().map(
                    s -> SelectionType.from(String.valueOf(s))
            ).orElse(SelectionType.UNKNOWN);
            return TargetSelector.of(
                    type,
                    Bukkit.selectEntities(context.source().origin(), raw)
            );
        }

        char last = raw.charAt(raw.length() - 1);
        if (commandInputStream.currentLetter().filter(
                c -> String.valueOf(c).equalsIgnoreCase(SelectionType.MENTION_CHARACTER)
        ).isEmpty()) {
            Player target = Bukkit.getPlayer(raw);
            if (target == null) {
                return TargetSelector.empty();
            }

            return TargetSelector.of(SelectionType.UNKNOWN, target);
        }

        SelectionType type = commandInputStream.popLetter().map((s) -> SelectionType.from(String.valueOf(s))).orElse(SelectionType.UNKNOWN);
        if (type == SelectionType.UNKNOWN) {
            throw new UnknownEntitySelectionTypeException(commandInputStream.currentLetter().orElseThrow() + "");
        }

        List<SelectionParameterInput<?>> inputParameters = new ArrayList<>();
        boolean parameterized = commandInputStream.popLetter().map((c) -> c == PARAMETER_START).orElse(false) && last == PARAMETER_END;
        if (parameterized) {
            commandInputStream.skipLetter();

            String params = commandInputStream.collectBeforeFirst(PARAMETER_END);
            inputParameters = SelectionParameterInput.parseAll(params, commandInputStream, context);
        }

        List<Entity> entities = type.getTargetEntities(context, commandInputStream);
        List<Entity> selected = new ArrayList<>();

        EntityCondition entityPredicted = getEntityPredicate(commandInputStream, inputParameters, context);
        for (Entity entity : entities) {
            if (entityPredicted.test(context.source(), entity)) {
                selected.add(entity);
            }
        }
        operateFields(inputParameters, selected);

        return TargetSelector.of(type, selected);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionResolver<BukkitSource> getSuggestionResolver() {
        return suggestionResolver;
    }

    private final class TargetSelectorSuggestionResolver implements SuggestionResolver<BukkitSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> autoComplete(
                SuggestionContext<BukkitSource> context,
                CommandParameter<BukkitSource> parameter
        ) {
            List<String> completions = new ArrayList<>(suggestions);
            Bukkit.getOnlinePlayers().stream().
                    map(Player::getName).forEach(completions::add);
            return completions;
        }
    }

}
