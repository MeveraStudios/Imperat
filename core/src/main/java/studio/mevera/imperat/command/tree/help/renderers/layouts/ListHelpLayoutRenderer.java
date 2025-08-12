package studio.mevera.imperat.command.tree.help.renderers.layouts;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

import java.util.List;

final class ListHelpLayoutRenderer<S extends Source>
        implements HelpLayoutRenderer<S, ListViewModel<S>> {
    
    @Override
    public void render(
            @NotNull ExecutionContext<S> context,
            @NotNull ListViewModel<S> model,
            @NotNull HelpEntryList<S> helpEntries,
            @NotNull HelpRenderOptions<S> options
    ) {
        S source = context.source();
        HelpTheme<S> theme = options.getTheme();
        
        // Header
        if (theme.isShowHeader()) {
            source.reply(theme.formatHeader(context));
        }
        
        // Check empty
        if (model.items().isEmpty()) {
            source.reply(theme.formatEmpty(context));
            if (theme.isShowFooter()) {
                source.reply(theme.formatFooter(context));
            }
            return;
        }
        
        for (ListViewModel.ListItem<S> item : model.items()) {
            
            StringBuilder line = new StringBuilder();
            
            // Bullet point
            line.append(theme.getBulletPoint());
            
            // Command with full path or just usage
            if (theme.isShowFullPath()) {
                line.append(theme.getCommandPrefix())
                        .append(theme.formatCommand(context.command().name()))
                        .append(theme.getPathSeparator());
            }
            
            // Format the usage parameters
            formatUsageParameters(item.usage(), line, theme);
            
            // Description
            if (theme.isShowDescriptions() && item.usage().description() != null) {
                line.append(theme.getDescriptionSeparator())
                        .append(theme.formatDescription(item.usage().description().toString()));
            }
            
            // Permission
            if (theme.isShowPermissions() && item.node().getPermission() != null) {
                line.append(" ").append(theme.formatPermission(item.node().getPermission()));
            }
            
            // Examples (if enabled)
            if (theme.isShowExamples() && !item.usage().getExamples().isEmpty()) {
                for (String example : item.usage().getExamples()) {
                    source.reply("  Example: " + theme.formatDescription(example));
                }
            }
            
            source.reply(line.toString());
        }
        
        // Footer
        if (theme.isShowFooter()) {
            source.reply(theme.formatFooter(context));
        }
    }
    
    private void formatUsageParameters(CommandUsage<S> usage, StringBuilder line, HelpTheme<S> theme) {
        List<CommandParameter<S>> params = usage.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) line.append(theme.getPathSeparator());
            
            CommandParameter<S> param = params.get(i);
            if (param.isCommand()) {
                line.append(theme.formatSubcommand(param.name()));
            } else if (param.isFlag()) {
                line.append(theme.formatFlag(param.name()));
            } else {
                line.append(theme.formatArgument(param.name(), param.isOptional()));
            }
        }
    }
}