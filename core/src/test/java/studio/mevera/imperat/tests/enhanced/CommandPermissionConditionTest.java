package studio.mevera.imperat.tests.enhanced;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.permissions.CommandPermissionCondition;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("CommandPermissionCondition Tests")
public class CommandPermissionConditionTest extends BaseImperatTest {

    private final static TestImperat IMPERAT = ImperatTestGlobals.IMPERAT;

    private static TestSource user(String... perms) {
        TestSource src = new TestSource(System.out);
        for (String perm : perms) {
            src.withPerm(perm);
        }
        return src;
    }

    @Test
    void testFluentAPI() {

        CommandPermissionCondition cond = CommandPermissionCondition
                                                  .has("A")
                                                  .or("B")
                                                  .and("C");  // (A OR B) AND C

        var user1 = user("A", "C");
        var user2 = user("B", "C");
        var user3 = user("A");

        assertTrue(cond.has(user1, IMPERAT.config().getPermissionChecker()));
        assertTrue(cond.has(user2, IMPERAT.config().getPermissionChecker()));
        assertFalse(cond.has(user3, IMPERAT.config().getPermissionChecker()));
    }


    @Test
    void testStaticAllHelper() {
        CommandPermissionCondition cond = CommandPermissionCondition.all("X", "Y", "Z"); // X & Y & Z

        var user1 = user("X", "Y", "Z");
        var user2 = user("X", "Y");
        var user3 = user("Z", "Y", "X");

        assertTrue(cond.has(user1, IMPERAT.config().getPermissionChecker()));
        assertFalse(cond.has(user2, IMPERAT.config().getPermissionChecker()));
        assertTrue(cond.has(user3, IMPERAT.config().getPermissionChecker()));
    }

    @Test
    void testFromTextSimple() {
        CommandPermissionCondition cond = CommandPermissionCondition.fromText("A | B & !C"); // A OR (B AND NOT C)

        var user1 = user("A");
        var user2 = user("B");
        var user3 = user("B", "C");

        assertTrue(cond.has(user1, IMPERAT.config().getPermissionChecker()));
        assertTrue(cond.has(user2, IMPERAT.config().getPermissionChecker()));
        assertFalse(cond.has(user3, IMPERAT.config().getPermissionChecker()));
    }

    @Test
    void testFromTextNested() {
        CommandPermissionCondition cond = CommandPermissionCondition.fromText("(A | B) & (!C | D)");

        var user1 = user("A");
        var user2 = user("B", "D");
        var user3 = user("B", "C");

        assertTrue(cond.has(user1, IMPERAT.config().getPermissionChecker()));
        assertTrue(cond.has(user2, IMPERAT.config().getPermissionChecker()));
        assertFalse(cond.has(user3, IMPERAT.config().getPermissionChecker()));
    }
}
