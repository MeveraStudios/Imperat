package dev.velix.imperat;


import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.tree.CommandPathSearch;
import dev.velix.imperat.commands.annotations.examples.BanCommand;
import dev.velix.imperat.components.TestSource;
import dev.velix.imperat.context.ArgumentInput;
import dev.velix.imperat.context.Context;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.internal.ContextFactory;
import dev.velix.imperat.util.Registry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static dev.velix.imperat.TestRun.IMPERAT;
import static dev.velix.imperat.TestRun.SOURCE;

public class TestSmartUsageResolve {

    TestSmartUsageResolve() {
    }

    private final static ContextFactory<TestSource> FACTORY = IMPERAT.config.getContextFactory();

    private static ExecutionContext<TestSource> inputResolve(String command, String... args) {
        System.out.println("-------------");
        System.out.println("args= " + Arrays.toString(args));
        ArgumentInput queue = ArgumentInput.parse(args);
        System.out.println("QUEUE: " + queue.join(","));

        Command<TestSource> cmd = IMPERAT.getCommand(command);
        Assertions.assertNotNull(cmd);

        Context<TestSource> context = FACTORY.createContext(IMPERAT, SOURCE, cmd, command, queue);
        CommandPathSearch<TestSource> res = cmd.contextMatch(context);

        CommandUsage<TestSource> usage = res.getFoundUsage();

        if (usage == null) {
            System.out.println("USAGE IS NULL");
            var param = res.getLastNode();
            if (param != null && param.isCommand()) {
                usage = param.getData().asCommand().getDefaultUsage();
            }
        }
        Assertions.assertNotNull(usage);
        
        System.out.println("GOT USAGE !!, RESOLVING ARGS");
        ExecutionContext<TestSource> resolvedContext = FACTORY.createExecutionContext(context, res);
        Assertions.assertDoesNotThrow(resolvedContext::resolve);
        System.out.println("RESOLVED ARGS !!");
        resolvedContext.debug();
        
        //CommandUsage<TestSource> finalUsage = usage;
        //Assertions.assertDoesNotThrow(() -> finalUsage.execute(IMPERAT, SOURCE, resolvedContext));
        return resolvedContext;
    }

    private static void test(String cmdLine, ResolvedArgsData expected) {
        String[] split = cmdLine.split(" ");
        String[] raws = new String[split.length - 1];
        System.arraycopy(split, 1, raws, 0, split.length - 1);
        //System.arraycopy(split, 1, raws, 0, raws.length - 1);

        var input = ResolvedArgsData.of(
            inputResolve(split[0], raws)
        );
        input.debug();
        Assertions.assertTrue(expected
            .matches(
                input
            )
        );
    }

    @Test
    public void banWithSwitchFlag() {
        IMPERAT.registerCommand(new BanCommand());
        test(
            "ban mqzen",
            ResolvedArgsData.empty()
                .arg("target", "mqzen")
                .flag("silent", false)
                .flag("ip", false)
                .arg("duration", null)
                .arg("reason", "Breaking server laws")
        );
        // /ban <user> [reason]
        // /ban mqzen "for being a good man"
        test(
            "ban mqzen -s",
            ResolvedArgsData.empty()
                .arg("target", "mqzen")
                .flag("silent", true)
                .flag("ip", false)
                .arg("duration", null)
                .arg("reason", "Breaking server laws")

        );
        test(
            "ban mqzen -s 1d",
            ResolvedArgsData.empty()
                    .arg("target", "mqzen")
                    .flag("silent", true)
                    .flag("ip", false)
                    .arg("duration", "1d")
                    .arg("reason", "Breaking server laws")

        );
        test(
            "ban mqzen -s 1d A disgrace to community",
            ResolvedArgsData.empty()
                .arg("target", "mqzen")
                .flag("silent", true)
                .flag("ip", false)
                .arg("duration", "1d")
                .arg("reason", "A disgrace to community")

        );
    }


    @Test
    public void testInputFlag() {
        test("git", ResolvedArgsData.empty());
        test("git commit", ResolvedArgsData.empty().flag("message", null));
        test("git commit -m \"allah akbar\"", ResolvedArgsData.empty().flag("message", "allah akbar"));
    }

    @Test
    public void testInputParse() {
        ArgumentInput queue = ArgumentInput.parse("Its me again and this is my msg: \"sorry for interruption\"");
        queue.forEach(System.out::println);
    }

