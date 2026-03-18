package studio.mevera.imperat.annotations.base.parsers;

import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.annotations.base.element.MethodElement;
import studio.mevera.imperat.annotations.base.element.ParameterElement;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public final class PathwaySyntaxParser<S extends CommandSource> {

    private final static Pattern LITERAL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    //create a  regex pattern that exactly matches [-hello <value>] or [--hello <anything>], call it TRUE_FLAG_PATTERN
    private final static Pattern TRUE_FLAG_PATTERN = Pattern.compile("^--?[a-zA-Z0-9_]+(\\s+<[^>]+>)?$");

    //create regex pattern matching '[-flag]' or '[--flag]'
    private final static Pattern SWITCH_ONLY_PATTERN = Pattern.compile("^--?[a-zA-Z0-9_]+$");

    private final static Pattern REQUIRED_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>");

    private final static Pattern OPTIONAL_PATTERN = Pattern.compile("\\[([a-zA-Z0-9_]+)]");

    private final Imperat<S> imperat;
    private final ParameterParser<S> parameterParser;

    private PathwaySyntaxParser(Imperat<S> imperat, ParameterParser<S> parameterParser) {
        this.imperat = imperat;
        this.parameterParser = parameterParser;
    }

    //static create/factory method
    static <S extends CommandSource> PathwaySyntaxParser<S> of(Imperat<S> imperat, ParameterParser<S> parameterParser) {
        return new PathwaySyntaxParser<>(imperat, parameterParser);
    }

    private static String extractArgName(String argFormat) {
        StringBuilder builder = new StringBuilder();
        char[] chars = argFormat.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '[' || c == '<' || c == ']' || c == '>' || c == '-') {
                continue;
            }
            if (Character.isWhitespace(c)) {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private static boolean isRequired(String formattedArg) {
        return REQUIRED_PATTERN.matcher(formattedArg).matches();
    }

    private static boolean isOptional(String formattedArg) {
        return OPTIONAL_PATTERN.matcher(formattedArg).matches();
    }

    private static boolean isSwitchOnly(String formattedArg) {
        return SWITCH_ONLY_PATTERN.matcher(formattedArg).matches();
    }

    private static boolean isLiteral(String formattedArg) {
        return LITERAL_PATTERN.matcher(formattedArg).matches();
    }

    private static boolean isTrueFlag(String formattedArg) {
        return TRUE_FLAG_PATTERN.matcher(formattedArg).matches();
    }

    public CommandPathway.Builder<S> loadPathway(String pathwaySyntax, MethodElement methodElement, boolean skipFirst) {
        String[] parts = pathwaySyntax.split(" ");
        CommandPathway.Builder<S> pathwayBuilder = CommandPathway.builder(methodElement);
        List<Argument<S>> arguments = new ArrayList<>(parts.length);
        int i = skipFirst ? 1 : 0;
        for (; i < parts.length; i++) {
            String part = parts[i];
            if (skipFirst && i == 0) {
                continue;
            }
            arguments.add(loadArgument(methodElement, part));
        }

        pathwayBuilder.arguments(arguments);
        return pathwayBuilder;
    }

    public Argument<S> loadArgument(MethodElement method, String formattedArg) {
        String argName = extractArgName(formattedArg);
        ParameterElement correspondingParameter = method.getParameters().stream()
                                                          .filter(p -> p.getName().equals(argName))
                                                          .findFirst().orElse(null);
        if (correspondingParameter == null) {
            if (isLiteral(formattedArg)) {
                return loadLiteralArg(argName);
            } else {
                return null;
            }
        }

        return parameterParser.parseParameter(correspondingParameter);
    }

    private Argument<S> loadLiteralArg(String argName) {
        String[] names = argName.split("\\|");
        return Argument.literal(names);
    }
}

