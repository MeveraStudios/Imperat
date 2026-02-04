package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("root")
public class SomeClass {


    @SubCommand("i1")
    public class InnerOne {

        @Usage
        public void def(TestSource source) {
        }

        @SubCommand("i1.1")
        public class InnerOne2 {

            @Usage
            public void def(TestSource source) {
            }

            @SubCommand("i1.1.1")
            public class InnerOne3 {

                @Usage
                public void def(TestSource source) {
                }

            }

        }


    }

    @SubCommand("i2")
    public class InnerTwo {

        @Usage
        public void def(TestSource source) {
        }

        @SubCommand("i2.1")
        public class InnerTwo2 {

            @Usage
            public void def(TestSource source) {
            }

            @SubCommand("i2.2")
            public class InnerTwo3 {

                @Usage
                public void def(TestSource source) {
                }

            }

        }

    }

}