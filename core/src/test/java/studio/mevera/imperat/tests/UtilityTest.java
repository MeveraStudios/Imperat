package studio.mevera.imperat.tests;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.util.priority.Prioritizable;
import studio.mevera.imperat.util.priority.Priority;
import studio.mevera.imperat.util.priority.PriorityList;

@DisplayName("Utility Test Suite")
public class UtilityTest {

    @Test
    public void testPriorityListAdd() {

        record Text(String value, Priority priority) implements Prioritizable {

            @Override
            public @NotNull Priority getPriority() {
                return Text.this.priority;
            }
        }
        PriorityList<Text> priorityList = new PriorityList<>();
        priorityList.add(new Text("Iron Man", Priority.LOW));
        priorityList.add(new Text("Captain America", Priority.HIGH));
        priorityList.add(new Text("Thor", Priority.NORMAL));

        String[] expectedOrder = {
                "Captain America",
                "Thor",
                "Iron Man"
        };
        String[] realOrder = new String[priorityList.size()];
        int i = 0;
        for (var e : priorityList.toList().stream().map(Text::value).toList()) {
            realOrder[i] = e;
            i++;
        }

        Assertions.assertArrayEquals(expectedOrder, realOrder);
    }

}
