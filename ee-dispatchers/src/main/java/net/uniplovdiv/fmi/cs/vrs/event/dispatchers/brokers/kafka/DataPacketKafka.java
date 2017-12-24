package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.kafka;

import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataPacket;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.Serializable;
import java.util.Map;

/**
 * Mixin helper class consisting of:
 * - A data packet that can be automatically de/serialized by Java and manually converted from/to {@link DataPacket}
 * - Kafka serializer of {@link DataPacket}
 * - Kafka deserializer of {@link DataPacket}
 * - Kafka serializer/deserializer (serde) of {@link DataPacket}
 */
public class DataPacketKafka implements Serializable, Serializer<DataPacket>, Deserializer<DataPacket>,
        Serde<DataPacket> {
    private static final long serialVersionUID = -5347110927997834770L;
    private byte[] data;

    /**
     * Constructor.
     */
    public DataPacketKafka() {}

    /**
     * Returns the stored inside data array.
     * @return A byte array. Can be null.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Sets the stored inside data array.
     * @param data The that to be stored inside. Can be null.
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Converts an initialized {@link DataPacketKafka} to {@link DataPacket}. Can throw Exception if the data cannot be
     * converted to {@link DataPacket}.
     * @return An initialized {@link DataPacket} or null if no data has been stored inside.
     */
    public DataPacket toDataPacket() {
        if (this.data == null || this.data.length < DataPacket.MIN_VALID_PACKET_LENGTH) {
            return null;
        }
        return new DataPacket(this.data);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (this.data != null && this.data.length > 0) {
            for (int i = 0; i < this.data.length; ++i) {
                result = 31 * result + (int)this.data[i];
            }
        } else {
            result = 31 * result /*+ 0*/;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof DataPacketKafka && (this.getClass() == obj.getClass())) {
            DataPacketKafka _obj = (DataPacketKafka)obj;
            if (this.data == _obj.data) {
                return true;
            }
            if (this.data == null || _obj.data == null || this.data.length != _obj.data.length) return false;
            for (int i = 0; i < this.data.length; ++i) {
                if (this.data[i] != _obj.data[i]) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return DataPacket.bytesToHexJavaCsv(this.data);
    }

    @Override
    public DataPacket deserialize(String s, byte[] bytes) {
        return new DataPacket(bytes);
    }

    @Override
    public Serializer<DataPacket> serializer() {
        return this;
    }

    @Override
    public Deserializer<DataPacket> deserializer() {
        return this;
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) { }

    @Override
    public byte[] serialize(String s, DataPacket dataPacket) {
        if (dataPacket == null) return null;
        return dataPacket.toBytes();
    }

    @Override
    public void close() { }
}
