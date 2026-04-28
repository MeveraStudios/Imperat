package studio.mevera.imperat.tests.parameters;

/**
 * Test fixture: a three-token coordinate triple. Used to exercise
 * multi-arity inner types inside {@code CompletableFuture<T>} —
 * a regression case for the legacy single-token drain bug.
 */
public final class TriCoord {

    public final double x;
    public final double y;
    public final double z;

    public TriCoord(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "TriCoord{" + x + "," + y + "," + z + "}";
    }
}
