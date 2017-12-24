module net.uniplovdiv.fmi.cs.vrs.event.dispatchers {
    requires java.base;
    requires java.naming;

    requires gson;
    requires fast.classpath.scanner;
    requires org.apache.commons.lang3;
    requires kafka.clients;
    requires activemq.client;
    // requires org.apache.geronimo;

    requires transitive net.uniplovdiv.fmi.cs.vrs.event;
    requires transitive net.uniplovdiv.fmi.cs.vrs.event.annotations;
    requires transitive net.uniplovdiv.fmi.cs.vrs.event.serializers;

    exports net.uniplovdiv.fmi.cs.vrs.event.dispatchers;
    exports net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.activemq;
    exports net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.kafka;
    exports net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation;
}