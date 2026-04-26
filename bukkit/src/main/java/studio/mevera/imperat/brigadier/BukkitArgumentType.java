package studio.mevera.imperat.brigadier;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;

public class BukkitArgumentType<T> extends ArgumentType<BukkitCommandSource, T> {

    private final MinecraftArgumentType minecraftArgumentType;
    private final com.mojang.brigadier.arguments.ArgumentType<T> argumentType;

    public BukkitArgumentType(Class<T> type, @NotNull MinecraftArgumentType minecraftArgumentType) {
        super(type);
        this.minecraftArgumentType = minecraftArgumentType;
        this.argumentType = minecraftArgumentType.get();
    }

    //for arg types that require parameters
    public BukkitArgumentType(Class<T> type, @NotNull MinecraftArgumentType minecraftArgumentType, com.mojang.brigadier.arguments.ArgumentType<T> argumentType) {
        super(type);
        this.minecraftArgumentType = minecraftArgumentType;
        this.argumentType = argumentType;
    }

    @Override
    public T parse(@NotNull CommandContext<BukkitCommandSource> context, @NonNull Argument<BukkitCommandSource> argument, @NotNull String input)
            throws CommandException {
        // Multi-token brigadier types (e.g. BlockPos: "x y z") are pre-joined
        // into {@code input} by the command tree because of the
        // {@link #getNumberOfParametersToConsume(Argument)} override below.
        try {
            return argumentType.parse(new com.mojang.brigadier.StringReader(input));
        } catch (CommandSyntaxException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<BukkitCommandSource> argument) {
        return Math.max(1, minecraftArgumentType.getConsumedArgs());
    }

    public MinecraftArgumentType getMinecraftArgumentType() {
        return minecraftArgumentType;
    }

    public com.mojang.brigadier.arguments.ArgumentType<T> getArgType() {
        return argumentType;
    }

    public boolean isSupported() {
        return this.minecraftArgumentType.isSupported();
    }
}
