package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.Version;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.selector.CharStream;
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
    private static <V> @NotNull EntityCondition getEntityPredicate(
            List<SelectionParameterInput<?>> inputParameters,
            CommandContext<BukkitCommandSource> ctx
    ) {
        EntityCondition entityPredicted = (sender, entity) -> true;
        for (var input : inputParameters) {
            if (!(input.getField() instanceof PredicateField<?>)) {
                continue;
            }
            PredicateField<V> predicateField = (PredicateField<V>) input.getField();
            entityPredicted = entityPredicted.and(
                    (sender, entity) -> predicateField.isApplicable(sender, entity, (V) input.getValue(), ctx)
            );
        }
        return entityPredicted;
    }

    /**
     * Reads the {@code @x} prefix from a selector input. Advances the stream
     * past the mention character and the type letter (so the cursor is left
     * pointing at whatever follows, typically {@code [} or end-of-input).
     */
    static @NotNull SelectionType resolveSelectionType(
            @NotNull CharStream stream,
            @NotNull String raw
    ) throws CommandException {
        if (!isSelectorInput(raw)) {
            return SelectionType.UNKNOWN;
        }

        stream.skip(); // skip MENTION_CHARACTER ('@')
        Character typeChar = stream.next();
        String selectorId = typeChar == null ? "" : String.valueOf(typeChar);
        SelectionType type = SelectionType.from(selectorId);
        if (type != SelectionType.UNKNOWN) {
            return type;
        }

        String invalidType = selectorId.isEmpty() ? raw : selectorId;
        throw ResponseException.of(BukkitResponseKey.UNKNOWN_SELECTION_TYPE)
                      .withPlaceholder("input", raw)
                      .withPlaceholder("type_entered", invalidType);
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

    static boolean isSelectorInput(@NotNull String raw) {
        return raw.startsWith(SelectionType.MENTION_CHARACTER);
    }

    @Override
    public TargetSelector parse(
            @NotNull CommandContext<BukkitCommandSource> context,
            @NotNull Argument<BukkitCommandSource> argument,
            @NotNull String input
    ) throws CommandException {
        if (input.isEmpty()) {
            return TargetSelector.empty();
        }

        if (!isSelectorInput(input)) {
            Player target = Bukkit.getPlayer(input);
            if (target == null) {
                return TargetSelector.empty();
            }
            return TargetSelector.of(SelectionType.UNKNOWN, target);
        }

        if (Version.isOrOver(1, 13, 0)) {
            // 1.13+: delegate to vanilla's selector dispatcher.
            CharStream typeStream = new CharStream(input);
            SelectionType type = resolveSelectionType(typeStream, input);
            return TargetSelector.of(
                    type,
                    Bukkit.selectEntities(context.source().origin(), input)
            );
        }

        // Legacy (<1.13): walk the input one character at a time.
        CharStream stream = new CharStream(input);
        SelectionType type = resolveSelectionType(stream, input);

        List<SelectionParameterInput<?>> inputParameters = new ArrayList<>();
        char last = input.charAt(input.length() - 1);
        Character current = stream.peek();
        boolean parameterized = current != null && current == PARAMETER_START && last == PARAMETER_END;
        if (parameterized) {
            stream.skip(); // consume PARAMETER_START
            String params = stream.collectUntil(PARAMETER_END);
            inputParameters = SelectionParameterInput.parseAll(params, input, context);
        }

        List<Entity> entities = type.getTargetEntities(context);
        List<Entity> selected = new ArrayList<>();

        EntityCondition entityPredicted = getEntityPredicate(inputParameters, context);
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
