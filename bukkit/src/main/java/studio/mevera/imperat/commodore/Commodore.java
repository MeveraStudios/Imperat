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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility for using Minecraft's 1.13 'brigadier' library in Bukkit plugins.
 */
public interface Commodore<C extends Command> {

    void register(C command, LiteralCommandNode<?> node, Predicate<? super Player> permissionTest);

    default void register(C command, LiteralArgumentBuilder<?> argumentBuilder, Predicate<? super Player> permissionTest) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        Objects.requireNonNull(permissionTest, "permissionTest");
        register(command, argumentBuilder.build(), permissionTest);
    }

    default void register(C command, LiteralCommandNode<?> node) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(node, "node");
        register(command, node, (p) -> true);
    }

    default void register(C command, LiteralArgumentBuilder<?> argumentBuilder) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        register(command, argumentBuilder.build());
    }

    void register(LiteralCommandNode<?> node);

    default void register(LiteralArgumentBuilder<?> argumentBuilder) {
        Objects.requireNonNull(argumentBuilder, "argumentBuilder");
        register(argumentBuilder.build());
    }

    CommandSender wrapNMSCommandSource(Object nmsCmdSource);
}
