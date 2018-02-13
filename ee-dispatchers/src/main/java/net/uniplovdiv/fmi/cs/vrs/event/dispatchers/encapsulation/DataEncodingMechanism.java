package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation;

/**
 * Data encoding or decoding mechanism variants enumeration. Can be used to designate serialization or other kind of
 * data transformation techniques.
 */
public enum SerializationMechanism {
    /**
     * Unknown serialization mechanism indicator value.
     */
    UNKNOWN((byte)0),

    /**
     * Standard Java serialization mechanism indicator value.
     */
    JAVA((byte)1),

    /**
     * JSON serialization mechanism indicator value.
     */
    JSON((byte)2),

    /**
     * BASE32 serialization mechanism indicator value.
     */
    BASE32((byte)65);

    private final byte code;

    SerializationMechanism(byte code) {
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
     * @param serializationMechanismType The SerializationMechanismType value whose code to be retrieved.
     * @return A byte code value.
     */
    public static byte getCode(SerializationMechanism serializationMechanismType) {
        return serializationMechanismType.code;
    }

    /**
     * Converts a byte code representing particular serialization mechanism to its corresponding enumeration value.
     * @param code The code to be converted.
     * @return The corresponding enumeration value.
     * @throws IllegalArgumentException if code is not supported.
     */
    public static SerializationMechanism fromCode(byte code) {
        switch (code) {
            case 0: return UNKNOWN;
            case 1: return JAVA;
            case 2: return JSON;
            default: throw new IllegalArgumentException("Not supported serialization mechanism code " + code);
        }
    }

    /**
     * Returns the size of the code representing any enumeration.
     * @return The integer value with size of the data type.
     */
    @SuppressWarnings("SameReturnValue")
    public static int getCodeSize() {
        return Byte.BYTES;
    }
}
