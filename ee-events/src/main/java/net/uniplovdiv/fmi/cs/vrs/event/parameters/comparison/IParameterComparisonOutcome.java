package net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison;

import java.io.Serializable;

/**
 * Used for mixing {@link ParameterComparisonOutcome}, {@link ParametersComparisonResult} types inside
 * {@link ParametersComparisonResult}.
 */
public interface IParameterComparisonOutcome extends Serializable {
    /**
     * Checks if the current instance represents the actual comparison result or stores instead another class that
     * probably contains the final (actual) comparison result.
     * @return True if the actual result is stored otherwise false.
     */
    boolean isActualResult();
}
