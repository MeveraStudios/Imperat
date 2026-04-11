package studio.mevera.imperat.tests.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.tests.TestCommandSource;

@DisplayName("Cursor Single String Tests")
class CursorSingleStringTest {

    @Test
    @DisplayName("ofSingleString should expose the provided token as current raw input")
    void ofSingleStringShouldExposeCurrentRawInput() {
        var argument = Argument.<TestCommandSource>requiredText("selector").build();
        Cursor<TestCommandSource> cursor = Cursor.ofSingleString(argument, "@a[tag=test]");

        assertThat(cursor.currentRaw()).hasValue("@a[tag=test]");
        assertThat(cursor.currentLetter()).hasValue('@');
        assertThat(cursor.getRawQueue()).hasSize(1);
        assertThat(cursor.getRawQueue().getOriginalRaw()).isEqualTo("@a[tag=test]");
    }
}
