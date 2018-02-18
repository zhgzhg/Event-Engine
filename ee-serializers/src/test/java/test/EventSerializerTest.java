package test;

import net.uniplovdiv.fmi.cs.vrs.event.DomainEvent;
import net.uniplovdiv.fmi.cs.vrs.event.Event;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ComparableArrayList;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.IParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcomeTemplate;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParametersComparisonResult;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.JsonEventSerializer;
import net.uniplovdiv.fmi.cs.vrs.event.annotations.EmbeddedParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import net.uniplovdiv.fmi.cs.vrs.event.location.EventLocationOccurrenceMedium;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.JavaEventSerializer;
import net.uniplovdiv.fmi.cs.vrs.event.annotations.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;


public class EventSerializerTest {

    static class LectureEvent extends DomainEvent {
        private static final long serialVersionUID = 123456789L;

        @EmbeddedParameter("lecturer")
        private String lecturer;

        @EmbeddedParameter("lecture_subject")
        private String subject;

        public String getLecturer() {
            return lecturer;
        }

        public void setLecturer(String lecturer) {
            this.lecturer = lecturer;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }
    static class CustomEvent extends Event {
        private static final long serialVersionUID = 5101153334783381560L;

        @EmbeddedParameter("some_custom_attribute")
        private String someCustomAttribute;

        public String getSomeCustomAttribute() {
            return someCustomAttribute;
        }

        public void setSomeCustomAttribute(String someCustomAttribute) {
            this.someCustomAttribute = someCustomAttribute;
        }
    }
    static class CustomEvent2 extends Event {
        private static final long serialVersionUID = -1538376119524454456L;

        @EmbeddedParameter("some_custom_attribute")
        private String someCustomAttribute;

        public String getSomeCustomAttribute() {
            return someCustomAttribute;
        }

        public void setSomeCustomAttribute(String someCustomAttribute) {
            this.someCustomAttribute = someCustomAttribute;
        }
    }

    static class CustomEventWithDeclaredClasses extends Event {
        private static final long serialVersionUID = -285100436140061168L;

        static class StringComparableArrayList extends ComparableArrayList<String> {
            private static final long serialVersionUID = 3799323847065277549L;

            public StringComparableArrayList() { super(); }
            public StringComparableArrayList(StringComparableArrayList o) { super(o); }
        }

        static class StringArrayComparableArrayList extends ComparableArrayList<StringComparableArrayList> {
            private static final long serialVersionUID = -3829798530329523528L;

            public StringArrayComparableArrayList() {super();}
            public StringArrayComparableArrayList(StringArrayComparableArrayList c) {super(c);}
        }

        @EmbeddedParameter("data")
        private StringComparableArrayList data = new StringComparableArrayList();

        @EmbeddedParameter("data2")
        private StringArrayComparableArrayList data2 = new StringArrayComparableArrayList();

        public StringComparableArrayList getData() {
            return data;
        }

        public void setData(StringComparableArrayList data) {
            this.data = data;
        }

        public StringArrayComparableArrayList getData2() {
            return data2;
        }

        public void setData2(StringArrayComparableArrayList data2) {
            this.data2 = data2;
        }
    }

    private LectureEvent le;

    @BeforeEach
    void init() {
        this.le = Event.makeInstance(LectureEvent.class);

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

        le.getDynamicParameters().put("olele", "yes");
        le.addSubEvent(Event.makeInstance(Event.class));
    }

    @Test
    void javaEventSerializerUidComputationTest() throws Exception {
        JavaEventSerializer jees = new JavaEventSerializer();
        byte[] sere = jees.serialize(this.le);
        assertEquals(jees.computeSerialVersionUidFromEvent(this.le),
                jees.getSerialVersionUidFromSerializedEvent(sere));
    }

    @Test
    void javaEventSerializerSerializationTest() throws Exception {
        JavaEventSerializer jees = new JavaEventSerializer();
        byte[] ser = jees.serialize(this.le);
        IEvent deser = jees.deserialize(ser);
        assertEquals(this.le, deser);
    }

    @Test
    void jsonEventSerializerSerializationWithRegisteredClassesTest() throws Exception {
        Set<Class<? extends IEvent>> implementors = new HashSet<>();
        implementors.add(LectureEvent.class);
        JsonEventSerializer jes = new JsonEventSerializer(
                StandardCharsets.UTF_16, implementors, null);
        byte[] ser = jes.serialize(this.le);
        assertEquals(this.le, jes.deserialize(ser));
    }

    @Test
    void jsonEventSerializerDeserializationFromBadTextTest() throws Exception {
        Set<Class<? extends IEvent>> implementors = new HashSet<>();
        implementors.add(CustomEvent.class);
        implementors.add(CustomEvent2.class);

        JsonEventSerializer jes = new JsonEventSerializer(
                StandardCharsets.UTF_16, implementors, null);
        String data = "{\"__event_type_class_name\":\"test.EventSerializerTest.CustomEvent\",\"someCustomAttribute\":\"blahblah\",\"id\":3,\"timestampMs\":1502083108905,\"priority\":0,\"eventLocation\":{\"address\":\"\",\"occurrenceMediumType\":0,\"extraData\":{}},\"description\":\"\",\"dynamicParameters\":{},\"subEvents\":{},\"serialVersionUID\":9048917202584703043}";
        byte[] bdata = jes.bytesFromString(data);
        System.err.println("-------THIS IS OK------");
        assertThrows(IOException.class, () -> jes.deserialize(bdata));
        System.err.println("-----------------------");
    }

