package studio.mevera.imperat;

import org.bukkit.Bukkit;
import studio.mevera.imperat.util.reflection.Reflections;

public final class Version {

    public static final String VERSION_EXACT = Bukkit.getBukkitVersion().split("-")[0];
    public static final boolean IS_FOLIA = Reflections.findClass("io.papermc.paper.threadedregions.RegionizedServer");
    public static final boolean IS_PAPER =
            Reflections.findClass("com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration");
    public static final boolean SUPPORTS_PAPER_ASYNC_TAB_COMPLETION =
            Reflections.findClass("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
    public static final int MAJOR, MINOR, PATCH;
    // initialize after IS_PAPER is initialized
    //public static final String NMS = findVersion();

    static {
        final String[] versions = VERSION_EXACT.split("\\.");
        MAJOR = Integer.parseInt(versions[0]);
        MINOR = Integer.parseInt(versions[1]);
        PATCH = versions.length > 2 ? Integer.parseInt(versions[2]) : 0;
    }

    public static boolean is(final int major, final int minor, final int patch) {
        return MAJOR == major && MINOR == minor && PATCH == patch;
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

    public static boolean isOrOver(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isOver(major, minor, patch);
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

    public static boolean isOrBelow(final int major, final int minor, final int patch) {
        return is(major, minor, patch) || isBelow(major, minor, patch);
    }

}
