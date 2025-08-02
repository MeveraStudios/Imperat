package dev.velix.imperat;

import dev.velix.imperat.advanced.DurationParameterType;
import dev.velix.imperat.advanced.GuildMOTDCommand;
import dev.velix.imperat.annotations.base.AnnotationFactory;
import dev.velix.imperat.annotations.base.SourceOrderHelper;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.type.BaseParameterType;
import dev.velix.imperat.command.parameters.type.ParameterType;
import dev.velix.imperat.command.parameters.type.ParameterTypes;
import dev.velix.imperat.command.tree.CommandPathSearch;
import dev.velix.imperat.commands.*;
import dev.velix.imperat.commands.annotations.FirstOptionalArgumentCmd;
import dev.velix.imperat.commands.annotations.KitCommand;
import dev.velix.imperat.commands.annotations.TestCommand;
import dev.velix.imperat.commands.annotations.contextresolver.ContextResolvingCmd;
import dev.velix.imperat.commands.annotations.contextresolver.PlayerData;
import dev.velix.imperat.commands.annotations.contextresolver.PlayerDataContextResolver;
import dev.velix.imperat.commands.annotations.examples.*;
import dev.velix.imperat.components.TestImperat;
import dev.velix.imperat.components.TestImperatConfig;
import dev.velix.imperat.components.TestSource;
import dev.velix.imperat.misc.CustomEnum;
import dev.velix.imperat.misc.CustomEnumParameterType;
import dev.velix.imperat.misc.ParameterGroup;
import dev.velix.imperat.misc.Test4Cmd;
import dev.velix.imperat.paramtypes.TestCompletableFutureParam;
import dev.velix.imperat.paramtypes.TestJavaOptionalParam;
import dev.velix.imperat.paramtypes.TestPlayer;
import dev.velix.imperat.paramtypes.TestPlayerParamType;
import dev.velix.imperat.special.PartyCommand;
import dev.velix.imperat.util.ImperatDebugger;
import dev.velix.imperat.util.TypeWrap;
import dev.velix.imperat.verification.UsageVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static dev.velix.imperat.commands.TestCommands.*;

public class TestRun {

    TestRun() {
        USAGE_EXECUTED = false;
    }

    public final static TestImperat IMPERAT;
    final static TestSource SOURCE = new TestSource(System.out);

    public static volatile boolean USAGE_EXECUTED = false;
    public static volatile int POST_PROCESSOR_INT = 0;
    public static volatile int PRE_PROCESSOR_INT = 0;

    
    private final static SomeInstance SOME_INSTANCE = new SomeInstance("Hello-world");
    static {
        ImperatDebugger.setEnabled(true);
        ImperatDebugger.setUsingTestCases(true);
        //ImperatDebugger.setTesting(true);

        IMPERAT = TestImperatConfig.builder()
                .globalDefaultUsageBuilder(CommandUsage.<TestSource>builder().execute((src, ctx)-> {
                    var rootCmd = ctx.command();
                    var lastCmd = ctx.getLastUsedCommand();
                    var correctUsage = rootCmd.getUsage((usage)->
                        usage.hasParameters((p)-> p.similarTo(lastCmd))
                    );
                    src.error("Correct usage: /" + CommandUsage.format(rootCmd, correctUsage));
                }))
                .dependencyResolver(Group.class, () -> new Group("my-global-group"))
                .dependencyResolver(SomeInstance.class, ()-> SOME_INSTANCE)
                .parameterType(Group.class, new ParameterGroup())
                .contextResolver(PlayerData.class, new PlayerDataContextResolver())
                .parameterType(CustomEnum.class, new CustomEnumParameterType())
                .parameterType(Duration.class, new DurationParameterType())
                .parameterType(TestPlayer.class, new TestPlayerParamType())
                .parameterType(dev.velix.imperat.commands.Duration.class, new ParameterDuration<>())
                .handleExecutionConsecutiveOptionalArguments(true)
                .build();

        IMPERAT.registerAnnotationReplacer(MyCustomAnnotation.class,(element, ann)-> {
            dev.velix.imperat.annotations.Command cmdAnn = AnnotationFactory.create(dev.velix.imperat.annotations.Command.class, "value",
                    new String[]{ann.name()});
            return List.of(cmdAnn);
        });
        IMPERAT.registerCommand(MULTIPLE_OPTIONAL_CMD);
        IMPERAT.registerCommand(CHAINED_SUBCOMMANDS_CMD);
        IMPERAT.registerCommand(new AnnotatedGroupCommand());
        IMPERAT.registerCommand(new OptionalArgCommand());
        //;
        IMPERAT.registerCommand(new GitCommand());
        IMPERAT.registerCommand(new MessageCmd());
        IMPERAT.registerCommand(new EmptyCmd());
        IMPERAT.registerCommand(new KitCommand());
        IMPERAT.registerCommand(new TestCommand());
        IMPERAT.registerCommand(new Test2Command());
        IMPERAT.registerCommand(new GiveCmd());
        
    }

