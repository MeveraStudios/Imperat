package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

/**
 * Represents the class responsible for defining how permissions
 * are loaded/deduced from a {@link CommandParameter}
 * @param <S> the source type.
 */
public interface PermissionLoader<S extends Source> {
    
    /**
     * Deduces a permission for a command parameter.
     * @param commandParameter the command parameter.
     * @return The permission for this parameter.
     */
    @Nullable String load(@NotNull CommandParameter<S> commandParameter);
    
    static <S extends Source> PermissionLoader<S> defaultLoader() {
        return (param)-> {
            if(param.getSinglePermission() == null) {
                return param.name();
            }
            return param.getSinglePermission();
        };
    }
    
}
