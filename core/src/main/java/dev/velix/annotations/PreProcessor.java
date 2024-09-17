package dev.velix.annotations;

import dev.velix.command.processors.CommandPreProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface PreProcessor {
    
    Class<? extends CommandPreProcessor<?>> value();
    
}
