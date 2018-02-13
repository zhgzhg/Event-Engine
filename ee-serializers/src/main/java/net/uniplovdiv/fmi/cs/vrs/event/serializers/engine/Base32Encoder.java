package net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;

import org.apache.commons.codec.binary.Base32;

import java.nio.charset.StandardCharsets;

/**
 * Provides base32 data en/decoding functionality.
 */
public class Base32Encoder {
    private Base32 encoder;

    /**
     * Constructor.
     */
    public Base32Encoder() {
        this.encoder = new Base32();
    }

    /**
     * Encodes data to base32 format.
     * @param data A nonnull array of data.
     * @return Encoded byte array of data or null or an empty array.
     */
    public byte[] encode(byte[] data) {
        if (data == null) return null;
        return this.encoder.encode(data);
    }

    /**
     * Encodes string data to base32 format.
     * @param sdata A nonnull string of data.
     * @return Encoded byte array of data or null or an empty array.
     */
    public byte[] encodeString(String sdata) {
        if (sdata == null) return null;
        return this.encoder.encode(sdata.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Decodes data from base32 format.
     * @param data A nonnull array of data.
     * @return Decoded byte array of data or null or an empty array.
     */
    public byte[] decode(byte[] data) {
        if (data == null) return null;
        return this.encoder.decode(data);
    }

    /**
     * Decodes data from base32 format to string.
     * @param data A nonnull array of data.
     * @return Decoded byte array of data or null or an empty array.
     */
    public String decodeString(byte[] data) {
        if (data == null) return null;
        return new String(this.encoder.decode(data), StandardCharsets.ISO_8859_1);
    }
}
