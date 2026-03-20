package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studio.mevera.imperat.tests.commands.FlatRankCommand;

@DisplayName("Test Flattened commands")
public class FlatCommandsTest extends EnhancedBaseImperatTest {

    @ParameterizedTest
    @CsvSource({
            "'rank member permission set hello.world', member, hello.world, true",
            "'rank member permission set hello.world true', member, hello.world, true",
            "'rank member permission set hello.world false', 'member', hello.world, false"
    })
    public void testRankFlatCmd(String line, String rank, String perm, String value) {
        var res = this.execute(FlatRankCommand.class, (cfg) -> {
        }, line);
        assertThat(res).isNotNull()
                .isSuccessful()
                .hasArgument("rank", rank)
                .hasArgument("perm", perm)
                .hasArgument("value", Boolean.valueOf(value));
    }


}
