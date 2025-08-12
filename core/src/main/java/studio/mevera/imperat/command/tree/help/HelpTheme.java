package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.tree.help.renderers.HelpTextItem;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import java.util.function.UnaryOperator;

/**
 * A streamlined, fully-utilized theme system for help rendering.
 */
public class HelpTheme<S extends Source> {
    
    // Tree Structure Elements
    private String branch = "├─ ";
    private String lastBranch = "└─ ";
    private String indent = "│  ";
    private String emptyIndent = "   ";
    
    // List Elements
    private String bulletPoint = "• ";
    private String commandPrefix = "/";
    
    // Argument Formatting
    private String optionalOpen = "[";
    private String optionalClose = "]";
    private String requiredOpen = "<";
    private String requiredClose = ">";
    private String flagPrefix = "-";
    
    // Separators
    private String descriptionSeparator = " - ";
    private String pathSeparator = " ";
    
    // Colors/Formatting
    private UnaryOperator<String> commandFormatter = UnaryOperator.identity();
    private UnaryOperator<String> subcommandFormatter = UnaryOperator.identity();
    private UnaryOperator<String> argumentFormatter = UnaryOperator.identity();
    private UnaryOperator<String> descriptionFormatter = UnaryOperator.identity();
    private UnaryOperator<String> permissionFormatter = UnaryOperator.identity();
    private UnaryOperator<String> headerFormatter = UnaryOperator.identity();
    private UnaryOperator<String> footerFormatter = UnaryOperator.identity();
    
    // Dynamic Messages
    private HelpTextItem<S> headerMessage = (ctx) ->
            "━━━━━━━━ Help: " + ctx.command().name() + " ━━━━━━━━";
    private HelpTextItem<S> footerMessage = (ctx) ->
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private HelpTextItem<S> emptyMessage = (ctx) ->
            "No commands available.";
    
    // Display Control
    private boolean showHeader = true;
    private boolean showFooter = true;
    private boolean showDescriptions = true;
    private boolean showPermissions = false;
    private boolean showExamples = false;
    private boolean showFullPath = true;
    private boolean showRootCommand = true;
    
    // Layout Control
    private boolean compactMode = false;
    private int indentMultiplier = 1; // Multiplies indent width
    
    // Formatters with proper parameter handling
    public String formatCommand(String command) {
        return commandFormatter.apply(command);
    }
    
    public String formatSubcommand(String subcommand) {
        return subcommandFormatter.apply(subcommand);
    }
    
    public String formatArgument(String arg, boolean optional) {
        String formatted = optional ?
                optionalOpen + arg + optionalClose :
                requiredOpen + arg + requiredClose;
        return argumentFormatter.apply(formatted);
    }
    
    public String formatFlag(String flag) {
        return argumentFormatter.apply(flagPrefix + flag);
    }
    
    public String formatDescription(String description) {
        if (description == null || description.isEmpty()) return "";
        return descriptionFormatter.apply(description);
    }
    
    public String formatPermission(String permission) {
        if (permission == null) return "";
        return permissionFormatter.apply("[" + permission + "]");
    }
    
    public String formatHeader(ExecutionContext<S> ctx) {
        return headerFormatter.apply(headerMessage.load(ctx));
    }
    
    public String formatFooter(ExecutionContext<S> ctx) {
        return footerFormatter.apply(footerMessage.load(ctx));
    }
    
    public String formatEmpty(ExecutionContext<S> ctx) {
        return descriptionFormatter.apply(emptyMessage.load(ctx));
    }
    
    // Tree helpers with indent multiplier
    public String getTreeBranch(boolean isLast) {
        return isLast ? lastBranch : branch;
    }
    
    public String getTreeIndent(boolean hasMore) {
        String base = hasMore ? indent : emptyIndent;
        if (indentMultiplier > 1) {
            return base.repeat(indentMultiplier);
        }
        return base;
    }
    
    // Simplified builders
    public HelpTheme<S> treeSymbols(String branch, String lastBranch, String indent, String empty) {
        this.branch = branch;
        this.lastBranch = lastBranch;
        this.indent = indent;
        this.emptyIndent = empty;
        return this;
    }
    
    public HelpTheme<S> brackets(String optOpen, String optClose, String reqOpen, String reqClose) {
        this.optionalOpen = optOpen;
        this.optionalClose = optClose;
        this.requiredOpen = reqOpen;
        this.requiredClose = reqClose;
        return this;
    }
    
    public HelpTheme<S> formatters(
            UnaryOperator<String> command,
            UnaryOperator<String> argument,
            UnaryOperator<String> description) {
        this.commandFormatter = command;
        this.subcommandFormatter = command; // Default to same as command
        this.argumentFormatter = argument;
        this.descriptionFormatter = description;
        return this;
    }
    
    public HelpTheme<S> compactMode(boolean compact) {
        this.compactMode = compact;
        if (compact) {
            this.showDescriptions = false;
            this.showPermissions = false;
            this.indentMultiplier = 0;
        }
        return this;
    }
    
    // All other setters...
    public HelpTheme<S> showDescriptions(boolean show) {
        this.showDescriptions = show;
        return this;
    }
    
