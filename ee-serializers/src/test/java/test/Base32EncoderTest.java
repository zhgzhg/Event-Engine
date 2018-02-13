package test;

import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.Base32Encoder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class Base32EncoderTest {
    @Test
    void encodeDecodeWithBase32() {
        String original = "Hello there!";
        String encoded = "JBSWY3DPEB2GQZLSMUQQ====";

        // ISO_8859_1 is required because any unknown characters will be dropped by the encoder
        Base32Encoder b32enc = new Base32Encoder();
        assertArrayEquals(b32enc.encode(original.getBytes(StandardCharsets.ISO_8859_1)),
                encoded.getBytes(StandardCharsets.ISO_8859_1));
        assertArrayEquals(b32enc.decode(encoded.getBytes(StandardCharsets.ISO_8859_1)),
                original.getBytes(StandardCharsets.ISO_8859_1));

        assertArrayEquals(b32enc.encodeString(original), encoded.getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(b32enc.decodeString(encoded.getBytes(StandardCharsets.ISO_8859_1)), original);
    }

    @Test
    void encodeDecodeNullWithBase32() {
        Base32Encoder b32enc = new Base32Encoder();
        assertNull(b32enc.encode(null));
        assertNotNull(b32enc.encode(new byte[0]));

        assertNull(b32enc.decode(null));
        assertNotNull(b32enc.decode(new byte[0]));

        assertNull(b32enc.encodeString(null));
        assertNotNull(b32enc.encodeString(""));

        assertNull(b32enc.decodeString(null));
        assertNotNull(b32enc.decodeString(new byte[0]));
    }
}


