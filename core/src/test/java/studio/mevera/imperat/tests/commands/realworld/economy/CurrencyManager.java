package studio.mevera.imperat.tests.commands.realworld.economy;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CurrencyManager {

    private static CurrencyManager instance = new CurrencyManager();
    private final Map<String, Currency> currencyMap = new HashMap<>();

    public CurrencyManager() {
        currencyMap.put("gold", new Currency("gold", BigDecimal.ONE));
        currencyMap.put("silver", new Currency("silver", BigDecimal.ZERO));
    }

    public static CurrencyManager getInstance() {
        if (instance == null) {
            instance = new CurrencyManager();
        }
        return instance;
    }

    public Currency getCurrencyByName(String name) {
        return currencyMap.get(name.toLowerCase());
    }

    public Collection<? extends Currency> getAllCurrencies() {
        return currencyMap.values();
    }
}
