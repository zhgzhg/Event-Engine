package net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Comparison template for events parameters, able to evaluate the comparison result for a specific set of parameters.
 */
public class ParameterComparisonOutcomeTemplate implements Serializable {
    private static final long serialVersionUID = 8996229821290181431L;

    private boolean inverted; // zero priority
    private ParametersComparisonResult expectedComparisonResult; // first priority
    private List<ParameterComparisonOutcomeTemplate> and; // second priority
    private List<ParameterComparisonOutcomeTemplate> or; // third priority

    /**
     * Constructor. All internal fields are initialized with nonnull values.
     */
    public ParameterComparisonOutcomeTemplate() {
        this(true);
    }

    /**
     * Constructor.
     * @param initialize Indicates whether to initialize the internal fields with empty structures or to leave them null.
     *                   The default approach is to initialize them.
     */
    public ParameterComparisonOutcomeTemplate(boolean initialize) {
        inverted = false;
        if (initialize) {
            expectedComparisonResult = new ParametersComparisonResult();
            and = new ArrayList<>();
            or = new ArrayList<>();
        } else {
            expectedComparisonResult = null;
            and = null;
            or = null;
        }
    }

    /**
     * Constructor.
     * @param expectedComparisonResult The desired result used as a comparison basis. Can contain other members of the
     *                                 same type that nest concrete results.
     */
    public ParameterComparisonOutcomeTemplate(ParametersComparisonResult expectedComparisonResult) {
        this();
        this.expectedComparisonResult = expectedComparisonResult;
    }

    /**
     * Constructor.
     * @param expectedComparisonResult The desired result used as a comparison basis. Can contain other members of the
     *                                 same type that nest concrete results.
     * @param and Additional desired result(s) used as a comparison basis computed using logical AND &amp;&amp; operator.
     * @param or Additional desired result(s) used as a comparison basis computed using logical OR || operator.
     * @param inverted Flag to indicate whether to inverted the final result or not.
     */
    public ParameterComparisonOutcomeTemplate(ParametersComparisonResult expectedComparisonResult,
                                              List<ParameterComparisonOutcomeTemplate> and,
                                              List<ParameterComparisonOutcomeTemplate> or,
                                              boolean inverted) {
        this.expectedComparisonResult = expectedComparisonResult;
        this.and = and;
        this.or = or;
        this.inverted = inverted;
    }


    /**
     * Returns if inversion of the final result will be applied (logical not operation).
     * @return True if inversion is turned on, otherwise false.
     */
    public boolean getInverted() {
        return inverted;
    }

    /**
     * Sets the inversion of the final result (logical not operation). By default the inversion if turned off.
     * @param inverted True to turn on inversion or false to turn it off.
     */
    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    /**
     * Returns the contained comparison result map currently used as a comparison template.
     * @return The comparison result map. Can be null or empty.
     */
    public ParametersComparisonResult getExpectedComparisonResult() {
        return expectedComparisonResult;
    }

    /**
     * Sets the contained comparison result map currently used as a comparison template.
     * @param expectedComparisonResult The comparison result map.
     */
    public void setExpectedComparisonResult(ParametersComparisonResult expectedComparisonResult) {
        this.expectedComparisonResult = expectedComparisonResult;
    }

    /**
     * Returns the nested ParameterComparisonOutcomeTemplate instance to which logical AND &amp;&amp; operation is
     * applied during the evaluation process.
     * @return List of nested ParameterComparisonOutcomeTemplate instances that can be also null.
     */
    public List<ParameterComparisonOutcomeTemplate> getAnd() {
        return and;
    }

    /**
     * Sets the nested ParameterComparisonOutcomeTemplate instance(s) to which logical AND &amp;&amp; operation is
     * applied during the evaluation process.
     * @param and List of ParameterComparisonOutcomeTemplate that can be also null.
     */
    public void setAnd(List<ParameterComparisonOutcomeTemplate> and) {
        this.and = and;
    }

    /**
     * Sets the nested alternative of ParameterComparisonOutcomeTemplate instance to which logical AND &amp;&amp;
     * operation is applied during the evaluation process. This method removes any other existing AND instances.
     * @param and Single ParameterComparisonOutcomeTemplate. Can be also null.
     */
    public void setAnd(ParameterComparisonOutcomeTemplate and) {
        if (and != null) {
            this.and = new ArrayList<>(1);
            this.and.add(and);
        } else {
            this.and = null;
        }
    }

    /**
     * Returns the nested alternatives of ParameterComparisonOutcomeTemplate instances to which logical OR || operation
     * is applied during the evaluation process.
     * @return List of nested ParameterComparisonOutcomeTemplate instances. Can be also null.
     */
    public List<ParameterComparisonOutcomeTemplate> getOr() {
        return or;
    }

    /**
     * Sets the nested alternatives of ParameterComparisonOutcomeTemplate instance(s) to which logical OR || operation
     * is applied during the evaluation process.
     * @param or List of ParameterComparisonOutcomeTemplate. Can be also null.
     */
    public void setOr(List<ParameterComparisonOutcomeTemplate> or) {
        this.or = or;
    }

    /**
     * Sets the nested alternative of ParameterComparisonOutcomeTemplate instance to which logical OR || operation
     * is applied during the evaluation process. This method removes any other existing OR instances.
     * @param or Single ParameterComparisonOutcomeTemplate. Can be also null.
     */
    public void setOr(ParameterComparisonOutcomeTemplate or) {
        if (or != null) {
            this.or = new ArrayList<>(1);
            this.or.add(or);
        } else {
            this.or = null;
        }
    }

