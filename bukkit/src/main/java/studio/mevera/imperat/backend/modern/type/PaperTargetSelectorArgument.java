package studio.mevera.imperat.backend.modern.type;

import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.backend.modern.argument.PaperNativeAware;
import studio.mevera.imperat.type.TargetSelectorArgument;

/**
 * Modern-Paper-aware variant of {@link TargetSelectorArgument}. Inherits
 * the Imperat-side selector parser (server-side filter logic, custom
 * {@code [...]} predicate fields) and additionally exposes Paper's
 * {@code ArgumentTypes.entities()} as the Brigadier-native type, so the
 * Mojang client renders {@code @e[type=zombie,name=Bob]} green and
 * provides native selector autocomplete (selector char menu, filter
 * keys, type cycling).
 *
 * @since 4.0.0 (Paper module)
 */
public final class PaperTargetSelectorArgument extends TargetSelectorArgument implements PaperNativeAware {

    @Override
    public @NotNull ArgumentType<?> paperNativeType() {
        return ArgumentTypes.entities();
    }
}