    @Test
    public void testKit() {
        test("kit create test", ResolvedArgsData.empty().arg("kit", "test")
            .arg("weight", 1));
    }

    @Test
    public void testGiveCmd() {
        
        /*test(
                "give apple mqzen 2",
                ResolvedArgsData.empty()
                        .arg("item", "apple")
                        .arg("player", new TestPlayer("mqzen"))
                        .arg("amount", 2)
        );
        
        test(
                "give apple",
                ResolvedArgsData.empty()
                        .arg("item", "apple")
                        .arg("player", null)
                        .arg("amount", null)
        );
        
        test("give apple mqzen",
                ResolvedArgsData.empty()
                        .arg("item", "apple")
                        .arg("player", new TestPlayer("mqzen"))
                        .arg("amount", null)
        );
        */
        test("give apple 2",
                ResolvedArgsData.empty()
                        .arg("item", "apple")
                        .arg("player", null)
                        .arg("amount", 2)
        );
    }
    
    static class ResolvedArgsData {

        private final Map<String, Object> args = new LinkedHashMap<>();
        private final Registry<String, Object> resolvedFlags = new Registry<>(LinkedHashMap::new);

        private ResolvedArgsData(ExecutionContext<TestSource> context) {
            if (context == null) return;

            for (var resolved : context.getResolvedArguments()) {
                System.out.println("Adding " + resolved.parameter().name());
                args.put(resolved.parameter().name(), resolved.value());
            }

            for (var param : context.getDetectedUsage().getParameters()) {
                if(!param.isFlag()) {
                    continue;
                }
                var flagParam = param.asFlagParameter();

                String name = param.asFlagParameter().flagData().name();

                Object resolvedValue = context.getFlagValue(name);
                if(resolvedValue == null && flagParam.isSwitch()) {
                    resolvedValue = false;
                }

                resolvedFlags.setData(name, resolvedValue);
            }
        }


        public static ResolvedArgsData of(ExecutionContext<TestSource> context) {
            return new ResolvedArgsData(context);
        }

        public static ResolvedArgsData empty() {
            return of(null);
        }

        public ResolvedArgsData arg(String paramName, Object resolvedValue) {
            args.put(paramName, resolvedValue);
            return this;
        }

        public ResolvedArgsData flag(String paramName, Object inputValue) {
            resolvedFlags.setData(paramName, inputValue);
            return this;
        }

        public boolean matches(ResolvedArgsData other) {
            System.out.println("----------------");
            System.out.println("Comparing THIS ARGS:");
            System.out.println(this.args);


            System.out.println("Comparing OTHER ARGS:");
            System.out.println(other.args);

            System.out.println("Comparing THIS FLAGS:");
            System.out.println(this.resolvedFlags.getMap());

            System.out.println("Comparing OTHER FLAGS:");
            System.out.println(other.resolvedFlags.getMap());


            if (args.size() != other.args.size() || resolvedFlags.size() != other.resolvedFlags.size()) {
                return false;
            }

            for (var entry : this.args.entrySet()) {
                Object thisObj = entry.getValue();
                Object otherObj = other.args.get(entry.getKey());
                if(thisObj == null && otherObj == null) {
                    return true;
                }
                if (!Objects.equals(thisObj, otherObj)) {
                    return false;
                }
            }

            for (String flagKey : resolvedFlags.getKeys()) {
                Object flagValue = resolvedFlags.getData(flagKey).orElse(null);
                if (!other.resolvedFlags.getMap().containsKey(flagKey)) {
                    System.out.println("Flag mismatch: " + flagKey + "=" + flagValue + " != " + flagKey + "=null");
                    return false;
                }
                if (!Objects.equals(flagValue, other.resolvedFlags.getData(flagKey)
                    .orElse(null))) {
                    System.out.println("Flag mismatch: " + flagKey + "=" + flagValue + " != " + flagKey + "=" + other.resolvedFlags.getData(flagKey).orElse(null));
                    return false;
                }
            }
            return true;
        }

        public void debug() {

            System.out.println("----------------");
            System.out.println("Args: ");
            for (var arg : args.keySet()) {
                System.out.println(" '" + arg + "' -> '" + args.get(arg) + "'");
            }

            System.out.println("Flags: ");
            for (var flag : resolvedFlags.getKeys()) {
                var flagInputValue = resolvedFlags.getData(flag).orElse(null);
                System.out.println(" '" + flag + "' -> '" + flagInputValue + "'");
            }

        }
    }

}
