package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shortcut Annotation Tests")
public class ShortcutTest extends EnhancedBaseImperatTest {

    @Test
    public void testPInvite() {
        var res = execute("pinvite Alice");
        assertThat(res)
                .hasArgument("receiver", "Alice");
    }


}
