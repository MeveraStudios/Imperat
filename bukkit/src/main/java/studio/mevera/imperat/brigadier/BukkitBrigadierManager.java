package studio.mevera.imperat.brigadier;

import static studio.mevera.imperat.commodore.CommodoreProvider.isSupported;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.commodore.Commodore;
import studio.mevera.imperat.commodore.CommodoreProvider;
import studio.mevera.imperat.permissions.PermissionChecker;
import studio.mevera.imperat.selector.TargetSelector;

import java.lang.reflect.Type;

public final class BukkitBrigadierManager extends BaseBrigadierManager<BukkitCommandSource> {

    private final Commodore<org.bukkit.command.Command> commodore;

    public BukkitBrigadierManager(BukkitImperat dispatcher) {
        super(dispatcher);
        this.commodore = CommodoreProvider.getCommodore(dispatcher);
        if (isSupported()) {

            for (MinecraftArgumentType type : MinecraftArgumentType.values()) {
                if (type.isSupported() && !type.requiresParameters()) {
                    var parsedType = type.getParsedType();
                    if (parsedType != null) {
                        registerAsArgumentType(type.getParsedType(), type);
                    }
                }
            }
            //we manually register the entity selector types as they require parameters
            registerAsArgumentType(Player.class, MinecraftArgumentType.ENTITY_SELECTOR, true, true);
            registerAsArgumentType(OfflinePlayer.class, MinecraftArgumentType.ENTITY_SELECTOR, true, true);
            registerAsArgumentType(TargetSelector.class, MinecraftArgumentType.ENTITY_SELECTOR, false, false);
        }

    }

    public static @Nullable BukkitBrigadierManager load(BukkitImperat bukkitCommandDispatcher) {
        if (!isSupported()) {
            return null;
        }
        return new BukkitBrigadierManager(bukkitCommandDispatcher);
    }

    private void registerAsArgumentType(Type type, MinecraftArgumentType argumentType) {
        dispatcher.config().registerArgType(type, new BukkitArgumentType<>(argumentType.getParsedType(), argumentType));
    }

    private void registerAsArgumentType(Type type, MinecraftArgumentType argumentType, Object... params) {
        dispatcher.config()
                .registerArgType(type, new BukkitArgumentType<>(argumentType.getParsedType(), argumentType, argumentType.create(params)));
    }

    @Override
    public BukkitCommandSource wrapCommandSource(Object commandSource) {
        return dispatcher.wrapSender(commodore.wrapNMSCommandSource(commandSource));
    }

    @Override
    public @NotNull ArgumentType<?> getArgumentType(Argument<BukkitCommandSource> imperatArgument) {
        var paramType = imperatArgument.type();
        if (paramType instanceof BukkitArgumentType<?> bukkitParamType) {
            if (bukkitParamType.isSupported()) {
                return bukkitParamType.getArgType();
            } else {
                throw new IllegalStateException(
                        "The parameter type " + paramType.getClass().getName() + " is not supported in the current server version.");
            }
        } else if (imperatArgument.isNumeric()) {
            return NumericArgUtil.numeric(imperatArgument.valueType(), imperatArgument.asNumeric().getRange());
        }
        return getStringArgType(imperatArgument);
    }

    public <T> void registerBukkitCommand(
            org.bukkit.command.Command bukkitCmd,
            Command<BukkitCommandSource> imperatCommand,
            PermissionChecker<BukkitCommandSource> resolver
    ) {
        LiteralCommandNode<T> brigRootNode = parseCommandIntoNode(imperatCommand);
        commodore.register(bukkitCmd, brigRootNode,
                (player) -> resolver.hasPermission(dispatcher.wrapSender(player), bukkitCmd.getPermission()));

        //BUG-FIX: registering root-level aliases using the redirect feature
        imperatCommand.aliases()
                .stream()
                .map((alias) -> LiteralArgumentBuilder.<T>literal(alias)
                                        .executes(brigRootNode.getCommand())
                                        .requires(brigRootNode.getRequirement())
                                        .redirect(brigRootNode)
                                        .build()
                )
                .forEach((aliasNode) -> commodore.register(bukkitCmd, aliasNode,
                        (player) -> resolver.hasPermission(dispatcher.wrapSender(player), bukkitCmd.getPermission())));
    }
}
