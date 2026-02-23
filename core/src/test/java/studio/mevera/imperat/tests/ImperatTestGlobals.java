package studio.mevera.imperat.tests;

import static studio.mevera.imperat.tests.commands.TestCommands.CHAINED_SUBCOMMANDS_CMD;
import static studio.mevera.imperat.tests.commands.TestCommands.MULTIPLE_OPTIONAL_CMD;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.base.AnnotationFactory;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.tests.commands.BuyCommand;
import studio.mevera.imperat.tests.commands.ContextResolvingCmd;
import studio.mevera.imperat.tests.commands.CustomEnumCommand;
import studio.mevera.imperat.tests.commands.FailCmd;
import studio.mevera.imperat.tests.commands.KingdomChatCommand;
import studio.mevera.imperat.tests.commands.MultipleVariantsCmd;
import studio.mevera.imperat.tests.commands.MyCustomAnnotation;
import studio.mevera.imperat.tests.commands.OptionalArgCommand;
import studio.mevera.imperat.tests.commands.RankCommand;
import studio.mevera.imperat.tests.commands.SetRankCmd;
import studio.mevera.imperat.tests.commands.SomeClass;
import studio.mevera.imperat.tests.commands.Test2Command;
import studio.mevera.imperat.tests.commands.Test3Command;
import studio.mevera.imperat.tests.commands.TestAC;
import studio.mevera.imperat.tests.commands.TestAC2;
import studio.mevera.imperat.tests.commands.TestCustomAnnotationCmd;
import studio.mevera.imperat.tests.commands.TestPerm;
import studio.mevera.imperat.tests.commands.complex.FirstOptionalArgumentCmd;
import studio.mevera.imperat.tests.commands.complex.TestCommand;
import studio.mevera.imperat.tests.commands.realworld.Ban2Command;
import studio.mevera.imperat.tests.commands.realworld.BanCommand;
import studio.mevera.imperat.tests.commands.realworld.GitCommand;
import studio.mevera.imperat.tests.commands.realworld.GiveCmd;
import studio.mevera.imperat.tests.commands.realworld.GuildMOTDCommand;
import studio.mevera.imperat.tests.commands.realworld.KitCommand;
import studio.mevera.imperat.tests.commands.realworld.MessageCmd;
import studio.mevera.imperat.tests.commands.realworld.PartyCommand;
import studio.mevera.imperat.tests.commands.realworld.TestCFParamTypeCmd;
import studio.mevera.imperat.tests.commands.realworld.TestJavaOptionalParamTypeCmd;
import studio.mevera.imperat.tests.commands.realworld.UpperCaseCmd;
import studio.mevera.imperat.tests.commands.realworld.economy.BalanceCmd;
import studio.mevera.imperat.tests.commands.realworld.economy.BigDecimalParamType;
import studio.mevera.imperat.tests.commands.realworld.economy.Currency;
import studio.mevera.imperat.tests.commands.realworld.economy.CurrencyParamType;
import studio.mevera.imperat.tests.commands.realworld.economy.EconomyCommand;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.AnnotatedGroupCommand;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.Group;
import studio.mevera.imperat.tests.commands.realworld.groupcommand.GroupArgument;
import studio.mevera.imperat.tests.contextresolver.PlayerData;
import studio.mevera.imperat.tests.contextresolver.PlayerDataContextArgumentResolver;
import studio.mevera.imperat.tests.contextresolver.SomeData;
import studio.mevera.imperat.tests.contextresolver.SomeDataCR;
import studio.mevera.imperat.tests.errors.CustomException;
import studio.mevera.imperat.tests.parameters.CustomDuration;
import studio.mevera.imperat.tests.parameters.CustomDurationArgumentType;
import studio.mevera.imperat.tests.parameters.JavaDurationArgumentType;
import studio.mevera.imperat.tests.parameters.TestPlayerParamType;
import studio.mevera.imperat.tests.syntax.commands.UsageTestCommand;
import studio.mevera.imperat.util.ImperatDebugger;
import studio.mevera.imperat.util.TypeWrap;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Global test source and base test infrastructure for Imperat command framework tests.
 */
public class ImperatTestGlobals {

