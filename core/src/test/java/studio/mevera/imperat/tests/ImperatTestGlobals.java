package studio.mevera.imperat.tests;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.base.AnnotationFactory;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.tests.commands.*;
import studio.mevera.imperat.tests.commands.complex.FirstOptionalArgumentCmd;
import studio.mevera.imperat.tests.commands.complex.TestCommand;
import studio.mevera.imperat.tests.commands.realworld.*;
import studio.mevera.imperat.tests.commands.realworld.economy.BigDecimalParamType;
import studio.mevera.imperat.tests.commands.realworld.economy.Currency;
import studio.mevera.imperat.tests.commands.realworld.economy.CurrencyParamType;
import studio.mevera.imperat.tests.commands.realworld.economy.EconomyCommand;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.AnnotatedGroupCommand;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.Group;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.ParameterGroup;
import studio.mevera.imperat.tests.contextresolver.PlayerData;
import studio.mevera.imperat.tests.contextresolver.PlayerDataContextResolver;
import studio.mevera.imperat.tests.parameters.CustomDuration;
import studio.mevera.imperat.tests.parameters.CustomDurationParameterType;
import studio.mevera.imperat.tests.parameters.JavaDurationParameterType;
import studio.mevera.imperat.tests.parameters.TestPlayerParamType;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.verification.UsageVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static studio.mevera.imperat.tests.commands.TestCommands.CHAINED_SUBCOMMANDS_CMD;
import static studio.mevera.imperat.tests.commands.TestCommands.MULTIPLE_OPTIONAL_CMD;

/**
 * Global test source and base test infrastructure for Imperat command framework tests.
 */
public class ImperatTestGlobals {
    
    /** Global test infrastructure instances */
    public static final TestImperat IMPERAT = TestImperatConfig.builder()
            .usageVerifier(UsageVerifier.typeTolerantVerifier())
            .permissionChecker((src, perm)-> perm == null || src.hasPermission(perm))
            .contextResolver(PlayerData.class, new PlayerDataContextResolver())
            .parameterType(Group.class, new ParameterGroup())
            .parameterType(Duration.class, new JavaDurationParameterType())
            .parameterType(TestPlayer.class, new TestPlayerParamType())
            .parameterType(CustomDuration.class, new CustomDurationParameterType<>())
            .parameterType(BigDecimal.class, new BigDecimalParamType())
            .parameterType(Currency.class, new CurrencyParamType())
            .handleExecutionConsecutiveOptionalArguments(true)
            .contextResolver(new TypeWrap<CommandHelp<TestSource>>() {}.getType(), (ctx, pe)-> CommandHelp.create(ctx))
            .contextResolver(new TypeWrap<Context<TestSource>>(){}.getType(), (ctx, pe)-> ctx)
            .handleExecutionConsecutiveOptionalArguments(true)
            .overlapOptionalParameterSuggestions(true)
            .build();
    
    static {
        IMPERAT.registerAnnotationReplacer(MyCustomAnnotation.class,(element, ann)-> {
            Command cmdAnn = AnnotationFactory.create(Command.class, "value",
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
        
        IMPERAT.registerCommands(new TestCommand(), new Test2Command(), new Test3Command(), new TestCustomAnnotationCmd());
        IMPERAT.registerCommand(new GiveCmd());
        IMPERAT.registerCommand(new BanCommand());
        IMPERAT.registerCommand(new KingdomChatCommand());
        IMPERAT.registerCommand(new Ban2Command());
        IMPERAT.registerCommands(new TestAC(), new TestAC2());
        IMPERAT.registerCommand(new PartyCommand());
        IMPERAT.registerCommand(new GuildMOTDCommand());
        IMPERAT.registerCommands(
                new TestJavaOptionalParamTypeCmd(),
                new TestCFParamTypeCmd(),
                new UpperCaseCmd(),
                new CustomEnumCommand(),
                new SetRankCmd(),
                new RankCommand(),
                new ContextResolvingCmd(),
                new FirstOptionalArgumentCmd(),
                new SomeClass(),
                new TestPerm(),
                new FailingCmd()
        );
        
        ImperatDebugger.setEnabled(true);
        IMPERAT.registerCommand(new EconomyCommand());
    }
    
    public static final TestSource GLOBAL_TEST_SOURCE = new TestSource(System.out);
    
    /** Reset global state for tests */
    public static void resetTestState() {
    }
}
