package studio.mevera.imperat.annotations.types;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a format string for parsing and displaying the annotated parameter.
 * Useful for types like dates or numbers that require specific formatting.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Format {

    String value();
}
