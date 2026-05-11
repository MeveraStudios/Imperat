package studio.mevera.imperat.config;

public record BehaviouralOptionKey(String keyValue) {

    public final static BehaviouralOptionKey PARSING_MODE = new BehaviouralOptionKey("parsing_mode");

    public final static BehaviouralOptionKey OVERLAP_OPTIONALS_ARGUMENTS_SUGGESTIONS = new BehaviouralOptionKey(
            "overlap_optionals_arguments_suggestions");

    public final static BehaviouralOptionKey COMMAND_PREFIX = new BehaviouralOptionKey("command_prefix");


}
