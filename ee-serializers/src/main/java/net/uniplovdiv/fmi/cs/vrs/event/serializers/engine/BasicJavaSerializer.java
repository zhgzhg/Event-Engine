package net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;

import java.io.*;

/**
 * Provides basic serialization and deserialization functionality for standard java serialization format.
 */
public class BasicJavaSerializer {
    protected ClassLoader contextClassLoader;

    /**
     * Constructor.
     */
    public BasicJavaSerializer() {
         this.contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * Serializes java object to a byte array.
     * @param o The instantiated object that will be serialized.
     * @return Array of bytes containing the serialized version of the object.
     * @throws java.io.IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if o is null
     */
    public byte[] serialize(Object o) throws IOException, SecurityException, NullPointerException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return baos.toByteArray();
    }

    /**
     * Deserializes byte data representing a java object to an actual instance of that object.
     * @param serializedObject The serialized object which will be converted to an actual object.
     * @return An initialized object implementing IEvent.
     * @throws java.io.StreamCorruptedException - if the provided data cannot form a stream header that is correct
     * @throws java.io.IOException - if an I/O error occurs while reading stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if serializedEvent is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system.
     */
    public Object deserialize(byte[] serializedObject) throws IOException, SecurityException, NullPointerException,
            ClassNotFoundException {
        ByteArrayInputStream bios = new ByteArrayInputStream(serializedObject);
        ObjectInputStream ois = new ObjectInputStream(bios);
        return ois.readObject();
    }

    /**
     * Returns the context class loader of the thread that have initialized this class.
     * @return An initialized ClassLoader object or null.
     */
    public ClassLoader getContextClassLoader() {
        return this.contextClassLoader;
    }
}
