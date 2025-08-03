package studio.mevera.imperat.command.suggestions;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.lang.reflect.Type;
import java.util.*;

@ApiStatus.Internal
public final class SuggestionResolverRegistry<S extends Source> {

    private final Map<String, SuggestionResolver<S>> resolversPerName;

    private final EnumSuggestionResolver enumSuggestionResolver = new EnumSuggestionResolver();
    private final FlagSuggestionResolver flagSuggestionResolver = new FlagSuggestionResolver();

    private final ImperatConfig<S> imperat;

    private SuggestionResolverRegistry(ImperatConfig<S> imperat) {
        super();
        this.imperat = imperat;
        resolversPerName = new HashMap<>();
    }

    public static <S extends Source> SuggestionResolverRegistry<S> createDefault(ImperatConfig<S> imperat) {
        return new SuggestionResolverRegistry<>(imperat);
    }

    public FlagSuggestionResolver getFlagSuggestionResolver() {
        return flagSuggestionResolver;
    }

    public EnumSuggestionResolver getEnumSuggestionResolver() {
        return enumSuggestionResolver;
    }

    public void registerNamedResolver(String name,
                                      SuggestionResolver<S> suggestionResolver) {
        resolversPerName.put(name, suggestionResolver);
    }

    public @Nullable SuggestionResolver<S> getResolverByName(String name) {
        return resolversPerName.get(name);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public final class EnumSuggestionResolver implements SuggestionResolver<S> {
        private final Map<Type, List<String>> PRE_LOADED_ENUMS = new HashMap<>();

        public void registerEnumResolver(Type raw) {
            Class<Enum> enumClass = (Class<Enum>) raw;
            PRE_LOADED_ENUMS.computeIfAbsent(raw,
                (v) -> Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList());
        }

        private Optional<List<String>> getResults(Type type) {
            return Optional.ofNullable(PRE_LOADED_ENUMS.get(type));
        }

        @Override
        public List<String> autoComplete(SuggestionContext<S> context, CommandParameter<S> parameter) {
            Type type = parameter.valueType();
            return getResults(type)
                .orElseGet(() -> {
                    registerEnumResolver(type);
                    return getResults(type).orElse(Collections.emptyList());
                });
        }
    }

    public final class FlagSuggestionResolver implements SuggestionResolver<S> {


        @Override
        public List<String> autoComplete(SuggestionContext<S> context, CommandParameter<S> parameter) {
            assert parameter.isFlag();
            FlagParameter<S> flagParameter = parameter.asFlagParameter();
            CompletionArg arg = context.getArgToComplete();
            FlagData<S> data = flagParameter.flagData();

            if (flagParameter.isSwitch()) {
                //normal one arg
                return autoCompleteFlagNames(data);
            }

            //normal flag with a value next!
            int paramPos = parameter.position();
            int argPos = arg.index();
            if (argPos > paramPos) {
                //auto-complete the value for the flag
                var inputType = imperat.getParameterType(data.inputType().type());
                SuggestionResolver<S> flagInputResolver = inputType == null ? null : inputType.getSuggestionResolver();
                //flag parameter's suggestion resolver is the same resolver for its data input.
                if (flagInputResolver == null)
                    flagInputResolver = flagParameter.inputSuggestionResolver();

                if (flagInputResolver == null) return List.of();
                else return flagInputResolver.autoComplete(context, parameter);
            } else {
                //auto-complete the flag-names
                return autoCompleteFlagNames(data);
            }

        }

        private List<String> autoCompleteFlagNames(FlagData<S> data) {
            List<String> results = new ArrayList<>();
            results.add("-" + data.name());
            for (var alias : data.aliases()) {
                results.add("-" + alias);
            }
            return results;
        }
    }

}
