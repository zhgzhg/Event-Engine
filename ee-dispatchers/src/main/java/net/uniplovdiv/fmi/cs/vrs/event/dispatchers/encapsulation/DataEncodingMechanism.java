package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation;

/**
 * Data encoding or decoding mechanism variants enumeration. Can be used to designate serialization or other kind of
 * data transformation techniques.
 */
public enum DataEncodingMechanism {
    /**
     * Unknown encoding mechanism indicator value.
     */
    UNKNOWN((byte)0),

    /**
     * Standard Java serialization mechanism indicator value.
     */
    JAVA((byte)1),

    /**
     * JSON encoding mechanism indicator value.
     */
    JSON((byte)2),

    /**
     * BASE32 encoding mechanism indicator value. Encoding events with it is not supported.
     */
    BASE32((byte)65);

    private final byte code;

    DataEncodingMechanism(byte code) {
        this.code = code;
    }

    /**
     * Returns the code representing a particular mechanism.
     * @return A byte code value.
     */
    public byte getCode() {
        return this.code;
    }

    /**
     * Returns the code representing a particular mechanism.
     * @param dataEncodingMechanismType The DataEncodingMechanism value whose code to be retrieved.
     * @return A byte code value.
     */
    public static byte getCode(DataEncodingMechanism dataEncodingMechanismType) {
        return dataEncodingMechanismType.code;
    }

    /**
     * Converts a byte code representing particular encoding mechanism to its corresponding enumeration value.
     * @param code The code to be converted.
     * @return The corresponding enumeration value.
     * @throws IllegalArgumentException if code is not supported.
     */
    public static DataEncodingMechanism fromCode(byte code) {
        switch (code) {
            case 0: return UNKNOWN;
            case 1: return JAVA;
            case 2: return JSON;
            case 65: return BASE32;
            default: throw new IllegalArgumentException("Not supported serialization mechanism code " + code);
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
