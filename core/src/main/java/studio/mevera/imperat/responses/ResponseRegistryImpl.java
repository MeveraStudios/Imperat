package studio.mevera.imperat.responses;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ResponseRegistryImpl implements ResponseRegistry {

    private final Map<String, Response> responses = new ConcurrentHashMap<>();

    ResponseRegistryImpl() {
        registerDefaultResponses();
    }

    private void registerDefaultResponses() {
        // Parse exceptions - simple input-based

        // InvalidBooleanException: String input
        registerResponse(
                new Response(ResponseKey.INVALID_BOOLEAN, () -> "Invalid boolean '%input%'")
                        .addPlaceholder("input")
                        .addContextPlaceholders()
        );

        // InvalidEnumException: String input, Class<? extends Enum> enumType
        registerResponse(
                new Response(ResponseKey.INVALID_ENUM, () -> "Invalid %enum_type% '%input%'")
                        .addPlaceholder("input")
                        .addPlaceholder("enum_type")
                        .addContextPlaceholders()
        );

        // InvalidNumberFormatException: String input, NumberFormatException originalError, String numberTypeDisplay, TypeWrap<? extends Number>
        // numericType
        registerResponse(
                new Response(ResponseKey.INVALID_NUMBER_FORMAT, () -> "Invalid %number_type% format '%input%'")
                        .addPlaceholder("input")
                        .addPlaceholder("number_type")
                        .addContextPlaceholders()
        );

        // InvalidMapEntryFormatException: String input, String requiredSeparator, Reason reason
        registerResponse(
                new Response(ResponseKey.INVALID_MAP_ENTRY_FORMAT, () -> "Invalid map entry '%input%'%extra_msg%")
                        .addPlaceholder("input")
                        .addPlaceholder("extra_msg")
                        .addContextPlaceholders()
        );

        // InvalidUUIDException: String input
        registerResponse(
                new Response(ResponseKey.INVALID_UUID, () -> "Invalid uuid-format '%input%'")
                        .addPlaceholder("input")
                        .addContextPlaceholders()
        );

        // WordOutOfRestrictionsException: String input, List<String> restrictions
        registerResponse(
                new Response(ResponseKey.WORD_OUT_OF_RESTRICTIONS, () -> "Word '%input%' is not within the given restrictions=%restrictions%")
                        .addPlaceholder("input")
                        .addPlaceholder("restrictions")
                        .addContextPlaceholders()
        );

        // ValueOutOfConstraintException: String input, Set<String> allowedValues
        registerResponse(
                new Response(ResponseKey.VALUE_OUT_OF_CONSTRAINT, () -> "Input '%input%' is not one of: [%allowed_values%]")
                        .addPlaceholder("input")
                        .addPlaceholder("allowed_values")
                        .addContextPlaceholders()
        );

        // Flag-related exceptions

        // UnknownFlagException: String input
        registerResponse(
                new Response(ResponseKey.UNKNOWN_FLAG, () -> "Unknown flag '%input%'")
                        .addPlaceholder("input")
                        .addContextPlaceholders()
        );

        // MissingFlagInputException: Set<String> flagsUsed, String rawFlagEntered
        registerResponse(
                new Response(ResponseKey.MISSING_FLAG_INPUT, () -> "Please enter the value for flag(s) '%flags%'")
                        .addPlaceholder("flags")
                        .addContextPlaceholders()
        );

        // FlagOutsideCommandScopeException: Command<?> wrongCmd, String flagInput
        // wrongCmd has: name(), aliases(), description(), etc.
        registerResponse(
                new Response(ResponseKey.FLAG_OUTSIDE_SCOPE,
                        () -> "Flag(s) '%flag_input%' were used (in %wrong_cmd%'s scope) outside of their command's scope")
                        .addPlaceholder("flag_input")
                        .addPlaceholder("wrong_cmd")
                        .addContextPlaceholders()
        );

        // Complex validation exceptions

        // NumberOutOfRangeException: String originalInput, NumericParameter<?> parameter, Number value, NumericRange range
        // NumericParameter has: format(), name(), description(), type(), defaultValue(), range()
        // NumericRange has: getMin(), getMax()
        registerResponse(
                new Response(ResponseKey.NUMBER_OUT_OF_RANGE, () -> "Value '%value%' entered for parameter '%parameter%' must be %range%")
                        .addContextPlaceholders()
                        .addPlaceholder("value")
                        .addPlaceholder("parameter")
                        .addPlaceholder("range")
                        .addPlaceholder("original_input")
                        .addPlaceholder("range_min")
                        .addPlaceholder("range_max")
                        .addPlaceholder("parameter_name")
        );

        // Permission exceptions

        // PermissionDeniedException: CommandPathway<?> usage, Argument<?> targetParameter
        // CommandPathway has: getCommand(), getPermission(), getDescription(), format()
        // Argument has: name(), description(), permission(), format()
        registerResponse(
                new Response(ResponseKey.PERMISSION_DENIED, () -> "You don't have permission to use this command!")
                        .addContextPlaceholders()
                        .addPlaceholder("usage")
                        .addPlaceholder("parameter")
        );

        // Command exceptions

        // InvalidSyntaxException: CommandPathSearch<?> result
        // CommandPathSearch has: getClosestUsage(), getFoundUsage(), getLastCommandNode()
        registerResponse(
                new Response(ResponseKey.INVALID_SYNTAX, () -> "Invalid command usage '%invalid_usage%', you probably meant '%closest_usage%'")
                        .addContextPlaceholders()
                        .addPlaceholder("invalid_usage")
                        .addPlaceholder("closest_usage")// Will be populated from exception data

        );

        // CooldownException: Duration cooldownDuration, Instant lastTimeExecuted, Duration remainingDuration
        registerResponse(
                new Response(ResponseKey.COOLDOWN, () -> "Please wait %seconds% second(s) to execute this command again!")
                        .addContextPlaceholders()
                        .addPlaceholder("seconds")
                        .addPlaceholder("remaining_duration")
                        .addPlaceholder("cooldown_duration")
                        .addPlaceholder("last_executed")
        );

        // Help exceptions

        // NoHelpException: no specific data, but we extract from context
        registerResponse(
                new Response(ResponseKey.NO_HELP, () -> "No Help available for '%command%'")
                        .addPlaceholder("command")
                        .addContextPlaceholders()
        );

        // NoHelpPageException: no specific data, page extracted from context
        registerResponse(
                new Response(ResponseKey.NO_HELP_PAGE, () -> "Page '%page%' doesn't exist!")
                        .addPlaceholder("page")
                        .addContextPlaceholders()
        );

    }

    @Override
    public void registerResponse(Response response) {
        responses.put(response.getKey().getKey(), response);
    }

    @Override
    public Response getResponse(ResponseKey key) {
        return responses.get(key.getKey());
    }
}
