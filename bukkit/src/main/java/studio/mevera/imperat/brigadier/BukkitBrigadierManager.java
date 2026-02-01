package studio.mevera.imperat.brigadier;

import static studio.mevera.imperat.commodore.CommodoreProvider.isSupported;

import com.mojang.brigadier.arguments.ArgumentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.commodore.Commodore;
import studio.mevera.imperat.commodore.CommodoreProvider;
import studio.mevera.imperat.resolvers.PermissionChecker;

public final class BukkitBrigadierManager extends BaseBrigadierManager<BukkitSource> {

    private final Commodore<org.bukkit.command.Command> commodore;

    public BukkitBrigadierManager(BukkitImperat dispatcher) {
        super(dispatcher);
        this.commodore = CommodoreProvider.getCommodore(dispatcher);
        if (isSupported()) {
//            registerArgumentResolver(String.class, DefaultArgTypeResolvers.STRING);
//            registerArgumentResolver(DefaultArgTypeResolvers.NUMERIC);
//            registerArgumentResolver(Boolean.class, DefaultArgTypeResolvers.BOOLEAN);
//            registerArgumentResolver(Player.class, DefaultArgTypeResolvers.PLAYER);
//            registerArgumentResolver(OfflinePlayer.class, DefaultArgTypeResolvers.PLAYER);
//            registerArgumentResolver(DefaultArgTypeResolvers.ENTITY_SELECTOR);

            for(MinecraftArgumentType type : MinecraftArgumentType.values()) {
                if(type.isSupported()) {
                    dispatcher.config().registerParamType(type.getParsedType(), new BukkitParameterType<>(type.getParsedType(), type));
                }
            }
        }

    }

    public static @Nullable BukkitBrigadierManager load(BukkitImperat bukkitCommandDispatcher) {
        if (!isSupported()) {
            return null;
        }
        return new BukkitBrigadierManager(bukkitCommandDispatcher);
    }

    @Override
    public BukkitSource wrapCommandSource(Object commandSource) {
        return dispatcher.wrapSender(commodore.wrapNMSCommandSource(commandSource));
    }

    @Override
    public @NotNull ArgumentType<?> getArgumentType(CommandParameter<BukkitSource> parameter) {
        var paramType = parameter.type();
        if(paramType instanceof BukkitParameterType<?> bukkitParamType) {
            if(bukkitParamType.isSupported()) {
                return bukkitParamType.getArgType();
            }else {
                throw new IllegalStateException("The parameter type " + paramType.getClass().getName() + " is not supported in the current server version.");
            }
        }
        return getStringArgType(parameter);
    }

    public void registerBukkitCommand(
            org.bukkit.command.Command bukkitCmd,
            Command<BukkitSource> imperatCommand,
            PermissionChecker<BukkitSource> resolver
    ) {
        commodore.register(bukkitCmd, parseCommandIntoNode(imperatCommand),
                (player) -> resolver.hasPermission(dispatcher.wrapSender(player), bukkitCmd.getPermission()));
    }
}
