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

        registerResponse(
                new Response(ResponseKey.INVALID_CHARACTER, () -> "Invalid input '%input%', expected a single character")
                        .addPlaceholder("input")
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

        // FlagOutsideCommandScopeException: RootCommand<?> wrongCmd, String flagInput
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
                new Response(ResponseKey.NUMBER_OUT_OF_RANGE, () -> "Value '%parsed_input%' entered for argument '%formatted_argument%' must be "
                                                                            + "%formatted_range%")
                        .addContextPlaceholders()
                        .addPlaceholder("parsed_input")
                        .addPlaceholder("formatted_argument")
                        .addPlaceholder("formatted_range")
                        .addPlaceholder("input")
                        .addPlaceholder("range_min")
                        .addPlaceholder("range_max")
        );

        // Permission exceptions are now handled directly by PermissionDeniedException

        // Command exceptions
        // InvalidSyntaxException is now handled directly by InvalidSyntaxException

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
