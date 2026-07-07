package org.nbfc.loanemicalculator.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates that a String value is the name of one of the constants of the supplied enum.
 * A {@code null} value is considered valid so it can be combined with {@code @NotNull}/{@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = ValueOfEnumValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ValueOfEnum {

    Class<? extends Enum<?>> enumClass();

    boolean ignoreCase() default true;

    String message() default "must be any of {enumValues}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
