package net.uniplovdiv.fmi.cs.vrs.event.serializers;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.IParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcomeTemplate;

import java.io.IOException;

/**
 * Interface for events serialization and deserialization.
 */
public interface IEventSerializer {
    /**
     * Serializes object implementing IEvent to a byte array.
     * @param event The instantiated object that will be serialized.
     * @return Array of bytes containing the serialized version of the object.
     * @throws java.io.IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     */
    byte[] serialize(IEvent event) throws IOException, SecurityException, NullPointerException;

    /**
     * Serializes object implementing IParameterComparisonOutcome to a byte array.
     * @param comparisonOutcome The instantiated object that will be serialized.
     * @return Array of bytes containing the serialized version of the object.
     * @throws java.io.IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     */
    byte[] serializePCO(IParameterComparisonOutcome comparisonOutcome) throws IOException, SecurityException,
            NullPointerException;

    /**
     * Serializes object implementing ParameterComparisonOutcomeTemplate to a byte array.
     * @param comparisonOutcomeTemplate The instantiated object that will be serialized.
     * @return Array of bytes containing the serialized version of the object.
     * @throws java.io.IOException - if an I/O error occurs while writing stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if event is null
     */
    byte[] serializePCOT(ParameterComparisonOutcomeTemplate comparisonOutcomeTemplate) throws IOException,
            SecurityException, NullPointerException;

    /**
     * Deserializes byte data representing object implementing IEvent to an actual object instance.
     * @param serializedEvent The serialized IEvent which will be converted to an actual object.
     * @return An initialized object implementing IEvent.
     * @throws java.io.StreamCorruptedException - if the provided data cannot form a stream header that is correct ot
     *                                            if the resulting object does not implement the IEvent interface
     * @throws java.io.IOException - if an I/O error occurs while reading stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if serializedEvent is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    IEvent deserialize(byte[] serializedEvent) throws IOException, SecurityException, NullPointerException,
            ClassNotFoundException;

    /**
     * Deserializes byte data representing object implementing IParameterComparisonOutcome to an actual object instance.
     * @param serializedComparisonOutcome The serialized IParameterComparisonOutcome which will be converted to an
     *                                    actual object.
     * @return An initialized object implementing IParameterComparisonOutcome.
     * @throws java.io.StreamCorruptedException - if the provided data cannot form a stream header that is correct ot
     *                                            if the resulting object does not implement the
     *                                            IParameterComparisonOutcome interface
     * @throws java.io.IOException - if an I/O error occurs while reading stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if serializedEvent is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    IParameterComparisonOutcome deserializePCO(byte[] serializedComparisonOutcome) throws IOException,
            SecurityException, NullPointerException, ClassNotFoundException;

    /**
     * Deserializes byte data representing object implementing IParameterComparisonOutcome to an actual object instance.
     * @param serializedComparisonOutcomeTemplate The serialized ParameterComparisonOutcomeTemplate which will be
     *                                            converted to an actual object.
     * @return An initialized object implementing ParameterComparisonOutcomeTemplate.
     * @throws java.io.StreamCorruptedException - if the provided data cannot form a stream header that is not correct
     * @throws java.io.IOException - if an I/O error occurs while reading stream header
     * @throws SecurityException - if untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException - if serializedEvent is null
     * @throws ClassNotFoundException - if the resulting of the deserialization object cannot be instantiated, because
     *                                  its class is not found in the system
     */
    ParameterComparisonOutcomeTemplate deserializePCOT(byte[] serializedComparisonOutcomeTemplate) throws IOException,
            SecurityException, NullPointerException, ClassNotFoundException;
}
