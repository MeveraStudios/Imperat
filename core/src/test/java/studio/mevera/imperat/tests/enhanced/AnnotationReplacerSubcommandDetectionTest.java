package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.annotations.base.AnnotationFactory;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Description;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.ParseOrder;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

class AnnotationReplacerSubcommandDetectionTest {

    @Test
    void customSubcommandsRemainPresentInTree() {
        TestImperat imperat = TestImperatConfig.builder()
                                      .annotationReplacer(VCommand.class, (element, annotation) ->
                                                                                  List.of(AnnotationFactory.create(RootCommand.class, "value",
                                                                                          new String[]{annotation.value()})))
                                      .annotationReplacer(VSubcommand.class, (element, annotation) ->
                                                                                     List.of(AnnotationFactory.create(SubCommand.class, "value",
                                                                                             annotation.value())))
                                      .build();

        imperat.registerCommand(VoxyLikeCurrencyCommand.class);

        Command<TestCommandSource> command = imperat.getCommand("currency");
        Assertions.assertThat(command).isNotNull();
        Assertions.assertThat(command.getSubCommand("give", false)).isNotNull();
        Assertions.assertThat(command.getSubCommand("add", false)).isNotNull();

        List<String> rootChildren = command.tree().rootNode().getChildren()
                                            .toList()
                                            .stream()
                                            .map(node -> node.getData().getName())
                                            .toList();

        Assertions.assertThat(rootChildren).contains("help", "give", "take", "set");
    }

    @Test
    void knownCustomAnnotationsShouldStillBeReplaced() {
        TestImperat imperat = TestImperatConfig.builder()
                                      .annotationReplacer(VSubcommand.class, (element, annotation) ->
                                                                                     List.of(AnnotationFactory.create(SubCommand.class, "value",
                                                                                             annotation.value())))
                                      .build();

        imperat.registerAnnotations(VSubcommand.class);
        imperat.registerCommand(KnownAnnotationCurrencyCommand.class);

        Command<TestCommandSource> command = imperat.getCommand("currency-known");
        Assertions.assertThat(command).isNotNull();
        Assertions.assertThat(command.getSubCommand("help", false)).isNotNull();
        Assertions.assertThat(command.getSubCommand("give", false)).isNotNull();
        Assertions.assertThat(command.tree().rootNode().getChildren()
                                      .toList()
                                      .stream()
                                      .map(node -> node.getData().getName())
                                      .toList())
                .contains("help", "give", "take", "set");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface VCommand {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface VSubcommand {

        String[] value();
    }

    @VCommand("currency")
    public static final class VoxyLikeCurrencyCommand {

        @Execute
        @ParseOrder(1)
        @VSubcommand("help")
        public void sendHelp(
                final TestCommandSource actor,
                final @Named("page") @Default("1") int page
        ) {
        }

        @ParseOrder(2)
        @VSubcommand({"give", "add"})
        @Description("Gives currency to a player")
        public void give(
                final TestCommandSource actor,
                final @Named("currency") String currency,
                final @Named("player") String target,
                final @Named("amount") double amount
        ) {
        }

        @ParseOrder(3)
        @VSubcommand({"take", "remove"})
        @Description("Removes currency from a player")
        public void take(
                final TestCommandSource actor,
                final @Named("currency") String currency,
                final @Named("player") String target,
                final @Named("amount") double amount
        ) {
        }

        @ParseOrder(4)
        @VSubcommand("set")
        @Description("Sets a player's currency balance")
        public void set(
                final TestCommandSource actor,
                final @Named("currency") String currency,
                final @Named("player") String target,
                final @Named("amount") double amount
        ) {
        }
    }

    @RootCommand("currency-known")
    public static final class KnownAnnotationCurrencyCommand {

        @Execute
        @ParseOrder(1)
        @VSubcommand("help")
        public void sendHelp(
                final TestCommandSource actor,
                final @Named("page") @Default("1") int page
        ) {
        }

        @ParseOrder(2)
        @VSubcommand({"give", "add"})
        @Description("Gives currency to a player")
        public void give(
                final TestCommandSource actor,
                final @Named("currency") String currency,
                final @Named("player") String target,
                final @Named("amount") double amount
        ) {
        }

        @ParseOrder(3)
        @VSubcommand({"take", "remove"})
        @Description("Removes currency from a player")
        public void take(
                final TestCommandSource actor,
                final @Named("currency") String currency,
                final @Named("player") String target,
                final @Named("amount") double amount
        ) {
        }

        @ParseOrder(4)
        @VSubcommand("set")
        @Description("Sets a player's currency balance")
        public void set(
                final TestCommandSource actor,
                final @Named("currency") String currency,
                final @Named("player") String target,
                final @Named("amount") double amount
        ) {
        }
    }
}
