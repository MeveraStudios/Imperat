package studio.mevera.imperat.command.parameters;

public final class NumericRange {

    private final double min, max;

    NumericRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public static NumericRange of(double min, double max) {
        return new NumericRange(min, max);
    }

    public static NumericRange min(double min) {
        return new NumericRange(min, Double.MAX_VALUE);
    }

    public static NumericRange max(double max) {
        return new NumericRange(Double.MIN_VALUE, max);
    }

    public static NumericRange empty() {
        return of(Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public boolean matches(double value) {
        return value >= min && value <= max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

}
