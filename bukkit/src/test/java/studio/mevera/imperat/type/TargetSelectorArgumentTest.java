package studio.mevera.imperat.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.BukkitResponseKey;
import studio.mevera.imperat.selector.SelectionType;
import studio.mevera.imperat.selector.TargetSelector;

class TargetSelectorArgumentTest {

    @Test
    void resolvesTypeAfterMentionCharacter() throws Exception {
        Cursor<BukkitCommandSource> cursor = selectorCursor("@p");

        SelectionType type = TargetSelectorArgument.resolveSelectionType(cursor, "@p");

        assertSame(SelectionType.CLOSEST_PLAYER, type);
        assertNull(cursor.currentLetter().orElse(null));
    }

    @Test
    void keepsCursorOnParameterBlockStart() throws Exception {
        Cursor<BukkitCommandSource> cursor = selectorCursor("@a[tag=test]");

        SelectionType type = TargetSelectorArgument.resolveSelectionType(cursor, "@a[tag=test]");

        assertSame(SelectionType.ALL_PLAYERS, type);
        assertEquals(Character.valueOf('['), cursor.currentLetter().orElse(null));
    }

    @Test
    void rejectsUnknownSelectionTypes() {
        Cursor<BukkitCommandSource> cursor = selectorCursor("@x");

        ResponseException exception = assertThrows(
                ResponseException.class,
                () -> TargetSelectorArgument.resolveSelectionType(cursor, "@x")
        );

        assertSame(BukkitResponseKey.UNKNOWN_SELECTION_TYPE, exception.getResponseKey());
    }

    private Cursor<BukkitCommandSource> selectorCursor(String input) {
        var argument = Argument.<BukkitCommandSource, TargetSelector>required("selector", new TargetSelectorArgument()).build();
        return Cursor.ofSingleString(argument, input);
    }
}
