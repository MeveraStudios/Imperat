package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("multopts")
public class MultipleOptionals {

    @Execute
    public void t(TestSource source,
            @Optional @Suggest("hi") String opt1,
            @Optional @Suggest("7.5") Double opt2,
            @Suggest("stop-point") String req1) {

    }

    @RootCommand("sametype")
    public void sametype(TestSource source,
            @Optional @Suggest({"option1", "option2"}) String opt1,
            @Optional @Suggest({"option3", "option4"}) String opt2,
            @Suggest("final") String required) {
    }

    @RootCommand("chain")
    public void chain(TestSource source,
            @Optional @Suggest({"text1", "text2"}) String opt1,
            @Optional @Suggest({"100", "200"}) Integer opt2,
            @Optional @Suggest({"3.14", "2.71"}) Double opt3,
            @Suggest("final") String required) {
    }

    @RootCommand("optreq")
    public void optreq(TestSource source,
            @Optional @Suggest("opt1-value") String optional1,
            @Suggest("required-value") String required,
            @Optional @Suggest("opt2-value") String optional2) {
    }

    @RootCommand("mixedcmd")
    public void mixedcmd(TestSource source,
            @Optional @Suggest({"first", "second"}) String optional1,
            @Suggest("999") Integer required,
            @Optional @Suggest("3.5") Double optional2) {
    }

    @RootCommand("deep")
    public void deep(TestSource source,
            @Suggest("req1") String required1,
            @Optional @Suggest({"opt1-a", "opt1-b"}) String optional1,
            @Optional @Suggest({"7", "8"}) Integer optional2,
            @Suggest({"end1", "end2"}) String required2) {
    }

    @RootCommand("simple")
    public void simple(TestSource source,
            @Optional @Suggest({"Player1", "Player2"}) String player,
            @Suggest({"50", "100", "200"}) Integer amount) {
    }

    @RootCommand("branch")
    public void branch(TestSource source,
            @Suggest("item1") String item,
            @Optional @Suggest({"path1", "path2"}) String path,
            @Optional @Suggest({"10", "20"}) Integer count,
            @Suggest("end") String destination) {
    }

    @RootCommand("allopts")
    public void allopts(TestSource source,
            @Optional @Suggest({"str1", "str2"}) String stringOpt,
            @Optional @Suggest({"1", "2"}) Integer intOpt,
            @Optional @Suggest({"1.5", "2.5"}) Double doubleOpt,
            @Optional @Suggest({"true", "false"}) Boolean boolOpt) {
    }

    @RootCommand("empty")
    public void empty(TestSource source,
            @Suggest("val1") String value1,
            @Suggest("val2") String value2,
            @Optional @Suggest("opt1") String optional1,
            @Optional @Suggest("5") Integer optional2,
            @Suggest("end") String required) {
    }

}
