package studio.mevera.imperat.tests.arguments;

import java.util.Objects;

public class TestPlayer {
    
    private final String name;
    
    
    public TestPlayer(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TestPlayer that)) return false;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}