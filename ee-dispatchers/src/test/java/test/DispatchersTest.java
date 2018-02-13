package test;

import net.uniplovdiv.fmi.cs.vrs.event.*;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.DispatchingType;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataEncodingMechanism;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataPacket;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.kafka.ConfigurationFactoryKafka;
import org.junit.jupiter.api.Test;


import javax.xml.crypto.Data;

import static org.junit.jupiter.api.Assertions.*;


import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("serial")
public class DispatchersTest {

    @Test
    void testEncapsulation() {
        byte[] payload = new byte[]{0x55, 0x55, 0x55, 0x55, 0x55};
        DataEncodingMechanism sm = DataEncodingMechanism.JAVA;
        Charset cs = null;

        DataPacket dp = new DataPacket(sm, cs, payload);

        byte[] packed = dp.toBytes();
        byte[] goodPacked = new byte[]{DataPacket.Version.CLASSIC.getCode(),
                DataEncodingMechanism.JAVA.getCode(), 0x00, 0x55, 0x55, 0x55, 0x55, 0x55};

        assertEquals(goodPacked.length, packed.length);
        assertArrayEquals(goodPacked, packed);

        assertEquals(cs, dp.getEncoding());
        assertEquals(sm, dp.getDataEncodingMechanismType());
        assertArrayEquals(payload, dp.getPayload());
        assertEquals(DataPacket.Version.CLASSIC, dp.getDataPacketVersion());
        assertNull(dp.getEncodingName_ISO_8859_1());

        DataPacket unpDp = new DataPacket(packed);
        assertEquals(cs, unpDp.getEncoding());
        assertNull(cs);
        assertEquals(sm, unpDp.getDataEncodingMechanismType());
        assertArrayEquals(payload, dp.getPayload());
        assertEquals(DataPacket.Version.CLASSIC, dp.getDataPacketVersion());
        assertNull(dp.getEncodingName_ISO_8859_1());

        assertSame(cs, unpDp.getEncoding());
        assertNotSame(payload, unpDp.getPayload());

        sm = DataEncodingMechanism.JSON;
        cs = StandardCharsets.UTF_16;
        dp = new DataPacket(sm, cs, payload);

        packed = dp.toBytes();
        goodPacked = new byte[DataPacket.Version.getCodeSize() + DataEncodingMechanism.getCodeSize() + 1
                + cs.name().length() + payload.length];
        goodPacked[0] = DataPacket.Version.CLASSIC.getCode();
        goodPacked[1] = DataEncodingMechanism.JSON.getCode();
        goodPacked[2] = (byte) cs.name().length();
        {
            byte[] name = StandardCharsets.UTF_16.name().getBytes(StandardCharsets.ISO_8859_1);
            int i = 3, j = 0;
            for (; j < name.length; ++j, ++i) {
                goodPacked[i] = name[j];
            }
            for (j = 0; j < payload.length; ++j, ++i) {
                goodPacked[i] = payload[j];
            }
        }

        assertEquals(goodPacked.length, packed.length);
        assertArrayEquals(goodPacked, packed);

        assertEquals(cs, dp.getEncoding());
        assertArrayEquals(cs.name().getBytes(StandardCharsets.ISO_8859_1), dp.getEncodingName_ISO_8859_1());
        assertEquals(sm, dp.getDataEncodingMechanismType());
        assertArrayEquals(payload, dp.getPayload());
        assertEquals(DataPacket.Version.CLASSIC, dp.getDataPacketVersion());

        assertNotSame(cs, unpDp.getEncoding());
        assertNotSame(payload, unpDp.getPayload());

        assertThrows(IllegalArgumentException.class, () -> new DataPacket(null));
        assertThrows(NullPointerException.class, () -> new DataPacket(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new DataPacket(DataEncodingMechanism.JAVA, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new DataPacket(DataEncodingMechanism.JSON, new Charset(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                                + "ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
                                + "ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
                                + "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
                                + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", null) {

                    @Override
                    public boolean contains(Charset cs) {
                        return false;
                    }

                    @Override
                    public CharsetDecoder newDecoder() {
                        return null;
                    }

                    @Override
                    public CharsetEncoder newEncoder() {
                        return null;
                    }
                }, new byte[1]));

        assertThrows(IllegalArgumentException.class, () ->
                new DataPacket(DataEncodingMechanism.JSON, null, StandardCharsets.UTF_8, null));
        assertThrows(IllegalArgumentException.class, () ->
                new DataPacket(DataEncodingMechanism.JSON, DataPacket.Version.BAD, StandardCharsets.UTF_8, null));
    }

    @Test
    void encapsulationOfNestedPackets() {
        byte[] payload = new byte[]{0x55, 0x55, 0x55, 0x55, 0x55};
        DataEncodingMechanism sm = DataEncodingMechanism.JAVA;
        Charset cs = null;
        DataPacket dpPayload = new DataPacket(sm, cs, payload);
        DataPacket dp = new DataPacket(DataEncodingMechanism.BASE32, DataPacket.Version.NESTED, null, dpPayload.toBytes());

        byte[] expectedResult = new byte[DataPacket.Version.getCodeSize() + DataEncodingMechanism.getCodeSize()
                + dp.getEncodingNameLengthFieldByteSize() + dpPayload.toBytes().length];
        expectedResult[0] = DataEncodingMechanism.BASE32.getCode();
        expectedResult[1] = DataPacket.Version.NESTED.getCode();
        expectedResult[2] = 0;
        System.arraycopy(dpPayload.toBytes(), 0, expectedResult, 3, dpPayload.toBytes().length);
        assertArrayEquals(expectedResult, dp.toBytes());

        DataPacket unpDp = new DataPacket(expectedResult);
        assertEquals(DataPacket.Version.NESTED, unpDp.getDataPacketVersion());
        assertArrayEquals(payload, new DataPacket(unpDp.getPayload()).getPayload());
    }

    @Test
    void encapsulationWithImproperDataLengthTest() {
        byte badSize = 0x50;
        byte[] badPacket = new byte[]{0x00, DataEncodingMechanism.JAVA.getCode(), badSize, 0x55, 0x55, 0x55, 0x55, 0x55};
        assertThrows(IllegalArgumentException.class, () -> new DataPacket(badPacket));
    }

    @Test
    void equalsAndHashCodeTest() {
        DataPacket dp1 = null;
        DataPacket dp2 = null;

        byte[] good = new byte[]{0x00, DataEncodingMechanism.JAVA.getCode(), 0x00, 0x55, 0x55, 0x55, 0x55, 0x55};
        dp1 = new DataPacket(good);
        assertFalse(dp1.equals(dp2));

        dp2 = new DataPacket(good);
        assertTrue(dp1.equals(dp2));
        assertTrue(dp2.equals(dp1));
        assertEquals(dp1.hashCode(), dp2.hashCode());

        byte[] good2 = new byte[]{0x00, DataEncodingMechanism.JAVA.getCode(), 0x00, 0x55, 0x55, 0x56, 0x57, 0x58};
        dp2 = new DataPacket(good2);
        assertFalse(dp1.equals(dp2));
        assertFalse(dp2.equals(dp1));
        assertNotEquals(dp1.hashCode(), dp2.hashCode());

        byte[] good_short = new byte[]{0x00, DataEncodingMechanism.JAVA.getCode(), 0x00, 0x55, 0x55, 0x58};
        dp2 = new DataPacket(good_short);
        assertFalse(dp1.equals(dp2));
        assertNotEquals(dp1.hashCode(), dp2.hashCode());
    }

    @Test
    void configFactoryKafkaInitialCfgTest() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigurationFactoryKafka(new Properties(),
                DataEncodingMechanism.BASE32, DispatchingType.CONSUME, null, null));

        ConfigurationFactoryKafka cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.CONSUME, null, null);
        HashSet<String> topics = new HashSet<>();
        topics.add("events");
        /*topics.add("system-events");
        topics.add("domain-events");
        topics.add("emergency-events");*/
        assertEquals(topics, cfk.getTopics());

