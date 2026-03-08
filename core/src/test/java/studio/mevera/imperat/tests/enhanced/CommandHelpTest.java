package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommandHelpTest")
public class CommandHelpTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Should display help properly in flat style")
    public void testFlatHelp() {
        var execResult = this.execute("group");
    }


}
