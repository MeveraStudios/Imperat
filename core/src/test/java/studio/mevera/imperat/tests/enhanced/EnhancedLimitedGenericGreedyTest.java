package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.tests.commands.realworld.greedy.LimitedGenericGreedy;

import java.util.Collection;
import java.util.Map;

/**
 * Tests for {@link LimitedGenericGreedy} — a command with three subcommands
 * ({@code array}, {@code collection}, {@code map}) each having a
 * {@code @Greedy(limit = 3)} generic parameter followed by a required
 * {@code int num} trailing argument.
 *
 * <p>Covers:
 * <ul>
 *   <li>Array subcommand — greedy String[] with limit=3 + trailing int</li>
 *   <li>Collection subcommand — greedy Collection&lt;String&gt; with limit=3 + trailing int</li>
 *   <li>Map subcommand — greedy Map&lt;String,String&gt; with limit=3 + trailing int</li>
 *   <li>Type-discriminated yielding (greedy yields to trailing int)</li>
 *   <li>Exact limit, under-limit, over-limit inputs</li>
 *   <li>Missing trailing arg → failure</li>
 * </ul>
 */
@DisplayName("Limited Generic Greedy Tests (@Greedy(limit=3) on arrays, collections, maps)")
class EnhancedLimitedGenericGreedyTest extends EnhancedBaseImperatTest {

    // ════════════════════════════════════════════════════════════════════════
    // Array subcommand: /lg array <args:String[]@limit=3> <num:int>
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Array subcommand — @Greedy(limit=3) String[]")
    class ArraySubcommand {

