package org.nbfc.loanemicalculator.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nbfc.loanemicalculator.enums.LoanType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the custom {@link ValueOfEnum} constraint through the real Bean Validation engine.
 */
class ValueOfEnumValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    static class Holder {
        @ValueOfEnum(enumClass = LoanType.class)
        final String loanType;

        Holder(String loanType) {
            this.loanType = loanType;
        }
    }

    @Test
    void acceptsValidEnumName() {
        assertThat(validator.validate(new Holder("HOME"))).isEmpty();
    }

    @Test
    void acceptsValidEnumNameIgnoringCase() {
        assertThat(validator.validate(new Holder("vehicle"))).isEmpty();
    }

    @Test
    void nullIsConsideredValid() {
        assertThat(validator.validate(new Holder(null))).isEmpty();
    }

    @Test
    void rejectsUnknownValue() {
        assertThat(validator.validate(new Holder("SPACESHIP"))).isNotEmpty();
    }
}
