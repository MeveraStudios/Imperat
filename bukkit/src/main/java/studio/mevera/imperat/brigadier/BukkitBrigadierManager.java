package studio.mevera.imperat.brigadier;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BaseBrigadierManager;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.commodore.Commodore;
import studio.mevera.imperat.commodore.CommodoreProvider;
import studio.mevera.imperat.resolvers.PermissionChecker;

import static studio.mevera.imperat.commodore.CommodoreProvider.isSupported;

public final class BukkitBrigadierManager extends BaseBrigadierManager<BukkitSource> {

    private final Commodore<org.bukkit.command.Command> commodore;

    public BukkitBrigadierManager(BukkitImperat dispatcher) {
        super(dispatcher);
        this.commodore = CommodoreProvider.getCommodore(dispatcher);
        if (isSupported()) {
            registerArgumentResolver(String.class, DefaultArgTypeResolvers.STRING);
            registerArgumentResolver(DefaultArgTypeResolvers.NUMERIC);
            registerArgumentResolver(Boolean.class, DefaultArgTypeResolvers.BOOLEAN);
            registerArgumentResolver(Player.class, DefaultArgTypeResolvers.PLAYER);
            registerArgumentResolver(OfflinePlayer.class, DefaultArgTypeResolvers.PLAYER);
            registerArgumentResolver(DefaultArgTypeResolvers.ENTITY_SELECTOR);
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

    public void registerBukkitCommand(
            org.bukkit.command.Command bukkitCmd,
            Command<BukkitSource> imperatCommand,
            PermissionChecker<BukkitSource> resolver
    ) {
        commodore.register(bukkitCmd, parseCommandIntoNode(imperatCommand),
                (player) -> resolver.hasPermission(dispatcher.wrapSender(player), bukkitCmd.getPermission()));
    }
}
