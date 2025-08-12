package studio.mevera.imperat.tests.commands.realworld.economy;

import java.math.BigDecimal;

public class Currency {
    String name;
    BigDecimal defaultAmount;
    
    public Currency(final String name, final BigDecimal defaultAmount) {
        this.name = name.toLowerCase();
        this.defaultAmount = defaultAmount;
    }
    
    public String getName() {
        return name;
    }
    
    public BigDecimal getDefaultAmount() {
        return defaultAmount;
    }
    
}