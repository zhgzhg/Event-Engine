package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers;

import net.uniplovdiv.fmi.cs.vrs.event.Event;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.CircularFifoSet;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.IEventDispatcher;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataPacket;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.SerializationMechanism;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ComparableArrayList;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.JavaEventSerializer;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.JsonEventSerializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractEventDispatcher implements IEventDispatcher {

    // Specialized structures for storing the hashes of the DataPackets of the latest sent/received events in order not
    // to receive them again in case of CONSUME_PRODUCE operation mode or 1 event sent to many distribution channels
    // and received by CONSUME or CONSUME_PRODUCE instance
    protected CircularFifoSet<Integer> latestEventsSent;
    protected CircularFifoSet<Integer> latestEventsReceived;
    protected final int latestEventsRememberCapacity;
    protected final boolean doNotReceiveEventsFromSameSource;

    protected final static String CLIENT_ID_HEADER_KEY = "event_src";

    /**
     * Constructor.
     * @param config The configuration settings that also include Kafka configuration options. Cannot be null.
     * @param latestEventsRememberCapacity The capacity of remembered latest events worked with. Must be 0 or a positive
     *                                     number. Used to prevent same event duplications coming from different
     *                                     channels.
     * @param doNotReceiveEventsFromSameSource Events sent from the same source as the current one will be totally
     *                                         ignored. Differentiation is made by the identifier set in the config and
     *                                         the available meta information in the received records.
     *                                         This is a more strict limitation compared to latestEventsRememberCapacity
     *                                         since the last one is restricted to the limited amount of run-time data.
     * @throws NullPointerException - If config is null.
     * @throws IllegalArgumentException - If latestEventsRememberCapacity is negative number.
     */
    public AbstractEventDispatcher(AbstractBrokerConfigFactory config, int latestEventsRememberCapacity,
                                   boolean doNotReceiveEventsFromSameSource) {
        if (latestEventsRememberCapacity < 0) {
            throw new IllegalArgumentException("latestEventsRememberCapacity must be a positive number");
        }
        if (config == null) {
            throw new NullPointerException("Not supported null configuration!");
        }

        this.latestEventsRememberCapacity = latestEventsRememberCapacity;
        this.doNotReceiveEventsFromSameSource = doNotReceiveEventsFromSameSource;

        DispatchingType dispatchingType = config.getDispatchingType();
        if (dispatchingType.equals(DispatchingType.PRODUCE)) {
            // PRODUCER
            this.latestEventsSent = (this.latestEventsRememberCapacity > 0 ?
                    new CircularFifoSet<>(this.latestEventsRememberCapacity) : null);
        } else {
            this.latestEventsReceived = (this.latestEventsRememberCapacity > 0 ?
                    new CircularFifoSet<>(this.latestEventsRememberCapacity) : null);
            if (dispatchingType.equals(DispatchingType.CONSUME_PRODUCE)) {
                // PRODUCER too
                this.latestEventsSent = (this.latestEventsRememberCapacity > 0 ?
                        new CircularFifoSet<>(this.latestEventsRememberCapacity) : null);
            }
        }
    }

    /**
     * Constructor.
     * @param config The configuration settings that also include broker specific configuration options. Cannot be null.
     *               The used latestEventsRemeberCapacity is 15 and doNotReceiveEventsFromSameSource is set to true.
     * @throws NullPointerException - If config is null.
     */
    public AbstractEventDispatcher(AbstractBrokerConfigFactory config) {
        this(config, 15, true);
    }

    /**
     * Returns the capacity of the remembered event count the dispatches has lastly interacted with.
     * @return Non-negative integer representing capacity.
     */
    public final int getLatestEventsRememberedCapacity() {
        return this.latestEventsRememberCapacity;
    }

    /**
     * Returns whether the strict prevention of received events from the same source (client identifier) is activated.
     * @return True if the filering is active otherwise false.
     */
    public final boolean getDoNotReceiveEventsFromSameSource() {
        return this.doNotReceiveEventsFromSameSource;
    }

    /**
     * Creates new instance with the same settings as of the current one.
     * @return New instance with effective configuration as the one of the current instance.
     */
    public abstract AbstractEventDispatcher makeNewWithSameConfig();

    /**
     * Retrieves the proper configuration instance based on the dispatching type parameter.
     * @param dt The dispatching type for which the configuration is required.
     * @return An initialized concrete instance or null if such instance cannot be found.
     */
    protected abstract AbstractBrokerConfigFactory retrieveConfig(DispatchingType dt);

    /**
     * Converts IEvent instance to binary data packet.
     * @param event The event instance to be used. Cannot be null.
     * @return The DataPacket representation of the provided event.
     * @throws IOException If event serialization fails.
     * @throws IllegalArgumentException If improper serialization mechanism was specified in the configuration or if
     *                                    the provided event was null.
     * @throws NullPointerException If the required configuration information for the used serialization type is null.
     */
    protected DataPacket eventToDataPacket(IEvent event) throws IOException, IllegalArgumentException {
        SerializationMechanism serializationMechanismType = null;
        AbstractBrokerConfigFactory conf = this.retrieveConfig(DispatchingType.PRODUCE);
        if (conf != null) {
            serializationMechanismType = conf.getSerializationMechanismType();
        }
        if (serializationMechanismType == null) {
            throw new NullPointerException("Null serialization mechanism type specified in the configuration!");
        }

        switch (serializationMechanismType) {
            case JAVA:
                return new DataPacket(serializationMechanismType, null,
                        new JavaEventSerializer().serialize(event));
            case JSON:
                JsonEventSerializer jes = new JsonEventSerializer();
                return new DataPacket(serializationMechanismType, jes.getEncoding(), jes.serialize(event));
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Cannot unpack data packet due to unknown format.");
        }
    }

    /**
     * Converts DataPacket with data to its IEvent instance representation.
     * @param dp The data packet instance to be used. Cannot be null.
     * @return An initialized IEvent or null if the provided data packet was null.
     * @throws IOException - If event deserialization fails.
     * @throws ClassNotFoundException - If event serialization fails.
     * @throws IllegalArgumentException - If the serialization mechanism is not supported or other error.
     */
    protected IEvent dataPacketToEvent(DataPacket dp) throws IOException, ClassNotFoundException,
            IllegalArgumentException {
        if (dp == null) return null;
        switch (dp.getSerializationMechanismType()) {
            case JAVA:
                return new JavaEventSerializer().deserialize(dp.getPayload());
            case JSON:
                return new JsonEventSerializer().deserialize(dp.getPayload());
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Cannot unpack data packet due to unknown format.");
        }
    }

    /**
     * Computes a hash based on IEvent hashCode appended with size of the byte representation (payload).
     * This makes more reliable hash even for Event inheritors that did not override hashCode method but still define
     * additional embedded parameters or other kind of not transient data.
     * @param event The event instance for which to be computed a packet hash. Cannot be null.
     * @param dataPacket The data packet instance representing the event. Cannot be null.
     * @return A complex hash value.
     */
    protected String computePacketHash(IEvent event, DataPacket dataPacket) {
        // we use dataPacket.getPayload() instead of getBytes because it's cheaper
        return String.valueOf(event.hashCode()) + String.valueOf(dataPacket.getPayload().length);
    }

    /**
     * Does pre-send checks to determine whether everything is good in order to send data.
     * @return True if everything is OK otherwise false.
     */
    protected abstract boolean doPreSendChecks();

    /**
     * Calls the actual sending procedure for the desired data and destination topic.
     * @param topic The topic (communication channel) to which the data to be sent.
     * @param dp The data that will be sent. Cannot be null.
     * @return True if the sending was successful otherwise false.
     */
    protected abstract boolean doActualSend(String topic, DataPacket dp);

    @Override
    public boolean send(IEvent event) {
        if (event == null || !doPreSendChecks()) return false;
        AbstractBrokerConfigFactory conf = this.retrieveConfig(DispatchingType.PRODUCE);
        if (conf == null) return false;

        @SuppressWarnings("unchecked")
        Class<? extends IEvent> ec = event.getClass();
        @SuppressWarnings("unchecked")
        Map<Class<? extends IEvent>, Set<String>> eventToTopicsMap = conf.getEventToTopicsMap();
        @SuppressWarnings("unchecked")
        Set<String> eventTopics = eventToTopicsMap.get(ec);
        if (eventTopics == null) {
            eventTopics = ConcurrentHashMap.newKeySet();
            eventToTopicsMap.put(ec, eventTopics);
        }

        String eventCat = event.getCategory();
        boolean isEventAlreadyMapped = (!eventTopics.add(eventCat));


        @SuppressWarnings("unchecked")
        Map<String, Set<Class<? extends IEvent>>> topicToEventsMap = conf.getTopicToEventsMap();
        Set<Class<? extends IEvent>> eventClasses = topicToEventsMap.get(eventCat);
        if (eventClasses == null) {
            eventClasses = ConcurrentHashMap.newKeySet();
            topicToEventsMap.put(eventCat, eventClasses);
        }
        boolean isEventAlreadyMapped2 = (!eventClasses.add(ec));

        if (!(isEventAlreadyMapped & isEventAlreadyMapped2)) {
            // the mapping of this event was incomplete so we inspect it to fill the missing information
            Class _cls = ec;
            Class baseObjectClass = Object.class;
            while ((_cls = _cls.getSuperclass()) != null) {
                if (_cls.equals(baseObjectClass)) { // lets prevent the known exceptions since it's faster
                    break;
                }

                IEvent _event;
                try {
                    @SuppressWarnings("unchecked")
                    Constructor<? extends IEvent> ctor = (Constructor<? extends IEvent>) _cls.getDeclaredConstructor();
                    _event = ctor.newInstance();
                } catch (Exception ex) {
                    break;
                }

                eventCat = _event.getCategory();
                Set<Class<? extends IEvent>> catClasses = topicToEventsMap.get(eventCat);
                if (catClasses == null) {
                    catClasses = ConcurrentHashMap.newKeySet();
                    topicToEventsMap.put(eventCat, catClasses);
                }
                catClasses.add(ec);

                Set<String> _topics = eventToTopicsMap.get(ec);
                if (_topics == null) {
                    _topics = ConcurrentHashMap.newKeySet();
                    eventToTopicsMap.put(ec, _topics);
                }
                _topics.add(eventCat);
            }
        }

        if (!eventTopics.isEmpty()) {
            try {
                DataPacket dp = this.eventToDataPacket(event);
                for (String topic : eventToTopicsMap.get(ec)) {
                    if (doActualSend(topic, dp)) {
                        if (this.latestEventsSent != null) this.latestEventsSent.add(dp.hashCode());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return false;
            }
        }
        return true;
    }

    /**
     * Does pre-receive checks to determine whether everything is good in order to receive data.
     * @return True if everything is OK otherwise false.
     */
    protected abstract boolean doPreReceiveChecks();

    /**
     * Calls the actual data receiving procedure.
     * @param timeout The time to spend in waiting for data if there's none. The unit depends on the particular broker.
     * @return List of data packets (if any) or a null (if none).
     */
    protected abstract List<DataPacket> doActualReceive(long timeout);

    @Override
    public List<IEvent> receive() {
        return this.receive(1500);
    }

    @Override
    public List<IEvent> receive(long timeout) {
        if (!doPreReceiveChecks()) return null;
        AbstractBrokerConfigFactory conf = this.retrieveConfig(DispatchingType.PRODUCE);
        if (conf == null) return null;
        @SuppressWarnings("unchecked")
        Set<String> topics = conf.getTopics();
        if (topics == null || topics.isEmpty()) return null;

        List<DataPacket> consumerRecords = doActualReceive(timeout);

        if (consumerRecords == null || consumerRecords.isEmpty()) return null;
        List<IEvent> events = new ComparableArrayList<IEvent>(consumerRecords.size()) {
            private static final long serialVersionUID = -7974246513379250713L;
        };

        boolean atLeastOneSucceeded = false;
        for (DataPacket cr : consumerRecords) {
            try {
                // prevent receiving events sent by us with filtering based on the runtime instance data for the sent
                // events until this period's X amount of events
                if ((this.latestEventsSent != null && this.latestEventsSent.contains(cr.hashCode()))
                        || (this.latestEventsReceived != null
                        && this.latestEventsReceived.contains(cr.hashCode()))) {
                    continue;
                }

                IEvent event = dataPacketToEvent(cr);
                Event.reserveNewId(event, true);
                events.add(event);
                atLeastOneSucceeded = true;
            } catch (IOException | ClassNotFoundException | IllegalAccessException e) {
                e.printStackTrace(System.err);
            }
        }

        if (!events.isEmpty()) {
            Collections.sort(events);
        }

        if (!atLeastOneSucceeded) return null;
        return events;
    }

    @Override
    public abstract void close();
}
