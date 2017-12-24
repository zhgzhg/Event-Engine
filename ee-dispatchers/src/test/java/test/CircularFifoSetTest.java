package test;

import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.CircularFifoSet;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class CircularFifoSetTest {

    @Test
    void simpleOperationsTest() {
        CircularFifoSet<Integer> cfs = new CircularFifoSet<>(5);
        assertEquals(5, cfs.getCapacity());

        cfs.add(1);
        cfs.add(3);
        cfs.add(2);

        assertArrayEquals(new Integer[]{2, 3, 1}, cfs.toArray());
        cfs.remove(1);
        assertArrayEquals(new Integer[]{2, 3}, cfs.toArray());

        cfs.add(3);
        assertArrayEquals(new Integer[]{3, 2}, cfs.toArray());

        cfs.add(4);
        cfs.add(5);
        cfs.add(6);
        cfs.add(7);
        cfs.add(8);
        assertArrayEquals(new Integer[]{8, 7, 6, 5, 4}, cfs.toArray());

        cfs.clear();
        assertTrue(cfs.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new CircularFifoSet<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularFifoSet<Integer>(-1));
    }

    @Test
    void iteratorTest() {
        CircularFifoSet<Integer> cfs = new CircularFifoSet<>(5);
        cfs.add(1);
        cfs.add(2);
        cfs.add(3);
        cfs.add(4);
        cfs.add(5);

        Iterator<Integer> it = cfs.iterator();
        assertNotNull(it);
        assertTrue(it.hasNext());

        Integer i = it.next();
        assertEquals(5, i.intValue());
        it.remove();

        assertArrayEquals(new Integer[]{4,3,2,1}, cfs.toArray());

        i = it.next();
        assertEquals(4, i.intValue());

        cfs.add(6);
        assertTrue(cfs.contains(1));
        assertTrue(cfs.contains(6));
    }


}
