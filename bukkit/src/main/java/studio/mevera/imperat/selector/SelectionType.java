package studio.mevera.imperat.selector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unchecked")
public interface SelectionType {

    String MENTION_CHARACTER = "@";

    String id();

    @NotNull <E extends Entity> List<E> getTargetEntities(
        @NotNull ExecutionContext<BukkitSource> context,
        @NotNull CommandInputStream<BukkitSource> commandInputStream
    ) throws ImperatException;


    SelectionType COMMAND_EXECUTOR = new SelectionType() {
        @Override
        public String id() {
            return "s";
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E extends Entity> @NotNull List<E> getTargetEntities(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream
        ) throws ImperatException {
            if (context.source().isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return List.of((E) context.source().asPlayer());
        }

    };

    SelectionType CLOSEST_PLAYER = new SelectionType() {
        @Override
        public String id() {
            return "p";
        }

        @Override
        public @NotNull <E extends Entity> List<E> getTargetEntities(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream
        ) throws ImperatException {
            if (context.source().isConsole()) {
                throw new OnlyPlayerAllowedException();
            }

            Player sender = context.source().asPlayer();
            Location location = sender.getLocation();
            double MIN_DISTANCE = Double.MAX_VALUE;
            Player target = sender;

            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(sender.getUniqueId())) {
                    continue;
                }
                double distance = other.getLocation().distanceSquared(location);
                if (distance < MIN_DISTANCE) {
                    MIN_DISTANCE = distance;
                    sender = other;
                }
            }

            return List.of((E) target);
        }

    };//@p

    SelectionType RANDOM_PLAYER = new SelectionType() {
        @Override
        public String id() {
            return "r";
        }

        @Override
        public @NotNull <E extends Entity> List<E> getTargetEntities(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream
        ) throws ImperatException {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            return List.of((E) onlinePlayers.get(ThreadLocalRandom.current().nextInt(onlinePlayers.size())));
        }

    }; //@r

    SelectionType ALL_PLAYERS = new SelectionType() {
        @Override
        public String id() {
            return "a";
        }

        @Override
        public @NotNull <E extends Entity> List<E> getTargetEntities(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream
        ) throws ImperatException {
            return (List<E>) new ArrayList<>(Bukkit.getOnlinePlayers());
        }


    }; //@a (parameterized)

    SelectionType ALL_ENTITIES = new SelectionType() {
        @Override
        public String id() {
            return "e";
        }

        @Override
        public @NotNull <E extends Entity> List<E> getTargetEntities(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream
        ) throws ImperatException {
            if (context.source().isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            Player player = context.source().asPlayer();
            World world = player.getWorld();

            return (List<E>) world.getEntities();
        }

    }; //@e (parameterized)

    SelectionType UNKNOWN = new SelectionType() {
        @Override
        public String id() {
            return "unknown";
        }

        @Override
        public @NotNull <E extends Entity> List<E> getTargetEntities(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream
        ) throws ImperatException {
            return List.of();
        }

    };


    List<SelectionType> TYPES = List.of(
        COMMAND_EXECUTOR,
        CLOSEST_PLAYER,
        RANDOM_PLAYER,
        ALL_ENTITIES,
        ALL_PLAYERS
    );

    static @NotNull SelectionType from(String id) {
        for (var type : TYPES)
            if (type.id().equalsIgnoreCase(id))
                return type;
        return SelectionType.UNKNOWN;
    }

}
