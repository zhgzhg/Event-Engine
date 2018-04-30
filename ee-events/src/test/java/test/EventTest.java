package test;

import net.uniplovdiv.fmi.cs.vrs.event.Event;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.SystemEvent;
import net.uniplovdiv.fmi.cs.vrs.event.annotations.EmbeddedParameter;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ComparableArrayList;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcomeTemplate;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParametersComparisonResult;
import net.uniplovdiv.fmi.cs.vrs.event.location.EventLocationOccurrenceMedium;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ParametersContainer;
import org.junit.jupiter.api.Test;
import test.helpers.LectureEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventTest {
    public static byte[] ser(Object o) throws IOException {
        ByteArrayOutputStream ew = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(ew);
        oos.writeObject(o);
        oos.close();
        return ew.toByteArray();
    }

    public static Object unser(byte[] o) throws IOException, ClassNotFoundException {
        ByteArrayInputStream ew = new ByteArrayInputStream(o);
        ObjectInputStream ois = new ObjectInputStream(ew);
        return ois.readObject();
    }

    @Test
    void refreshEventIdTest() throws Exception {
        class RefreshTestEvent extends Event {
            private static final long serialVersionUID = -6217121310402792950L;
            @EmbeddedParameter("something")
            public String something;

            @EmbeddedParameter("embedded_event")
            public Event embeddedEvent;

            @EmbeddedParameter("embedded_event2")
            public Event embeddedEvent2;
        }

        RefreshTestEvent e1 = RefreshTestEvent.makeInstance(RefreshTestEvent.class, this);

        long eId = e1.getId();
        Event.reserveNewId(e1);
        assertTrue(eId < e1.getId());

        e1.embeddedEvent = RefreshTestEvent.makeInstance(RefreshTestEvent.class, this);
        eId = e1.getId();
        long nestedEventId = e1.embeddedEvent.getId();

        Event.reserveNewId(e1, false);
        assertTrue(eId < e1.getId());
        assertEquals(nestedEventId, e1.embeddedEvent.getId());

        eId = e1.getId();
        nestedEventId = e1.embeddedEvent.getId();
        long dynamicNestedEventId = e1.embeddedEvent.addSubEvent(Event.makeInstance(Event.class));
        Event.reserveNewId(e1);

        assertTrue(eId < e1.getId());
        assertTrue(nestedEventId < e1.embeddedEvent.getId());
        assertTrue(dynamicNestedEventId < e1.embeddedEvent.getSubEvents().firstKey());
        assertEquals(e1.embeddedEvent.getSubEvents().firstKey().longValue(), e1.embeddedEvent.getSubEvent(e1.embeddedEvent.getSubEvents().firstKey()).getId());
        assertEquals(1, e1.embeddedEvent.getSubEvents().size());
    }

    private static class __TestArraysAndLists extends Event {
        private static final long serialVersionUID = -4070334097034981710L;

        static class IntegerComparableArrayList extends ComparableArrayList<Integer> {
            private static final long serialVersionUID = 7049838361812247581L;

            public IntegerComparableArrayList() {super();}
            public IntegerComparableArrayList(IntegerComparableArrayList c) {
                super(c);
            }
        }

        static class StringArrayComparableArrayList extends ComparableArrayList<ComparableArrayList<String>> {
            private static final long serialVersionUID = -3650659129887207029L; // this cannot be serialized because of the template inside template generic type param. to fix that a proper class needs to be declared separately
            public StringArrayComparableArrayList() {super();}
            public StringArrayComparableArrayList(StringArrayComparableArrayList c) {super(c);}
        }

        @EmbeddedParameter("data")
        byte[] data;

        @EmbeddedParameter(value = "data2")
        IntegerComparableArrayList data2;

        @EmbeddedParameter("data3")
        StringArrayComparableArrayList data3;

        public __TestArraysAndLists() {
            this.data = new byte[] {0x01,0x02,0x03,0x04,0x05};
            data2 = new IntegerComparableArrayList();
            data2.add(1);
            data3 = new StringArrayComparableArrayList();
            data3.add(new ComparableArrayList<String>() {
                private static final long serialVersionUID = -54714092899834077L;
            });
            data3.get(0).add("lqlqlq");
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public List<Integer> getData2() {
            return this.data2;
        }

        public void setData2(IntegerComparableArrayList data2) {
            this.data2 = data2;
        }

        /*public StringArrayComparableArrayList getData3() {
            return data3;
        }

        public void setData3(StringArrayComparableArrayList data3) {
            this.data3 = data3;
        }*/
    }

    @Test
    void testEventParametersOfArraysAndLists() throws Exception {
        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate();
        ParametersComparisonResult pcr = new ParametersComparisonResult();
        pcot.setExpectedComparisonResult(pcr);
        pcr.put("data", ParameterComparisonOutcome.EQUAL);

        __TestArraysAndLists a = __TestArraysAndLists.makeInstance(__TestArraysAndLists.class);
        __TestArraysAndLists b = __TestArraysAndLists.makeInstance(__TestArraysAndLists.class);

        assertTrue(pcot.evaluate(a.compareParametersTo(b)));

        pcr.put("data", ParameterComparisonOutcome.LESS);

        b.setData(new byte[]{0x02,0x03,0x04,0x05,0x06});

        assertTrue(pcot.evaluate(a.compareParametersTo(b)));
    }

    static class __TestUninitialized extends Event {
        private static final long serialVersionUID = -5057896957717187725L;
        @EmbeddedParameter(value = "uninitialized")
        byte[] uninitialized;

        @EmbeddedParameter(value = "uninitialized2")
        String[] uninitialized2;

        @EmbeddedParameter(value = "uninitialized3")
        String[][] uninitialized3;

        @EmbeddedParameter(value = "uninitialized4")
        String[][][] uninitialized4;
    }

    @Test
    void uninitializedPrimitiveArrayInsideEventTest() {
        assertNotNull(Event.makeInstance(__TestUninitialized.class));
    }

    @Test
    void checkSameEventAsSubEvent() {
        LectureEvent le = Event.makeInstance(LectureEvent.class);
        assertThrows(IllegalArgumentException.class, () -> le.addSubEvent(le));
    }

    @Test
    void complexTest9() throws Exception {
        LectureEvent le = Event.makeInstance(LectureEvent.class);

        le.setLecturer("John Smith");
        le.setSubject("Computer Accounting");

        le.getEventLocation().setAddress("Boulevard Bulgaria 236, FMI");
        le.getEventLocation().addExtraData("room", "423");
        le.getEventLocation().addExtraData("online",
                "rtmp://stream1.fmi-plovdiv.org/computerAccounting.mp4");
        le.getEventLocation().setOccurrenceType(
                EventLocationOccurrenceMedium.prepare(
                        EventLocationOccurrenceMedium.PHYSICAL,
                        EventLocationOccurrenceMedium.VIRTUAL
                )
        );

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate();
        pcot.setExpectedComparisonResult(new ParametersComparisonResult());
        pcot.getExpectedComparisonResult().put("lecturer", ParameterComparisonOutcome.EQUAL);

        LectureEvent lectureWithJohnSmith = new LectureEvent();
        lectureWithJohnSmith.setLecturer("John Smith");

        boolean isJohnSmithLecturer = pcot.evaluate(lectureWithJohnSmith.compareParametersTo(le));

        assertTrue(isJohnSmithLecturer);
    }

    public static class _TestEvent5 extends Event {
        private static final long serialVersionUID = 1374334555503709288L;
        @EmbeddedParameter("e-e")
        public Integer i;
    }

    public static class _TestEvent6 extends Event {
        private static final long serialVersionUID = -7693810695557683774L;
        @EmbeddedParameter("e-e")
        public String i;
    }

    @Test
    void differentEventParameterTypesTest() throws Exception {
        _TestEvent5 te5 = Event.makeInstance(_TestEvent5.class);
        _TestEvent6 te6 = Event.makeInstance(_TestEvent6.class);
        ParametersComparisonResult pcr = new ParametersComparisonResult();
        pcr.put("e-e", ParameterComparisonOutcome.INCOMPARABLE);
        te5.i = 8;
        te6.i = "8";

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate();
        pcot.setExpectedComparisonResult(pcr);

        System.err.println("-----------This IS OK---------");
        ParametersComparisonResult result = te5.compareParametersTo(te6);
        System.err.println("------------------------------");
        assertTrue(pcot.evaluate(result));
    }

    @Test
    void basicCopyCtorTest() throws Exception {
        SystemEvent se1 = Event.makeInstance(SystemEvent.class);
        SystemEvent se2 = new SystemEvent(se1);
        assertEquals(se1, se2);
    }

    @Test
    void serializationTest() throws Exception {
        Event e = Event.makeInstance(Event.class);

        ParametersComparisonResult pcr = new ParametersComparisonResult();
        ParametersComparisonResult pcr0 = new ParametersComparisonResult();
        pcr.put("testEvent1", pcr0);
        pcr0.put("testParam1", ParameterComparisonOutcome.EQUAL);

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate(pcr);

        byte[] b = ser(e);
        Event ue = (Event)unser(b);
        assertEquals(e, ue);

        b = ser(pcr);
        ParametersComparisonResult upcr = pcr;
        assertEquals(pcr, upcr);

        b = ser(pcot);
        ParameterComparisonOutcomeTemplate upcot = (ParameterComparisonOutcomeTemplate)unser(b);
        assertEquals(pcot, upcot);

        upcot.setAnd(new ParameterComparisonOutcomeTemplate());
        assertNotEquals(pcot, upcot);
    }

    class _TestEvent1 extends Event {
        private static final long serialVersionUID = 2926275522197125279L;

        @EmbeddedParameter("testParam1")
        public String testParam1 = "fdfds";

        public _TestEvent1() {
            super();
        }

        public _TestEvent1(_TestEvent1 te1) {
            super(te1);
            this.testParam1 = te1.testParam1;
        }
    }

    class _TestEvent2 extends Event {
        private static final long serialVersionUID = 2926275522197125279L;

        @EmbeddedParameter("testParam2")
        public String testParam2;

        public _TestEvent2() {
            super();
        }

        private _TestEvent2(_TestEvent2 te2) {
            super(te2);
            this.testParam2 = te2.testParam2;
        }
    }

    class _TestEvent3 extends Event {
        private static final long serialVersionUID = 2926275522197125279L;

        @EmbeddedParameter(value = "testEvent1")
        public _TestEvent1 testEvent1 = new _TestEvent1();

        @EmbeddedParameter(value = "testEvent2")
        public _TestEvent2 testEvent2 = new _TestEvent2();

        @EmbeddedParameter("fdgf")
        public static final String aaa = "gfd";
    }

    @Test
    void complexTest5() throws Exception {
        _TestEvent3 te3 = new _TestEvent3();
        ParametersContainer pc = null;
        pc = te3.getWithEmbeddedParameters();
        assertNotNull(pc);

        ParametersComparisonResult pcr = new ParametersComparisonResult();
        ParametersComparisonResult pcr0 = new ParametersComparisonResult();
        pcr.put("testEvent1", pcr0);
        pcr0.put("testParam1", ParameterComparisonOutcome.EQUAL);

        _TestEvent3 te3_2 = new _TestEvent3();

        _TestEvent3 te3_3 = new _TestEvent3();
        te3_3.testEvent1.testParam1 = "fsdfsdsadasdasdasd";

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate(pcr);
        assertTrue(pcot.evaluate(te3.compareParametersTo(te3_2)));
        assertFalse(pcot.evaluate(te3.compareParametersTo(te3_3)));
    }

    @Test
    void complexTest4() throws Exception {
        IEvent e1 = Event.makeInstance(Event.class);
        e1.setDescription("'MAIN EVENT'");
        e1.getDynamicParameters().put("'main test dynamic param'", "v@luEEEEEEEE");

        IEvent se1 = SystemEvent.makeInstance(SystemEvent.class);
        se1.setDescription("lqlqlq");
        se1.getDynamicParameters().put("'testing_dynamic_param'", "123333322111");
        se1.getDynamicParameters().put("'testing_dynamic_param_unique_for_se1'", "12121233");

        e1.addSubEvent(se1);

        ParametersContainer pc = e1.getWithEmbeddedParameters();
        pc = null;

        // $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

        Event e2 = Event.makeInstance(Event.class);
        e2.setDescription("'NOT matching main event'");
        SystemEvent se2 = SystemEvent.makeInstance(SystemEvent.class);
        e2.addSubEvent(se2);
        se2.setDescription("lqlqlq");
        se2.getDynamicParameters().put("'testing_dynamic_param'", "123333322111");
        se2.getDynamicParameters().put("'testing_dynamic_param_unique_for_se2'", "654654654");

        // ####################################################

        // compare e1 to e2

        ParametersComparisonResult cr = new ParametersComparisonResult();
        cr.put(Event.ParamNames.SUBEVENTS, ParameterComparisonOutcome.EQUAL);
        cr.put("'main test dynamic param'", ParameterComparisonOutcome.GREATER);

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate();
        pcot.setExpectedComparisonResult(cr);

        boolean doMatch = false;
        doMatch = pcot.evaluate(e1.compareParametersTo(e2)); //must be still false
        assertFalse(doMatch);

        ParametersContainer allParamsE1 = new ParametersContainer();
        ParametersContainer allParamsE2 = new ParametersContainer();
        allParamsE1 = e1.getWithEmbeddedParameters();
        allParamsE2 = e2.getEmbeddedParameters();

        ParametersComparisonResult comparisonResult = Event.compareParameters(allParamsE1, allParamsE2);
        boolean doMatchShort = pcot.evaluate(comparisonResult); // false
        assertFalse(doMatchShort);

        // =========================================

        ParametersComparisonResult goodCompRes = new ParametersComparisonResult();
        goodCompRes.put("'main test dynamic param'", ParameterComparisonOutcome.NOTCOMPARED);

        ParametersComparisonResult goodCompResSubEvents = new ParametersComparisonResult();
        goodCompRes.put(Event.ParamNames.SUBEVENTS, goodCompResSubEvents);

        ParametersComparisonResult goodCompResSubEvent = new ParametersComparisonResult();
        goodCompResSubEvents.put(String.valueOf(se1.getId()), goodCompResSubEvent);

        goodCompResSubEvent.put(Event.ParamNames.DESCRIPTION, ParameterComparisonOutcome.EQUAL);
        goodCompResSubEvent.put("'testing_dynamic_param'", ParameterComparisonOutcome.EQUAL);
        goodCompResSubEvent.put("'testing_dynamic_param_unique_for_se1'",
                ParameterComparisonOutcome.NOTCOMPARED_UNKNOWN /*ParameterComparisonOutcome.NOTCOMPARED*/);
        goodCompResSubEvent.put("'testing_dynamic_param_unique_for_se2'",
                ParameterComparisonOutcome.NOTCOMPARED_UNKNOWN_INCOMPARABLE /*ParameterComparisonOutcome.UNKNOWN*/);

        ParameterComparisonOutcomeTemplate goodCompare = new ParameterComparisonOutcomeTemplate();
        goodCompare.setExpectedComparisonResult(goodCompRes);

        boolean doMatchGood = goodCompare.evaluate(comparisonResult);
        assertTrue(doMatchGood);
    }

    @Test
    void complexTest3() throws Exception {
        assertNotEquals(new Event().hashCode(), new SystemEvent().hashCode());

        SystemEvent se = SystemEvent.makeInstance(SystemEvent.class);
        ParametersContainer pc = se.getEmbeddedParameters(); // assert not throws
        se.setDynamicParameters(null);
        pc = se.getEmbeddedParameters(); // assert not throws

        final ParametersContainer _pc = pc;
        assertThrows(IllegalArgumentException.class, () -> {
            _pc.put("invalid_object", new Object());
        });

        pc.toggleSerializationChecks(false);
        pc.put("invalid_object", new Object()); // assert not throws

        pc.toggleSerializationChecks(true);
        assertThrows(IllegalArgumentException.class, () -> {
            _pc.put("invalid_object", new Object());
        });

        pc.put("invalid_object", null); // assert not throws
        Event e = null;
        pc.put("should_be_fine", e); // assert not throws

        se.setDynamicParameters(pc);
        byte[] b = ser(se);
        SystemEvent se2 = (SystemEvent)unser(b); // assert not throws

        se2.getDynamicParameters().toggleSerializationChecks(false);
        se2.getDynamicParameters().put("bad", new Object());
        assertThrows(NotSerializableException.class, () -> { ser(se2); });

        se2.getDynamicParameters().put("bad", "no more");
        assertTrue(se2.toString().contains("bad=no more"));
    }

    @Test
    void complexTest2() {
        ParametersContainer templateParameters = new ParametersContainer();
        templateParameters.put("name", "Petko");
        templateParameters.put("age", 25);

        ParametersContainer eventParameters = new ParametersContainer();
        eventParameters.put("name", "Petko");
        eventParameters.put("age", 26);

        ParametersComparisonResult result = Event.compareParameters(eventParameters, templateParameters);

        ParametersComparisonResult desiredComparisonResult1 = new ParametersComparisonResult();
        desiredComparisonResult1.put("name", ParameterComparisonOutcome.EQUAL);
        desiredComparisonResult1.put("age", ParameterComparisonOutcome.EQUAL);

        ParameterComparisonOutcomeTemplate pcrt1 = new ParameterComparisonOutcomeTemplate();
        pcrt1.setExpectedComparisonResult(desiredComparisonResult1);

        assertFalse(pcrt1.evaluate(result));

        ParametersComparisonResult desiredComparisonResult2 = new ParametersComparisonResult();
        // desiredComparisonResult2.put("name", ParameterComparisonOutcome.EQUAL); // no need to specify this one again in this case since this is independent, but nested check
        desiredComparisonResult2.put("age", ParameterComparisonOutcome.GREATER);

        pcrt1.setOr(new ArrayList<>());
        pcrt1.getOr().add(new ParameterComparisonOutcomeTemplate(desiredComparisonResult2, null, null, false));
        //pcrt1.getOr().add(new ParameterComparisonOutcomeTemplate(desiredComparisonResult1, null, null, false));
        pcrt1.setAnd(new ParameterComparisonOutcomeTemplate(desiredComparisonResult2, null, null, false));

        assertTrue(pcrt1.evaluate(result));
    }

    @Test
    void complexTest1() {
        IEvent e = new Event(0, 1);
        IEvent ee = new Event(0, 1);

        assertEquals(0, e.compareTo(e));
        assertTrue(e.equals(ee));
        assertEquals(e.hashCode(), ee.hashCode());
        assertFalse(e == ee);

        IEvent eee1 = new SystemEvent();
        IEvent eee2 = new Event();
        IEvent eee3 = new Event();
        assertFalse(eee1.equals(eee2));
        assertTrue(eee3.equals(eee2));

        IEvent eee = SystemEvent.makeInstance(SystemEvent.class);
        IEvent eeee = Event.makeInstance(Event.class);

        eee.addSubEvent(0L, ee);
        String eeeState1 = eee.toString();
        eee.addSubEvent(1L, e);
        eee.removeSubEvent(0L);

        assertNotEquals(eeeState1, eee.toString());
        assertFalse(eeee.equals(eee));
        assertEquals(1, eeee.compareTo(eee));

        assertFalse(ee.hasDescription());
        assertFalse(ee.hasDynamicParameters());
        assertFalse(ee.hasLocation());
        assertFalse(ee.hasSubEvents());

        int oldEeHashCode = ee.hashCode();

        ee.setDescription("main");
        ee.getDynamicParameters().put("ole", "mai");
        ee.getEventLocation().setAddress("dasdasdasdsdadadsas adres4e");
        ee.getEventLocation().setOccurrenceType(
                EventLocationOccurrenceMedium.prepare(EventLocationOccurrenceMedium.PHYSICAL,
                        EventLocationOccurrenceMedium.VIRTUAL)
        );
        ee.addSubEvent(eee);

        assertTrue(ee.hasDescription());
        assertTrue(ee.hasDynamicParameters());
        assertTrue(ee.hasLocation());
        assertTrue(ee.hasSubEvents());

        assertFalse(oldEeHashCode == ee.hashCode());

        SystemEvent selele = SystemEvent.makeInstance(SystemEvent.class);
        try {
            byte[] b = ser(ee);
            Object o = unser(b);
            //System.out.println(o);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            assertTrue(false);
        }

        assertTrue(ee.equals(ee));

        ParametersContainer newParams = new ParametersContainer();

        IEvent newEvent = null;
        try {
            newEvent = (Event)unser(ser(ee));
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }

        assertTrue(newEvent.equals(ee));

        ParametersComparisonResult stringParameterComparisonResultMap = null;
        try {
            stringParameterComparisonResultMap = newEvent.compareParametersTo(ee);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }

        ParametersComparisonResult expected = new ParametersComparisonResult();
        expected.put(Event.ParamNames.ID, ParameterComparisonOutcome.EQUAL);
        expected.put(Event.ParamNames.TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        expected.put(Event.ParamNames.VALID_FROM_TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        expected.put(Event.ParamNames.VALID_THROUGH_TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        expected.put(Event.ParamNames.PRIORITY, ParameterComparisonOutcome.EQUAL);
        expected.put(Event.ParamNames.DESCRIPTION, ParameterComparisonOutcome.EQUAL);
        expected.put(Event.ParamNames.LOCATION, ParameterComparisonOutcome.EQUAL);
        expected.put("ole", ParameterComparisonOutcome.EQUAL);

        ParametersComparisonResult pcr1 = new ParametersComparisonResult();
        expected.put(Event.ParamNames.SUBEVENTS, pcr1);

        ParametersComparisonResult pcr2 = new ParametersComparisonResult();
        pcr1.put(newEvent.getSubEvents().firstKey().toString(), pcr2);
        pcr2.put(Event.ParamNames.ID, ParameterComparisonOutcome.EQUAL);
        pcr2.put(Event.ParamNames.TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        pcr2.put(Event.ParamNames.VALID_FROM_TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        pcr2.put(Event.ParamNames.VALID_THROUGH_TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        pcr2.put(Event.ParamNames.PRIORITY, ParameterComparisonOutcome.EQUAL);
        pcr2.put(Event.ParamNames.DESCRIPTION, ParameterComparisonOutcome.EQUAL);
        pcr2.put(Event.ParamNames.LOCATION, ParameterComparisonOutcome.EQUAL);

        ParametersComparisonResult pcr3 = new ParametersComparisonResult();
        pcr2.put(Event.ParamNames.SUBEVENTS, pcr3);

        ParametersComparisonResult pcr4 = new ParametersComparisonResult();
        pcr3.put(newEvent.getSubEvents().get(newEvent.getSubEvents().firstKey())
                        .getSubEvents().firstKey().toString(),
                pcr4);

        pcr4.put(Event.ParamNames.ID, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.VALID_FROM_TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.VALID_THROUGH_TIMESTAMP, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.PRIORITY, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.DESCRIPTION, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.LOCATION, ParameterComparisonOutcome.EQUAL);
        pcr4.put(Event.ParamNames.SUBEVENTS, ParameterComparisonOutcome.EQUAL);

        assertEquals(expected, stringParameterComparisonResultMap);

        //stringParameterComparisonResultMap.forEach((String key, ParameterComparisonOutcome value) -> {
        //    System.out.printf("%s - %s" , key, value.toString());
        //});
    }
}