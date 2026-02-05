package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

public sealed class ArgumentBuilder<S extends Source, T> permits FlagBuilder {

    protected final String name;
    protected final List<ArgValidator<S>> validators = new ArrayList<>();
    private final ArgumentType<S, T> type;
    private final boolean optional;
    private final boolean greedy;
    protected PermissionsData permission = PermissionsData.empty();
    protected Description description = Description.EMPTY;
    private @NotNull OptionalValueSupplier valueSupplier;
    private SuggestionResolver<S> suggestionResolver = null;

    ArgumentBuilder(String name, ArgumentType<S, T> type, boolean optional, boolean greedy) {
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.greedy = greedy;
        this.valueSupplier = type == null ? OptionalValueSupplier.empty() : type.supplyDefaultValue();
    }

    ArgumentBuilder(String name, ArgumentType<S, T> type, boolean optional) {
        this(name, type, optional, false);
    }

    public static <S extends Source> ArgumentBuilder<S, Command<S>> literalBuilder(String name) {
        return new ArgumentBuilder<>(name, ArgumentTypes.command(name, new ArrayList<>()), false, false);
    }

    public ArgumentBuilder<S, T> permission(@Nullable PermissionsData permission) {
        this.permission = permission;
        return this;
    }

    public ArgumentBuilder<S, T> description(@NotNull Description description) {
        Preconditions.notNull(description, "description");
        this.description = description;
        return this;
    }

    public ArgumentBuilder<S, T> description(String descValue) {
        return description(Description.of(descValue));
    }

    public ArgumentBuilder<S, T> defaultValue(@NotNull OptionalValueSupplier defaultValueSupplier) {
        this.valueSupplier = defaultValueSupplier;
        return this;
    }

    public ArgumentBuilder<S, T> defaultValue(@Nullable T value) {
        return defaultValue(value == null ? OptionalValueSupplier.empty() : OptionalValueSupplier.of(String.valueOf(value)));
    }

    public ArgumentBuilder<S, T> suggest(SuggestionResolver<S> suggestionResolver) {
        this.suggestionResolver = suggestionResolver;
        return this;
    }

    public ArgumentBuilder<S, T> suggest(String... suggestions) {
        return suggest(SuggestionResolver.staticSuggestions(suggestions));
    }

    public ArgumentBuilder<S, T> validate(ArgValidator<S> validator) {
        Preconditions.notNull(validator, "validator");
        validators.add(validator);
        return this;
    }

    public Argument<S> build() {
        return Argument.of(
                name, type, permission, description,
                optional, greedy, valueSupplier,
                suggestionResolver, validators
        );
    }

}
