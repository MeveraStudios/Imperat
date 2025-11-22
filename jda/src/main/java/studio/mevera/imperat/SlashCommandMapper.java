package studio.mevera.imperat;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts Imperat {@link Command} usages into JDA slash command data.
 */
final class SlashCommandMapper {

    CommandData toSlashData(Command<JdaSource> command) {
        return mapCommand(command).commandData();
    }

    SlashMapping mapCommand(Command<JdaSource> command) {
        String commandName = command.name().toLowerCase();
        SlashCommandData data = Commands.slash(commandName, command.description().toString());
        List<UsagePath> usagePaths = collectUsagePaths(command, List.of(), List.of());
        Map<InvocationKey, Invocation> invocations = new LinkedHashMap<>();

        boolean onlyRootLevel = usagePaths.stream().allMatch(usage -> usage.path().isEmpty());
        if (onlyRootLevel) {
            OptionsBuild options = mergeOptions(usagePaths);
            if (!options.options().isEmpty()) {
                data.addOptions(options.options());
            }
            invocations.put(new InvocationKey(null, null), new Invocation(List.of(), options.invocationOrder()));
            return new SlashMapping(commandName, data, invocations);
        }

        Map<List<String>, UsageBucket> buckets = bucketize(usagePaths);
        buckets.forEach((path, bucket) -> {
            if (path.isEmpty()) {
                return;
            }

            if (path.size() == 1) {
                OptionsBuild options = bucket.toOptions();
                SubcommandData sub = new SubcommandData(path.get(0), bucket.description());
                sub.addOptions(options.options());
                data.addSubcommands(sub);
                invocations.put(new InvocationKey(null, path.get(0)), new Invocation(List.copyOf(path), options.invocationOrder()));
                return;
            }

            String groupName = path.get(0);
            String subName = String.join("-", path.subList(1, path.size()));
            SubcommandGroupData group = data.getSubcommandGroups().stream()
                    .filter(existing -> existing.getName().equals(groupName))
                    .findFirst()
                    .orElseGet(() -> {
                        SubcommandGroupData created = new SubcommandGroupData(groupName, bucket.description());
                        data.addSubcommandGroups(created);
                        return created;
                    });

            OptionsBuild options = bucket.toOptions();
            SubcommandData sub = new SubcommandData(subName, bucket.description());
            sub.addOptions(options.options());
            group.addSubcommands(sub);
            invocations.put(new InvocationKey(groupName, subName), new Invocation(List.copyOf(path), options.invocationOrder()));
        });

        return new SlashMapping(commandName, data, invocations);
    }

    private Map<List<String>, UsageBucket> bucketize(Collection<UsagePath> usagePaths) {
        Map<List<String>, UsageBucket> buckets = new LinkedHashMap<>();
        for (UsagePath usage : usagePaths) {
            UsageBucket bucket = buckets.computeIfAbsent(usage.path(), key -> new UsageBucket());
            bucket.setDescriptionIfAbsent(usage.description());
            bucket.includeUsage(usage.parameters());
        }
        return buckets;
    }

    private OptionsBuild mergeOptions(Collection<UsagePath> usagePaths) {
        UsageBucket bucket = new UsageBucket();
        usagePaths.forEach(usage -> bucket.includeUsage(usage.parameters()));
        return bucket.toOptions();
    }

    private List<UsagePath> collectUsagePaths(
            Command<JdaSource> command,
            List<String> path,
            List<CommandParameter<JdaSource>> inherited
    ) {
        List<UsagePath> paths = new ArrayList<>();
        List<CommandParameter<JdaSource>> inheritedForChildren = new ArrayList<>(inherited);

        for (CommandParameter<JdaSource> parameter : command.getMainUsage().getParametersWithoutFlags()) {
            if (!parameter.isCommand()) {
                inheritedForChildren.add(parameter);
            }
        }

        if (command.getSubCommands().isEmpty()) {
            for (CommandUsage<JdaSource> usage : command.usages()) {
                List<CommandParameter<JdaSource>> parameters = new ArrayList<>(inherited);
                for (CommandParameter<JdaSource> parameter : usage.getParametersWithoutFlags()) {
                    if (!parameter.isCommand()) {
                        parameters.add(parameter);
                    }
                }
                paths.add(new UsagePath(List.copyOf(path), List.copyOf(parameters), usage.description().toString()));
            }
            return paths;
        }

        for (Command<JdaSource> sub : command.getSubCommands()) {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(sub.name().toLowerCase());
            paths.addAll(collectUsagePaths(sub, childPath, inheritedForChildren));
        }
        return paths;
    }

