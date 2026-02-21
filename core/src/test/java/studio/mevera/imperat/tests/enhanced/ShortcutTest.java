package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.Group;

@DisplayName("Shortcut Annotation Tests")
public class ShortcutTest extends EnhancedBaseImperatTest {

    @Test
    public void testPInvite() {
        var res = execute("pinvite Alice");
        assertThat(res)
                .hasArgument("receiver", "Alice");

        var defRes = execute("pinvite");
        Assertions.assertNotNull(defRes.getError());
        Assertions.assertTrue(
                defRes.getError() instanceof CommandException e && e.getResponseKey() != null && e.getResponseKey() == ResponseKey.INVALID_SYNTAX);
    }

    @Test
    public void testSetGroupPerm() {
        var res = execute("setgroupperm member lobby.fly");
        assertThat(res)
                .hasArgument("group", new Group("member"))
                .hasArgument("permission", "lobby.fly");

        var defRes = execute("setgroupperm member");
        assertThat(defRes)
                .hasFailedWith(CommandException.class);
    }

    @Test
    public void testSetGroupPrefix() {
        var res = execute("setgroupprefix member [Guest]");
        assertThat(res)
                .hasArgument("group", new Group("member"))
                .hasArgument("prefix", "[Guest]");


        var defRes = execute("setgroupprefix member");
        assertThat(defRes)
                .hasFailedWith(CommandException.class);
    }

}
