package io.papermc.paper.command.brigadier;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface CommandSourceStack {

    Location getLocation();

    CommandSender getSender();

    @Nullable
    Entity getExecutor();
}
