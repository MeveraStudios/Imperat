package studio.mevera.imperat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.reflection.FieldAccessor;
import studio.mevera.imperat.util.reflection.Reflections;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BukkitUtil {
    
    public final static Pattern PLAYER_USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    public final static Pattern SPLIT_LINE = Pattern.compile(" ", Pattern.LITERAL);
    public final static FieldAccessor<SimpleCommandMap> COMMAND_MAP_ACCESSOR = Reflections.getField(Bukkit.getServer().getClass(), SimpleCommandMap.class);
    public static CommandMap COMMAND_MAP;
    public static @Nullable Field KNOWN_COMMANDS;

    static {
        try {
            loadBukkitFieldMappings();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            ImperatDebugger.error(BukkitImperat.class, "static-init",
                e, "Failed to fetch bukkit command-map, disabling plugin");
        }
    }

    private BukkitUtil() {
    }

    private static void loadBukkitFieldMappings() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException  {
        COMMAND_MAP = COMMAND_MAP_ACCESSOR.get(Bukkit.getServer());
        updateCommandActualMap();
    }

    public static void writeCommandMapInstance(SimpleCommandMap commandMap) throws NoSuchFieldException {
        COMMAND_MAP_ACCESSOR.set(Bukkit.getServer(), commandMap);
        COMMAND_MAP = commandMap;
        updateCommandActualMap();
    }

    private static void updateCommandActualMap() throws NoSuchFieldException {
        if (COMMAND_MAP != null) {
            KNOWN_COMMANDS = SimpleCommandMap.class.getDeclaredField("knownCommands");
            KNOWN_COMMANDS.setAccessible(true);
        }
    }

    public static final class ClassesRefUtil {
        private static final String SERVER_VERSION = getServerVersion();

        private ClassesRefUtil() {
        }

        private static String getServerVersion() {
            Class<?> server = Bukkit.getServer().getClass();
            if (!server.getSimpleName().equals("CraftServer")) {
                return ".";
            }
            if (server.getName().equals("org.bukkit.craftbukkit.CraftServer")) {
                // Non versioned class
                return ".";
            } else {
                String version = server.getName().substring("org.bukkit.craftbukkit".length());
                return version.substring(0, version.length() - "CraftServer".length());
            }
        }

        public static String mc(String name) {
            return "net.minecraft." + name;
        }

        public static String nms(String className) {
            return "net.minecraft.server" + SERVER_VERSION + className;
        }

        public static Class<?> mcClass(String className) throws ClassNotFoundException {
            return Class.forName(mc(className));
        }

        public static Class<?> nmsClass(String className) throws ClassNotFoundException {
            return Class.forName(nms(className));
        }

        public static String obc(String className) {
            return "org.bukkit.craftbukkit" + SERVER_VERSION + className;
        }

        public static Class<?> obcClass(String className) throws ClassNotFoundException {
            return Class.forName(obc(className));
        }

        public static int minecraftVersion() {
            try {
                final Matcher matcher = Pattern.compile("\\(MC: (\\d)\\.(\\d+)\\.?(\\d+?)?( .*)?\\)").matcher(Bukkit.getVersion());
                if (matcher.find()) {
                    return Integer.parseInt(matcher.toMatchResult().group(2), 10);
                } else {
                    throw new IllegalArgumentException(String.format("No match found in '%s'", Bukkit.getVersion()));
                }
            } catch (final IllegalArgumentException ex) {
                throw new RuntimeException("Failed to determine Minecraft version", ex);
            }
        }

    }

    public static <T> Set<T> mergedSet(Set<T> set1, Set<T> set2, Supplier<Set<T>> supplier) {
        Set<T> merged = supplier.get();
        merged.addAll(set1);
        merged.addAll(set2);
        return merged;
    }

    @ApiStatus.Experimental
    public static Command<BukkitSource> convertBukkitCmdToImperatCmd(Imperat<BukkitSource> imperat, org.bukkit.command.Command bukkitCmd) {
        return Command.create(imperat, bukkitCmd.getName())
                .aliases(bukkitCmd.getAliases())
                .description(bukkitCmd.getDescription())
                .permission(bukkitCmd.getPermission())
                //TODO add forced syntax format
                .usage(CommandUsage.<BukkitSource>builder()
                        .parameters(CommandParameter.requiredGreedy("args"))
                        .execute((source, context)-> {
                            String argsOneLine = context.getArgument("args");
                            String[] args = SPLIT_LINE.split(argsOneLine);
                            bukkitCmd.execute(source.origin(), context.label(), args);
                        })
                )
                .build();
    }
}