    private OptionType mapOptionType(CommandParameter<JdaSource> parameter) {
        Class<?> raw = parameter.wrappedType().getRawType();
        if (raw == null) {
            return OptionType.STRING;
        }

        if (String.class.isAssignableFrom(raw)) {
            return OptionType.STRING;
        } else if (Boolean.class.isAssignableFrom(raw) || raw == boolean.class) {
            return OptionType.BOOLEAN;
        } else if (Number.class.isAssignableFrom(raw) || raw.isPrimitive() && raw != char.class) {
            if (raw == Double.class || raw == double.class || raw == Float.class || raw == float.class) {
                return OptionType.NUMBER;
            }
            return OptionType.INTEGER;
        } else if (User.class.isAssignableFrom(raw) || Member.class.isAssignableFrom(raw)) {
            return OptionType.USER;
        } else if (Role.class.isAssignableFrom(raw)) {
            return OptionType.ROLE;
        } else if (GuildChannel.class.isAssignableFrom(raw)) {
            return OptionType.CHANNEL;
        }
        return OptionType.STRING;
    }

    private record UsagePath(List<String> path, List<CommandParameter<JdaSource>> parameters, String description) {
    }

    private final class UsageBucket {
        private final Map<String, OptionSpec> options = new LinkedHashMap<>();
        private String description = "";

        void setDescriptionIfAbsent(String description) {
            if (this.description == null || this.description.isEmpty()) {
                this.description = description;
            }
        }

        void includeUsage(List<CommandParameter<JdaSource>> parameters) {
            Set<String> seen = new LinkedHashSet<>();
            for (CommandParameter<JdaSource> parameter : parameters) {
                String optionName = parameter.name().toLowerCase();
                options.compute(optionName, (key, existing) -> {
                    if (existing == null) {
                        return OptionSpec.from(optionName, parameter, mapOptionType(parameter));
                    }
                    existing.merge(parameter, mapOptionType(parameter));
                    return existing;
                });
                seen.add(optionName);
            }

            for (OptionSpec spec : options.values()) {
                if (!seen.contains(spec.name())) {
                    spec.markOptional();
                }
            }
        }

        OptionsBuild toOptions() {
            List<OptionData> required = new ArrayList<>();
            List<OptionData> optional = new ArrayList<>();
            List<String> invocationOrder = new ArrayList<>(options.size());

            for (OptionSpec spec : options.values()) {
                invocationOrder.add(spec.name());
                OptionData option = new OptionData(spec.type(), spec.name(), spec.description(), spec.required());
                if (spec.required()) {
                    required.add(option);
                } else {
                    optional.add(option);
                }
            }

            required.addAll(optional);
            return new OptionsBuild(required, invocationOrder);
        }

        String description() {
            return description == null ? "" : description;
        }
    }

    private static final class OptionSpec {
        private final String name;
        private OptionType type;
        private String description;
        private boolean required;

        private OptionSpec(String name, OptionType type, String description, boolean required) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
        }

        static OptionSpec from(String name, CommandParameter<JdaSource> parameter, OptionType type) {
            return new OptionSpec(name, type, parameter.description().toString(), !parameter.isOptional());
        }

        void merge(CommandParameter<JdaSource> parameter, OptionType resolvedType) {
            type = compatibleType(type, resolvedType);
            description = description == null || description.isEmpty()
                    ? parameter.description().toString()
                    : description;
            required = required && !parameter.isOptional();
        }

        void markOptional() {
            required = false;
        }

        OptionType type() {
            return type;
        }

        String name() {
            return name;
        }

        String description() {
            return description == null ? "" : description;
        }

        boolean required() {
            return required;
        }

        private OptionType compatibleType(OptionType first, OptionType other) {
            if (first == other) {
                return first;
            }
            return OptionType.STRING;
        }
    }

    private record OptionsBuild(List<OptionData> options, List<String> invocationOrder) {}

    record SlashMapping(String commandName, SlashCommandData commandData, Map<InvocationKey, Invocation> invocations) {
        Invocation invocationFor(String group, String subcommand) {
            return invocations.get(new InvocationKey(group, subcommand));
        }
    }

    record InvocationKey(String group, String subcommand) {}
    record Invocation(List<String> path, List<String> optionOrder) {}
}