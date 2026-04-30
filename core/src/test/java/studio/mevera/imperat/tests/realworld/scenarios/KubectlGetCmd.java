package studio.mevera.imperat.tests.realworld.scenarios;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Models {@code kubectl get <resource> [name] [-n <ns>] [-o <format>] [--all-namespaces] [-l <selector>]}.
 *
 * <p>Real-world reference:
 * {@code kubectl get pods my-pod -n production -o yaml -l app=web}.</p>
 */
@RootCommand("kubectl")
public final class KubectlGetCmd {

    public static volatile String LAST_RESOURCE;
    public static volatile String LAST_NAME;
    public static volatile String LAST_NAMESPACE;
    public static volatile String LAST_OUTPUT;
    public static volatile Boolean LAST_ALL_NAMESPACES;
    public static volatile String LAST_SELECTOR;

    @SubCommand("get")
    public static final class Get {
        @Execute
        public void run(
                TestCommandSource sender,
                @Named("resource") String resource,
                @Flag({"namespace", "n"}) @Default("default") String namespace,
                @Flag({"output", "o"}) String output,
                @Switch("all-namespaces") Boolean allNamespaces,
                @Flag({"selector", "l"}) String selector,
                @Named("name") @Optional String name
        ) {
            LAST_RESOURCE = resource;
            LAST_NAME = name;
            LAST_NAMESPACE = namespace;
            LAST_OUTPUT = output;
            LAST_ALL_NAMESPACES = allNamespaces;
            LAST_SELECTOR = selector;
        }
    }
}
