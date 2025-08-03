package studio.mevera.imperat.adventure;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.help.HelpProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * A highly customizable help provider that renders commands in a tree-like structure.
 * Supports custom themes, formatting options, and advanced styling.
 *
 * @param <S> The source type for command execution, extending Source
 */
public class TreeHelpProvider<S extends Source> implements HelpProvider<S> {

    /**
     * Configuration class to customize the appearance of the help display
     */
    @Getter
    public static class Theme {
        // Getters and setters with builder pattern
        // Visual elements
        private Component prefix = Component.text("/", NamedTextColor.GRAY);
        private Component branch = Component.text("├─ ", NamedTextColor.DARK_GRAY);
        private Component lastBranch = Component.text("└─ ", NamedTextColor.DARK_GRAY);
        private Component indent = Component.text("│  ", NamedTextColor.DARK_GRAY);
        private Component emptyIndent = Component.text("   ", NamedTextColor.DARK_GRAY);

        // Text colors
        private TextColor commandNameColor = NamedTextColor.GOLD;
        private TextColor subCommandNameColor = NamedTextColor.YELLOW;
        private TextColor usageColor = NamedTextColor.AQUA;
        private TextColor descriptionColor = NamedTextColor.WHITE;
        private TextColor separatorColor = NamedTextColor.GRAY;
        private TextColor headerColor = NamedTextColor.GRAY;
        private TextColor footerColor = NamedTextColor.GRAY;

        // Text decorations
        private List<TextDecoration> commandNameDecorations = new ArrayList<>();
        private List<TextDecoration> subCommandNameDecorations = new ArrayList<>();
        private List<TextDecoration> usageDecorations = new ArrayList<>();
        private List<TextDecoration> descriptionDecorations = new ArrayList<>();
        private TextDecoration[] headerDecorations = new TextDecoration[0], footerDecorations = new TextDecoration[0];


        // Separators and formatting
        private String separator = " - ";
        private String usagePrefix = " ";
        private boolean showDescriptions = true;
        private boolean showUsage = true;
        private boolean showSubCommandCount = false;
        private boolean showFooter = true;
        private boolean showHeader = true;

        // Header/footer messages
        private String headerMessage = "Available subcommands:";
        private String noCommandsMessage = "This command has no additional parameters.";
        private String footerMessage = "Contact an admin for more information";

        // Indentation options
        private int maxDepth = -1; // -1 means no limit
        private boolean compactView = false;

        public Theme() {}

        public Theme setPrefix(Component prefix) {
            this.prefix = prefix;
            return this;
        }

        public Theme setPrefix(String prefix, TextColor color) {
            this.prefix = Component.text(prefix, color);
            return this;
        }

        public Theme setBranch(Component branch) {
            this.branch = branch;
            return this;
        }

        public Theme setBranch(String branch, TextColor color) {
            this.branch = Component.text(branch, color);
            return this;
        }

        public Theme setLastBranch(Component lastBranch) {
            this.lastBranch = lastBranch;
            return this;
        }

        public Theme setLastBranch(String lastBranch, TextColor color) {
            this.lastBranch = Component.text(lastBranch, color);
            return this;
        }

        public Theme setIndent(Component indent) {
            this.indent = indent;
            return this;
        }

        public Theme setIndent(String indent, TextColor color) {
            this.indent = Component.text(indent, color);
            return this;
        }

        public Theme setEmptyIndent(Component emptyIndent) {
            this.emptyIndent = emptyIndent;
            return this;
        }

        public Theme setEmptyIndent(String emptyIndent, TextColor color) {
            this.emptyIndent = Component.text(emptyIndent, color);
            return this;
        }

        public Theme setCommandNameColor(TextColor commandNameColor) {
            this.commandNameColor = commandNameColor;
            return this;
        }

