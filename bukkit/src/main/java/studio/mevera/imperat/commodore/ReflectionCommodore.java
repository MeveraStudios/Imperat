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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import studio.mevera.imperat.BukkitUtil;
import studio.mevera.imperat.Version;
import studio.mevera.imperat.util.ImperatDebugger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class ReflectionCommodore extends AbstractCommodore {

    // obc.CraftServer#console field
    private static final Field CONSOLE_FIELD;

    // nms.MinecraftServer#getCommandDispatcher method
    private static final Method GET_COMMAND_DISPATCHER_METHOD;

    // nms.CommandDispatcher#getDispatcher (obfuscated) method
    private static final Method GET_BRIGADIER_DISPATCHER_METHOD;

    // obc.command.BukkitCommandWrapper constructor
    private static final Constructor<?> COMMAND_WRAPPER_CONSTRUCTOR;

    static {
        try {
            /*if (ClassesRefUtil.minecraftVersion() >= 19) {
                throw new UnsupportedOperationException("ReflectionCommodore is not supported on MC 1.19 or above. Switch to Paper :)");
            }*/

            final Class<?> minecraftServer;
            final Class<?> commandDispatcher;

            if (Version.isOver(1, 16, 5)) {
                minecraftServer = BukkitUtil.ClassesRefUtil.mcClass("server.MinecraftServer");
                commandDispatcher = BukkitUtil.ClassesRefUtil.mcClass("commands.CommandDispatcher");
            } else {
                minecraftServer = BukkitUtil.ClassesRefUtil.nmsClass("MinecraftServer");
                commandDispatcher = BukkitUtil.ClassesRefUtil.nmsClass("CommandDispatcher");
            }

            Class<?> craftServer = BukkitUtil.ClassesRefUtil.obcClass("CraftServer");
            CONSOLE_FIELD = craftServer.getDeclaredField("console");
            CONSOLE_FIELD.setAccessible(true);

            GET_COMMAND_DISPATCHER_METHOD = Arrays.stream(minecraftServer.getDeclaredMethods())
                    .filter(method -> method.getParameterCount() == 0)
                    .filter(method -> commandDispatcher.isAssignableFrom(method.getReturnType()))
                    .findFirst().orElseThrow(NoSuchMethodException::new);
            GET_COMMAND_DISPATCHER_METHOD.setAccessible(true);

            GET_BRIGADIER_DISPATCHER_METHOD = Arrays.stream(commandDispatcher.getDeclaredMethods())
                    .filter(method -> method.getParameterCount() == 0)
                    .filter(method -> CommandDispatcher.class.isAssignableFrom(method.getReturnType()))
                    .findFirst().orElseThrow(NoSuchMethodException::new);
            GET_BRIGADIER_DISPATCHER_METHOD.setAccessible(true);

            Class<?> commandWrapperClass = BukkitUtil.ClassesRefUtil.obcClass("command.BukkitCommandWrapper");
            COMMAND_WRAPPER_CONSTRUCTOR = commandWrapperClass.getConstructor(craftServer, Command.class);

        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Plugin plugin;
    private final List<LiteralCommandNode<?>> registeredNodes = new ArrayList<>();

    ReflectionCommodore(Plugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(new ServerReloadListener(this), this.plugin);
    }

    static void ensureSetup() {
        // do nothing - this is only called to trigger the static initializer
    }

    private CommandDispatcher<?> getDispatcher() {
        try {
            Object mcServerObject = CONSOLE_FIELD.get(Bukkit.getServer());
            Object commandDispatcherObject = GET_COMMAND_DISPATCHER_METHOD.invoke(mcServerObject);
            return (CommandDispatcher<?>) GET_BRIGADIER_DISPATCHER_METHOD.invoke(commandDispatcherObject);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void register(LiteralCommandNode<?> node) {
        Objects.requireNonNull(node, "node");

        CommandDispatcher dispatcher = getDispatcher();
        RootCommandNode root = dispatcher.getRoot();

        removeChild(root, node.getName());
        root.addChild(node);
        this.registeredNodes.add(node);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void register(Command command, LiteralCommandNode<?> node, Predicate<? super Player> permissionTest) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(permissionTest, "permissionTest");

        try {
            SuggestionProvider<?> wrapper = (SuggestionProvider<?>) COMMAND_WRAPPER_CONSTRUCTOR.newInstance(this.plugin.getServer(), command);
            setRequiredHackyFieldsRecursively(node, wrapper);
        } catch (Throwable ex) {
            ImperatDebugger.error(ReflectionCommodore.class, "register(Command, LiteralCommandNode<?>, Predicate<? super Player>", ex);
        }

        Collection<String> aliases = getAliases(command);
        if (!aliases.contains(node.getLiteral())) {
            node = renameLiteralNode(node, command.getName());
        }

        for (String alias : aliases) {
            if (node.getLiteral().equals(alias)) {
                register(node);
            } else {
                register(LiteralArgumentBuilder.literal(alias).redirect((LiteralCommandNode<Object>) node).build());
            }
        }

        this.plugin.getServer().getPluginManager().registerEvents(new CommandDataSendListener(command, permissionTest), this.plugin);
    }

    /**
     * Listens for server (re)loads, and re-adds all registered nodes to the dispatcher.
     */
    private record ServerReloadListener(ReflectionCommodore commodore) implements Listener {

        @SuppressWarnings({"rawtypes", "unchecked"})
        @EventHandler
        public void onLoad(ServerLoadEvent e) {
            if (e.getType() == ServerLoadEvent.LoadType.STARTUP) {
                return;
            }
            CommandDispatcher dispatcher = this.commodore.getDispatcher();
            RootCommandNode root = dispatcher.getRoot();

            for (LiteralCommandNode<?> node : this.commodore.registeredNodes) {
                removeChild(root, node.getName());
                root.addChild(node);
            }
        }
    }

    /**
     * Removes minecraft namespaced argument data, & data for players without permission to view the
     * corresponding commands.
     */
    private static final class CommandDataSendListener implements Listener {
        private final Set<String> aliases;
        private final Set<String> minecraftPrefixedAliases;
        private final Predicate<? super Player> permissionTest;

        CommandDataSendListener(Command pluginCommand, Predicate<? super Player> permissionTest) {
            this.aliases = new HashSet<>(getAliases(pluginCommand));
            this.minecraftPrefixedAliases = this.aliases.stream().map(alias -> "minecraft:" + alias).collect(Collectors.toSet());
            this.permissionTest = permissionTest;
        }

        @EventHandler
        public void onCommandSend(PlayerCommandSendEvent e) {
            // always remove 'minecraft:' prefixed aliases added by craftbukkit.
            // this happens because bukkit thinks our injected commands are vanilla commands.
            e.getCommands().removeAll(this.minecraftPrefixedAliases);

            // remove the actual aliases if the player doesn't pass the permission test
            if (!this.permissionTest.test(e.getPlayer())) {
                e.getCommands().removeAll(this.aliases);
            }
        }
    }

}