    private static CommandPathSearch.Result testCmdTreeExecution(String cmdName, String input) {
        return IMPERAT.execute(SOURCE, cmdName, input);
    }

    private static void debugCommand(Command<TestSource> command) {
        command.visualizeTree();

        System.out.println("Debugging sub commands: ");
        System.out.println("CommandProcessingChain '" + command.name() + "' has usages: ");
        for (CommandUsage<TestSource> usage : command.usages()) {
            System.out.println("- " + CommandUsage.format(command, usage));
        }

    }


    public static void testCommand(String name, Object cmdClassInstance, String... usages) {

        System.out.println("-----------------------------");
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(cmdClassInstance);
        });

        var cmd = IMPERAT.getCommand(name);
        Assertions.assertNotNull(cmd);
        debugCommand(cmd);

        for(String usage : usages) {
            System.out.println("Executing '/" + name + " " + usage + "'");
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution(name, usage));
            System.out.println("----------------");
        }

    }

    @Test
    public void testTypeWrap() {
        final TypeWrap<List<String>> typeWrap = new TypeWrap<>() {
        };
        Assertions.assertEquals("java.util.List<java.lang.String>", typeWrap.getType().getTypeName());
    }

    @Test
    public void testTypeTolerantVerifierAmbiguity() {
        UsageVerifier<TestSource> verifier = UsageVerifier.typeTolerantVerifier();

        CommandUsage.Builder<TestSource> usage1 = CommandUsage.<TestSource>builder()
            .parameters(
                CommandParameter.requiredText("arg1")
            );

        CommandUsage.Builder<TestSource> usage2 = CommandUsage.<TestSource>builder()
            .parameters(
                CommandParameter.requiredBoolean("arg2")
            );

        Assertions.assertFalse(verifier.areAmbiguous(usage1.build(GROUP_CMD), usage2.build(GROUP_CMD)));
    }

    @Test
    public void testHelp() {
        debugCommand(Objects.requireNonNull(IMPERAT.getCommand("test")));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "help"));
    }

    @Test
    public void testIncompleteSubCommand() {
        //syntax -> /group <group> setperm <permission> [value]
        var result = testCmdTreeExecution("group", "member setperm");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, result);
    }

    @Test
    public void testCompleteSubCommand() {
        var result = testCmdTreeExecution("group", "member setperm command.group");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, result);
    }

    @Test
    public void testHelpSubCommand() {
        //syntax -> /group help [page]
        var result = testCmdTreeExecution("group", "help");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, result);
    }
    
    /*@Test
    public void testPreProcessor() {
        USAGE_EXECUTED = false;
        GROUP_CMD.setPreProcessor(new CustomPreProcessor());
        var setResult = testCmdTreeExecution("group", "member");
        
        Assertions.assertEquals(PRE_PROCESSOR_INT, 1);
        Assertions.assertEquals(Result.COMPLETE, setResult);
        Assertions.assertTrue(USAGE_EXECUTED);
    }
    
    @Test
    public void testPostProcessor() {
        USAGE_EXECUTED = false;
        GROUP_CMD.setPostProcessor(new CustomPostProcessor());
        
        var setResult = testCmdTreeExecution("group", "member");
        Assertions.assertEquals(POST_PROCESSOR_INT, 1);
        Assertions.assertEquals(Result.COMPLETE, setResult);
        Assertions.assertTrue(USAGE_EXECUTED);
    }*/

    @Test
    public void testSubInheritance() {
        System.out.println("----------------------------");
        debugCommand(Objects.requireNonNull(IMPERAT.getCommand("test")));
        debugCommand(Objects.requireNonNull(IMPERAT.getCommand("embedded")));

        var result = testCmdTreeExecution("test", "first-value secondValue first a1 second a3");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, result);
    }

    @Test
    public void testExec() {
        debugCommand(Objects.requireNonNull(IMPERAT.getCommand("test")));
        Assertions.assertDoesNotThrow(() -> {
            var result = IMPERAT.execute(SOURCE, "test");
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, result);
            Assertions.assertEquals(CommandPathSearch.Result.UNKNOWN, IMPERAT.execute(SOURCE, "test hi bye invalid"));
        });
        
        
    }

    @Test
    public void tempo() {
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "otherText1 otherText2 sub1"));
    }
    @Test
    public void testInnerClassParsing() {


        System.out.println("----------------------------");
        debugCommand(Objects.requireNonNull(IMPERAT.getCommand("test")));
        //debugCommand(Objects.requireNonNull(IMPERAT.getCommand("embedded")));

        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", ""));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub1 hi"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub1 hi sub2"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub1 hi sub2 bye"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub1 hi sub2 bye sub3"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub1 hi sub2 bye sub3 hello"));

        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub4"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub4 hi"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub4 hi sub5"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub4 hi sub5 bye"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub4 hi sub5 bye sub6"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test", "text text2 sub4 hi sub5 bye sub6 hello"));

    }
    

    @Test
    public void testAutoCompletion2() {
        var cmd = IMPERAT.getCommand("test");
        assert cmd != null;
        debugCommand(cmd);
        
        var results = IMPERAT.autoComplete(new TestSource(System.out), "test ");
        Assertions.assertEquals(List.of("help", "hi", "bye"), results.join());
        
        var results2 = IMPERAT.autoComplete(new TestSource(System.out), "test ");
        Assertions.assertEquals(List.of("help", "hi", "bye"), results2.join());
        
        var results3 = IMPERAT.autoComplete(new TestSource(System.out),"test a b ");
        Assertions.assertEquals(List.of("othersub", "first", "sub1", "sub4"), results3.join());
    }


    @Test
    public void testAutoCompletion3() {
        var cmd = IMPERAT.getCommand("test");
        assert cmd != null;
        debugCommand(cmd);
        var results = IMPERAT.autoComplete(new TestSource(System.out), "test hi bye first ");
        var res = results.join();
        Assertions.assertLinesMatch(Stream.of("x", "y", "z", "sexy"), res.stream());
    }

    @Test
    public void testAutoCompletion4() {
        var cmd = IMPERAT.getCommand("message");
        assert cmd != null;
        debugCommand(cmd);
        var results = IMPERAT.autoComplete(new TestSource(System.out), "message mqzen ");
        var res = results.join();
        Assertions.assertLinesMatch(Stream.of("this is a long greedy", "some sentence", "idk"), res.stream());
    }

    @Test
    public void testAutoCompletion9() {
        var cmd = IMPERAT.getCommand("printnum");
        assert cmd != null;
        debugCommand(cmd);
        var results = IMPERAT.autoComplete(new TestSource(System.out), "printnum ");
        var res = results.join();
        Assertions.assertLinesMatch(Stream.of("1.0"), res.stream());
    }

    @Test
    public void testOptionalArgCmd() {
        var cmd = IMPERAT.getCommand("opt");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("opt", "hi"));
    }

    @Test
    public void testBanCmd() {
        var cmd = IMPERAT.getCommand("ban");
        assert cmd != null;
        debugCommand(cmd);

        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban", "mqzen"));
    }

    @Test
    public void testUpperCaseCommandName() {
        IMPERAT.registerCommand(Command.create(IMPERAT, "UPPER_CAsE")
            .defaultExecution((src, ctx) -> src.reply("Worked !"))
            .build());
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("upper_case", ""));
    }

    @Test
    public void testKitCmd() {
        var cmd = IMPERAT.getCommand("kit");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("kit", "create test"));
    }

    @Test
    public void testValuesAnnotation() {
        var cmd = IMPERAT.getCommand("ban");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban", "mqzen -s 1d"));
    }

    @Test
    public void testArrayParamExecution() {
        var cmd = IMPERAT.getCommand("test2");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test2", "array member mod srmod owner"));
        // /test2 hi, hello man
        // /test2 <myList>
    }

    @Test
    public void testCollectionParamExecution() {
        var cmd = IMPERAT.getCommand("test2");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test2", "collection hi bro hello man"));
        // /test2 hi, hello man
        // /test2 <myList>
    }

    @Test
    public void testMapParamExecution() {
        var cmd = IMPERAT.getCommand("test2");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test2", "map hi,hello hey,man"));
        // /test2 hi, hello man
        // /test2 <myList>
    }

    @Test
    public void testAnnotationReplacer() {
        IMPERAT.registerCommand(new TestCustomAnnotationCmd());
        var cmd = IMPERAT.getCommand("testreplacer");
        assert cmd != null;
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("testreplacer", ""));
    }

    @Test
    public void testMiddleOptionals() {
        
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new TestMiddleOptionals());
        });
        
        var moCmd = IMPERAT.getCommand("mo");
        Assertions.assertNotNull(moCmd);
        
        //TODO test after finishing best design for dispatch.
        
        var cmd = IMPERAT.getCommand("ot");
        Assertions.assertNotNull(cmd);
        debugCommand(cmd);
        Assertions.assertEquals(CommandPathSearch.Result.UNKNOWN, testCmdTreeExecution("ot", "myr1"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ot","myr1 myr2"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ot","myr1 myo1 myr2"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ot","myr1 myo1 myr2 myo2"));
    }

    @Test
    public void contextResolve() {
        ImperatDebugger.setTesting(true);
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new ContextResolvingCmd());
        });
        var cmd = IMPERAT.getCommand("ctx");
        Assertions.assertNotNull(cmd);
        Assertions.assertDoesNotThrow(()-> {
            testCmdTreeExecution("ctx", "");
        });

        Assertions.assertEquals(CommandPathSearch.Result.FAILURE, testCmdTreeExecution("ctx", "sub"));
    }

    @Test
    public void testFirstOptionalArgCmd() {
        ImperatDebugger.setTesting(true);
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new FirstOptionalArgumentCmd());
        });
        var cmd = IMPERAT.getCommand("foa");
        Assertions.assertNotNull(cmd);
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("foa", ""));
        });
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("foa", "1"));
        });
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("foa", "1 sub"));
        });

        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("foa", "1 sub 2"));
        });
    }

    @Test
    public void testEmptyAttachmentSubToDefaultWithOneOptionalArg() {
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new Test3Command());
        });

        var cmd = IMPERAT.getCommand("test3");
        Assertions.assertNotNull(cmd);
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test3", ""));
        });
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test3", "hello-world"));
        });


        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.UNKNOWN, testCmdTreeExecution("test3", "hello-world sub"));
        });
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.UNKNOWN, testCmdTreeExecution("test3", "hello-world sub HI_BRO"));
        });

        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test3", "sub"));
        });

        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test3", "sub HI_BRO"));
        });
    }

    @Test
    public void testCustomEnum() {

        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new Test4Cmd());
        });

        var cmd = IMPERAT.getCommand("test4");
        Assertions.assertNotNull(cmd);
        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test4", "VALUE_1"));
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test4", "VALUE_2"));
            Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("test4", "VALUE_3"));
        });

        Assertions.assertDoesNotThrow(()-> {
            Assertions.assertEquals(CommandPathSearch.Result.FAILURE, testCmdTreeExecution("test4", "HELLO"));
        });
    }
    @Test
    public void testKingdomChatWeirdCmd() {
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new KingdomChatCommand());
        });

        var cmd = IMPERAT.getCommand("kingdomchat");
        Assertions.assertNotNull(cmd);
        debugCommand(cmd);

        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("kingdomchat", ""));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("kingdomchat", "hello world"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("kingdomchat", "hello world hi"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("kingdomchat", "hello world hi bro"));
    }

    @Test
    public void testTwoConsecutiveMiddleOptionalSwitches() {
        System.out.println("-----------------------------");
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new BanCommand());
        });

        var cmd = IMPERAT.getCommand("ban");
        Assertions.assertNotNull(cmd);
        debugCommand(cmd);

        System.out.println("Executing '/ban mqzen -s'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban", "mqzen -s"));

        System.out.println("----------------");

        System.out.println("Executing '/ban mqzen -s -ip'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban", "mqzen -s -ip"));

        System.out.println("----------------");

        System.out.println("Executing '/ban mqzen -ip'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban", "mqzen -ip"));

        System.out.println("----------------");

        System.out.println("Executing '/ban mqzen -ip -s'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban", "mqzen -ip -s"));


    }

    @Test
    public void motdCommand() {
        //String usage1 = "\"Hello world, im mazen\" 1h";
        String usage2 = "-time 1h Hello world, im mazen";
        String usage3 = "Hello world, im mazen";

        testCommand("motd",new GuildMOTDCommand(), usage2);
        testCommand("motd",new GuildMOTDCommand(), usage3);
    }

    @Test
    public void addPermRankCmd() {
        System.out.println("-----------------------------");
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new RankCommand());
        });

        var cmd = IMPERAT.getCommand("rank");
        Assertions.assertNotNull(cmd);
        debugCommand(cmd);

        System.out.println("Executing '/rank addperm mod server.fly'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("rank", "addperm mod server.fly"));

        System.out.println("----------------");

        System.out.println("Executing '/rank addperm mod server.fly -duration 1d -force'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("rank", "addperm mod server.fly -duration 1d -force"));

        System.out.println("----------------");

        System.out.println("Executing '/rank addperm mod server.fly -duration 1d'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("rank", "addperm mod server.fly -duration 1d"));

        System.out.println("----------------");

        System.out.println("Executing '/rank addperm mod server.fly -force'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("rank", "addperm mod server.fly -force"));
    }


    @Test @SuppressWarnings("unchecked")
    public void testExtractedTypes() {

        ParameterType<TestSource, String> stringResolver = (ParameterType<TestSource, String>) IMPERAT.config.getParameterType(String.class);
        BaseParameterType<TestSource, String[]> stringArrType = ParameterTypes.array(new TypeWrap<>() {
        }, String[]::new, stringResolver);


        BaseParameterType<TestSource, CompletableFuture<String>> completableFutureType = ParameterTypes.future(
                new TypeWrap<>() {
                }, stringResolver);

        Assertions.assertEquals("java.lang.String[]", stringArrType.type().getTypeName());
        Assertions.assertEquals("java.util.concurrent.CompletableFuture<java.lang.String>", completableFutureType.type().getTypeName());
    }

    @Test
    public void testSpecialParamTypes() {

        //register cf cmd
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new TestCompletableFutureParam());
            IMPERAT.registerCommand(new TestJavaOptionalParam());
        });


        var testCF = IMPERAT.getCommand("testcf");
        Assertions.assertNotNull(testCF);

        var testOpt = IMPERAT.getCommand("testoptional");
        Assertions.assertNotNull(testOpt);

        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("testcf", "Thor is the best hero"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("testoptional", "Hulk is always angry"));
    }

    @Test
    public void testCumulativeSuggestions() {
        //tests if it respects the order of the nodes during suggestion resolving.

        IMPERAT.registerCommand(new TestAC());

        var cmd = IMPERAT.getCommand("testac");
        assert cmd != null;
        debugCommand(cmd);

        var results = IMPERAT.autoComplete(new TestSource(System.out), "testac ").join();
        Assertions.assertLinesMatch(Stream.of("any_text"), results.stream());

        var results2 = IMPERAT.autoComplete(new TestSource(System.out),"testac hello ").join();
        Assertions.assertLinesMatch(Stream.of("2", "5", "10"), results2.stream());
    }

    /*@Test
    public void testCumulativeSuggestions2() {
        //tests if it respects the order of the nodes during suggestion resolving.

        IMPERAT.registerCommand(new TestAC2());

        var cmd = IMPERAT.getCommand("testac2");
        assert cmd != null;
        debugCommand(cmd);
        
        System.out.println("TEST 1 :-");
        var results = IMPERAT.autoComplete(cmd, new TestSource(System.out),"testac2", new String[]{""}).join();
        Assertions.assertLinesMatch(Stream.of("any_text"), results.stream());
        
        
        System.out.println("TEST 2 :-");
        var results2 = IMPERAT.autoComplete(cmd, new TestSource(System.out),"testac2", new String[]{"my_text", ""}).join();
        Assertions.assertLinesMatch(Stream.of("2", "5", "10"), results2.stream());

        System.out.println("TEST 3 :-");
        var results3 = IMPERAT.autoComplete(cmd, new TestSource(System.out),"testac2", new String[]{"my_text", "5", ""}).join();
        Assertions.assertLinesMatch(Stream.of("3.1", "6.2", "9.5"), results3.stream());
        
        
        System.out.println("TEST 4 :-");
        IMPERAT.config().setOptionalParameterSuggestionOverlap(true);
        var results4 = IMPERAT.autoComplete(cmd, new TestSource(System.out),"testac2", new String[]{"my_text", ""}).join();
        Assertions.assertLinesMatch(Stream.of("2", "5", "10", "3.1", "6.2", "9.5"), results4.stream());
        IMPERAT.config().setOptionalParameterSuggestionOverlap(false);
        
    }
    */
    
    @Test
    public void testMiddleFlag() {
        
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new Ban2Command());
        });
        
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban2", "mqzen -t 7d Cheating"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("ban2", "mqzen -t 7d Cheating is not good !"));
        
    }
    
    @Test
    public void testPlainCmd() {
        
        var cmd = Command.create(IMPERAT, "plain")
                .defaultExecution(
                        (source, ctx)-> {
                            System.out.println("RUNNING DEFAULT !!");
                        }
                )
                .build();
        
        
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(cmd);
        });
        
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("plain", ""));
    }
    
    @Test
    public void testInnerNonStaticClassesReflection() throws Exception {
        
        System.out.println("Checking inner classes: ");
        
        System.out.println("---------------> Debugging SomeClass:");
        for(var e : SourceOrderHelper.getInnerClassesInSourceOrder(SomeClass.class)) {
            System.out.println("INNER= " + e.getTypeName());
        }
        
        
        System.out.println("----------------> Debugging SomeClass.InnerOne:");
        for(var e : SourceOrderHelper.getInnerClassesInSourceOrder(SomeClass.InnerOne.class)) {
            System.out.println("INNER= " + e.getTypeName());
        }
        
        System.out.println("----------------> Debugging SomeClass.InnerTwo:");
        for(var e : SourceOrderHelper.getInnerClassesInSourceOrder(SomeClass.InnerTwo.class)) {
            System.out.println("INNER= " + e.getTypeName());
        }
    }
    
    @Test
    public void testNonStaticMemberClasses() {
    
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new SomeClass());
        });
        
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("root", "i1"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("root", "i1 i1.1"));
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("root", "i1 i1.1 i1.1.1"));
    }
    
    @Test
    public void testClosestUsage() {
        Assertions.assertDoesNotThrow(() -> {
            IMPERAT.registerCommand(new PartyCommand());
        });
        
        var cmd = IMPERAT.getCommand("party");
        Assertions.assertNotNull(cmd);
        
        cmd.visualizeTree();
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("party", "invite mqzen"));
        System.out.println("-------------");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("party", "invite"));
        System.out.println("-------------");
        Assertions.assertEquals(CommandPathSearch.Result.UNKNOWN, testCmdTreeExecution("party", "invite hello world damn"));
    }
    
    @Test
    public void testSetRank() {
        Assertions.assertDoesNotThrow(()-> {
            IMPERAT.registerCommand(new SetRankCmd());
        });
        
        var res = testCmdTreeExecution("setrank", "Mqzen undead permanent Giveaway Winner");
        Assertions.assertEquals(CommandPathSearch.Result.FAILURE, res);
    }
    
    @Test
    public void testTwoOptionals() {
        System.out.println("Running '/give Apple mqzen'");
        Assertions.assertEquals(CommandPathSearch.Result.COMPLETE, testCmdTreeExecution("give", "Apple mqzen"));
    }
    
    @Test
    public void testSimpleAutoCompletion() {
        IMPERAT.registerCommand(new PlainCmd());
        var cmd = IMPERAT.getCommand("plain");
        Assertions.assertNotNull(cmd);
        Assertions.assertLinesMatch(List.of("hi", "hello", "bruh", "awesomeee", "i-love-imperat"),
                IMPERAT.autoComplete(SOURCE, "plain ").join());
    }
    
    @Test
    public void testArgumentsInheritanceBySubCmdMethod() {
        //TODO test later on...
    }
}

