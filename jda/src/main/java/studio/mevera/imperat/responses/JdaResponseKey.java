package studio.mevera.imperat.responses;

public interface JdaResponseKey extends ResponseKey {

    JdaResponseKey UNKNOWN_USER = () -> "args.parsing.unknown_user";

    JdaResponseKey UNKNOWN_ROLE = () -> "args.parsing.unknown_role";

    JdaResponseKey UNKNOWN_MEMBER = () -> "args.parsing.unknown_member";

}
