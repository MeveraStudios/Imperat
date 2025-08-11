package studio.mevera.imperat.command.tree.help;

import java.util.function.Function;

/**
 * A highly customizable theme system for help rendering.
 * Supports color codes, formatting, and various visual styles.
 */
public class HelpTheme {
    
    // Visual elements
    private String prefix = "/";
    private String branch = "├─ ";
    private String lastBranch = "└─ ";
    private String indent = "│  ";
    private String emptyIndent = "   ";
    private String bulletPoint = "•";
    
    // Brackets and symbols
    private String optionalOpen = "[";
    private String optionalClose = "]";
    private String requiredOpen = "<";
    private String requiredClose = ">";
    private String flagPrefix = "--";
    private String separator = " - ";
    private String usagePrefix = " ";
    
    // Color codes (can be Minecraft codes like &6, or ANSI, or custom)
    private String commandColor = "";
    private String subcommandColor = "";
    private String argumentColor = "";
    private String descriptionColor = "";
    private String syntaxColor = "";
    private String headerColor = "";
    private String footerColor = "";
    private String separatorColor = "";
    private String resetColor = "";
    
    // Messages
    private String headerMessage = "Available commands:";
    private String noCommandsMessage = "No additional parameters available.";
    private String footerMessage = "Use /{command} --help for more information";
    
    // Display options
    private boolean showDescriptions = true;
    private boolean showPermissions = false;
    private boolean showUsage = true;
    private boolean showExamples = false;
    private boolean showAliases = false;
    private boolean showSubcommandCount = false;
    private boolean showHeader = true;
    private boolean showFooter = true;
    
    // Layout options
    private int maxDepth = -1; // -1 means unlimited
    private boolean compactView = false;
    private int indentSize = 2;
    
    // Formatting functions
    private Function<String, String> commandFormatter = Function.identity();
    private Function<String, String> argumentFormatter = Function.identity();
    private Function<String, String> descriptionFormatter = Function.identity();
    
