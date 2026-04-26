package studio.mevera.imperat.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.selector.CharStream;
import studio.mevera.imperat.selector.SelectionType;

class TargetSelectorArgumentTest {

    @Test
    void resolvesTypeAfterMentionCharacter() throws Exception {
        CharStream stream = new CharStream("@p");

        SelectionType type = TargetSelectorArgument.resolveSelectionType(stream, "@p");

        assertSame(SelectionType.CLOSEST_PLAYER, type);
        assertNull(stream.peek());
    }

    @Test
    void keepsCursorOnParameterBlockStart() throws Exception {
        CharStream stream = new CharStream("@a[tag=test]");

        SelectionType type = TargetSelectorArgument.resolveSelectionType(stream, "@a[tag=test]");

        assertSame(SelectionType.ALL_PLAYERS, type);
        assertEquals(Character.valueOf('['), stream.peek());
    }

    @Test
    void rejectsUnknownSelectionTypes() {
        CharStream stream = new CharStream("@x");

        ResponseException exception = assertThrows(
                ResponseException.class,
                () -> TargetSelectorArgument.resolveSelectionType(stream, "@x")
        );

        assertSame(BukkitResponseKey.UNKNOWN_SELECTION_TYPE, exception.getResponseKey());
    }
}
