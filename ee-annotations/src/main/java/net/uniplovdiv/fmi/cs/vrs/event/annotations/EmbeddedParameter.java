package net.uniplovdiv.fmi.cs.vrs.event.annotations;

import net.uniplovdiv.fmi.cs.vrs.event.annotations.wrappers.UnknownParameterWrapper;

import java.lang.annotation.*;

/**
 * Marks that the particular field is actually an embedded event parameter, natively provided by the class.
 * Such parameters are automatically included during the construction of events comparison template.
 * All data types are supported, but for those that are not primitive a copy constructor is required!
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EmbeddedParameter {
    /**
     * The string name used as a key. It will be used during the dynamic creation of maps for different purposes
     * like for e.g. constructing a template for value comparison of parameters. The name should be unique. Duplicates
     * are allowed, but their values will be overwritten down on the inheritance hierarchy.
     * @return The assigned string value.
     */
    String value();

    /**
     * The wrapper class used to provide value comparison implementation, to pack the object when pushed to
     * ParametersContainer or other structures and provide a copy constructor for itself.
     * @return The appropriate class "wrapper" (sometimes pseudo) object.
     */
    Class<? extends Comparable<?>> wrapper() default UnknownParameterWrapper.class;
}
