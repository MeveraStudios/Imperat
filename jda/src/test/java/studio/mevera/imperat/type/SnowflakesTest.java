package studio.mevera.imperat.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class SnowflakesTest {

    private static final long ID = 123456789012345678L;

    @Test
    void readsRawIdsAndEveryMentionForm() {
        assertThat(Snowflakes.parse("123456789012345678")).isEqualTo(ID);
        assertThat(Snowflakes.parse("<@123456789012345678>")).isEqualTo(ID);
        assertThat(Snowflakes.parse("<@!123456789012345678>")).isEqualTo(ID);
        assertThat(Snowflakes.parse("<@&123456789012345678>")).isEqualTo(ID);
        assertThat(Snowflakes.parse("<#123456789012345678>")).isEqualTo(ID);
        assertThat(Snowflakes.parse(" 123456789012345678 ")).isEqualTo(ID);
    }

    @Test
    void refusesAnythingElse() {
        assertThat(Snowflakes.parse("someone")).isZero();
        assertThat(Snowflakes.parse("12345")).isZero();
        assertThat(Snowflakes.parse("<:emoji:123456789012345678>")).isZero();
        assertThat(Snowflakes.parse("1234567890123456789012")).isZero();
    }
}
