package studio.mevera.imperat.annotations.base.system.parsers;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.ArgType;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.DefaultProvider;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.Format;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.annotations.Switch;
import studio.mevera.imperat.annotations.Validators;
import studio.mevera.imperat.annotations.Values;
import studio.mevera.imperat.annotations.base.AnnotationHelper;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.validator.ArgValidator;
import studio.mevera.imperat.command.parameters.validator.ConstrainedValueValidator;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.TypeWrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ParameterParser<S extends Source> {

    private static final String VALUES_SEPARATION_CHAR = "\\|";


    private final ImperatConfig<S> config;

    ParameterParser(ImperatConfig<S> config) {
        this.config = config;
    }

    @Nullable Argument<S> parseParameter(ParameterElement param) {
        if (param.isContextResolved()) {
            return null;
        }

        // Check for flag/switch conflicts
        Flag flag = param.getAnnotation(Flag.class);
        Switch switchAnn = param.getAnnotation(Switch.class);

        if (flag != null && switchAnn != null) {
            throw new IllegalStateException("Parameter cannot have both @Flag and @Switch: " + param.getName());
        }

        // Resolve argument type
        ArgumentType<S, ?> type = resolveArgumentType(param);

        // Build base argument using Argument.of() factory
        String name = param.getName();
        boolean optional = param.isOptional();
        boolean greedy = param.getAnnotation(Greedy.class) != null;

        // Collect validators
        List<ArgValidator<S>> validators = parseValidators(param);

        // Create the argument
        Argument<S> argument = Argument.of(
                name,
                type,
                parsePermissions(param),
                parseDescription(param),
                optional,
                greedy,
                parseDefaultValue(param),
                parseSuggestions(param),
                validators
        );

        // Apply additional decorators
        applyParameterDecorators(argument, param);

        return argument;
    }

    @SuppressWarnings("unchecked")
    private <T> ArgumentType<S, T> resolveArgumentType(ParameterElement param) {
        ArgType argTypeAnn = param.getAnnotation(ArgType.class);

        if (argTypeAnn != null) {
            if (param.getAnnotation(Flag.class) != null || param.getAnnotation(Switch.class) != null) {
                throw new IllegalStateException("@ArgType cannot be used on flag/switch parameters");
            }
            return (ArgumentType<S, T>) config.getInstanceFactory()
                                                .createInstance(config, argTypeAnn.value());
        }

        TypeWrap<T> typeWrap = (TypeWrap<T>) TypeWrap.of(param.getElement().getParameterizedType());
        ArgumentType<S, T> type = (ArgumentType<S, T>) config.getArgumentType(typeWrap.getType());

        if (type == null) {
            throw new IllegalArgumentException("Unknown type: " + typeWrap.getType().getTypeName());
        }

        return type;
    }

    private void applyParameterDecorators(Argument<S> argument, ParameterElement param) {
        // Format
        Format formatAnn = param.getAnnotation(Format.class);
        if (formatAnn != null) {
            argument.setFormat(config.replacePlaceholders(formatAnn.value()));
        }
    }

    // ==================== Parameter Parsing Helpers ====================

    private Description parseDescription(ParameterElement param) {
        studio.mevera.imperat.annotations.Description descAnn =
                param.getAnnotation(studio.mevera.imperat.annotations.Description.class);
        if (descAnn != null) {
            return Description.of(config.replacePlaceholders(descAnn.value()));
        }
        return Description.EMPTY;
    }

    private PermissionsData parsePermissions(ParameterElement param) {
        Permission[] perms = param.getAnnotationsByType(Permission.class);
        PermissionsData data = PermissionsData.empty();
        for (Permission p : perms) {
            data.append(PermissionsData.fromText(config.replacePlaceholders(p.value())));
        }
        return data;
    }

    private @Nullable SuggestionProvider<S> parseSuggestions(ParameterElement param) {
        Suggest suggestAnn = param.getAnnotation(Suggest.class);
        if (suggestAnn != null) {
            return SuggestionProvider.staticSuggestions(
                    config.replacePlaceholders(suggestAnn.value())
            );
        }

        studio.mevera.imperat.annotations.SuggestionProvider providerAnn =
                param.getAnnotation(studio.mevera.imperat.annotations.SuggestionProvider.class);
        if (providerAnn != null) {
            try {
                return (SuggestionProvider<S>) config.getInstanceFactory()
                                                       .createInstance(config, providerAnn.value());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to create suggestion provider for parameter: " + param.getName(), e);
            }
        }

        return null;
    }

    private DefaultValueProvider parseDefaultValue(ParameterElement param) {
        if (!param.isOptional()) {
            return DefaultValueProvider.empty();
        }

        Default defaultAnn = param.getAnnotation(Default.class);
        DefaultProvider providerAnn = param.getAnnotation(DefaultProvider.class);

        try {
            return AnnotationHelper.deduceOptionalValueSupplier(
                    param, defaultAnn, providerAnn, DefaultValueProvider.empty());
        } catch (CommandException e) {
            ImperatDebugger.error(AnnotationHelper.class, "deduceOptionalValueSupplier", e);
            return DefaultValueProvider.empty();
        }
    }

    private List<ArgValidator<S>> parseValidators(ParameterElement param) {
        List<ArgValidator<S>> validators = new ArrayList<>();

        // @Validators annotation
        Validators validatorsAnn = param.getAnnotation(Validators.class);
        if (validatorsAnn != null) {
            for (Class<? extends ArgValidator<?>> validatorClass : validatorsAnn.value()) {
                ArgValidator<S> validator = (ArgValidator<S>)
                                                    config.getInstanceFactory().createInstance(config, validatorClass);
                validators.add(validator);
            }
        }

        // @Values annotation (constrained values)
        Values valuesAnn = param.getAnnotation(Values.class);
        if (valuesAnn != null) {
            Set<String> values = Arrays.stream(valuesAnn.value())
                                         .distinct()
                                         .map(config::replacePlaceholders)
                                         .flatMap(replaced -> {
                                             if (replaced.contains("|")) {
                                                 return Arrays.stream(replaced.split(VALUES_SEPARATION_CHAR));
                                             } else {
                                                 return Stream.of(replaced);
                                             }
                                         })
                                         .collect(Collectors.toCollection(LinkedHashSet::new));

            validators.add(new ConstrainedValueValidator<>(values, valuesAnn.caseSensitive()));
        }

        return validators;
    }

}
