package studio.mevera.imperat.tests.parameters;

import java.util.Date;

/**
 * A utility class representing a duration.
 * <p>
 * The duration is defined by its creation time and expiration time.
 * It provides methods to check if it has expired, extend its duration, and retrieve its creation date.
 * </p>
 */
public class CustomDuration {

    private final long creationTime;
    private long expirary;

    /**
     * Constructs a new CustomDuration object with the given expiration time.
     *
     * @param expirary the expiration time in milliseconds
     */
    public CustomDuration(final long expirary) {
        this.creationTime = System.currentTimeMillis();
        this.expirary = expirary;
    }

    /**
     * Checks if the duration has expired.
     *
     * @return true if the duration has expired, otherwise false
     */
    public boolean hasExpired() {
        return !isPermanent() && System.currentTimeMillis() >= (creationTime + expirary);
    }

    /**
     * Checks if the duration is permanent.
     *
     * @return true if the duration is permanent, otherwise false
     */
    public boolean isPermanent() {
        return expirary == -1;
    }

    /**
     * Extends the duration by the given time.
     *
     * @param time the time to extend the duration by in milliseconds
     * @return true if the extension was successful, otherwise false
     */
    public boolean extend(final long time) {
        if (isPermanent() || hasExpired()) {
            return false;
        }
        expirary += time;
        return true;
    }

    /**
     * Retrieves the creation date of the duration.
     *
     * @return the creation date as a Date object
     */
    public Date creationDate() {
        return new Date(creationTime);
    }

    /**
     * Returns the time left for the duration to expire.
     *
     * @param big true to return time in a larger unit, false otherwise
     * @return a string representing the time left for the duration to expire
     */
    public String timeLeft(boolean big) {
        if (hasExpired()) {
            return "Expired";
        }

        if (isPermanent()) {
            return "Permanent";
        }

        long currentTimeMs = System.currentTimeMillis();
        long timeLeftMs = expirary - (currentTimeMs - creationTime);

        return TimeUtil.calculateTimeLeft(timeLeftMs, big);
    }


    /**
     * Returns a string representation of the CustomDuration object.
     *
     * @return a string representation of the CustomDuration object
     */
    @Override
    public String toString() {
        if (hasExpired()) {
            return "Expired";
        }
        return TimeUtil.calculateTimeLeft(expirary, true);
    }

}