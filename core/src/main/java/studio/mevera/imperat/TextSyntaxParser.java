package studio.mevera.imperat;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagBuilder;
import studio.mevera.imperat.command.parameters.ParameterBuilder;
import studio.mevera.imperat.command.parameters.type.ParameterType;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class TextSyntaxParser<S extends Source> {
    
    private final static Pattern LITERAL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    
    //create a  regex pattern that exactly matches [-hello <value>] or [--hello <anything>], call it TRUE_FLAG_PATTERN
    private final static Pattern TRUE_FLAG_PATTERN = Pattern.compile("^--?[a-zA-Z0-9_]+(\\s+<[^>]+>)?$");
    
    //create regix pattern matching '[-flag]' or '[--flag]'
    private final static Pattern SWITCH_ONLY_PATTERN = Pattern.compile("^--?[a-zA-Z0-9_]+$");
    
    
    private final Imperat<S> imperat;
    
    private TextSyntaxParser(Imperat<S> imperat) {
        this.imperat = imperat;
    }
    
    //static create/factory method
    public static <S extends Source> TextSyntaxParser<S> of(Imperat<S> imperat) {
        return new TextSyntaxParser<>(imperat);
    }
    
    /**
     * Parses a command syntax string into a CommandUsage object.
     * The syntax string should follow the format:
     * - It must start with the command name, optionally prefixed by the command prefix (e.g., "/").
     * - Followed by parameters which can be:
     *   - Required arguments: <argName>:type
     *   - Optional arguments: [argName]:type
     *   - True-flags (flags with input): [-flag <value>]:type
     *   - Switches (boolean flags): [--switchFlag]
     * - Parameters are separated by spaces.
     *
     * <p>
     * NOTE: For custom parameter types, ensure they are registered in the Imperat configuration,
     * Moreover, custom GENERIC-BASED parameter-types ARE NOT SUPPORTED in this syntax parser, EXCEPT
     * The only supported generic-based parameter types arethe one who are SPECIFICALLY set/registered:
     * - Primitive Array typesONLY (e.g., String[], Integer[]) are also supported.
     * </p>
     *
     * @param syntax the syntax string to parse into a {@link CommandUsage}
     * @return the parsed {@link CommandUsage} object
     * @throws IllegalArgumentException if the syntax string is invalid
     */
    public CommandUsage<S> parseSyntaxToUsage(final String syntax) {
        String trimmedSyntax = syntax.trim();
        final String cmdPrefix = imperat.config().commandPrefix();
        if(trimmedSyntax.startsWith(imperat.config().commandPrefix())) {
            trimmedSyntax = trimmedSyntax.substring(cmdPrefix.length());
        }
        
        // Split the syntax into parts
        String[] parts = trimmedSyntax.split(" ");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid command syntax: " + syntax);
        }
        
        Command<S> command = imperat.getCommand(parts[0]);
        if(command == null) {
            command = Command.create(imperat, parts[0])
                    .build();
        }
        
        CommandUsage.Builder<S> commandUsage = CommandUsage.builder();
        
        List<ParameterBuilder<S, ?>> parameters = new ArrayList<>();
        
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            String[] partArr = part.split(":");
            String paramNameFormat = partArr[0];
            String paramType = partArr.length > 1 ? partArr[1] : "String";
            
            ParameterBuilder<S, ?> parameter;
            if(isLiteral(paramNameFormat)) {
                // Literal part
                parameter = ParameterBuilder.literalBuilder(paramNameFormat);
                parameters.add(parameter);
                continue;
            }
            if(isTrueFlag(paramNameFormat)) {
                String id = extractFlagName(paramNameFormat);
                parameter = FlagBuilder.ofFlag(id, deduceParamTypeFromString(paramType));
                parameters.add(parameter);
                continue;
            }
            else if(isSwitchOnlyFlag(paramNameFormat)) {
                String id = extractFlagName(paramNameFormat);
                parameter = FlagBuilder.ofSwitch(id);
                parameters.add(parameter);
                continue;
            }
            
            boolean optional = paramNameFormat.startsWith("[") && paramNameFormat.endsWith("]");
            String paramName = extractArgName(paramNameFormat);
            parameter = optional ? CommandParameter.optional(paramName, deduceParamTypeFromString(paramType))
                    : CommandParameter.required(paramName, deduceParamTypeFromString(paramType));
            parameters.add(parameter);
        }
        
        return commandUsage
                .parameterBuilders(parameters)
                .build(command);
    }
    
    //e.g: "/rank setperm <user> <perm> [value]
    public Command<S> parseShortcutSyntax(Command<S> shortcutOwner, final String syntax) {
        String trimmedSyntax = syntax.trim();
        final String cmdPrefix = imperat.config().commandPrefix();
        if(trimmedSyntax.startsWith(imperat.config().commandPrefix())) {
            trimmedSyntax = trimmedSyntax.substring(cmdPrefix.length());
        }
        
        // Split the syntax into parts
        String[] parts = trimmedSyntax.split(" ");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid command syntax: " + syntax);
        }
        
        String root = parts[0];
        if(!isLiteral(root)) {
            throw new IllegalArgumentException("The root command must be a literal (alphanumeric and underscores only): " + root);
        }
        
        Command<S> rootCommand = Command.create(imperat, root)
                .setMetaPropertiesFromOtherCommand(shortcutOwner)
                .build();
        
        CommandUsage.Builder<S> commandUsage = CommandUsage.<S>builder()
                .setPropertiesFromCommandMainUsage(shortcutOwner);
        
        //now lets add parameters to the command usage, part by part
        // FOR EACH PART, search for the parameter that suits it, whether literal, or any other type of argument.
        //the search is by name NOT neccessarily by the position of the parameter.
        
        Map<String, CommandParameter<S>> params = new HashMap<>();
        
        for(CommandUsage<S> usage : shortcutOwner.usages()) {
            for(CommandParameter<S> param : usage.getParameters()) {
                if(params.containsKey(param.format())) {
                    //if already exists, skip
                    //check position
                    var existing = params.get(param.format());
                    if(existing.position() != param.position()) {
                        throw new IllegalArgumentException("Duplicate parameter state with different positions[" + existing.position() + ", " + param.position() + "] : " + param.format() + " in command " + shortcutOwner.name());
                    }
                    continue;
                }
                params.put(param.format(), param);
            }
        }
        
        List<CommandParameter<S>> orderedParameters = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            CommandParameter<S> parameter = params.get(part);
            if (parameter == null) {
                //check if it's a literal
                if(isLiteral(part)) {
                    parameter = CommandParameter.literal(part);
                } else {
                    throw new IllegalArgumentException("No parameter found for part: " + part);
                }
                
            }
            orderedParameters.add(parameter.copyWithDifferentPosition(i));
        }
        
        commandUsage.parameters(orderedParameters);
        rootCommand.addUsage(commandUsage);
        return rootCommand;
    }
    
    private boolean isLiteral(String part) {
        return LITERAL_PATTERN.matcher(part).matches();
    }
    
    private boolean isTrueFlag(String part) {
        return TRUE_FLAG_PATTERN.matcher(part).matches();
    }
    
    private boolean isSwitchOnlyFlag(String part) {
        return SWITCH_ONLY_PATTERN.matcher(part).matches();
    }
    
    private String extractFlagName(String part) {
        String name = extractArgName(part);
        String[] split = name.split(" ");
        if(split.length > 0) {
            name = split[0];
        }
        
        if(name.startsWith("--")) {
            return name.substring(2);
        } else if(name.startsWith("-")) {
            return name.substring(1);
        }
        return name;
    }
    
    private String extractArgName(String part) {
        return part.substring(1, part.length() - 1);
    }
    
    private ParameterType<S, ?> deduceParamTypeFromString(String typeName) {
        
        var cfg = imperat.config();
        if(typeName.isBlank()) {
            return cfg.getParameterType(String.class);
        }
        
        //check if its array
        if(typeName.endsWith("[]")) {
            String elementTypeName = typeName.substring(0, typeName.length() - 2).trim();
            ParameterType<S, ?> elementType = deduceParamTypeFromString(elementTypeName);
            if(elementType == null) {
                throw new IllegalArgumentException("No parameter type registered for array element-type: " + elementTypeName);
            }
            TypeWrap<?> arrayType = TypeWrap.ofArray(elementType.type());
            return cfg.getParameterType(arrayType.getType());
        }
        
        if(typeName.equalsIgnoreCase("int") || typeName.equalsIgnoreCase("integer")) {
            return cfg.getParameterType(Integer.class);
        } else if(typeName.equalsIgnoreCase("long")) {
            return cfg.getParameterType(Long.class);
        } else if(typeName.equalsIgnoreCase("double")) {
            return cfg.getParameterType(Double.class);
        } else if(typeName.equalsIgnoreCase("float")) {
            return cfg.getParameterType(Float.class);
        } else if(typeName.equalsIgnoreCase("boolean") || typeName.equalsIgnoreCase("bool")) {
            return cfg.getParameterType(Boolean.class);
        } else if(typeName.equalsIgnoreCase("string") || typeName.equalsIgnoreCase("str")) {
            return cfg.getParameterType(String.class);
        } else {
            //try to find a custom parameter type by name
            if(isGenericType(typeName)) {
                String rawType = extractRawTypeName(typeName);
                try {
                    Type rawClass = Class.forName(rawType);
                    ParameterType<S, ?> rawParamType = cfg.getParameterType(rawClass);
                    if(rawParamType == null) {
                        throw new IllegalArgumentException("No parameter type registered for raw-type: " + rawType);
                    }
                    
                    String[] genericTypesNames = extractGenericTypesNames(typeName);
                    ParameterType<S,?>[] genericParamTypes = new ParameterType[genericTypesNames.length];
                    
                    for(int i = 0; i < genericTypesNames.length; i++) {
                        String genericTypeName = genericTypesNames[i];
                        
                        ParameterType<S, ?> genericParamType = deduceParamTypeFromString(genericTypeName.trim());
                        if(genericParamType == null) {
                            throw new IllegalArgumentException("No parameter type registered for generic-type: " + genericTypeName);
                        }
                        genericParamTypes[i] = genericParamType;
                    }
                    TypeWrap<?> wrappedType = TypeWrap.ofParameterized(rawClass, List.of(genericParamTypes));
                    return cfg.getParameterType(wrappedType.getType());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }else {
                ParameterType<S, ?> customType;
                try {
                    customType = cfg.getParameterType(Class.forName(typeName));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unknown type from syntax: '" + typeName + "'", e);
                }
                return customType;
            }
            
        }
    }
    
    private String[] extractGenericTypesNames(String typeName) {
        int genericStart = typeName.indexOf("<");
        int genericEnd = typeName.lastIndexOf(">");
        if(genericStart == -1 || genericEnd == -1 || genericEnd <= genericStart) {
            throw new IllegalArgumentException("Invalid generic type format: " + typeName);
        }
        var genericLine = typeName.substring(genericStart + 1, genericEnd).trim();
        if(genericLine.contains(",")) {
            return genericLine.split(",");
        } else {
            return new String[] { genericLine };
        }
    }
    
    private String extractRawTypeName(String typeName) {
        int genericStart = typeName.indexOf("<");
        if(genericStart == -1) {
            return typeName;
        }
        return typeName.substring(0, genericStart);
    }
    
    private boolean isGenericType(String typeName) {
        return typeName.contains("<") && typeName.endsWith(">");
    }
}