        Map<Class<? extends IEvent>, Set<String>> eventToTopicsMap = new HashMap<>();
        eventToTopicsMap.put(Event.class, new HashSet<String>(1){{add("events");}});
        eventToTopicsMap.put(DomainEvent.class, new HashSet<String>(2){{add("events");add("domain-events");}});
        eventToTopicsMap.put(EmergencyEvent.class, new HashSet<String>(3){{add("events");add("domain-events");add("emergency-events");}});
        eventToTopicsMap.put(SystemEvent.class, new HashSet<String>(2){{add("events");add("system-events");}});
        assertEquals(eventToTopicsMap, cfk.getEventToTopicsMap());

        Map<String, Set<Class<? extends IEvent>>> topicToEventsMap = new HashMap<>();
        topicToEventsMap.put("events", new HashSet<Class<? extends IEvent>>(){{add(Event.class);add(DomainEvent.class);add(EmergencyEvent.class);add(SystemEvent.class);}});
        topicToEventsMap.put("domain-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);add(EmergencyEvent.class);}});
        topicToEventsMap.put("emergency-events", new HashSet<Class<? extends IEvent>>(){{add(EmergencyEvent.class);}});
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(SystemEvent.class);}});

        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());

        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.PRODUCE, null, null);
        topics.add("system-events");
        topics.add("domain-events");
        topics.add("emergency-events");
        assertEquals(topics, cfk.getTopics());
        assertEquals(eventToTopicsMap, cfk.getEventToTopicsMap());
        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());

        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.CONSUME_PRODUCE, null, null);
        assertEquals(topics, cfk.getTopics());
        assertEquals(eventToTopicsMap, cfk.getEventToTopicsMap());
        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());

        topics = new HashSet<>();
        topics.add("events");
        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.CONSUME,  topics, null);
        assertEquals(topics, cfk.getTopics());
        assertTrue(cfk.getEventToTopicsMap().isEmpty());
        assertTrue(cfk.getTopicToEventsMap().isEmpty());

        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.PRODUCE,  topics, null);
        topics.clear();
        topics.add("events");
        topics.add("system-events");
        topics.add("domain-events");
        topics.add("emergency-events");
        assertEquals(topics, cfk.getTopics());

        eventToTopicsMap.clear();
        eventToTopicsMap.put(Event.class, new HashSet<String>(1){{add("events");}});
        eventToTopicsMap.put(DomainEvent.class, new HashSet<String>(2){{add("events");add("domain-events");}});
        eventToTopicsMap.put(EmergencyEvent.class, new HashSet<String>(3){{add("events");add("domain-events");add("emergency-events");}});
        eventToTopicsMap.put(SystemEvent.class, new HashSet<String>(2){{add("events");add("system-events");}});
        assertEquals(eventToTopicsMap, cfk.getEventToTopicsMap());

        topicToEventsMap.clear();
        topicToEventsMap.put("events", new HashSet<Class<? extends IEvent>>(){{add(Event.class);add(DomainEvent.class);add(EmergencyEvent.class);add(SystemEvent.class);}});
        topicToEventsMap.put("domain-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);add(EmergencyEvent.class);}});
        topicToEventsMap.put("emergency-events", new HashSet<Class<? extends IEvent>>(){{add(EmergencyEvent.class);}});
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(SystemEvent.class);}});
        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());

        topics.clear();
        topics.add("events");
        final Set<String> _tpcs = topics;
        assertThrows(IllegalArgumentException.class, () -> {
            new ConfigurationFactoryKafka(new Properties(), null,
                    DispatchingType.PRODUCE,  _tpcs, topicToEventsMap);
        });

        topics.clear();
        topics.add("events");
        topics.add("system-events");
        topicToEventsMap.clear();
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);}});
        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.PRODUCE,  topics, topicToEventsMap);
        topics.clear();
        topics.add("events");
        topics.add("system-events");
        topics.add("domain-events");
        topics.add("emergency-events");
        assertEquals(topics, cfk.getTopics());
        topicToEventsMap.clear();
        topicToEventsMap.put("events", new HashSet<Class<? extends IEvent>>(){{add(Event.class);add(DomainEvent.class);add(EmergencyEvent.class);add(SystemEvent.class);}});
        topicToEventsMap.put("domain-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);add(EmergencyEvent.class);}});
        topicToEventsMap.put("emergency-events", new HashSet<Class<? extends IEvent>>(){{add(EmergencyEvent.class);}});
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(SystemEvent.class);add(DomainEvent.class);}});
        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());

        topics.clear();
        topics.add("events");
        topics.add("system-events");
        topicToEventsMap.clear();
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);}});
        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.CONSUME_PRODUCE,  topics, topicToEventsMap);
        topics.clear();
        topics.add("events");
        topics.add("system-events");
        topics.add("domain-events");
        topics.add("emergency-events");
        assertEquals(topics, cfk.getTopics());
        topicToEventsMap.clear();
        topicToEventsMap.put("events", new HashSet<Class<? extends IEvent>>(){{add(Event.class);add(DomainEvent.class);add(EmergencyEvent.class);add(SystemEvent.class);}});
        topicToEventsMap.put("domain-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);add(EmergencyEvent.class);}});
        topicToEventsMap.put("emergency-events", new HashSet<Class<? extends IEvent>>(){{add(EmergencyEvent.class);}});
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(SystemEvent.class);add(DomainEvent.class);}});
        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());

        topics.clear();
        topics.add("events");
        topics.add("system-events");
        topicToEventsMap.clear();
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);}});
        cfk = new ConfigurationFactoryKafka(new Properties(), null,
                DispatchingType.CONSUME,  topics, topicToEventsMap);
        assertEquals(topics, cfk.getTopics());
        topicToEventsMap.clear();
        topicToEventsMap.put("system-events", new HashSet<Class<? extends IEvent>>(){{add(DomainEvent.class);}});
        assertEquals(topicToEventsMap, cfk.getTopicToEventsMap());
    }
}
