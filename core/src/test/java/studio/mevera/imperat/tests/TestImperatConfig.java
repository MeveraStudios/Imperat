package studio.mevera.imperat.tests;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.ConfigBuilder;

public final class TestImperatConfig extends ConfigBuilder<TestSource, TestImperat, TestImperatConfig> {

    public TestImperatConfig() {
        super();
    }

    public static TestImperatConfig builder() {
        return new TestImperatConfig();
    }

    @Override
    public @NotNull TestImperat build() {
        return new TestImperat(config);
    }


}