package studio.mevera.imperat.command.parameters;

public final class Priority {
    public static final Priority MINIMUM = new Priority(Integer.MIN_VALUE);
    public static final Priority LOW = new Priority(0);
    public static final Priority NORMAL = new Priority(20);
    public static final Priority HIGH = new Priority(100);
    public static final Priority MAXIMUM = new Priority(Integer.MAX_VALUE);

    private final int level;

    public static Priority of(int level) {
        return new Priority(level);
    }

    private Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public Priority plus(int n) {
        return new Priority(this.level + n);
    }
}