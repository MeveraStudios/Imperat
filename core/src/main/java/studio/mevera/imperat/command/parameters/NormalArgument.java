package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Description;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.permissions.PermissionsData;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.StringUtils;

class NormalArgument<S extends Source> extends InputParameter<S> {

    NormalArgument(String name,
            ArgumentType<S, ?> type,
            @NotNull PermissionsData permission,
            Description description,
            boolean optional,
            boolean greedy,
            @NotNull DefaultValueProvider valueSupplier,
            @Nullable SuggestionProvider<S> suggestionProvider) {
        super(
                name, type, permission, description, optional,
                false, greedy, valueSupplier, suggestionProvider
        );
    }

    /**
     * Formats the usage parameter
     *
     * @return the formatted parameter
     */
    @Override
    public String format() {
        if (!this.format.equals(this.name)) {
            return super.format();
        }

        var content = getName();
        if (isGreedy()) {
            content += "...";
        }
        return StringUtils.normalizedParameterFormatting(content, isOptional());
    }

}