        public Theme setCommandNameColor(String hexColor) {
            this.commandNameColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setSubCommandNameColor(TextColor subCommandNameColor) {
            this.subCommandNameColor = subCommandNameColor;
            return this;
        }

        public Theme setSubCommandNameColor(String hexColor) {
            this.subCommandNameColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setUsageColor(TextColor usageColor) {
            this.usageColor = usageColor;
            return this;
        }

        public Theme setUsageColor(String hexColor) {
            this.usageColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setDescriptionColor(TextColor descriptionColor) {
            this.descriptionColor = descriptionColor;
            return this;
        }

        public Theme setDescriptionColor(String hexColor) {
            this.descriptionColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setSeparatorColor(TextColor separatorColor) {
            this.separatorColor = separatorColor;
            return this;
        }

        public Theme setSeparatorColor(String hexColor) {
            this.separatorColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setHeaderColor(TextColor headerColor) {
            this.headerColor = headerColor;
            return this;
        }

        public Theme setHeaderColor(String hexColor) {
            this.headerColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setFooterColor(TextColor footerColor) {
            this.footerColor = footerColor;
            return this;
        }

        public Theme setFooterColor(String hexColor) {
            this.footerColor = TextColor.fromHexString(hexColor);
            return this;
        }

        public Theme setHeaderDecorations(TextDecoration... decorations) {
            this.headerDecorations = decorations;
            return this;
        }

        public Theme setFooterDecorations(TextDecoration... decorations) {
            this.footerDecorations = decorations;
            return this;
        }

        public Theme setCommandNameDecorations(List<TextDecoration> commandNameDecorations) {
            this.commandNameDecorations = commandNameDecorations;
            return this;
        }

        public Theme setCommandNameDecorations(TextDecoration... decorations) {
            this.commandNameDecorations = Arrays.asList(decorations);
            return this;
        }

        public Theme setSubCommandNameDecorations(List<TextDecoration> subCommandNameDecorations) {
            this.subCommandNameDecorations = subCommandNameDecorations;
            return this;
        }

        public Theme setSubCommandNameDecorations(TextDecoration... decorations) {
            this.subCommandNameDecorations = Arrays.asList(decorations);
            return this;
        }

        public Theme setUsageDecorations(List<TextDecoration> usageDecorations) {
            this.usageDecorations = usageDecorations;
            return this;
        }

        public Theme setUsageDecorations(TextDecoration... decorations) {
            this.usageDecorations = Arrays.asList(decorations);
            return this;
        }

        public Theme setDescriptionDecorations(List<TextDecoration> descriptionDecorations) {
            this.descriptionDecorations = descriptionDecorations;
            return this;
        }

        public Theme setDescriptionDecorations(TextDecoration... decorations) {
            this.descriptionDecorations = Arrays.asList(decorations);
            return this;
        }

        public Theme setSeparator(String separator) {
            this.separator = separator;
            return this;
        }

        public Theme setUsagePrefix(String usagePrefix) {
            this.usagePrefix = usagePrefix;
            return this;
        }

        public Theme setShowDescriptions(boolean showDescriptions) {
            this.showDescriptions = showDescriptions;
            return this;
        }

        public Theme setShowUsage(boolean showUsage) {
            this.showUsage = showUsage;
            return this;
        }

        public Theme setShowSubCommandCount(boolean showSubCommandCount) {
            this.showSubCommandCount = showSubCommandCount;
            return this;
        }

        public Theme setShowFooter(boolean showFooter) {
            this.showFooter = showFooter;
            return this;
        }

        public Theme setShowHeader(boolean showHeader) {
            this.showHeader = showHeader;
            return this;
        }

        public Theme setHeaderMessage(String headerMessage) {
            this.headerMessage = headerMessage;
            return this;
        }

        public Theme setNoCommandsMessage(String noCommandsMessage) {
            this.noCommandsMessage = noCommandsMessage;
            return this;
        }

        public Theme setFooterMessage(String footerMessage) {
            this.footerMessage = footerMessage;
            return this;
        }

        public Theme setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Theme setCompactView(boolean compactView) {
            this.compactView = compactView;
            return this;
        }

        // Static factory methods for predefined themes

        /**
         * Creates the default theme with standard ASCII tree branches
         */
        public static Theme createDefaultTheme() {
            return new Theme();
        }

        /**
         * Creates a minimalist theme with simplified graphics
         */
        public static Theme createMinimalTheme() {
            return new Theme()
                    .setBranch("+ ", NamedTextColor.GRAY)
                    .setLastBranch("+ ", NamedTextColor.GRAY)
                    .setIndent("  ", NamedTextColor.GRAY)
                    .setEmptyIndent("  ", NamedTextColor.GRAY)
                    .setCompactView(true);
        }

        /**
         * Creates a modern theme with stylish Unicode characters
         */
        public static Theme createModernTheme() {
            return new Theme()
                    .setBranch("├─ ", TextColor.fromHexString("#4A90E2"))
                    .setLastBranch("└─ ", TextColor.fromHexString("#4A90E2"))
                    .setIndent("│   ", TextColor.fromHexString("#4A90E2"))
                    .setEmptyIndent("    ", TextColor.fromHexString("#4A90E2"))
                    .setCommandNameColor(TextColor.fromHexString("#50C878"))
                    .setSubCommandNameColor(TextColor.fromHexString("#00BFFF"))
                    .setDescriptionColor(TextColor.fromHexString("#F5F5F5"))
                    .setUsageColor(TextColor.fromHexString("#FFD700"))
                    .setSeparatorColor(TextColor.fromHexString("#A9A9A9"))
                    .setHeaderColor(TextColor.fromHexString("#B0C4DE"))
                    .setFooterColor(TextColor.fromHexString("#B0C4DE"))
                    .setCommandNameDecorations(TextDecoration.BOLD);
        }

        /**
         * Creates a dark theme optimized for dark backgrounds
         */
        public static Theme createDarkTheme() {
            return new Theme()
                    .setBranch("├─ ", TextColor.fromHexString("#555555"))
                    .setLastBranch("└─ ", TextColor.fromHexString("#555555"))
                    .setIndent("│  ", TextColor.fromHexString("#555555"))
                    .setEmptyIndent("   ", TextColor.fromHexString("#555555"))
                    .setCommandNameColor(TextColor.fromHexString("#FF7F50"))
                    .setSubCommandNameColor(TextColor.fromHexString("#FF6347"))
                    .setDescriptionColor(TextColor.fromHexString("#CCCCCC"))
                    .setUsageColor(TextColor.fromHexString("#87CEFA"))
                    .setSeparatorColor(TextColor.fromHexString("#777777"))
                    .setHeaderColor(TextColor.fromHexString("#AAAAAA"))
                    .setFooterColor(TextColor.fromHexString("#AAAAAA"));
        }

        /**
         * Creates a vibrant theme with colorful elements
         */
        public static Theme createVibrantTheme() {
            return new Theme()
                    .setBranch("►─ ", TextColor.fromHexString("#FF00FF"))
                    .setLastBranch("▼─ ", TextColor.fromHexString("#FF00FF"))
                    .setIndent("│  ", TextColor.fromHexString("#9370DB"))
                    .setEmptyIndent("   ", TextColor.fromHexString("#9370DB"))
                    .setCommandNameColor(TextColor.fromHexString("#9251c8"))
                    .setSubCommandNameColor(TextColor.fromHexString("#cbcd37"))
                    .setDescriptionColor(TextColor.fromHexString("#F0F8FF"))
                    .setUsageColor(TextColor.fromHexString("#00FFFF"))
                    .setSeparatorColor(TextColor.fromHexString("#DA70D6"))
                    .setHeaderColor(TextColor.fromHexString("#EE82EE"))
                    .setFooterColor(TextColor.fromHexString("#EE82EE"));
        }

        /**
         * Creates an RPG-themed style suitable for fantasy games
         */
        public static Theme createRpgTheme() {
            return new Theme()
                    .setBranch("├─ ", TextColor.fromHexString("#CD853F"))
                    .setLastBranch("└─ ", TextColor.fromHexString("#CD853F"))
                    .setIndent("│  ", TextColor.fromHexString("#8B4513"))
                    .setEmptyIndent("   ", TextColor.fromHexString("#8B4513"))
                    .setCommandNameColor(TextColor.fromHexString("#FFD700"))
                    .setSubCommandNameColor(TextColor.fromHexString("#DAA520"))
                    .setDescriptionColor(TextColor.fromHexString("#F5DEB3"))
                    .setUsageColor(TextColor.fromHexString("#AFEEEE"))
                    .setSeparatorColor(TextColor.fromHexString("#A0522D"))
                    .setHeaderColor(TextColor.fromHexString("#D2B48C"))
                    .setFooterColor(TextColor.fromHexString("#D2B48C"))
                    .setHeaderMessage("⚜ Available magical commands:")
                    .setFooterMessage("Use /{command} help for ancient knowledge");
        }

        /**
         * Creates a cyberpunk-themed style
         */
        public static Theme createCyberpunkTheme() {
            return new Theme()
                    .setBranch("》 ", TextColor.fromHexString("#FF00FF"))
                    .setLastBranch("》 ", TextColor.fromHexString("#FF00FF"))
                    .setIndent("┃ ", TextColor.fromHexString("#00FFFF"))
                    .setEmptyIndent("  ", TextColor.fromHexString("#00FFFF"))
                    .setCommandNameColor(TextColor.fromHexString("#FF355E"))
                    .setSubCommandNameColor(TextColor.fromHexString("#00FF00"))
                    .setDescriptionColor(TextColor.fromHexString("#FFFFFF"))
                    .setUsageColor(TextColor.fromHexString("#FFFF00"))
                    .setSeparatorColor(TextColor.fromHexString("#FF69B4"))
                    .setHeaderColor(TextColor.fromHexString("#00FFFF"))
                    .setFooterColor(TextColor.fromHexString("#00FFFF"))
                    .setHeaderMessage("[SYSTEM] Available subroutines:")
                    .setFooterMessage("[SYSTEM] This is the documentation for /{command}")
                    .setSeparator(" :: ");
        }

        /**
         * Creates a clean, professional theme
         */
        public static Theme createProfessionalTheme() {
            return new Theme()
                    .setBranch("├── ", TextColor.fromHexString("#B0B0B0")) // Soft gray
                    .setLastBranch("└── ", TextColor.fromHexString("#B0B0B0")) // Soft gray
                    .setIndent("│   ", TextColor.fromHexString("#B0B0B0")) // Soft gray
                    .setEmptyIndent("    ", TextColor.fromHexString("#B0B0B0")) // Soft gray
                    .setCommandNameColor(TextColor.fromHexString("#4169E1")) // Royal blue
                    .setSubCommandNameColor(TextColor.fromHexString("#4169E1")) // Royal blue
                    .setDescriptionColor(TextColor.fromHexString("#5F9EA0")) // Cadet Blue
                    .setUsageColor(TextColor.fromHexString("#50C878")) // Emerald green
                    .setSeparatorColor(TextColor.fromHexString("#808080")) // Medium gray
                    .setHeaderColor(TextColor.fromHexString("#FFD700")) // Warm gold
                    .setFooterColor(TextColor.fromHexString("#C0C0C0")) // Muted silver
                    .setCommandNameDecorations(TextDecoration.BOLD); // Bold decoration
        }

        /**
         * Creates a synth-wave theme with retro 80s colors
         */
        public static Theme createSynthwaveTheme() {
            return new Theme()
                    .setBranch("▸ ", TextColor.fromHexString("#F92AFF")) // Neon pink
                    .setLastBranch("▹ ", TextColor.fromHexString("#F92AFF")) // Neon pink
                    .setIndent("│ ", TextColor.fromHexString("#A64DFF")) // Purple
                    .setEmptyIndent("  ", TextColor.fromHexString("#A64DFF")) // Purple
                    .setCommandNameColor(TextColor.fromHexString("#347338")) // Neon teal
                    .setSubCommandNameColor(TextColor.fromHexString("#72F1B8")) // Mint green
                    .setDescriptionColor(TextColor.fromHexString("#F0F0F0")) // Light gray
                    .setUsageColor(TextColor.fromHexString("#FFF568")) // Light yellow
                    .setSeparatorColor(TextColor.fromHexString("#F26DF9")) // Pink
                    .setHeaderColor(TextColor.fromHexString("#FF8B8B")) // Salmon
                    .setFooterColor(TextColor.fromHexString("#FF8B8B")) // Salmon
                    .setHeaderMessage("▓▒░ COMMAND LIST ░▒▓")
                    .setFooterMessage("▓▒░ Use /{command} for details ░▒▓")
                    .setSeparator(" → ");
        }

    }

    @Getter
    private final Theme config;
    private final ResponseHandler<S> responseHandler;
    private Function<String, String> footerCommandFormatter = cmd -> cmd;

    /**
     * Functional interface to handle how responses are sent to the source
     */
    @FunctionalInterface
    public interface ResponseHandler<S extends Source> {
        void reply(S source, Component message);
    }

    /**
     * Create a new CommandTreeHelpProvider with default configuration
     * @param responseHandler The handler that will send messages to the source
     */
    public TreeHelpProvider(ResponseHandler<S> responseHandler) {
        this.config = new Theme();
        this.responseHandler = responseHandler;
    }

    /**
     * Create a new CommandTreeHelpProvider with custom configuration
     * @param config The format configuration to use
     * @param responseHandler The handler that will send messages to the source
     */
    public TreeHelpProvider(Theme config, ResponseHandler<S> responseHandler) {
        this.config = config;
        this.responseHandler = responseHandler;
    }

    /**
     * Set a formatter to customize the command name in the footer message
     * @param formatter A function that accepts and returns a string
     * @return This provider instance for chaining
     */
    public TreeHelpProvider<S> setFooterCommandFormatter(Function<String, String> formatter) {
        this.footerCommandFormatter = formatter;
        return this;
    }

    @Override
    public void provide(ExecutionContext<S> context, S source) {
        Command<S> command = context.command();

        // Display main command with usage
        TextComponent.Builder headerBuilder = Component.text();
        headerBuilder.append(config.getPrefix());

        // Apply decorations to command name
        Component nameComponent = Component.text(command.name(), config.getCommandNameColor());
        for (TextDecoration decoration : config.getCommandNameDecorations()) {
            nameComponent = nameComponent.decoration(decoration, true);
        }
        headerBuilder.append(nameComponent);

        // Add the root command's usage if enabled
        if (config.isShowUsage()) {
            String mainUsage = CommandUsage.format((String)null, command.getMainUsage());
            if (!mainUsage.isEmpty()) {
                Component usageComponent = Component.text(config.getUsagePrefix() + mainUsage, config.getUsageColor());
                for (TextDecoration decoration : config.getUsageDecorations()) {
                    usageComponent = usageComponent.decoration(decoration, true);
                }
                headerBuilder.append(usageComponent);
            }
        }

        // Add description if available and enabled
        if (config.isShowDescriptions() && !command.description().isEmpty()) {
            Component descComponent = Component.text(config.getSeparator(), config.getSeparatorColor())
                    .append(Component.text(command.description().toString(), config.getDescriptionColor()));
            for (TextDecoration decoration : config.getDescriptionDecorations()) {
                descComponent = descComponent.decoration(decoration, true);
            }
            headerBuilder.append(descComponent);
        }

        responseHandler.reply(source, headerBuilder.build());

        // Display subcommands
        List<Command<S>> subCommands = new ArrayList<>(command.getSubCommands());
        if (subCommands.isEmpty()) {
            // No need to show usage again if we already showed it in the header and it's not empty
            String mainUsage = CommandUsage.format((String)null, command.getMainUsage());
            if (mainUsage.isEmpty() || !config.isShowUsage()) {
                responseHandler.reply(source, Component.text(config.getNoCommandsMessage(), config.getHeaderColor()));
            }
        } else {
            // Show header if enabled
            if (config.isShowHeader()) {
                responseHandler.reply(source, Component.text(config.getHeaderMessage(), config.getHeaderColor()).decorate(config.getHeaderDecorations()));
            }

            // Process each subcommand
            for (int i = 0; i < subCommands.size(); i++) {
                boolean isLast = i == subCommands.size() - 1;
                displaySubCommand(source, subCommands.get(i), isLast, new ArrayList<>(), 0);
            }

            // Show footer if enabled
            if (config.isShowFooter()) {
                String footerText = config.getFooterMessage().replace("{command}",
                        footerCommandFormatter.apply(command.name()));
                responseHandler.reply(source, Component.text(footerText, config.getFooterColor()).decorate(config.getFooterDecorations()));
            }
        }
    }

    private void displaySubCommand(S source, Command<S> command, boolean isLast,
                                   List<Boolean> indentationPattern, int depth) {
        // Check if we've reached the max depth
        if (config.getMaxDepth() > 0 && depth >= config.getMaxDepth()) {
            return;
        }

        Component prefix = buildPrefix(indentationPattern, isLast);

        // Build the command name component with decorations
        Component nameComponent = Component.text(command.name(), config.getSubCommandNameColor());
        for (TextDecoration decoration : config.getSubCommandNameDecorations()) {
            nameComponent = nameComponent.decoration(decoration, true);
        }

        // Add usage if applicable and enabled
        Component usageComponent = Component.empty();
        List<Command<S>> children = new ArrayList<>(command.getSubCommands());

        if (config.isShowUsage()) {
            usageComponent = formatUsage(command.getMainUsage());
        }

        // Add description if enabled
        Component descriptionComponent = Component.empty();
        if (config.isShowDescriptions() && !command.description().isEmpty()) {
            descriptionComponent = Component.text(config.getSeparator(), config.getSeparatorColor())
                    .append(Component.text(command.description().toString(), config.getDescriptionColor()));
            for (TextDecoration decoration : config.getDescriptionDecorations()) {
                descriptionComponent = descriptionComponent.decoration(decoration, true);
            }
        }

        // Add subcommand count if enabled
        if (config.isShowSubCommandCount() && !children.isEmpty()) {
            String countText = " [" + children.size() + "]";
            descriptionComponent = descriptionComponent.append(
                    Component.text(countText, config.getSeparatorColor()));
        }

        // Send the complete line
        responseHandler.reply(source, Component.empty()
                .append(prefix)
                .append(nameComponent)
                .append(usageComponent)
                .append(descriptionComponent));

        // Handle children with indentation if there are any
        if (!children.isEmpty()) {
            List<Boolean> newPattern = new ArrayList<>(indentationPattern);
            newPattern.add(!isLast);

            for (int i = 0; i < children.size(); i++) {
                boolean isLastChild = i == children.size() - 1;
                displaySubCommand(source, children.get(i), isLastChild, newPattern, depth + 1);
            }
        }
    }

    private Component buildPrefix(List<Boolean> indentationPattern, boolean isLast) {
        Component prefix = Component.empty();

        // Handle compact view option
        if (config.isCompactView()) {
            // In compact view, we don't add the full indentation, just enough to align
            for (int i = 0; i < indentationPattern.size(); i++) {
                prefix = prefix.append(config.getEmptyIndent());
            }
        } else {
            // Standard view with full tree visualization
            for (Boolean shouldIndent : indentationPattern) {
                prefix = prefix.append(shouldIndent ? config.getIndent() : config.getEmptyIndent());
            }
        }

        // Add branch symbol
        prefix = prefix.append(isLast ? config.getLastBranch() : config.getBranch());

        return prefix;
    }

    private Component formatUsage(CommandUsage<S> commandUsage) {
        if (commandUsage == null || commandUsage.size() == 0) {
            return Component.empty();
        }
        String usage = CommandUsage.format((String)null, commandUsage);
        Component usageComponent = Component.text(config.getUsagePrefix() + usage, config.getUsageColor());
        for (TextDecoration decoration : config.getUsageDecorations()) {
            usageComponent = usageComponent.decoration(decoration, true);
        }
        return usageComponent;
    }
}
