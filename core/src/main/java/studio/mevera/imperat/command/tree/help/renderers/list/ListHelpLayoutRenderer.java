package studio.mevera.imperat.command.tree.help.renderers.list;

import studio.mevera.imperat.command.tree.help.HelpEntryList;
import studio.mevera.imperat.command.tree.help.HelpRenderOptions;
import studio.mevera.imperat.command.tree.help.HelpTheme;
import studio.mevera.imperat.command.tree.help.renderers.HelpLayoutRenderer;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

public class ListHelpLayoutRenderer<S extends Source>
        implements HelpLayoutRenderer<S, ListViewModel> {
    
    @Override
    public void render(ExecutionContext<S> context, ListViewModel model, HelpEntryList<S> helpEntries, HelpRenderOptions options) {
        Source source = context.source();
        HelpTheme theme = options.getTheme();
        for (ListViewModel.ListItem item : model.items) {
            StringBuilder line = new StringBuilder();
            line.append(theme.getPrefix());
            line.append(theme.getBulletPoint()).append(" ");
            line.append(theme.formatCommand(item.command()));
            
            if (theme.isShowDescriptions() && item.description() != null) {
                line.append(theme.formatSeparator());
                line.append(theme.formatDescription(item.description()));
            }
            
            source.reply(line.toString());
        }
    }
}