package studio.mevera.imperat.command;


import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.cooldown.UsageCooldown;

import java.util.concurrent.TimeUnit;

public interface CooldownHolder {

    @Nullable
    UsageCooldown getCooldown();

    void setCooldown(UsageCooldown cooldown);

    default void setCooldown(long value, TimeUnit unit) {
        setCooldown(value, unit, null);
    }

    default void setCooldown(long value, TimeUnit unit, @Nullable String permission) {
        setCooldown(new UsageCooldown(value, unit, permission));
    }

}
