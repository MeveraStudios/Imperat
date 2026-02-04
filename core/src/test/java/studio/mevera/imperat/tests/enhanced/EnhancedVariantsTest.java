package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Enhanced Variants Test")
public class EnhancedVariantsTest extends EnhancedBaseImperatTest {


    @Test
    @DisplayName("Should handle variant commands correctly")
    void testVariantCommands() {
        // Test logic for variant commands goes here
        var res = execute("mv Hello");
        assertThat(res).isSuccessful()
                .hasArgument("str1", "Hello")
                .hasArgumentOfType("str1", String.class);

        var res2 = execute("mv 123");
        assertThat(res2).isSuccessful()
                .hasArgument("i1", 123)
                .hasArgumentOfType("i1", Integer.class);

    }

}