    public HelpTheme<S> showPermissions(boolean show) {
        this.showPermissions = show;
        return this;
    }
    
    public HelpTheme<S> showExamples(boolean show) {
        this.showExamples = show;
        return this;
    }
    
    public static <S extends Source> HelpTheme<S> defaultTheme() {
        return new HelpTheme<>();
    }
    
    // Predefined themes
    public static <S extends Source> HelpTheme<S> minimal() {
        return new HelpTheme<S>()
                .treeSymbols("+ ", "+ ", "  ", "  ")
                .compactMode(true);
    }
    
    public static <S extends Source> HelpTheme<S> detailed() {
        return new HelpTheme<S>()
                .showDescriptions(true)
                .showPermissions(true)
                .showExamples(true);
    }
    
    public static <S extends Source> HelpTheme<S> colorful() {
        return new HelpTheme<S>()
                .formatters(
                        cmd -> "§6" + cmd + "§r",      // Gold commands
                        arg -> "§e" + arg + "§r",      // Yellow arguments
                        desc -> "§7" + desc + "§r"     // Gray descriptions
                );
    }
    
    // Getters
    public String getBranch() { return branch; }
    public String getLastBranch() { return lastBranch; }
    public String getIndent() { return indent; }
    public String getEmptyIndent() { return emptyIndent; }
    public String getBulletPoint() { return bulletPoint; }
    public String getCommandPrefix() { return commandPrefix; }
    public String getDescriptionSeparator() { return descriptionSeparator; }
    public String getPathSeparator() { return pathSeparator; }
    public boolean isShowHeader() { return showHeader; }
    public boolean isShowFooter() { return showFooter; }
    public boolean isShowDescriptions() { return showDescriptions; }
    public boolean isShowPermissions() { return showPermissions; }
    public boolean isShowExamples() { return showExamples; }
    public boolean isShowFullPath() { return showFullPath; }
    public boolean isShowRootCommand() { return showRootCommand; }
    public boolean isCompactMode() { return compactMode; }
    public int getIndentMultiplier() { return indentMultiplier; }
    
    // NEW BUILDER METHODS
    
    public HelpTheme<S> bulletPoint(String bulletPoint) {
        this.bulletPoint = bulletPoint;
        return this;
    }
    
    public HelpTheme<S> commandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
        return this;
    }
    
    public HelpTheme<S> flagPrefix(String flagPrefix) {
        this.flagPrefix = flagPrefix;
        return this;
    }
    
    public HelpTheme<S> descriptionSeparator(String descriptionSeparator) {
        this.descriptionSeparator = descriptionSeparator;
        return this;
    }
    
    public HelpTheme<S> pathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator;
        return this;
    }
    
    public HelpTheme<S> commandFormatter(UnaryOperator<String> commandFormatter) {
        this.commandFormatter = commandFormatter;
        return this;
    }
    
    public HelpTheme<S> subcommandFormatter(UnaryOperator<String> subcommandFormatter) {
        this.subcommandFormatter = subcommandFormatter;
        return this;
    }
    
    public HelpTheme<S> argumentFormatter(UnaryOperator<String> argumentFormatter) {
        this.argumentFormatter = argumentFormatter;
        return this;
    }
    
    public HelpTheme<S> descriptionFormatter(UnaryOperator<String> descriptionFormatter) {
        this.descriptionFormatter = descriptionFormatter;
        return this;
    }
    
    public HelpTheme<S> permissionFormatter(UnaryOperator<String> permissionFormatter) {
        this.permissionFormatter = permissionFormatter;
        return this;
    }
    
    public HelpTheme<S> headerFormatter(UnaryOperator<String> headerFormatter) {
        this.headerFormatter = headerFormatter;
        return this;
    }
    
    public HelpTheme<S> footerFormatter(UnaryOperator<String> footerFormatter) {
        this.footerFormatter = footerFormatter;
        return this;
    }
    
    public HelpTheme<S> headerMessage(HelpTextItem<S> headerMessage) {
        this.headerMessage = headerMessage;
        return this;
    }
    
    public HelpTheme<S> footerMessage(HelpTextItem<S> footerMessage) {
        this.footerMessage = footerMessage;
        return this;
    }
    
    public HelpTheme<S> emptyMessage(HelpTextItem<S> emptyMessage) {
        this.emptyMessage = emptyMessage;
        return this;
    }
    
    public HelpTheme<S> showHeader(boolean showHeader) {
        this.showHeader = showHeader;
        return this;
    }
    
    public HelpTheme<S> showFooter(boolean showFooter) {
        this.showFooter = showFooter;
        return this;
    }
    
    public HelpTheme<S> showFullPath(boolean showFullPath) {
        this.showFullPath = showFullPath;
        return this;
    }
    
    public HelpTheme<S> showRootCommand(boolean showRootCommand) {
        this.showRootCommand = showRootCommand;
        return this;
    }
    
    public HelpTheme<S> indentMultiplier(int indentMultiplier) {
        this.indentMultiplier = indentMultiplier;
        return this;
    }
}