    // Builder methods for visual elements
    public HelpTheme prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }
    
    public HelpTheme branch(String branch) {
        this.branch = branch;
        return this;
    }
    
    public HelpTheme lastBranch(String lastBranch) {
        this.lastBranch = lastBranch;
        return this;
    }
    
    public HelpTheme indent(String indent) {
        this.indent = indent;
        return this;
    }
    
    public HelpTheme emptyIndent(String emptyIndent) {
        this.emptyIndent = emptyIndent;
        return this;
    }
    
    public HelpTheme bulletPoint(String bulletPoint) {
        this.bulletPoint = bulletPoint;
        return this;
    }
    
    // Builder methods for brackets and symbols
    public HelpTheme optionalOpen(String optionalOpen) {
        this.optionalOpen = optionalOpen;
        return this;
    }
    
    public HelpTheme optionalClose(String optionalClose) {
        this.optionalClose = optionalClose;
        return this;
    }
    
    public HelpTheme requiredOpen(String requiredOpen) {
        this.requiredOpen = requiredOpen;
        return this;
    }
    
    public HelpTheme requiredClose(String requiredClose) {
        this.requiredClose = requiredClose;
        return this;
    }
    
    public HelpTheme flagPrefix(String flagPrefix) {
        this.flagPrefix = flagPrefix;
        return this;
    }
    
    public HelpTheme separator(String separator) {
        this.separator = separator;
        return this;
    }
    
    public HelpTheme usagePrefix(String usagePrefix) {
        this.usagePrefix = usagePrefix;
        return this;
    }
    
    // Builder methods for colors (individual)
    public HelpTheme commandColor(String commandColor) {
        this.commandColor = commandColor;
        return this;
    }
    
    public HelpTheme subcommandColor(String subcommandColor) {
        this.subcommandColor = subcommandColor;
        return this;
    }
    
    public HelpTheme argumentColor(String argumentColor) {
        this.argumentColor = argumentColor;
        return this;
    }
    
    public HelpTheme descriptionColor(String descriptionColor) {
        this.descriptionColor = descriptionColor;
        return this;
    }
    
    public HelpTheme syntaxColor(String syntaxColor) {
        this.syntaxColor = syntaxColor;
        return this;
    }
    
    public HelpTheme headerColor(String headerColor) {
        this.headerColor = headerColor;
        return this;
    }
    
    public HelpTheme footerColor(String footerColor) {
        this.footerColor = footerColor;
        return this;
    }
    
    public HelpTheme separatorColor(String separatorColor) {
        this.separatorColor = separatorColor;
        return this;
    }
    
    public HelpTheme resetColor(String resetColor) {
        this.resetColor = resetColor;
        return this;
    }
    
    // Convenience method to set multiple colors at once
    public HelpTheme colors(String command, String subcommand, String argument, String description) {
        this.commandColor = command;
        this.subcommandColor = subcommand;
        this.argumentColor = argument;
        this.descriptionColor = description;
        return this;
    }
    
    // Convenience method to set all colors
    public HelpTheme allColors(String command, String subcommand, String argument, String description,
                               String syntax, String header, String footer, String separator, String reset) {
        this.commandColor = command;
        this.subcommandColor = subcommand;
        this.argumentColor = argument;
        this.descriptionColor = description;
        this.syntaxColor = syntax;
        this.headerColor = header;
        this.footerColor = footer;
        this.separatorColor = separator;
        this.resetColor = reset;
        return this;
    }
    
    // Convenience method for brackets
    public HelpTheme brackets(String optOpen, String optClose, String reqOpen, String reqClose) {
        this.optionalOpen = optOpen;
        this.optionalClose = optClose;
        this.requiredOpen = reqOpen;
        this.requiredClose = reqClose;
        return this;
    }
    
    // Builder methods for messages
    public HelpTheme headerMessage(String headerMessage) {
        this.headerMessage = headerMessage;
        return this;
    }
    
    public HelpTheme noCommandsMessage(String noCommandsMessage) {
        this.noCommandsMessage = noCommandsMessage;
        return this;
    }
    
    public HelpTheme footerMessage(String footerMessage) {
        this.footerMessage = footerMessage;
        return this;
    }
    
    // Builder methods for display options
    public HelpTheme showDescriptions(boolean showDescriptions) {
        this.showDescriptions = showDescriptions;
        return this;
    }
    
    public HelpTheme showPermissions(boolean showPermissions) {
        this.showPermissions = showPermissions;
        return this;
    }
    
    public HelpTheme showUsage(boolean showUsage) {
        this.showUsage = showUsage;
        return this;
    }
    
    public HelpTheme showExamples(boolean showExamples) {
        this.showExamples = showExamples;
        return this;
    }
    
    public HelpTheme showAliases(boolean showAliases) {
        this.showAliases = showAliases;
        return this;
    }
    
    public HelpTheme showSubcommandCount(boolean showSubcommandCount) {
        this.showSubcommandCount = showSubcommandCount;
        return this;
    }
    
    public HelpTheme showHeader(boolean showHeader) {
        this.showHeader = showHeader;
        return this;
    }
    
    public HelpTheme showFooter(boolean showFooter) {
        this.showFooter = showFooter;
        return this;
    }
    
    // Builder methods for layout options
    public HelpTheme maxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }
    
    public HelpTheme compactView(boolean compactView) {
        this.compactView = compactView;
        return this;
    }
    
    public HelpTheme indentSize(int indentSize) {
        this.indentSize = indentSize;
        return this;
    }
    
    // Builder methods for formatting functions
    public HelpTheme commandFormatter(Function<String, String> commandFormatter) {
        this.commandFormatter = commandFormatter;
        return this;
    }
    
    public HelpTheme argumentFormatter(Function<String, String> argumentFormatter) {
        this.argumentFormatter = argumentFormatter;
        return this;
    }
    
    public HelpTheme descriptionFormatter(Function<String, String> descriptionFormatter) {
        this.descriptionFormatter = descriptionFormatter;
        return this;
    }
    
    // Convenience builder methods to disable/enable all display options
    public HelpTheme showAll() {
        this.showDescriptions = true;
        this.showPermissions = true;
        this.showUsage = true;
        this.showExamples = true;
        this.showAliases = true;
        this.showSubcommandCount = true;
        this.showHeader = true;
        this.showFooter = true;
        return this;
    }
    
    public HelpTheme hideAll() {
        this.showDescriptions = false;
        this.showPermissions = false;
        this.showUsage = false;
        this.showExamples = false;
        this.showAliases = false;
        this.showSubcommandCount = false;
        this.showHeader = false;
        this.showFooter = false;
        return this;
    }
    
    // Formatting methods
    public String formatCommand(String command) {
        return commandColor + commandFormatter.apply(command) + resetColor;
    }
    
    public String formatSubcommand(String subcommand) {
        return subcommandColor + commandFormatter.apply(subcommand) + resetColor;
    }
    
    public String formatArgument(String arg, boolean optional) {
        String brackets = optional ?
                optionalOpen + arg + optionalClose :
                requiredOpen + arg + requiredClose;
        return argumentColor + argumentFormatter.apply(brackets) + resetColor;
    }
    
    public String formatDescription(String description) {
        return descriptionColor + descriptionFormatter.apply(description) + resetColor;
    }
    
    public String formatSyntax(String syntax) {
        return syntaxColor + syntax + resetColor;
    }
    
    public String formatHeader(String header) {
        return headerColor + header + resetColor;
    }
    
    public String formatFooter(String footer) {
        return footerColor + footer + resetColor;
    }
    
    public String formatSeparator() {
        return separatorColor + separator + resetColor;
    }
    
    // Tree helpers
    public String getTreeBranch(boolean isLast) {
        return isLast ? lastBranch : branch;
    }
    
    public String getTreeIndent(boolean hasMore) {
        return hasMore ? indent : emptyIndent;
    }
    
    // All getters
    public String getPrefix() { return prefix; }
    public String getBranch() { return branch; }
    public String getLastBranch() { return lastBranch; }
    public String getIndent() { return indent; }
    public String getEmptyIndent() { return emptyIndent; }
    public String getBulletPoint() { return bulletPoint; }
    public String getOptionalOpen() { return optionalOpen; }
    public String getOptionalClose() { return optionalClose; }
    public String getRequiredOpen() { return requiredOpen; }
    public String getRequiredClose() { return requiredClose; }
    public String getFlagPrefix() { return flagPrefix; }
    public String getSeparator() { return separator; }
    public String getUsagePrefix() { return usagePrefix; }
    public String getCommandColor() { return commandColor; }
    public String getSubcommandColor() { return subcommandColor; }
    public String getArgumentColor() { return argumentColor; }
    public String getDescriptionColor() { return descriptionColor; }
    public String getSyntaxColor() { return syntaxColor; }
    public String getHeaderColor() { return headerColor; }
    public String getFooterColor() { return footerColor; }
    public String getSeparatorColor() { return separatorColor; }
    public String getResetColor() { return resetColor; }
    public String getHeaderMessage() { return headerMessage; }
    public String getNoCommandsMessage() { return noCommandsMessage; }
    public String getFooterMessage() { return footerMessage; }
    public boolean isShowDescriptions() { return showDescriptions; }
    public boolean isShowPermissions() { return showPermissions; }
    public boolean isShowUsage() { return showUsage; }
    public boolean isShowExamples() { return showExamples; }
    public boolean isShowAliases() { return showAliases; }
    public boolean isShowSubcommandCount() { return showSubcommandCount; }
    public boolean isShowHeader() { return showHeader; }
    public boolean isShowFooter() { return showFooter; }
    public boolean isCompactView() { return compactView; }
    public int getMaxDepth() { return maxDepth; }
    public int getIndentSize() { return indentSize; }
    public Function<String, String> getCommandFormatter() { return commandFormatter; }
    public Function<String, String> getArgumentFormatter() { return argumentFormatter; }
    public Function<String, String> getDescriptionFormatter() { return descriptionFormatter; }
    
    // Predefined themes (updated to use new builder methods)
    public static HelpTheme defaultTheme() {
        return new HelpTheme();
    }
    
    public static HelpTheme minimalTheme() {
        return new HelpTheme()
                .branch("+ ")
                .lastBranch("+ ")
                .indent("  ")
                .emptyIndent("  ")
                .showSubcommandCount(false)
                .compactView(true);
    }
    
    public static HelpTheme modernTheme() {
        return new HelpTheme()
                .branch("├─▶ ")
                .lastBranch("└─▶ ")
                .indent("│   ")
                .emptyIndent("    ")
                .commandColor("&b")
                .subcommandColor("&e")
                .argumentColor("&6")
                .descriptionColor("&7")
                .separator(" :: ");
    }
    
    public static HelpTheme asciiArtTheme() {
        return new HelpTheme()
                .branch("╠══ ")
                .lastBranch("╚══ ")
                .indent("║   ")
                .emptyIndent("    ")
                .bulletPoint("◆")
                .brackets("⌈", "⌋", "《", "》");
    }
    
    public static HelpTheme rpgTheme() {
        return new HelpTheme()
                .branch("├─⚔ ")
                .lastBranch("└─⚔ ")
                .indent("│   ")
                .emptyIndent("    ")
                .commandColor("&6")
                .subcommandColor("&e")
                .argumentColor("&b")
                .descriptionColor("&f")
                .headerMessage("⚜ Available magical commands:")
                .footerMessage("⚜ Seek wisdom with /{command} --help");
    }
    
    public static HelpTheme cyberpunkTheme() {
        return new HelpTheme()
                .branch("》")
                .lastBranch("》")
                .indent("┃ ")
                .emptyIndent("  ")
                .commandColor("&d")
                .subcommandColor("&a")
                .argumentColor("&e")
                .descriptionColor("&f")
                .separator(" :: ")
                .headerMessage("[SYSTEM] Available subroutines:")
                .footerMessage("[SYSTEM] Access documentation: /{command} --help");
    }
    
    public static HelpTheme synthwaveTheme() {
        return new HelpTheme()
                .branch("▸ ")
                .lastBranch("▹ ")
                .indent("│ ")
                .emptyIndent("  ")
                .commandColor("&d")
                .subcommandColor("&b")
                .argumentColor("&e")
                .descriptionColor("&f")
                .headerMessage("▓▒░ COMMAND LIST ░▒▓")
                .footerMessage("▓▒░ Use /{command} --help ░▒▓")
                .separator(" → ");
    }
    
    public static HelpTheme markdownTheme() {
        return new HelpTheme()
                .branch("- ")
                .lastBranch("- ")
                .indent("  ")
                .emptyIndent("  ")
                .prefix("## ")
                .commandFormatter(cmd -> "**" + cmd + "**")
                .argumentFormatter(arg -> "`" + arg + "`")
                .separator(" - ")
                .showHeader(false)
                .showFooter(false);
    }
}