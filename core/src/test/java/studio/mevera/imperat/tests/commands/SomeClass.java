package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

@Command("root")
public class SomeClass {


    @SubCommand("i1")
    public class InnerOne {

        @Execute
        public void def(TestSource source) {
        }

        @SubCommand("i1.1")
        public class InnerOne2 {

            @Execute
            public void def(TestSource source) {
            }

            @SubCommand("i1.1.1")
            public class InnerOne3 {

                @Execute
                public void def(TestSource source) {
                }

            }

        }


    }

    @SubCommand("i2")
    public class InnerTwo {

        @Execute
        public void def(TestSource source) {
        }

        @SubCommand("i2.1")
        public class InnerTwo2 {

            @Execute
            public void def(TestSource source) {
            }

            @SubCommand("i2.2")
            public class InnerTwo3 {

                @Execute
                public void def(TestSource source) {
                }

            }

        }

    }

}