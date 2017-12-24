package net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison;

import java.util.HashMap;

/**
 * Holder for the results produced by an executed parameter comparison.
 */
public final class ParametersComparisonResult extends HashMap<String, IParameterComparisonOutcome>
        implements IParameterComparisonOutcome {
    private static final long serialVersionUID = -370654021878201974L;

    /**
     * String of the virtual field name that is containing the particular IParameterComparisonOutcome class serial
     * version uid number.
     */
    public static final String ___SV_UID_FIELD_NAME = "serialVersionUID";

    @Override
    public boolean isActualResult() {
        return false;
    }
}
