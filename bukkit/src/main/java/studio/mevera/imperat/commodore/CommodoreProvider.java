/*
 * This file is part of commodore, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package studio.mevera.imperat.commodore;

import org.bukkit.command.Command;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.BukkitImperat;
import studio.mevera.imperat.util.ImperatDebugger;

import java.util.Objects;

/**
 * Factory for getting instances of {@link Commodore}.
 */
public final class CommodoreProvider {

    private static boolean SUPPORTED;

    static {
        try {
            Class.forName("com.mojang.brigadier.CommandDispatcher");
            SUPPORTED = true;
        } catch (Throwable e) {
            if (System.getProperty("commodore.visualize") != null) {
                ImperatDebugger.error(CommodoreProvider.class, "static<init>", e);
            }
            SUPPORTED = false;
        }
    }

    private CommodoreProvider() {
        throw new AssertionError();
    }

    private static @Nullable Commodore<Command> load(BukkitImperat imperat) {
        if (!SUPPORTED) {
            return null;
        }

        Plugin plugin = imperat.getPlatform();

        try {
            return new ModernPaperCommodore(plugin);
        } catch (Throwable e) {
            printDebugInfo(imperat, e);
        }

        // try the paper impl
        try {
            return new LegacyPaperCommodore(imperat);
        } catch (Throwable e) {
            //printDebugInfo(e);
            ImperatDebugger.warning("Paper not found, falling back to Reflection for brigadier");
        }

        // try reflection impl
        try {
            ReflectionCommodore.ensureSetup();
            return new ReflectionCommodore(plugin);
        } catch (Throwable e) {
            printDebugInfo(imperat, e);
        }

        return null;
    }

    private static void printDebugInfo(BukkitImperat imperat, Throwable e) {
        if (System.getProperty("commodore.visualize") != null) {
            System.err.println("Exception while initialising commodore:");
            imperat.config().getThrowablePrinter().print(e);
        }
    }

    /**
     * Checks to see if the Brigadier command system is supported by the server.
     *
     * @return true if commodore is supported.
     */
    public static boolean isSupported() {
        return SUPPORTED;
    }

    /**
     * Obtains a {@link Commodore} instance for the given Imperat dispatcher.
     *
     * @param imperat the BukkitImperat dispatcher
     * @return the commodore instance
     * @throws BrigadierUnsupportedException if brigadier is not {@link #isSupported() supported}
     *                                       by the server.
     */
    public static Commodore<Command> getCommodore(BukkitImperat imperat) throws BrigadierUnsupportedException {
        Objects.requireNonNull(imperat, "imperat");
        Commodore<Command> commodore = load(imperat);
        if (commodore == null) {
            throw new BrigadierUnsupportedException(
                    "Brigadier is not supported by the server. " +
                            "Set -Dcommodore.visualize=true for visualize info."
            );
        }
        return commodore;
    }

}
