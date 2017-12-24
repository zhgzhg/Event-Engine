module net.uniplovdiv.fmi.cs.vrs.event.serializers {
    requires java.base;
    requires java.instrument;
    requires org.apache.commons.lang3;
    requires fast.classpath.scanner;
    requires gson;

    requires net.uniplovdiv.fmi.cs.vrs.event;

    exports net.uniplovdiv.fmi.cs.vrs.event.serializers;
    exports net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;
}