        @Test
        @DisplayName("Exactly 3 tokens + trailing int: 'lg array a b c 5' → args=[a,b,c], num=5")
        void exactlyThreeTokensPlusInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array a b c 5"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        String[] arr = (String[]) value;
                        Assertions.assertThat(arr).containsExactly("a", "b", "c");
                    })
                    .hasArgument("num", 5);
        }

        @Test
        @DisplayName("2 tokens then int yields early: 'lg array hello world 10' → args=[hello,world], num=10")
        void twoTokensThenIntYieldsEarly() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array hello world 10"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        String[] arr = (String[]) value;
                        Assertions.assertThat(arr).containsExactly("hello", "world");
                    })
                    .hasArgument("num", 10);
        }

        @Test
        @DisplayName("1 token then int: 'lg array one 42' → args=[one], num=42")
        void singleTokenThenInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array one 42"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        String[] arr = (String[]) value;
                        Assertions.assertThat(arr).containsExactly("one");
                    })
                    .hasArgument("num", 42);
        }

        @Test
        @DisplayName("Limit reached then trailing int: 'lg array a b c 7' → args=[a,b,c], num=7")
        void limitReachedThenTrailingInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array a b c 7"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        String[] arr = (String[]) value;
                        Assertions.assertThat(arr).hasSize(3);
                    })
                    .hasArgument("num", 7);
        }

        @Test
        @DisplayName("3 non-int tokens without trailing int → fail")
        void threeTokensNoTrailingInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array a b c"))
                    .hasFailed();
        }

        @Test
        @DisplayName("Over-limit: 4 non-int tokens + int → fail (4th token is not int)")
        void overLimitNonIntBeforeInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array a b c d 5"))
                    .hasFailed();
        }

        @Test
        @DisplayName("No args at all → fail")
        void noArgs() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg array"))
                    .hasFailed();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Collection subcommand: /lg collection <args:Collection<String>@limit=3> <num:int>
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Collection subcommand — @Greedy(limit=3) Collection<String>")
    class CollectionSubcommand {

        @Test
        @DisplayName("Exactly 3 tokens + trailing int: 'lg collection x y z 99' → args=[x,y,z], num=99")
        void exactlyThreeTokensPlusInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg collection x y z 99"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        @SuppressWarnings("unchecked")
                        Collection<String> col = (Collection<String>) value;
                        Assertions.assertThat(col).containsExactly("x", "y", "z");
                    })
                    .hasArgument("num", 99);
        }

        @Test
        @DisplayName("2 tokens then int yields early: 'lg collection foo bar 3' → args=[foo,bar], num=3")
        void twoTokensThenIntYieldsEarly() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg collection foo bar 3"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        @SuppressWarnings("unchecked")
                        Collection<String> col = (Collection<String>) value;
                        Assertions.assertThat(col).containsExactly("foo", "bar");
                    })
                    .hasArgument("num", 3);
        }

        @Test
        @DisplayName("1 token then int: 'lg collection single 1' → args=[single], num=1")
        void singleTokenThenInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg collection single 1"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        @SuppressWarnings("unchecked")
                        Collection<String> col = (Collection<String>) value;
                        Assertions.assertThat(col).containsExactly("single");
                    })
                    .hasArgument("num", 1);
        }

        @Test
        @DisplayName("3 non-int tokens without trailing int → fail")
        void threeTokensNoTrailingInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg collection a b c"))
                    .hasFailed();
        }

        @Test
        @DisplayName("Over-limit: 4 non-int tokens + int → fail")
        void overLimitNonIntBeforeInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg collection a b c d 5"))
                    .hasFailed();
        }

        @Test
        @DisplayName("No args at all → fail")
        void noArgs() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg collection"))
                    .hasFailed();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Map subcommand: /lg map <args:Map<String,String>@limit=3> <num:int>
    // Each map entry is "key,value" (comma-separated pair per token)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Map subcommand — @Greedy(limit=3) Map<String,String>")
    class MapSubcommand {

        @Test
        @DisplayName("Exactly 3 entries + trailing int: 'lg map k1,v1 k2,v2 k3,v3 5' → 3 entries, num=5")
        void exactlyThreeEntriesPlusInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map k1,v1 k2,v2 k3,v3 5"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (Map<String, String>) value;
                        Assertions.assertThat(map)
                                .hasSize(3)
                                .containsEntry("k1", "v1")
                                .containsEntry("k2", "v2")
                                .containsEntry("k3", "v3");
                    })
                    .hasArgument("num", 5);
        }

        @Test
        @DisplayName("2 entries then int yields early: 'lg map a,b c,d 7' → 2 entries, num=7")
        void twoEntriesThenIntYieldsEarly() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map a,b c,d 7"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (Map<String, String>) value;
                        Assertions.assertThat(map)
                                .hasSize(2)
                                .containsEntry("a", "b")
                                .containsEntry("c", "d");
                    })
                    .hasArgument("num", 7);
        }

        @Test
        @DisplayName("1 entry then int: 'lg map foo,bar 42' → 1 entry, num=42")
        void singleEntryThenInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map foo,bar 42"))
                    .isSuccessful()
                    .hasArgumentSatisfying("args", value -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (Map<String, String>) value;
                        Assertions.assertThat(map)
                                .hasSize(1)
                                .containsEntry("foo", "bar");
                    })
                    .hasArgument("num", 42);
        }

        @Test
        @DisplayName("3 entries without trailing int → fail")
        void threeEntriesNoTrailingInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map k1,v1 k2,v2 k3,v3"))
                    .hasFailed();
        }

        @Test
        @DisplayName("Over-limit: 4 entries + int → fail")
        void overLimitEntriesBeforeInt() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map k1,v1 k2,v2 k3,v3 k4,v4 5"))
                    .hasFailed();
        }

        @Test
        @DisplayName("Invalid map entry format (no comma) → fail")
        void invalidEntryFormat() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map badentry 5"))
                    .hasFailed();
        }

        @Test
        @DisplayName("No args at all → fail")
        void noArgs() {
            assertThat(execute(LimitedGenericGreedy.class, cfg -> {
            }, "lg map"))
                    .hasFailed();
        }
    }
}

