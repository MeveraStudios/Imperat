package studio.mevera.imperat.annotations.base.parsers;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.MethodCommandExecutor;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class PathwaySyntaxParser<S extends CommandSource> {

    public static final String LITERAL_SPLIT = "\\|";
    private static final Pattern LITERAL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern TRUE_FLAG_PATTERN = Pattern.compile("^--?[a-zA-Z0-9_]+(\\s+<[^>]+>)?$");
    private static final Pattern SWITCH_ONLY_PATTERN = Pattern.compile("^--?[a-zA-Z0-9_]+$");
    private static final Pattern REQUIRED_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>");
    private static final Pattern OPTIONAL_PATTERN = Pattern.compile("\\[([a-zA-Z0-9_]+)]");

    private final Imperat<S> imperat;
    private final ParameterParser<S> parameterParser;
    private final CommandElementParser<S> elementParser;

    private final Map<String, Command<S>> mappedCommands = new HashMap<>();

    private PathwaySyntaxParser(
            Imperat<S> imperat,
            CommandElementParser<S> elementParser,
            ParameterParser<S> parameterParser
    ) {
        this.imperat = imperat;
        this.elementParser = elementParser;
        this.parameterParser = parameterParser;
    }

    static <S extends CommandSource> PathwaySyntaxParser<S> of(
            Imperat<S> imperat,
            CommandElementParser<S> elementParser,
            ParameterParser<S> parameterParser
    ) {
        return new PathwaySyntaxParser<>(imperat, elementParser, parameterParser);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    private static String extractArgName(String argFormat) {
        StringBuilder sb = new StringBuilder();
        for (char c : argFormat.toCharArray()) {
            if (c == '[' || c == '<' || c == ']' || c == '>' || c == '-') {
                continue;
            }
            if (Character.isWhitespace(c)) {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isLiteral(String token) {
        return LITERAL_PATTERN.matcher(token).matches();
    }

    // -------------------------------------------------------------------------
    // Core parsing
    // -------------------------------------------------------------------------

    private static boolean isRequired(String token) {
        return REQUIRED_PATTERN.matcher(token).matches();
    }

    // -------------------------------------------------------------------------
    // Pathway registration on root
    // -------------------------------------------------------------------------

    private static boolean isOptional(String token) {
        return OPTIONAL_PATTERN.matcher(token).matches();
    }

    private static boolean isSwitchOnly(String token) {
        return SWITCH_ONLY_PATTERN.matcher(token).matches();
    }

    // -------------------------------------------------------------------------
    // Argument loading
    // -------------------------------------------------------------------------

    private static boolean isTrueFlag(String token) {
        return TRUE_FLAG_PATTERN.matcher(token).matches();
    }

    // -------------------------------------------------------------------------
    // Subcommand / literal helpers
    // -------------------------------------------------------------------------

    public void loadCommand(@Nullable Command<S> providedRoot, String pathwaySyntax, MethodElement method) {
        String[] tokens = pathwaySyntax.split(" ");
        String rootName = resolveLiteralName(tokens[0]);

        mappedCommands.compute(rootName, (name, existing) -> {
            Command<S> root = (existing != null) ? existing
                                      : (providedRoot != null) ? providedRoot
                                                : createLiteralCommand(null, tokens[0]);
            parseTokens(root, tokens, method);
            return root;
        });
    }

    public Set<Command<S>> getParsedPathwayCommands() {
        return Set.copyOf(mappedCommands.values());
    }

    // -------------------------------------------------------------------------
    // Token classification
    // -------------------------------------------------------------------------

    /**
     * Walks the token array building:
     * - cumulativeArgs: the absolute arg list from root (excluding root itself)
     *   — used so every pathway registered on root carries its full prefix
     * - pendingArgs: args accumulated since the last literal
     * - newLinks: new subcommand relationships to wire after pathways are registered
     *
     * All pathways are added to ROOT's tree so the tree never needs to
     * re-merge across multiple addSubCommand levels.
     */
    private void parseTokens(Command<S> root, String[] tokens, MethodElement method) {
        Command<S> currentCommand = root;
        List<Argument<S>> pendingArgs = new ArrayList<>();
        String lastArgFormat = "";

        // Full absolute arg list from root (root itself is implied — not included)
        List<Argument<S>> cumulativeArgs = new ArrayList<>();

        // New subcommand relationships — wired AFTER pathways are registered on root
        List<PendingLink<S>> newLinks = new ArrayList<>();

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];

            if (isLiteral(token)) {
                if (!pendingArgs.isEmpty()) {
                    // No-executor pathway: full args = everything accumulated so far + pendingArgs
                    List<Argument<S>> fullArgs = new ArrayList<>(cumulativeArgs);
                    fullArgs.addAll(pendingArgs);
                    registerOnRoot(method, root, currentCommand, fullArgs, false, null);

                    // Advance cumulative state
                    cumulativeArgs = fullArgs;
                    pendingArgs.clear();
                }

                String primaryName = resolveLiteralName(token);
                Command<S> existing = currentCommand.getSubCommand(primaryName, false);
                Command<S> subCommand;

                if (existing != null) {
                    subCommand = existing;
                    // Already structurally wired — do NOT re-add to newLinks
                } else {
                    subCommand = createLiteralCommand(currentCommand, token);
                    // Defer structural wiring until pathways are registered
                    newLinks.add(new PendingLink<>(currentCommand, subCommand, lastArgFormat));
                }

                // Record the subcommand literal as part of the absolute path
                cumulativeArgs.add(subCommand);
                currentCommand = subCommand;
                lastArgFormat = "";

            } else {
                Argument<S> arg = loadArgument(method, token);
                if (arg == null) {
                    throw new IllegalArgumentException(
                            "Token '" + token + "' in pathway syntax is not a valid argument format, "
                                    + "or no matching parameter was found in method '" + method.getName() + "'"
                    );
                }
                pendingArgs.add(arg);
                lastArgFormat = token;
            }
        }

        // -------------------------------------------------------------------------
        // Phase 1 — Register the executor pathway on root with full absolute args
        // -------------------------------------------------------------------------
        List<Argument<S>> executorArgs = new ArrayList<>(cumulativeArgs);
        executorArgs.addAll(pendingArgs);
        registerOnRoot(method, root, currentCommand, executorArgs, true, method);

        // -------------------------------------------------------------------------
        // Phase 2 — Wire structural subcommand relationships
        // Subtrees are empty at this point so parseSubTree does no pathway merging,
        // only addChild for tree structure and subCommands collection registration.
        // -------------------------------------------------------------------------
        for (PendingLink<S> link : newLinks) {
            link.parent().addSubCommand(link.child(), link.attachTo());
        }
    }

    /**
     * Adds a pathway to the ROOT command's tree (not to the intermediate owning command).
     * The pathway carries the full absolute arg list so the tree sees the complete
     * path from root without needing any post-hoc merge.
     *
     * The owningCommand is stored as metadata on the pathway for execution context
     * resolution — it is NOT the command whose tree receives the pathway.
     */
    private void registerOnRoot(
            MethodElement method,
            Command<S> root,
            Command<S> owningCommand,
            List<Argument<S>> fullArgs,
            boolean withExecutor,
            @Nullable MethodElement executorMethod
    ) {
        if (pathwayAlreadyExists(root, fullArgs)) {
            return;
        }

        CommandPathway.Builder<S> builder = CommandPathway.<S>builder(method)
                                                    .arguments(fullArgs);

        builder = elementParser.processedPathway(method, builder);

        if (withExecutor && executorMethod != null) {
            builder = elementParser.finalizedPathway(executorMethod, owningCommand, builder);
            builder.execute(MethodCommandExecutor.of(imperat, executorMethod));
        }

        // Build with owningCommand (e.g. "set") for execution context,
        // but register on root so the tree stores the full absolute path.
        root.addPathway(builder.build(owningCommand));
    }

    /**
     * Checks root's dedicated pathways for an existing entry with the same
     * argument signature (names + wrapped types + order).
     */
    private boolean pathwayAlreadyExists(Command<S> root, List<Argument<S>> args) {
        outer:
        for (CommandPathway<S> existing : root.getDedicatedPathways()) {
            List<? extends Argument<S>> existingArgs = existing.getArguments();
            if (existingArgs.size() != args.size()) {
                continue;
            }
            for (int i = 0; i < args.size(); i++) {
                if (!args.get(i).getName().equals(existingArgs.get(i).getName())) {
                    continue outer;
                }
                if (!args.get(i).wrappedType().equals(existingArgs.get(i).wrappedType())) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    public Argument<S> loadArgument(MethodElement method, String formattedArg) {
        String argName = extractArgName(formattedArg);

        ParameterElement param = method.getParameters().stream()
                                         .filter(p -> p.getName().equals(argName))
                                         .findFirst()
                                         .orElse(null);

        if (param == null) {
            return null;
        }

        return parameterParser.parseParameter(param);
    }

    private Command<S> createLiteralCommand(@Nullable Command<S> parent, String literalToken) {
        String[] names = literalToken.split(LITERAL_SPLIT);
        return Argument.literal(imperat, names);
    }

    private String resolveLiteralName(String literalToken) {
        return literalToken.split(LITERAL_SPLIT)[0];
    }

    // -------------------------------------------------------------------------
    // Internal record
    // -------------------------------------------------------------------------

    private record PendingLink<S extends CommandSource>(
            Command<S> parent,
            Command<S> child,
            String attachTo
    ) {

    }
}