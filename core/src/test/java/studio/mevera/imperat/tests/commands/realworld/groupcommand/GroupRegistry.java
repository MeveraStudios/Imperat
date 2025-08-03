package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.util.Registry;

import java.util.HashMap;
import java.util.Map;

public final class GroupRegistry extends Registry<String, Group> {

    private final static GroupRegistry instance = new GroupRegistry();
    private final Map<String, Group> userGroups = new HashMap<>();

    GroupRegistry() {
        Group g = new Group("member");
        setData("member", g);
        setData("mod", new Group("mod"));
        setData("srmod", new Group("srmod"));
        setData("owner", new Group("owner"));

        setGroup("mqzen", g);
    }

    public static GroupRegistry getInstance() {
        return instance;
    }

    public void setGroup(String name, Group group) {
        userGroups.put(name, group);
    }

    public Group getGroup(String username) {
        return userGroups.get(username);
    }


}