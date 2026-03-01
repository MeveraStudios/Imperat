package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("root")
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