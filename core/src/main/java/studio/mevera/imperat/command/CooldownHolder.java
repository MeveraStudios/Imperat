package studio.mevera.imperat.command;


import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.cooldown.CooldownRecord;

import java.util.concurrent.TimeUnit;

public interface CooldownHolder {

    @Nullable
    CooldownRecord getCooldown();

    void setCooldown(CooldownRecord cooldown);

    default void setCooldown(long value, TimeUnit unit) {
        setCooldown(value, unit, null);
    }

    default void setCooldown(long value, TimeUnit unit, @Nullable String permission) {
        setCooldown(new CooldownRecord(value, unit, permission));
    }

}
