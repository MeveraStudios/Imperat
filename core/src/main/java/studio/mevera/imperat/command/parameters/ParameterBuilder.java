package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

public sealed class ParameterBuilder<S extends Source, T> permits FlagBuilder {

    protected final String name;
    protected final List<ArgValidator<S>> validators = new ArrayList<>();
    private final ParameterType<S, T> type;
    private final boolean optional;
    private final boolean greedy;
    protected PermissionsData permission = PermissionsData.empty();
    protected Description description = Description.EMPTY;
    private @NotNull OptionalValueSupplier valueSupplier;
    private SuggestionResolver<S> suggestionResolver = null;

    ParameterBuilder(String name, ParameterType<S, T> type, boolean optional, boolean greedy) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.greedy = greedy;
        this.valueSupplier = type == null ? OptionalValueSupplier.empty() : type.supplyDefaultValue();
    }

    ParameterBuilder(String name, ParameterType<S, T> type, boolean optional) {
        this(name, type, optional, false);
    }

    public static <S extends Source> ParameterBuilder<S, Command<S>> literalBuilder(String name) {
        return new ParameterBuilder<>(name, ParameterTypes.command(name, new ArrayList<>()), false, false);
    }

    public ParameterBuilder<S, T> permission(@Nullable PermissionsData permission) {
        this.permission = permission;
        return this;
    }

    public ParameterBuilder<S, T> description(@NotNull Description description) {
        Preconditions.notNull(description, "description");
        this.description = description;
        return this;
    }

    public ParameterBuilder<S, T> description(String descValue) {
        return description(Description.of(descValue));
    }

    public ParameterBuilder<S, T> defaultValue(@NotNull OptionalValueSupplier defaultValueSupplier) {
        this.valueSupplier = defaultValueSupplier;
        return this;
    }

    public ParameterBuilder<S, T> defaultValue(@Nullable T value) {
        return defaultValue(value == null ? OptionalValueSupplier.empty() : OptionalValueSupplier.of(String.valueOf(value)));
    }

    public ParameterBuilder<S, T> suggest(SuggestionResolver<S> suggestionResolver) {
        this.suggestionResolver = suggestionResolver;
        return this;
    }

    public ParameterBuilder<S, T> suggest(String... suggestions) {
        return suggest(SuggestionResolver.staticSuggestions(suggestions));
    }

    public ParameterBuilder<S, T> validate(ArgValidator<S> validator) {
        Preconditions.notNull(validator, "validator");
        validators.add(validator);
        return this;
    }

    public CommandParameter<S> build() {
        return CommandParameter.of(
                name, type, permission, description,
                optional, greedy, valueSupplier,
                suggestionResolver, validators
        );
    }

}
