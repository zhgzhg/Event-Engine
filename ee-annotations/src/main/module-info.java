/**
 * Annotations and annotation processors used when defining new event classes.
 */
module net.uniplovdiv.fmi.cs.vrs.event.annotations {
    requires java.base;
    requires java.compiler;
    requires org.apache.commons.lang3;

    exports net.uniplovdiv.fmi.cs.vrs.event.annotations;
    exports net.uniplovdiv.fmi.cs.vrs.event.annotations.processors;
    exports net.uniplovdiv.fmi.cs.vrs.event.annotations.wrappers;
}