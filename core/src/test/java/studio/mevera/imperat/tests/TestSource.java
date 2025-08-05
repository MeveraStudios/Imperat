package studio.mevera.imperat.tests;

import studio.mevera.imperat.context.Source;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class TestSource implements Source {
    private final PrintStream origin;
    private final Set<String> permissions = new HashSet<>();
    
    public TestSource(PrintStream origin) {
        this.origin = origin;
        //permissions.add("group.group");
        //permissions.add("group.group.setperm");
    }
    
    public TestSource withPerm(String perm) {
        permissions.add(perm);
        return this;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    public void sendMsg(String msg) {
        origin.println(msg);
    }
    
    @Override
    public String name() {
        return "CONSOLE";
    }
    
    @Override
    public void reply(String message) {
        sendMsg(message);
    }
    
    @Override
    public void warn(String message) {
        sendMsg(message);
    }
    
    @Override
    public void error(String message) {
        sendMsg(message);
    }
    
    @Override
    public boolean isConsole() {
        return true;
    }
    
    @Override
    public PrintStream origin() {
        return origin;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TestSource) obj;
        return Objects.equals(this.origin, that.origin);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(origin);
    }
    
    @Override
    public String toString() {
        return "TestSource[" +
                "origin=" + origin + ']';
    }
    
    
    public void debugPerms() {
        StringBuilder builder = new StringBuilder();
        for(var str : permissions) {
            builder.append(str).append(",");
        }
        if(!builder.isEmpty()) {
            builder.deleteCharAt(builder.length()-1);
        }
        System.out.println("Permissions: [" + builder.toString() + "]");
    }
}