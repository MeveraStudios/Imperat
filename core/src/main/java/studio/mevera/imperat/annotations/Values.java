package studio.mevera.imperat.annotations;

import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@ApiStatus.AvailableSince("1.7.6")
public @interface Values {

    /**
     * Specific allowed values
     * @return the only allowed input values.
     */
    String[] value();

    /**
     * Whether the values should be checked upon for
     * being exactly the SAME as the input (including upper and lower case)
     * <p>
     * example:
     * values: ["Hello"]
     * input: "hello"
     * </p>
     * if this method returns true, then the input wouldn't be allowed and vice versa.
     *
     * @return if checks on values should allow difference in lower/upper cases between input and the values...
     */
    boolean caseSensitive() default true;

}