    /** Global test infrastructure instances */
    public static final TestImperat IMPERAT = TestImperatConfig.builder()
                                                      .permissionChecker((src, perm) -> perm == null || src.hasPermission(perm))
                                                      .argType(Group.class, new GroupArgument())
                                                      .argType(Duration.class, new JavaDurationArgumentType())
                                                      .argType(TestPlayer.class, new TestPlayerParamType())
                                                      .argType(CustomDuration.class, new CustomDurationArgumentType<>())
                                                      .argType(BigDecimal.class, new BigDecimalParamType())
                                                      .argType(Currency.class, new CurrencyParamType())
                                                      .handleMiddleOptionalArgSkipping(true)
                                                      .contextResolver(new TypeWrap<CommandHelp<TestSource>>() {
                                                      }.getType(), (ctx, pe) -> CommandHelp.create(ctx))
                                                      .contextResolver(new TypeWrap<Context<TestSource>>() {
                                                      }.getType(), (ctx, pe) -> ctx)
                                                      .contextResolver(PlayerData.class, new PlayerDataContextArgumentResolver())
                                                      .contextResolver(SomeData.class, new SomeDataCR())
                                                      .handleMiddleOptionalArgSkipping(true)
                                                      .overlapOptionalParameterSuggestions(true)
                                                      .throwableResolver(CustomException.class, (exc, ctx) -> {
                                                          System.out.println("CustomException occurred: " + exc.getMessage());
                                                      })
                                                      .build();
    public static final TestSource GLOBAL_TEST_SOURCE = new TestSource(System.out);

    static {
        System.out.println("=== ImperatTestGlobals static initializer START ===");
        IMPERAT.registerAnnotationReplacer(MyCustomAnnotation.class, (element, ann) -> {
            Command cmdAnn = AnnotationFactory.create(Command.class, "value",
                    new String[]{ann.name()});
            return List.of(cmdAnn);
        });
        IMPERAT.registerCommand(BuyCommand.class);
        IMPERAT.registerCommand(ReqCmd.class);
        IMPERAT.registerCommand(MultipleVariantsCmd.class);
        IMPERAT.registerSimpleCommand(MULTIPLE_OPTIONAL_CMD);
        IMPERAT.registerSimpleCommand(CHAINED_SUBCOMMANDS_CMD);
        IMPERAT.registerCommand(AnnotatedGroupCommand.class);
        IMPERAT.registerCommand(OptionalArgCommand.class);
        //;
        IMPERAT.registerCommand(GitCommand.class);
        IMPERAT.registerCommand(MessageCmd.class);
        //IMPERAT.registerCommand(EmptyCmd.class);
        IMPERAT.registerCommand(KitCommand.class);

        IMPERAT.registerCommands(TestCommand.class, Test2Command.class, Test3Command.class, TestCustomAnnotationCmd.class);

        IMPERAT.registerCommand(GiveCmd.class);
        IMPERAT.registerCommand(BanCommand.class);
        IMPERAT.registerCommand(KingdomChatCommand.class);
        IMPERAT.registerCommand(Ban2Command.class);

        IMPERAT.registerCommands(TestAC.class, TestAC2.class);

        IMPERAT.registerCommand(PartyCommand.class);
        IMPERAT.registerCommand(GuildMOTDCommand.class);

        IMPERAT.registerCommands(
                TestJavaOptionalParamTypeCmd.class,
                TestCFParamTypeCmd.class,
                UpperCaseCmd.class,
                CustomEnumCommand.class,
                SetRankCmd.class,
                RankCommand.class,
                ContextResolvingCmd.class,
                FirstOptionalArgumentCmd.class,
                SomeClass.class,
                TestPerm.class,
                FailCmd.class
        );

        IMPERAT.registerCommands(EconomyCommand.class, BalanceCmd.class);

        // Register syntax test commands
        IMPERAT.registerCommand(UsageTestCommand.class);


        ImperatDebugger.setEnabled(true);
        IMPERAT.debug(false);
        System.out.println("=== ImperatTestGlobals static initializer END ===");
    }

    /** Reset global state for tests */
    public static void resetTestState() {
    }
}
