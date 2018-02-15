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
     * Contains the supported by DataPacket versions (effectively formats) of data packets.
     */
    public enum Version {
        /**
         * The first version of the data packet defining the standard format of:
         * b_packet_version, b_serialization_mechanism_code, b_encoding_charset_name_length, ba_encoding_name,
         * ba_payload.
         * Where b_ prefix means byte and ba_ prefix means byte array.
         */
        CLASSIC((byte)0),

        /**
         * A packet nesting another packet within itself. The nested packet is usually encoded into another format.
         * Check {@link #CLASSIC} for more information on the classical packet structure.
         */
        NESTED((byte)65),

        /**
         * Used to designate unknown, unsupported or invalid version.
         */
        BAD((byte)255);

        private byte version;

        Version(byte version) { this.version = version; }

        /**
         * Returns the byte value representing the version.
         * @return The byte value equivalent of the selected version member.
         */
        public byte getCode() {
            return version;
        }

        /**
         * Returns the code representing a particular version.
         * @param version The Version value whose code to be retrieved.
         * @return A byte code value.
         */
        public static byte getCode(Version version) {
            return version.version;
        }

        /**
         * Converts to Version member a specified byte value.
         * @param version The byte value to be converted to Version.
         * @return The corresponding version member or Version.BAD if the version is bad, unknown or not supported.
         */
        public static Version fromCode(byte version) {
            switch (version) {
                case 0: return CLASSIC;
                case 65: return NESTED;
                default:
                    return BAD;
            }
        }

        /**
         * Returns the size of the code representing any enumeration in bytes.
         * @return The integer value with size of the data type in bytes.
         */
        @SuppressWarnings("SameReturnValue")
        public static int getCodeSize() {
            return Byte.BYTES; // * 1
        }
    }

    /**
     * The maximum allowed encoding charset name length in bytes.
     */
    public static final int MAX_ENCODING_CHARSET_LENGTH = (int)(Math.pow(2, Byte.SIZE * Byte.BYTES) - 1);

    /**
     * The theoretical minimum amount of bytes that can describe a valid data packet.
     */
    public static final int MIN_VALID_PACKET_LENGTH = 4;

    private boolean initSuccess;

    private Version dataPacketVersion;

    private DataEncodingMechanism dataEncodingMechanismType;

    private Charset encoding;
    private byte[] encodingName_ISO_8859_1;

    private byte[] payload;

    /**
     * Internal constructor used for initialization of the structure.
     */
    protected DataPacket() {
        this.initSuccess = false;
        this.dataPacketVersion = Version.CLASSIC;
        this.encoding = null;
        this.encodingName_ISO_8859_1 = null;
        this.payload = null;
    }

    /**
     * Constructor used for packaging of data that will be sent as a binary message through another environment.
     * To get the produced package consisting of byte data see {@link #toBytes() toBytes()} method.
     * @param dataEncodingMechanismType The serialization mechanism used to create payload.
     * @param encoding The encoding used during the creation of payload. Can be null.
     * @param payload The payload with the actual data.
     * @throws IllegalArgumentException if the provided parameters cannot be packaged.
     */
    public DataPacket(DataEncodingMechanism dataEncodingMechanismType, Charset encoding, byte[] payload) {
        this();
        if (dataEncodingMechanismType == null) {
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
        this.dataEncodingMechanismType = dataEncodingMechanismType;
        this.encoding = encoding;
        this.payload = payload.clone();
        this.initSuccess = true;
    }

    /**
     * Constructor used for packaging of data that will be sent as a binary message through another environment.
     * To get the produced package consisting of byte data see {@link #toBytes() toBytes()} method.
     * @param dataEncodingMechanismType The serialization mechanism used to create payload.
     * @param packetVersion The version (format) of the packet.
     * @param encoding The encoding used during the creation of payload. Can be null.
     * @param payload The payload with the actual data.
     * @throws IllegalArgumentException If the provided parameters cannot be packaged.
     */
    public DataPacket(DataEncodingMechanism dataEncodingMechanismType, Version packetVersion, Charset encoding,
                      byte[] payload) {
        this(dataEncodingMechanismType, encoding, payload);
        if (packetVersion == null || packetVersion == Version.BAD)
            throw new IllegalArgumentException("Invalid packet version specified!");
        this.dataPacketVersion = packetVersion;
    }

    /**
     * Constructor used to unpack data into dataPacketVersion, serializationMechanism, encoding and payload fields.
     * See {@link #getDataEncodingMechanismType() getDataEncodingMechanismType()},
     * {@link #getEncoding() getEncoding()} and {@link #getPayload() getPayload()} methods.
     *
     * @param data The data to be unpacked.
     * @throws IllegalArgumentException - If the provided parameters cannot be unpacked.
     * @throws java.nio.charset.IllegalCharsetNameException If the charset inside the data is illegal.
     * @throws java.nio.charset.UnsupportedCharsetException If no support for the charset is available in this instance
     *                                                      of the Java virtual machine.
     */
    public DataPacket(byte[] data) {
        this();
        if (data == null || data.length < MIN_VALID_PACKET_LENGTH)
            throw new IllegalArgumentException("Malformed data packet");
        if ((this.dataPacketVersion = Version.fromCode(data[0])) == Version.BAD)
            throw new IllegalArgumentException(String.format("Not supported packet dataPacketVersion 0x%02X", data[0]));

        this.dataEncodingMechanismType = DataEncodingMechanism.fromCode(data[1]);
        int encLength = Byte.toUnsignedInt(data[2]);

        final int FIRST_3_PACKET_FIELDS_SZ = Version.getCodeSize() + DataEncodingMechanism.getCodeSize()
                + getEncodingNameLengthFieldByteSize();

        {
            int computedPayloadLength = data.length - (FIRST_3_PACKET_FIELDS_SZ + encLength);
            if (computedPayloadLength < 1)
                throw new IllegalArgumentException("Malformed data packet without payload");
        }

        int i = 0, j = FIRST_3_PACKET_FIELDS_SZ;
        if (encLength > 0) {
            this.encodingName_ISO_8859_1 = new byte[encLength];
            for ( ; i < encLength; ++i, ++j) {
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

        byte[] packet = new byte[Version.getCodeSize() + DataEncodingMechanism.getCodeSize()
                + getEncodingNameLengthFieldByteSize() + encLength + this.payload.length];

        packet[0] = this.dataPacketVersion.getCode();
        packet[1] = dataEncodingMechanismType.getCode(); // potential point for fixing in future if the length of this field is increased
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
     * Returns the byte size of the code representing the string representing the used encoding mechanism.
     * @return A non-negative integer representing size in bytes.
     */
    public int getEncodingNameLengthFieldByteSize() {
        return 1;
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
     * @return Version enumerator describing the version.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     */
    public Version getDataPacketVersion() {
        checkInit();
        return dataPacketVersion;
    }

    /**
     * Returns the used serialization mechanism for the payload of this class instance.
     * @throws UnsupportedOperationException If the operation cannot be performed.
     * @return A serialization mechanism value.
     */
    public DataEncodingMechanism getDataEncodingMechanismType() {
        checkInit();
        return dataEncodingMechanismType;
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

        result = 31 * result + (int) dataPacketVersion.getCode();
        result = 31 * result + (int) dataEncodingMechanismType.getCode();
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
                    && safeEquals(this.getDataEncodingMechanismType(), _obj.getDataEncodingMechanismType())
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
        for (int j = 0; j < bytes.length; ++j) {
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
                "{ initSuccess=%s, dataPacketVersion=0x%02x/%s, dataEncodingMechanismType=0x%02x/%s, encoding=%s, "
                + "payload=%s }", initSuccess, dataPacketVersion.getCode(), dataPacketVersion.name(),
                dataEncodingMechanismType.getCode(), dataEncodingMechanismType.name(), encoding,
                bytesToHexJavaCsv(payload)
        );
    }
}
