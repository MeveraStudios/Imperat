package studio.mevera.imperat;

import org.bukkit.Bukkit;
import studio.mevera.imperat.util.reflection.Reflections;

public final class Version {

    public static final String VERSION_EXACT = Bukkit.getBukkitVersion().split("-")[0];
    public static final boolean IS_FOLIA = Reflections.findClass("io.papermc.paper.threadedregions.RegionizedServer");
    public static final boolean IS_PAPER = Reflections.findClass("com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration");

    public static final int MAJOR, MINOR, PATCH;

    static {
        final String[] versions = VERSION_EXACT.split("\\.");
        MAJOR = Integer.parseInt(versions[0]);
        MINOR = Integer.parseInt(versions[1]);
        PATCH = versions.length > 2 ? Integer.parseInt(versions[2]) : 0;
    }

    // initialize after IS_PAPER is initialized
    public static final String NMS = findVersion();
    public static final boolean IS_13_R2_PLUS = isOrOver(1, 13, 2);
    public static final boolean IS_20_R2_PLUS = isOrOver(1, 20, 2);
    public static final boolean IS_20_R4_PLUS = isOrOver(1, 20, 5);

    public static boolean is(final int minor) {
        return MINOR == minor;
    }

    public static boolean is(final int major, final int minor, final int patch) {
        return MAJOR == major && MINOR == minor && PATCH == patch;
    }

    public static boolean isOver(final int minor) {
        return MINOR > minor;
    }

    public static boolean isOver(final int major, final int minor, final int patch) {
        if (MAJOR > major) {
            return true;
        } else if (MAJOR == major) {
            if (MINOR > minor) {
                return true;
            } else if (MINOR == minor) {
                return PATCH > patch;
            }
        }
        return false;
    }

    public static boolean isOrOver(final int minor) {
        return MINOR >= minor;
    }

    public static boolean isOrOver(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isOver(major, minor, patch);
    }

    public static boolean isBelow(final int minor) {
        return MINOR < minor;
    }

    public static boolean isBelow(final int major, final int minor, final int patch) {
        if (MAJOR < major) {
            return true;
        } else if (MAJOR == major) {
            if (MINOR < minor) {
                return true;
            } else if (MINOR == minor) {
                return PATCH < patch;
            }
        }
        return false;
    }

    public static boolean isOrBelow(final int minor) {
        return MINOR <= minor;
    }

    public static boolean isOrBelow(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isBelow(major, minor, patch);
    }

    private static String findVersion() {
        if (IS_PAPER && MINOR >= 20) {
            return switch (VERSION_EXACT) {
                case "1.20", "1.20.1" -> "1_20_R1";
                case "1.20.2", "1.20.3" -> "1_20_R2";
                case "1.20.4" -> "1_20_R3";
                case "1.20.5", "1.20.6" -> "1_20_R4";
                case "1.21", "1.21.1" -> "1_21_R1";
                case "1.21.2", "1.21.3" -> "1_21_R2";
                case "1.21.4" -> "1_21_R3";
                case "1.21.5" -> "1_21_R4";
                case "1.21.6", "1.21.7" -> "1_21_R5";
                default -> "UNKNOWN";
            };
        }
        return Bukkit.getServer().getClass().getPackage().getName().substring(24);
    }

}
