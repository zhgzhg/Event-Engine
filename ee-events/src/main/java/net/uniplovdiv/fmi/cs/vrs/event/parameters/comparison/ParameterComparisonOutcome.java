package net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison;

/**
 * Represents all possible different outcome results, when to event parameters are compared to each other.
 * Used by {@link ParameterComparisonOutcomeTemplate} and stored inside {@link ParametersComparisonResult}.
 */
public enum ParameterComparisonOutcome implements IParameterComparisonOutcome {
    /**
     * The base is less than, relative to the passed argument for comparison.
     */
    LESS(-1), // 0b11111111 11111111 11111111 11111111

    /**
     * The base is equal with, relative to the passed argument for comparison.
     */
    EQUAL(0), // 0b00000000 00000000 00000000 00000000

    /**
     * The base is greater than, relative to the passed argument for comparison.
     */
    GREATER(1), // 0b00000000 00000000 00000000 00000001

    /**
     * When the argument is present in the origin (source), but it is not in the relation.
     */
    NOTCOMPARED(2), // 0b00000000 00000000 00000000 00000010

    /**
     * When the argument is missing from the origin (source of comparison), but is present in the relation.
     */
    UNKNOWN(4), // 0b00000000 00000000 00000000 00000100

    /**
     * SPECIAL - Supported in {@link #specialTest(ParameterComparisonOutcome) specialTest} method or during evaluation
     * using {@link ParameterComparisonOutcomeTemplate}.
     * Combines NOTCOMPARED and UNKNOWN.
     */
    NOTCOMPARED_UNKNOWN(6), // 0b00000000 00000000 00000000 00000110

    /**
     * For data types that cannot be instantiated or does not implement compareTo() method.
     */
    INCOMPARABLE(8), // 0b00000000 00000000 00000000 00001000

    /**
     * SPECIAL - Supported in {@link #specialTest(ParameterComparisonOutcome) specialTest} method or during evaluation
     * using {@link ParameterComparisonOutcomeTemplate}.
     * Combines NOTCOMPARED, UNKNOWN and INCOMPARABLE.
     */
    NOTCOMPARED_UNKNOWN_INCOMPARABLE(14); // 0b00000000 00000000 00000000 00001110

    /**
     * String of the virtual {@link IParameterComparisonOutcome} field name that is containing the particular
     * IParameterComparisonOutcome class human-readable name string.
     */
    public static final String ___RESULT_TYPE_CLASS_NAME = "__result_type_class_name";

    private final int result;

    ParameterComparisonOutcome(int result) {
        this.result = result;
    }

    /**
     * Returns numerical code representation of the comparison result.
     * @return Integer numerical code representation of the comparison result.
     */
    public int getResultCode() {
        return this.result;
    }

    /**
     * Indicates if the comparison result is of a definite type - LESS, EQUAL or GREATER.
     * @return True if the comparison result represents a definite one, otherwise false.
     */
    public boolean isDefinite() {
        return (result >= -1 && result < 2);
    }

    /**
     * Indicates if the comparison result is of a special value - NOTCOMPARED_UNKNOWN, NOTCOMPARED_UNKNOWN_INCOMPARABLE.
     * @return True if the comparison result of a special value, otherwise false.
     */
    public boolean isSpecial() {
        return (result == 6 || result == 14);
    }

    /**
     * Special test performed over any value specified in the parameters value, only if the current value is of a
     * special outcome enumerator like NOTCOMPARED_UNKNOWN or NOTCOMPARED_UNKNOWN_INCOMPARABLE.
     * The test is intended to check whether the passed value "fits" into the current special case.
     * @param v The value outcome enumerator value to test against.
     * @return True if the passed value fits into the the current special enumerator value or false if it doesn't or if
     * the current enumerator value is not special.
     */
    public boolean specialTest(ParameterComparisonOutcome v) {
        return this.isSpecial() && (this.result == (this.result | v.result));
    }

    /**
     * Returns enumeration from integer value.
     * @param value The integer value that will be converted. If an invalid one is provided the resulting value will be
     *              ParameterComparisonOutcome.INCOMPARABLE
     * @return The corresponding ParameterComparisonOutcome enumeration value.
     */
    public static ParameterComparisonOutcome fromInteger(int value) {
        switch (value) {
            case -1: return LESS;
            case 0: return EQUAL;
            case 1: return GREATER;
            case 2: return NOTCOMPARED;
            case 4: return UNKNOWN;
            case 6: return NOTCOMPARED_UNKNOWN;
            case 8: return INCOMPARABLE;
            case 14: return NOTCOMPARED_UNKNOWN_INCOMPARABLE;
            default:
                return INCOMPARABLE;
        }
    }

    @Override
    public boolean isActualResult() {
        return true;
    }
}