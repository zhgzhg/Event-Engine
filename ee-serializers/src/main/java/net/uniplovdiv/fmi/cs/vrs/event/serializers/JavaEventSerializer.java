package net.uniplovdiv.fmi.cs.vrs.event.serializers;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.IParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcomeTemplate;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.BasicJavaSerializer;
import org.apache.commons.lang3.mutable.MutableLong;

import java.io.*;

/**
 * Provides serialization and deserialization logic from/to standard java serialization representation and objects
 * implementing IEvent.
 */
public class JavaEventSerializer implements IEventSerializer {

    protected BasicJavaSerializer basicJavaSerializer;

    /**
     * Constructor.
     */
    public JavaEventSerializer() {
        this.basicJavaSerializer = new BasicJavaSerializer();
    }

    @Override
    public byte[] serialize(IEvent event) throws IOException, SecurityException, NullPointerException {
        return this.basicJavaSerializer.serialize(event);
    }

    @Override
    public byte[] serializePCO(IParameterComparisonOutcome comparisonOutcome) throws IOException, SecurityException, NullPointerException {
        return this.basicJavaSerializer.serialize(comparisonOutcome);
    }

    @Override
    public byte[] serializePCOT(ParameterComparisonOutcomeTemplate comparisonOutcomeTemplate) throws IOException, SecurityException, NullPointerException {
        return this.basicJavaSerializer.serialize(comparisonOutcomeTemplate);
    }

    @Override
    public IEvent deserialize(byte[] serializedEvent) throws IOException, SecurityException, NullPointerException,
            ClassNotFoundException {
        Object o = this.basicJavaSerializer.deserialize(serializedEvent);
        if (!(o instanceof IEvent)) {
            throw new StreamCorruptedException(
                    "The provided serialized data does not represent an object implementing IEvent!");
        }
        return (IEvent)o;
    }

    @Override
    public IParameterComparisonOutcome deserializePCO(byte[] serializedComparisonOutcome) throws IOException,
            SecurityException, NullPointerException, ClassNotFoundException {
        Object o = this.basicJavaSerializer.deserialize(serializedComparisonOutcome);
        if (!(o instanceof IParameterComparisonOutcome)) {
            throw new StreamCorruptedException("The provided serialized data does not represent an object "
                    + "implementing IParameterComparisonOutcome!");
        }
        return (IParameterComparisonOutcome)o;
    }

    @Override
    public ParameterComparisonOutcomeTemplate deserializePCOT(byte[] serializedComparisonOutcomeTemplate) throws
            IOException, SecurityException, NullPointerException, ClassNotFoundException {
        Object o = this.basicJavaSerializer.deserialize(serializedComparisonOutcomeTemplate);
        if (!(o instanceof ParameterComparisonOutcomeTemplate)) {
            throw new StreamCorruptedException("The provided serialized data does not represent an object "
                    + "implementing ParameterComparisonOutcomeTemplate!");
        }
        return (ParameterComparisonOutcomeTemplate)o;
    }

    /**
     * Reads the serialVersionUID from an already serialized event.
     * @param serializedEvent The serialized event to be used.
     * @return The long value of the serialVersionUID for the class.
     * @throws IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    public long getSerialVersionUidFromSerializedEvent(byte[] serializedEvent) throws IOException, SecurityException,
            NullPointerException, ClassNotFoundException {
        InputStream in = new ByteArrayInputStream(serializedEvent);
        final MutableLong suid = new MutableLong();
        try (ObjectInputStream ois = new ObjectInputStream(in) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                //ClassLoader cl = Thread.currentThread().getContextClassLoader();
                ClassLoader cl = basicJavaSerializer.getContextClassLoader();
                if (cl == null)  return super.resolveClass(desc);
                return Class.forName(desc.getName(), false, cl);
            }

            @Override
            protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
                ObjectStreamClass objectStreamClass = super.readClassDescriptor();
                //Class c = basicJavaSerializer.getContextClassLoader().loadClass(objectStreamClass.getName());
                if (suid.getValue() == 0 && IEvent.class.isAssignableFrom(resolveClass(objectStreamClass))) {
                    // this method will read the current object, but is also called for any other internal objects
                    // so we try to assign a value only once upon success
                    //System.out.println(objectStreamClass.getName());
                    //System.out.println(objectStreamClass.getSerialVersionUID());
                    suid.setValue(objectStreamClass.getSerialVersionUID());
                }
                return objectStreamClass;
            }
        }) {
            ois.readObject();
        }
        return suid.getValue();
    }

    /**
     * Reads the serialVersionUID from an event instance. In the process a serialization will be made.
     * @param event The event instance to be used.
     * @return The long value of the serialVersionUID for the class.
     * @throws IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    public long computeSerialVersionUidFromEvent(IEvent event) throws IOException, SecurityException,
            NullPointerException, ClassNotFoundException {
        return ObjectStreamClass.lookup(event.getClass()).getSerialVersionUID();
    }

    /**
     * Reads the serialVersionUID from an event instance. In the process a serialization will be made.
     * @param helper The event helper instance to be used.
     * @return The long value of the serialVersionUID for the class.
     * @throws IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    public long computeSerialVersionUidFromEventHelper(IParameterComparisonOutcome helper) throws IOException,
            SecurityException, NullPointerException, ClassNotFoundException {
        return ObjectStreamClass.lookup(helper.getClass()).getSerialVersionUID();
    }

    /**
     * Reads the serialVersionUID from an event instance. In the process a serialization will be made.
     * @param helper The event helper instance to be used.
     * @return The long value of the serialVersionUID for the class.
     * @throws IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    public long computeSerialVersionUidFromEventHelper(ParameterComparisonOutcomeTemplate helper) throws IOException,
            SecurityException, NullPointerException, ClassNotFoundException {
        return ObjectStreamClass.lookup(helper.getClass()).getSerialVersionUID();
    }
}