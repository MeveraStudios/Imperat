package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class ImperatDebugger {

    private static @NotNull Logger LOGGER = Logger.getLogger("IMPERAT");
    private static boolean enabled = false;
    private static boolean testing = false;

    private static boolean usingTestCases = false;

    private ImperatDebugger() {
    }

    public static boolean isUsingTestCases() {
        return usingTestCases;
    }

    public static void setUsingTestCases(boolean usingTestCases) {
        ImperatDebugger.usingTestCases = usingTestCases;
    }

    public static void setEnabled(boolean enabled) {
        ImperatDebugger.enabled = enabled;
    }

    public static boolean isTesting() {
        return testing;
    }

    public static void setTesting(boolean testing) {
        ImperatDebugger.testing = testing;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setLogger(Logger LOGGER) {
        ImperatDebugger.LOGGER = LOGGER;
    }

    public static void debug(String msg, Object... args) {
        if (!enabled) return;
        if(testing || usingTestCases) {
            System.out.println(String.format("INFO > " + msg, args));
        }else {
            LOGGER.log(Level.INFO, () -> String.format(msg, args));
        }
    }

    public static void debugForTesting(String msg, Object... args) {
        if (!enabled || !testing) return;
        System.out.println(String.format("TEST-INFO > " + msg, args));
    }

    public static void warning(String msg, Object... args) {
        if (!enabled) return;
        if(testing) {
            System.out.println(String.format("WARNING > " + msg, args));
        }else {
            LOGGER.log(Level.WARNING, () -> String.format(msg, args));
        }
    }

    public static void error(Class<?> owningClass, String method, @NotNull Throwable ex) {
        LOGGER.log(Level.SEVERE, ex, ()-> String.format("Error in class '%s', in method '%s'", owningClass.getName(), method));
    }

    public static void error(Class<?> owningClass, String method, Throwable ex, String message) {
        LOGGER.log(Level.SEVERE, ex, ()-> String.format("Error in class '%s', in method '%s' due to '%s'", owningClass.getName(), method, message));
    }

    public static <S extends Source> void debugParameters(String msg, List<CommandParameter<S>> parameters) {
        if (!enabled) return;
        LOGGER.log(Level.INFO, ()-> String.format(msg, parameters.stream().map(CommandParameter::format)
            .collect(Collectors.joining(","))));
    }

}
