package dev.velix.imperat.commands;

import dev.velix.imperat.TestSource;
import dev.velix.imperat.annotations.Command;
import dev.velix.imperat.annotations.Named;
import dev.velix.imperat.annotations.SubCommand;
import dev.velix.imperat.annotations.Usage;

import java.util.List;
import java.util.Map;

@Command("test2")
public class Test2Command {

    @Usage
    @SubCommand("help")
    public void def(TestSource source) {
        source.reply("Send help input");
        // /test2
    }

    @SubCommand("array")
    public void def(TestSource source, @Named("myArray") String[] array) {
        source.reply("SIZE= " + array.length);
        for(var entry : array) {
            source.reply("-> " + entry);
        }
        // /test2 array hi hello how are you
    }

    @SubCommand("collection")
    public void def(TestSource source, @Named("myCollection") List<String> collection) {
        source.reply("SIZE= " + collection.size());
        for(var entry : collection) {
            source.reply("-> " + entry);
        }
    }

    @SubCommand("map")
    public void def(TestSource source, @Named("myMap") Map<String, String> map) {
        source.reply("SIZE= " + map.size());
        for(var entry : map.entrySet()) {
            source.reply("-> " + entry.getKey() + ":" + entry.getValue());
        }
    }

}
