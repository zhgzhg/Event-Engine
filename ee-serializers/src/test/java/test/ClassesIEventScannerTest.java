package test;

import net.uniplovdiv.fmi.cs.vrs.event.*;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.ClassesIEventScanner;
//import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//import java.net.URL;
//import java.util.ArrayList;
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

    /*private static String whereFrom(Object o) {
        if ( o == null ) {
            return null;
        }
        Class<?> c = (o instanceof Class ? (Class<?>) o : o.getClass());
        ClassLoader loader = c.getClassLoader();
        if ( loader == null ) {
            // Try the bootstrap classloader - obtained from the ultimate parent of the System Class Loader.
            loader = ClassLoader.getSystemClassLoader();
            while ( loader != null && loader.getParent() != null ) {
                loader = loader.getParent();
            }
        }
        if (loader != null) {
            String name = c.getCanonicalName();
            URL resource = loader.getResource(name.replace(".", "/") + ".class");
            if ( resource != null ) {
                return resource.toString().replace("/" + c.getSimpleName() + ".class", "")
                        .replaceFirst("^(\\w+:\\/{1,2})", "");

            }
        }
        return "Unknown";
    }

    private ArrayList<String> ieventClassPathStr;

    @BeforeEach
    void determineIEventClassPath() {
        String _ieventClassPathStr = whereFrom(IEvent.class);
        System.out.println(_ieventClassPathStr);
        if (_ieventClassPathStr != null) {
            this.ieventClassPathStr = new ArrayList<>(1);
            this.ieventClassPathStr.add(_ieventClassPathStr);
        }
    }*/

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
