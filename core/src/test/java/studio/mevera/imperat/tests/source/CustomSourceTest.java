package studio.mevera.imperat.tests.source;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.io.PrintStream;
import java.util.function.Function;

/**
 * Smoke test for the v4 custom-source SPI scaffolding. Exercises the
 * {@link CommandSourceMapper} interface + the
 * {@link studio.mevera.imperat.ImperatConfig#sourceClass() sourceClass}
 * / {@link studio.mevera.imperat.ImperatConfig#sourceMapper() sourceMapper}
 * accessors that platform builders rely on at the wrap seam.
 *
 * <p>End-to-end custom-source flow (mapper invocation during real
 * dispatch + ArgumentType receiving the user's {@code S}) is exercised
 * in the bukkit module's tests, where {@code BukkitImperat<S>} is
 * fully generified. This file targets only the core-level SPI shape.</p>
 */
@DisplayName("Custom source SPI")
class CustomSourceTest {

    @Test
    @DisplayName("Default path installs identity mapper + correct sourceClass")
    void defaultPathUsesIdentityMapper() {
        TestImperat imperat = TestImperatConfig.builder().build();

        Assertions.assertThat(imperat.config().sourceMapper())
                .as("default path must install an identity mapper")
                .isSameAs(CommandSourceMapper.identity());
        Assertions.assertThat(imperat.config().sourceClass())
                .as("default-path sourceClass = platform default")
                .isEqualTo(TestCommandSource.class);
    }

    @Test
    @DisplayName("CommandSourceMapper.of wraps platform source into the custom subtype")
    void mapperOfWrapsCorrectly() {
        Function<TestCommandSource, FlavoredSource> wrap = p -> new FlavoredSource(p, "vanilla");
        Function<FlavoredSource, TestCommandSource> unwrap = f -> f;

        CommandSourceMapper<TestCommandSource, FlavoredSource> mapper = CommandSourceMapper.of(wrap, unwrap);
        TestCommandSource platform = new TestCommandSource(System.out);

        FlavoredSource lifted = mapper.wrap(platform);

        Assertions.assertThat(lifted)
                .isInstanceOf(FlavoredSource.class);
        Assertions.assertThat(lifted.flavor())
                .isEqualTo("vanilla");
        Assertions.assertThat(mapper.unwrap(lifted))
                .as("unwrap must round-trip back to the platform source")
                .isSameAs(lifted);  // S extends P → identity is valid
    }

    @Test
    @DisplayName("CommandSourceMapper.identity returns same instance on repeated calls")
    void identityIsSingleton() {
        CommandSourceMapper<TestCommandSource, TestCommandSource> a = CommandSourceMapper.identity();
        CommandSourceMapper<TestCommandSource, TestCommandSource> b = CommandSourceMapper.identity();

        Assertions.assertThat(a)
                .as("identity factory must reuse the cached instance")
                .isSameAs(b);

        TestCommandSource platform = new TestCommandSource(System.out);
        Assertions.assertThat(a.wrap(platform))
                .as("identity wrap returns input unchanged")
                .isSameAs(platform);
    }

    @Test
    @DisplayName("Setting a non-default mapper on config exposes it via sourceMapper()")
    void setSourceMapperRoundTrips() {
        TestImperat imperat = TestImperatConfig.builder().build();
        CommandSourceMapper<TestCommandSource, FlavoredSource> custom = CommandSourceMapper.of(
                p -> new FlavoredSource(p, "spicy"),
                f -> f
        );

        imperat.config().setSourceMapper(custom);

        Assertions.assertThat(imperat.config().sourceMapper())
                .as("setSourceMapper must replace the identity default")
                .isSameAs(custom);
    }

    /** Plugin-defined custom source extending the platform source. */
    static final class FlavoredSource extends TestCommandSource {

        private final String flavor;

        FlavoredSource(TestCommandSource platform, String flavor) {
            super((PrintStream) platform.origin());
            this.flavor = flavor;
        }

        String flavor() {
            return flavor;
        }
    }
}
