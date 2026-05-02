package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Verifies the v4 {@code SourceProvider} resolution chain inside
 * {@code ExecutionContextImpl.provideSource(Type)}:
 * <ol>
 *     <li>{@code S}-identity / supertype fast path</li>
 *     <li>Registered {@code SourceProvider} — explicit user override</li>
 *     <li>{@code source.origin()} instance check — platform default</li>
 *     <li>{@code ContextArgumentProvider} — domain-type fallback</li>
 * </ol>
 */
@DisplayName("SourceProvider Resolution Test")
public class SourceProviderTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Registered SourceProvider materialises a derived view from the canonical source")
    void testSourceProviderResolvesFirstParam() {
        var res = execute(DerivedViewCommand.class, cfg -> {
            cfg.registerSourceProvider(
                    DerivedView.class,
                    src -> new DerivedView("from-provider:" + src.name())
            );
        }, "derived");

        assertThat(res).isSuccessful();
        Assertions.assertThat(DerivedViewCommand.LAST)
                .isEqualTo(new DerivedView("from-provider:CONSOLE"));
    }

    @Test
    @DisplayName("SourceProvider returning null falls through to the origin-instance default")
    void testSourceProviderNullFallsThroughToOrigin() {
        var res = execute(OriginCommand.class, cfg -> {
            // Provider returns null → resolution must continue to step 3
            // (clazz.isInstance(source.origin())) instead of throwing.
            cfg.registerSourceProvider(PrintStream.class, src -> null);
        }, "origin");

        assertThat(res).isSuccessful();
        Assertions.assertThat(OriginCommand.LAST).isSameAs(SOURCE.origin());
    }

    @Test
    @DisplayName("SourceProvider takes precedence over the origin-instance default when both apply")
    void testSourceProviderBeatsOriginPath() {
        PrintStream override = new PrintStream(OutputStream.nullOutputStream());
        var res = execute(OriginCommand.class, cfg -> {
            cfg.registerSourceProvider(PrintStream.class, src -> override);
        }, "origin");

        assertThat(res).isSuccessful();
        Assertions.assertThat(OriginCommand.LAST).isSameAs(override);
        Assertions.assertThat(OriginCommand.LAST).isNotSameAs(SOURCE.origin());
    }

    // -- fixtures -----------------------------------------------------

    public record DerivedView(String tag) {

    }

    @RootCommand("derived")
    public static final class DerivedViewCommand {

        static DerivedView LAST;

        @Execute
        public void execute(DerivedView view) {
            LAST = view;
        }
    }

    @RootCommand("origin")
    public static final class OriginCommand {

        static PrintStream LAST;

        @Execute
        public void execute(PrintStream stream) {
            LAST = stream;
        }
    }
}
