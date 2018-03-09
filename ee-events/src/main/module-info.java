/**
 * Core event definitions and algorithms.
 */
module net.uniplovdiv.fmi.cs.vrs.event {
    requires java.base;
    requires org.apache.commons.lang3;
    requires com.fasterxml.classmate;

    requires transitive net.uniplovdiv.fmi.cs.vrs.event.annotations;

    exports net.uniplovdiv.fmi.cs.vrs.event;
    exports net.uniplovdiv.fmi.cs.vrs.event.location;
    exports net.uniplovdiv.fmi.cs.vrs.event.parameters;
    exports net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison;
    opens net.uniplovdiv.fmi.cs.vrs.event to net.uniplovdiv.fmi.cs.vrs.event.annotations;
}