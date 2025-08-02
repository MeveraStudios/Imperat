package dev.velix.imperat;

import dev.velix.imperat.annotations.Command;
import dev.velix.imperat.annotations.Default;
import dev.velix.imperat.annotations.Usage;
import dev.velix.imperat.components.TestSource;

@Command("mo")
public class TestMiddleOptionals {
    
    @Usage
    public void mainUsage(TestSource source,
                          String r1,
                          @Default("def-o1") String o1,
                          @Default("def-o2") String o2,
                          @Default("def-o3") String o3,
                          String r2
    ) {
        source.reply(
                "r1='" + r1 +"'" + ", " +
                "o1='" + o1 + "'" + ", " +
                "o2='" + o2 + "'" + ", " +
                "o3='" + o3 + "'" + ", " +
                "r2='" + r2 + "'"
        );
    }
    
    
    
}
