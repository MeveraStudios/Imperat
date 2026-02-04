package com.destroystokyo.paper.event.brigadier;

import com.mojang.brigadier.tree.RootCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class AsyncPlayerSendCommandsEvent<S extends CommandSourceStack> extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final RootCommandNode<S> node;
    private final boolean hasFiredAsync;

    @ApiStatus.Internal
    public AsyncPlayerSendCommandsEvent(Player player, RootCommandNode<S> node, boolean hasFiredAsync) {
        super(player, !Bukkit.isPrimaryThread());
        this.node = node;
        this.hasFiredAsync = hasFiredAsync;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public RootCommandNode<S> getCommandNode() {
        return this.node;
    }

    public boolean hasFiredAsync() {
        return this.hasFiredAsync;
    }

    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
