/**
 * Various mechanisms for de/serialization of events.
 */
module net.uniplovdiv.fmi.cs.vrs.event.serializers {
    requires java.base;
    requires java.instrument;
    requires org.apache.commons.lang3;
    requires org.apache.commons.codec;
    requires io.github.classgraph;
    requires com.google.gson;

    requires net.uniplovdiv.fmi.cs.vrs.event;

    exports net.uniplovdiv.fmi.cs.vrs.event.serializers;
    exports net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;
}