    @Test
    void jsonEventSerializerDeserializationFromTextTest() throws Exception {
        Set<Class<? extends IEvent>> implementors = new HashSet<>();
        implementors.add(CustomEvent.class);
        implementors.add(CustomEvent2.class);

        JsonEventSerializer jes = new JsonEventSerializer(
                StandardCharsets.UTF_16, implementors, null);
        String data = "{\"__event_type_class_name\":\"test.EventSerializerTest.CustomEvent2\",\"someCustomAttribute\":\"blahblah\",\"id\":3,\"timestampMs\":1502083108905,\"priority\":0,\"eventLocation\":{\"address\":\"\",\"occurrenceMediumType\":0,\"extraData\":{}},\"description\":\"\",\"dynamicParameters\":{},\"subEvents\":{},\"serialVersionUID\":-1538376119524454456}";
        byte[] bdata = jes.bytesFromString(data);
        IEvent eZero = jes.deserialize(bdata);
        assertEquals(CustomEvent2.class, eZero.getClass());
    }

    @Test
    void javaEventSerializerHelpersSerializationDeserializationTest() throws Exception {
        ParametersComparisonResult pcr = new ParametersComparisonResult();
        ParametersComparisonResult pcr0 = new ParametersComparisonResult();
        pcr.put("testEvent1", pcr0);
        pcr0.put("testParam1", ParameterComparisonOutcome.EQUAL);

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate(pcr);

        JavaEventSerializer jes = new JavaEventSerializer();
        byte[] b = jes.serializePCO(pcr);

        IParameterComparisonOutcome upcr = jes.deserializePCO(b);
        assertEquals(pcr, upcr);

        b = jes.serializePCOT(pcot);
        ParameterComparisonOutcomeTemplate upcot = jes.deserializePCOT(b);
        assertEquals(pcot, upcot);

        upcot.setAnd(new ParameterComparisonOutcomeTemplate());
        assertNotEquals(pcot, upcot);
    }

    @Test
    void jsonEventSerializerHelpersSerializationDeserializationTest() throws Exception {
        ParametersComparisonResult pcr = new ParametersComparisonResult();
        ParametersComparisonResult pcr0 = new ParametersComparisonResult();
        pcr.put("testEvent1", pcr0);
        pcr0.put("testParam1", ParameterComparisonOutcome.EQUAL);

        ParameterComparisonOutcomeTemplate pcot = new ParameterComparisonOutcomeTemplate(pcr);

        JsonEventSerializer jes = new JsonEventSerializer();
        byte[] b = jes.serializePCO(pcr);

        IParameterComparisonOutcome upcr = jes.deserializePCO(b);
        assertEquals(pcr, upcr);

        b = jes.serializePCOT(pcot);
        ParameterComparisonOutcomeTemplate upcot = jes.deserializePCOT(b);
        assertEquals(pcot, upcot);

        upcot.setAnd(new ParameterComparisonOutcomeTemplate());
        assertNotEquals(pcot, upcot);
    }

    @Test
    void javaEventWithNestedClassDeclarationsSerializationDeserializationTest() throws Exception {
        CustomEventWithDeclaredClasses event = CustomEventWithDeclaredClasses.makeInstance(CustomEventWithDeclaredClasses.class);
        event.getData().add("lqlqlq");

        event.getData2().add(new CustomEventWithDeclaredClasses.StringComparableArrayList());
        event.getData2().get(0).add("bobobo");

        System.out.println(event.toString());

        JavaEventSerializer jees = new JavaEventSerializer();
        byte[] ser = jees.serialize(event);
        IEvent deser = jees.deserialize(ser);
        assertEquals(event, deser);
    }

    @Test
    void jsonEventWithNestedClassDeclarationsSerializationDeserializationTest() throws Exception {
        CustomEventWithDeclaredClasses event = CustomEventWithDeclaredClasses.makeInstance(CustomEventWithDeclaredClasses.class);
        event.getData().add("lqlqlq");

        event.getData2().add(new CustomEventWithDeclaredClasses.StringComparableArrayList());
        event.getData2().get(0).add("bobobo");

        Set<Class<? extends IEvent>> implementors = new HashSet<>();
        implementors.add(CustomEventWithDeclaredClasses.class);
        JsonEventSerializer jes = new JsonEventSerializer(
                StandardCharsets.UTF_16, implementors, null);

        byte[] ser = jes.serialize(event);
        //System.out.println(jes.stringFromBytes(ser));
        IEvent deser = jes.deserialize(ser);
        assertEquals(event, deser);
        //System.out.println(deser.toString());
    }
}
