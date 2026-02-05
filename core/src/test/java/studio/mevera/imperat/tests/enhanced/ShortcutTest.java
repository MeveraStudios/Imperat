package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.Group;

@DisplayName("Shortcut Annotation Tests")
public class ShortcutTest extends EnhancedBaseImperatTest {

    @Test
    public void testPInvite() {
        var res = execute("pinvite Alice");
        assertThat(res)
                .hasArgument("receiver", "Alice");
    }

    @Test
    public void testSetGroupPerm() {
        var res = execute("setgroupperm member lobby.fly");
        assertThat(res)
                .hasArgument("group", new Group("member"))
                .hasArgument("permission", "lobby.fly");
    }

    @Test
    public void testSetGroupPrefix() {
        var res = execute("setgroupprefix member [Guest]");
        assertThat(res)
                .hasArgument("group", new Group("member"))
                .hasArgument("prefix", "[Guest]");

    }

}
