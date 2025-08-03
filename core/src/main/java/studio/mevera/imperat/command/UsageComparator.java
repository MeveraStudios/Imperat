package studio.mevera.imperat.command;

import studio.mevera.imperat.command.parameters.CommandParameter;

import java.util.Comparator;

final class UsageComparator implements Comparator<CommandUsage<?>> {

    private static UsageComparator instance;

    static UsageComparator getInstance() {
        if (instance == null) {
            instance = new UsageComparator();
            return instance;
        }
        return instance;
    }


    @Override
    public int compare(CommandUsage<?> firstUsage, CommandUsage<?> secondUsage) {
        if (firstUsage.size() == secondUsage.size()) {

            for (int i = 0; i < firstUsage.size(); i++) {
                CommandParameter<?> p1 = firstUsage.getParameter(i);
                CommandParameter<?> p2 = secondUsage.getParameter(i);
                if (p1 == null || p2 == null) break;
                if (p1.isCommand() && !p2.isCommand()) {
                    return -1;
                } else if (!p1.isCommand() && p2.isCommand()) {
                    return 1;
                }
            }

        }

        return firstUsage.size() - secondUsage.size();
    }
}
