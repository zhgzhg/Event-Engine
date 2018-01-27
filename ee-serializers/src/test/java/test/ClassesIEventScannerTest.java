package test;

import net.uniplovdiv.fmi.cs.vrs.event.*;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.ClassesIEventScanner;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassesIEventScannerTest {

    private HashSet<Class<? extends IEvent>> foundEventClasses = new HashSet<Class<? extends IEvent>>() {
        private static final long serialVersionUID = 938705210137734396L;
        {
            add(Event.class);
            add(DomainEvent.class);
            add(SystemEvent.class);
            add(EmergencyEvent.class);
        }
    };

    @Test
    void defaultScanParams() {
        ClassesIEventScanner cis = new ClassesIEventScanner();
        assertTrue(cis.getFoundEventClasses().isEmpty());
        assertArrayEquals(new String[] { IEvent.class.getPackage().getName() }, cis.getPackagesToScan());
        assertEquals(foundEventClasses, cis.scan());
        assertEquals(foundEventClasses, cis.getFoundEventClasses());
    }

    @Test
    void customScanParams() {
        ClassesIEventScanner cis = new ClassesIEventScanner(IEvent.class.getPackage().getName());
        assertTrue(cis.getFoundEventClasses().isEmpty());
        assertArrayEquals(new String[] { IEvent.class.getPackage().getName(), Event.class.getPackage().getName() },
            cis.getPackagesToScan());
        assertEquals(foundEventClasses, cis.scan());
        assertEquals(foundEventClasses, cis.getFoundEventClasses());
    }
}
