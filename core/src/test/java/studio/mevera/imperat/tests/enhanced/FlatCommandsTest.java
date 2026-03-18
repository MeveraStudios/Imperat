package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.commands.FlatRankCommand;

@DisplayName("Test Flattened commands")
public class FlatCommandsTest extends EnhancedBaseImperatTest {

    @Test
    public void testRankFlatCmd() {
        var res = this.execute(FlatRankCommand.class, (cfg) -> {
        }, "rank member permission set hello.world");
        assertThat(res).isNotNull()
                .isSuccessful()
                .hasArgument("rank", "member")
                .hasArgument("permission", "hello.world")
                .hasArgument("value", false);
    }


}
