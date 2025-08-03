package studio.mevera.imperat.command.cooldown;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a required cooldown of a {@link CommandUsage}
 *
 * @see TimeUnit
 * @see Duration
 */
@ApiStatus.AvailableSince("1.0.0")
public final class UsageCooldown {

    private final Duration duration;
    private final long value;
    private final TimeUnit unit;
    private final @Nullable String permission;

    /**
     * @param value the value for this
     * @param unit  the unit for the time
     *
     */
    public UsageCooldown(long value, TimeUnit unit, @Nullable String permission) {
        this.value = value;
        this.unit = unit;
        this.permission = permission;
        this.duration = Duration.of(value, unit.toChronoUnit());
    }

    public Duration toDuration() {
        return duration;
    }

    public long value() {
        return value;
    }

    public TimeUnit unit() {
        return unit;
    }

    public @Nullable String permission() {
        return permission;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UsageCooldown) obj;
        return this.value == that.value &&
                Objects.equals(this.unit, that.unit) &&
                Objects.equals(this.permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit, permission);
    }

}