    /**
     * Checks if all sub-parameters used for comparison purposes are null or empty.
     * @return True if all sub-parameters are unspecified or false if at least one of them is.
     */
    private boolean isComparisonDataUnspecified() {
        return (expectedComparisonResult == null || expectedComparisonResult.size() == 0) &&
                (and == null || and.size() == 0) &&
                (or == null || or.size() == 0);
    }

    /**
     * Evaluates the values (ParameterComparisonOutcome or ParameterComparisonResult) inside 2 ParameterComparisonResult
     * instances. The evaluation is done from source to relation, where source is used as a base template.
     * @param source The base template used for the comparison.
     * @param relation The data that will be compared against "source".
     * @return True if there is an exact match otherwise false.
     */
    private static boolean evaluateParameterComparisonResult(ParametersComparisonResult source,
                                                     ParametersComparisonResult relation) {
        if (source == null || relation == null) return false;

        for (String k : source.keySet()) {
            if (relation.containsKey(k)) {
                IParameterComparisonOutcome src = source.get(k);
                IParameterComparisonOutcome rel = relation.get(k);

                if (src == null || rel == null) return false;

                if (src.isActualResult() && rel.isActualResult()) {
                    if (!src.equals(rel)) {
                        // Check for special ParameterComparisonOutcome values that cover several at once, before
                        // marking the comparison as failed
                        if (src instanceof ParameterComparisonOutcome && rel instanceof ParameterComparisonOutcome) {
                            ParameterComparisonOutcome _src = (ParameterComparisonOutcome) src;
                            ParameterComparisonOutcome _rel = (ParameterComparisonOutcome) rel;
                            if (!_src.specialTest(_rel)) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                } else { // Evaluation of nested parameters
                    if (src instanceof ParametersComparisonResult && rel instanceof ParametersComparisonResult) {
                        if (!evaluateParameterComparisonResult((ParametersComparisonResult)src,
                                (ParametersComparisonResult)rel)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

	/**
     * Evaluate a concrete comparison result whether it fits into (matches) the current comparison template.
	 * @param pcrt The ParameterComparisonOutcomeTemplate instance to use as a template.
     * @param concreteComparisonResult The comparison result that will be checked against the current template instance.
     * @return True if it fits, otherwise false.
     */
    public static boolean evaluate(ParameterComparisonOutcomeTemplate pcrt,
                                   ParametersComparisonResult concreteComparisonResult) {
		if (concreteComparisonResult == null || concreteComparisonResult.size() == 0) {
            if (pcrt.isComparisonDataUnspecified()) {
                // everything is unspecified, so the comparison succeeds with null == null ==> true
                return !pcrt.inverted;
            } else {
                // the comparison template is specified, however there is no concreteComparisonResult to compare with
                // so the comparison fails with null == (!null) ==> false
                return pcrt.inverted;
            }
        } else {
            if (pcrt.isComparisonDataUnspecified()) {
                // the concrete comparison map is specified, but the comparison template is not
                // so the comparison fails with (!null) == null ==> false
                return pcrt.inverted;
            }
        }

        boolean result = evaluateParameterComparisonResult(pcrt.expectedComparisonResult, concreteComparisonResult);

		//noinspection PointlessBooleanExpression
        if (result == false && pcrt.or != null && pcrt.or.size() > 0) {
			for (ParameterComparisonOutcomeTemplate _pcrt : pcrt.or) {
				result = evaluate(_pcrt, concreteComparisonResult); // equivalent to "|| =" operation
				if (result) {
					break;
				}
			}
		}

        //noinspection PointlessBooleanExpression
        if (result != false && pcrt.and != null && pcrt.and.size() > 0) {
            for (ParameterComparisonOutcomeTemplate _pcrt : pcrt.and) {
                result = evaluate(_pcrt, concreteComparisonResult); // equivalent to "&& =" operation
                if (!result) {
                    break;
                }
            }
        }

        return ((!pcrt.inverted) ? result : (!result));
	}

    /**
     * Evaluate a concrete comparison result whether it fits into the current comparison template.
     * @param concreteComparisonResult The comparison result that will be checked against the current template instance.
     * @return True if it fits, otherwise false.
     */
    public boolean evaluate(ParametersComparisonResult concreteComparisonResult) {
		return evaluate(this, concreteComparisonResult);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (inverted ? 0 : 1);
        result = 31 * result + (expectedComparisonResult == null ? 0 : expectedComparisonResult.hashCode());
        result = 31 * result + (and == null ? 0 : and.hashCode());
        result = 31 * result + (or == null ? 0 : or.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof ParameterComparisonOutcomeTemplate) {
            ParameterComparisonOutcomeTemplate _cmp = (ParameterComparisonOutcomeTemplate)obj;
            if (this.isComparisonDataUnspecified() && _cmp.isComparisonDataUnspecified()) {
                return true;
            }

            return (inverted == _cmp.getInverted()
                    && IEvent.safeEquals(expectedComparisonResult, _cmp.getExpectedComparisonResult())
                    && IEvent.safeEquals(and, _cmp.getAnd())
                    && IEvent.safeEquals(or, _cmp.getOr())
            );
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ inverted=").append(this.inverted)
                .append(", expectedComparisonResult=").append(this.expectedComparisonResult.toString())
                .append(", or=");
        if (this.or != null) {
            sb.append(this.or.toString());
        } else {
            sb.append("[]");
        }
        sb.append(", and=");
        if (this.and != null) {
            sb.append(this.and.toString());
        } else {
            sb.append("[]");
        }
        sb.append(" }");
        return sb.toString();
    }
}