package org.nbfc.loanemicalculator.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, CharSequence> {

    private List<String> acceptedValues;
    private boolean ignoreCase;
    private String enumValues;

    @Override
    public void initialize(ValueOfEnum annotation) {
        this.ignoreCase = annotation.ignoreCase();
        this.acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
        this.enumValues = String.join(", ", acceptedValues);
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null || value.toString().isBlank()) {
            return true;
        }
        String candidate = value.toString();
        boolean valid = ignoreCase
                ? acceptedValues.stream().anyMatch(candidate::equalsIgnoreCase)
                : acceptedValues.contains(candidate);
        if (!valid && context != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "must be any of " + enumValues).addConstraintViolation();
        }
        return valid;
    }
}
