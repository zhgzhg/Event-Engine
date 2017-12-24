package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Encapsulates data into a binary form that's suitable for transfer via various media.
 * This class should not be inherited.
 */
public class DataPacket {
    /**
     * The maximum allowed encoding charset name length in bytes.
     */
    public static final int MAX_ENCODING_CHARSET_LENGTH = (int)(Math.pow(2, Byte.SIZE * Byte.BYTES) - 1);

    /**
     * The theoretical minimum amount of bytes that can describe a valid data packet.
     */
    public static final int MIN_VALID_PACKET_LENGTH = 4;

    private boolean initSuccess;

    private byte dataPacketVersion;

    private SerializationMechanism serializationMechanismType;

    private Charset encoding;
    private byte[] encodingName_ISO_8859_1;

    private byte[] payload;

    /**
     * Internal constructor used for initialization of the structure.
     */
    protected DataPacket() {
        this.initSuccess = false;
        this.dataPacketVersion = 0x00;
        this.encoding = null;
        this.encodingName_ISO_8859_1 = null;
        this.payload = null;
    }

    /**
     * Constructor used for packaging of data that will be sent as a binary message through another environment.
     * To get the produced package consisting of byte data see {@link #toBytes() toBytes()} method.
     * @param serializationMechanismType The serialization mechanism used to create payload.
     * @param encoding The encoding used during the creation of payload. Can be null.
     * @param payload The payload with the actual data.
     * @throws IllegalArgumentException if the provided parameters cannot be packaged.
     */
    public DataPacket(SerializationMechanism serializationMechanismType, Charset encoding, byte[] payload) {
        this();
        if (serializationMechanismType == null) {
            throw new NullPointerException("Serialization mechanism must be specified");
        }
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("The payload must be at least 1 byte long");
        }
        if (encoding != null) {
            if (encoding.name().length() > MAX_ENCODING_CHARSET_LENGTH) {
                Optional<String> encName = encoding.aliases().stream()
                        .filter(name -> name.length() <= MAX_ENCODING_CHARSET_LENGTH).findFirst();
                if (!encName.isPresent()) {
                    throw new IllegalArgumentException("The encoding's human readable name is longer than "
                            + MAX_ENCODING_CHARSET_LENGTH + " symbols");
                } else {
                    this.encodingName_ISO_8859_1 = encName.get().getBytes(StandardCharsets.ISO_8859_1);
                }
            } else {
                this.encodingName_ISO_8859_1 = encoding.name().getBytes(StandardCharsets.ISO_8859_1);
            }
        }
        this.serializationMechanismType = serializationMechanismType;
        this.encoding = encoding;
        this.payload = payload.clone();
        this.initSuccess = true;
    }

    /**
     * Constructor used to unpack data into the serializationMechanism, encoding and payload fields.
     * See {@link #getSerializationMechanismType() getSerializationMechanismType()},
     * {@link #getEncoding() getEncoding()} and {@link #getPayload() getPayload()} methods.
     *
     * @param data The data to be unpacked.
     * @throws IllegalArgumentException - If the provided parameters cannot be unpacked.
     * @throws java.nio.charset.IllegalCharsetNameException - If the charset inside the data is illegal.
     * @throws java.nio.charset.UnsupportedCharsetException - If no support for the charset is available in this
     *                                                        instance of the Java virtual machine.
     */
    public DataPacket(byte[] data) {
        this();
        if (data == null || data.length < MIN_VALID_PACKET_LENGTH)
            throw new IllegalArgumentException("Malformed data packet");
        if (data[0] != this.dataPacketVersion)
            throw new IllegalArgumentException("Not supported packet dataPacketVersion " + data[0]);

        // skip initialization of this.dataPacketVersion since there is only 1 dataPacketVersion

        this.serializationMechanismType = SerializationMechanism.fromCode(data[1]);
        int encLength = Byte.toUnsignedInt(data[2]);

        {
            int computedPayloadLength = data.length - (3 + encLength);
            if (computedPayloadLength < 1)
                throw new IllegalArgumentException("Malformed data packet missing no payload");
        }

        int i = 0, j = 3;
        if (encLength > 0) {
            this.encodingName_ISO_8859_1 = new byte[encLength];
            for (; i < encLength; ++i, ++j) {
                this.encodingName_ISO_8859_1[i] = data[j];
            }

            this.encoding = Charset.forName(new String(this.encodingName_ISO_8859_1, StandardCharsets.ISO_8859_1));
        }

        this.payload = new byte [data.length - j];
        for (i = 0; j < data.length; ++i, ++j) {
            this.payload[i] = data[j];
        }

        this.initSuccess = true;
    }

    /**
     * Checks if the class has been instantiated successfully.
     * @throws UnsupportedOperationException If the instantiation was not successful.
     */
    protected void checkInit() {
        if (!this.initSuccess) {
            throw new UnsupportedOperationException("Tried to work with unsuccessfully initialized DataPacket");
        }
    }

    /**
     * Returns the packaged equivalent of the constructed object.
     * @return Array of bytes.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     */
    public byte[] toBytes() {
        checkInit();

        int encLength = (this.encoding != null ? this.encodingName_ISO_8859_1.length : 0);
        byte[] packet = new byte[1 + SerializationMechanism.getCodeSize() + 1 + encLength + this.payload.length];

        packet[0] = this.dataPacketVersion;
        packet[1] = serializationMechanismType.getCode(); // potential point for fixing in future if the length of this field is increased
        packet[2] = (byte)encLength;

        int i = 0, j = 3;
        for ( ; i < encLength; ++i, ++j) {
            packet[j] = this.encodingName_ISO_8859_1[i];
        }

        for (i = 0; i < this.payload.length; ++i, ++j) {
            packet[j] = payload[i];
        }

        return packet;
    }

    /**
     * Checks whether the initialization of the class was successful.
     * @return True if the initialization was successful otherwise false.
     */
    public boolean isInitSuccess() {
        return initSuccess;
    }

    /**
     * Returns the version format of the constructed instance.
     * @return Byte describing the version.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     */
    public byte getDataPacketVersion() {
        checkInit();
        return dataPacketVersion;
    }

    /**
     * Returns the used serialization mechanism for the payload of this class instance.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     * @return A serialization mechanism value.
     */
    public SerializationMechanism getSerializationMechanismType() {
        checkInit();
        return serializationMechanismType;
    }

    /**
     * Returns the Charset used for encoding of the contained payload.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     * @return A Charset object representing the used encoding or null if no encoding has been used.
     */
    public Charset getEncoding() {
        checkInit();
        return encoding;
    }

    /**
     * Returns the string alias of the used encoding mechanism.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     * @return A byte array guaranteed to be not bigger than
     *         {@link #MAX_ENCODING_CHARSET_LENGTH MAX_ENCODING_CHARSET_LENGTH} bytes.
     */
    public byte[] getEncodingName_ISO_8859_1() {
        checkInit();
        if (encodingName_ISO_8859_1 != null) {
            return encodingName_ISO_8859_1.clone();
        }
        return null;
    }

    /**
     * Returns the contained byte array payload.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     * @return The contained byte array payload.
     */
    public byte[] getPayload() {
        checkInit();
        return payload.clone();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (initSuccess ? 1 : 0);
        if (!initSuccess) return result;

        result = 31 * result + (int)dataPacketVersion;
        result = 31 * result + (int)serializationMechanismType.getCode();
        result = 31 * result + (encoding != null ? encoding.hashCode() : 0);
        if (payload != null && payload.length > 0) {
            for (int i = 0; i < payload.length; ++i) {
                result = 31 * result + (int)payload[i];
            }
        } else {
            result = 31 * result /*+ 0*/;
        }
        return result;
    }

    /**
     * Safe equals operation that will work with nulls.
     * @param a Source
     * @param b Reference
     * @param <T> The generic data type for which the equality check will be performed.
     * @return The result equivalent of a.equals(b)
     */
    private static <T> boolean safeEquals(T a, T b) {
        return (a == b) || (a != null && b != null && a.equals(b));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof DataPacket && (this.getClass() == obj.getClass())) {
            DataPacket _obj = (DataPacket) obj;
            if (this.isInitSuccess() == _obj.isInitSuccess()
                    && this.getDataPacketVersion() == _obj.getDataPacketVersion()
                    && safeEquals(this.getSerializationMechanismType(), _obj.getSerializationMechanismType())
                    && safeEquals(this.getEncoding(), _obj.getEncoding())) {

                byte[] p1 = this.getPayload();
                byte[] p2 = _obj.getPayload();

                if (p1 != p2) {
                    if (p1 == null || p2 == null || p1.length != p2.length) return false;
                    for (int i = 0; i < p1.length; ++i) {
                        if (p1[i] != p2[i]) return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Converts array of bytes to Java convention csv array using [ 0x, 0x...] notation.
     * @param bytes The array of bytes to be converted. Can be null.
     * @return An initialized string with [ 0x, 0x...] notation or empty [] one or a null.
     */
    public static String bytesToHexJavaCsv(byte[] bytes) {
        if (bytes == null) return "null";
        if (bytes.length == 0) return "[]";

        char[] hexChars = new char[(bytes.length * 6) - 2];
        int j = 0;
        for ( ; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 6]     = '0';
            hexChars[j * 6 + 1] = 'x';
            hexChars[j * 6 + 2] = hexArray[v >>> 4];
            hexChars[j * 6 + 3] = hexArray[v & 0x0F];
            if (j < bytes.length - 1) {
                hexChars[j * 6 + 4] = ',';
                hexChars[j * 6 + 5] = ' ';
            }
        }
        return "[" + new String(hexChars) + "]";
    }

    @Override
    public String toString() {
        return String.format(
                "{ initSuccess=%s, dataPacketVersion=%x, serializationMechanismType=%s, encoding=%s, payload=%s }",
                initSuccess, dataPacketVersion, serializationMechanismType, encoding, bytesToHexJavaCsv(payload)
        );
    }
}
