package studio.mevera.imperat.command.arguments;

public final class NumericRange {

    private final double min, max;
    private boolean hasMin, hasMax;
    NumericRange(double min, double max) {
        this.min = min;
        this.max = max;
        this.hasMin = true;
        this.hasMax = true;
    }

    public static NumericRange of(double min, double max) {
        return new NumericRange(min, max);
    }

    public static NumericRange min(double min) {
        NumericRange range = new NumericRange(min, Double.MAX_VALUE);
        range.hasMin = true;
        range.hasMax = false;
        return range;
    }

    public static NumericRange max(double max) {
        NumericRange range = new NumericRange(-Double.MAX_VALUE, max);
        range.hasMin = false;
        range.hasMax = true;
        return range;
    }

    public static NumericRange empty() {
        return of(-Double.MAX_VALUE, Double.MAX_VALUE);
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

    public boolean hasMax() {
        return hasMax;
    }

    public boolean hasMin() {
        return hasMin;
    }

}
