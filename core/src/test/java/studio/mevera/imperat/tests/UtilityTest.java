package studio.mevera.imperat.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.PriorityList;

@DisplayName("Utility Test Suite")
public class UtilityTest {

    @Test
    public void testPriorityListAdd() {
        PriorityList<String> priorityList = new PriorityList<>();
        priorityList.add(Priority.LOW, "Iron Man");
        priorityList.add(Priority.HIGH, "Captain America");
        priorityList.add(Priority.NORMAL, "Thor");

        String[] expectedOrder = {
            "Captain America",
            "Thor",
            "Iron Man"
        };
        String[] realOrder = new String[priorityList.size()];
        int i = 0;
        for(var e : priorityList) {
            realOrder[i] = e;
            i++;
        }

        Assertions.assertArrayEquals(expectedOrder, realOrder);
    }

}
