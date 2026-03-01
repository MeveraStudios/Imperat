package studio.mevera.imperat.tests.contextresolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.enhanced.EnhancedBaseImperatTest;


@DisplayName("CommandContext Resolving Test")
public class ContextResolvingTest extends EnhancedBaseImperatTest {

    @Test
    public void testBasic() {
        var res = execute("ctx sub2");
        assertThat(res)
                .isSuccessful()
                .hasContextArgumentOf(SomeData.class, (data)-> data.data().equals("test"));
    }

}
