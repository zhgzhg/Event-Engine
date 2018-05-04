package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers;

import net.uniplovdiv.fmi.cs.vrs.event.Event;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.CircularFifoSet;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.IEventDispatcher;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataEncodingMechanism;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataPacket;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ComparableArrayList;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.IEventSerializer;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.JavaEventSerializer;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.JsonEventSerializer;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.Base32Encoder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class AbstractEventDispatcher implements IEventDispatcher {

    // Specialized structures for storing the hashes of the DataPackets of the latest sent/received events in order not
    // to receive them again in case of CONSUME_PRODUCE operation mode or 1 event sent to many distribution channels
    // and received by CONSUME or CONSUME_PRODUCE instance
    protected Collection<Integer> latestEventsSent;
    protected Collection<Integer> latestEventsReceived;
    protected final int latestEventsRememberCapacity;
    protected final boolean doNotReceiveEventsFromSameSource;

    protected final static String CLIENT_ID_HEADER_KEY = "event_src";

    protected Map<Class<?>, IEventSerializer> serializerHelpers;
    protected Base32Encoder base32EncoderHelper;

    protected String[] packagesWithEvents;

    /**
     * Constructor.
     * @param config The configuration settings that also include concrete broker configuration options. Cannot be null.
     * @param latestEventsRememberCapacity The capacity of remembered latest events worked with. Must be 0 or a positive
     *                                     number. Used to prevent same event duplications coming from different
     *                                     channels.
     * @param doNotReceiveEventsFromSameSource Events sent from the same source as the current one will be totally
     *                                         ignored. Differentiation is made by the identifier set in the config and
     *                                         the available meta information in the received records.
     *                                         This is a more strict limitation compared to latestEventsRememberCapacity
     *                                         since the last one is restricted to the limited amount of run-time data.
     * @throws NullPointerException If config is null.
     * @throws IllegalArgumentException If latestEventsRememberCapacity is negative number.
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
            this.latestEventsSent = (this.latestEventsRememberCapacity > 0 ? Collections.synchronizedCollection(
                    new CircularFifoSet<Integer>(this.latestEventsRememberCapacity)) : null);
        } else {
            this.latestEventsReceived = (this.latestEventsRememberCapacity > 0 ? Collections.synchronizedCollection(
                    new CircularFifoSet<Integer>(this.latestEventsRememberCapacity)) : null);
            if (dispatchingType.equals(DispatchingType.CONSUME_PRODUCE)) {
                // PRODUCER too
                this.latestEventsSent = (this.latestEventsRememberCapacity > 0 ? Collections.synchronizedCollection(
                        new CircularFifoSet<Integer>(this.latestEventsRememberCapacity)) : null);
            }
        }

        this.serializerHelpers = new HashMap<>();
    }

    /**
     * Constructor.
     * @param config The configuration settings that also include broker specific configuration options. Cannot be null.
     *               The used latestEventsRemeberCapacity is 15 and doNotReceiveEventsFromSameSource is set to true.
     * @throws NullPointerException If config is null.
     */
    public AbstractEventDispatcher(AbstractBrokerConfigFactory config) {
        this(config, 15, true, null);
    }

    /**
     * Constructor - the most complete one.
     * @param config The configuration settings that also include concrete broker configuration options. Cannot be null.
     * @param latestEventsRememberCapacity The capacity of remembered latest events worked with. Must be 0 or a positive
     *                                     number. Used to prevent same event duplications coming from different
     *                                     channels.
     * @param doNotReceiveEventsFromSameSource Events sent from the same source as the current one will be totally
     *                                         ignored. Differentiation is made by the identifier set in the config and
     *                                         the available meta information in the received records.
     *                                         This is a more strict limitation compared to latestEventsRememberCapacity
     *                                         since the last one is restricted to the limited amount of run-time data.
     * @param packagesWithEvents Array of full package names containing custom IEvent implementations. Useful during
     *                           event de/serialization to increase performance and prevent exceptions. Can be null.
     * @throws NullPointerException If config is null.
     * @throws IllegalArgumentException If latestEventsRememberCapacity is negative number.
     */
    public AbstractEventDispatcher(AbstractBrokerConfigFactory config, int latestEventsRememberCapacity,
                                   boolean doNotReceiveEventsFromSameSource, String[] packagesWithEvents) {
        this(config, latestEventsRememberCapacity, doNotReceiveEventsFromSameSource);
        this.packagesWithEvents = packagesWithEvents;
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
     *                                  the provided event was null.
     * @throws NullPointerException If the required configuration information for the used serialization type is null.
     */
    protected DataPacket eventToDataPacket(IEvent event) throws IOException, IllegalArgumentException {
        DataEncodingMechanism dataEncodingMechanismType = null;
        AbstractBrokerConfigFactory conf = this.retrieveConfig(DispatchingType.PRODUCE);
        if (conf != null) {
            dataEncodingMechanismType = conf.getDataEncodingMechanismType();
        }
        if (dataEncodingMechanismType == null) {
            throw new NullPointerException("Null serialization mechanism type specified in the configuration!");
        }

        switch (dataEncodingMechanismType) {
            case JAVA:
                IEventSerializer serializer = this.serializerHelpers.computeIfAbsent(
                        JavaEventSerializer.class, clazz -> new JavaEventSerializer());
                return new DataPacket(dataEncodingMechanismType, null, serializer.serialize(event));

            case JSON:
                JsonEventSerializer jes = (JsonEventSerializer) this.serializerHelpers.computeIfAbsent(
                        JsonEventSerializer.class, clazz -> {
                            JsonEventSerializer res = new JsonEventSerializer(this.packagesWithEvents);
                            res.setAttemptAutomaticClassRegistration(true);
                            return res;
                        });
                return new DataPacket(dataEncodingMechanismType, jes.getEncoding(), jes.serialize(event));

            case BASE32:
                throw new IllegalArgumentException("Event cannot be packed using BASE32 format.");
            case UNKNOWN:
            default:
                throw new IllegalArgumentException("Cannot pack event in data packet due to unknown format.");
        }
    }

    /**
     * Nests an existing data packet into another one.
     * @param packet An initialized data packet to be nested inside another data packet.
     * @param dataEncodingMechanism Used for the packet nesting. Only BASE32 is supported.
     * @return A new data packet containing a nested copy of the existing one in its payload section.
     * @throws IllegalArgumentException If the provided data packet or data encoding mechanisms are null or invalid.
     */
    protected DataPacket nestDataPacket(DataPacket packet, DataEncodingMechanism dataEncodingMechanism) {
        if (packet == null || dataEncodingMechanism != DataEncodingMechanism.BASE32) {
            throw new IllegalArgumentException("Bad packet and/or data encoding mechanism provided");
        }

        if (base32EncoderHelper == null) {
            base32EncoderHelper = new Base32Encoder();
        }
        byte[] payload = base32EncoderHelper.encode(packet.toBytes());
        Charset encoding = packet.getEncoding(); // providing the encoding of the inner packet since BASE32 procedure
        // works with bytes only and it's not interested in any encodings
        return new DataPacket(DataEncodingMechanism.BASE32, DataPacket.Version.NESTED, encoding, payload);
    }

    /**
     * Converts DataPacket with data to its IEvent instance representation.
     * @param dp The data packet instance to be used. Cannot be null.
     * @return An initialized IEvent or null if the provided data packet was null.
     * @throws IOException If event deserialization fails.
     * @throws ClassNotFoundException If event serialization fails.
     * @throws IllegalArgumentException If the serialization mechanism is not supported or other error or if the payload
     *                                  of the data packet is another data packet that failed extracting.
     */
    protected IEvent dataPacketToEvent(DataPacket dp) throws IOException, ClassNotFoundException,
            IllegalArgumentException {
        if (dp == null) return null;
        switch (dp.getDataEncodingMechanismType()) {
            case JAVA:
                IEventSerializer serializer = this.serializerHelpers.computeIfAbsent(
                        JavaEventSerializer.class, clazz -> new JavaEventSerializer());
                return serializer.deserialize(dp.getPayload());

            case JSON:
                JsonEventSerializer jes = (JsonEventSerializer) this.serializerHelpers.computeIfAbsent(
                        JsonEventSerializer.class, clazz -> {
                            JsonEventSerializer res = new JsonEventSerializer(this.packagesWithEvents);
                            res.setAttemptAutomaticClassRegistration(true);
                            return res;
                        });
                if (dp.getEncoding() != null && !jes.getEncoding().equals(dp.getEncoding())) {
                    jes = new JsonEventSerializer(dp.getEncoding(), null, null, this.packagesWithEvents);
                    jes.setAttemptAutomaticClassRegistration(true);
                }
                return jes.deserialize(dp.getPayload());

            case BASE32:
                if (dp.getDataPacketVersion() != DataPacket.Version.NESTED) {
                    throw new IllegalArgumentException("Cannot unpack not nested data packet encoded using BASE32!");
                }
                if (base32EncoderHelper == null) {
                    base32EncoderHelper = new Base32Encoder();
                }
                byte[] nestedPacket = base32EncoderHelper.decode(dp.getPayload());
                if (nestedPacket == null || nestedPacket.length == 0) {
                    throw new IllegalArgumentException("Failed to extract nested data packet that is null or with a "
                            + "length of zero!");
                }
                return dataPacketToEvent(new DataPacket(nestedPacket));

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

    /**
     * Prepares to send event by doing internal not initialized structures initialization followed by send pre-checks.
     * @param event The event to send.
     * @return True if the preparation is successful, otherwise false.
     */
    protected boolean prepareToSend(IEvent event) {
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
        return true;
    }

    @Override
    public void send(IEvent event, BiConsumer<Boolean, IEvent> onCompletion) {
        if (prepareToSend(event)) {
            final DataPacket dp;
            try {
                dp = this.eventToDataPacket(event);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                if (onCompletion != null) {
                    onCompletion.accept(Boolean.FALSE, event);
                }
                return;
            }

            @SuppressWarnings("unchecked")
            Class<? extends IEvent> ec = event.getClass();
            @SuppressWarnings("unchecked")
            ConcurrentMap<Class<? extends IEvent>, Set<String>> eventToTopicsMap =
                    this.retrieveConfig(DispatchingType.PRODUCE).getEventToTopicsMap();
            @SuppressWarnings("unchecked")
            Set<String> eventTopics = eventToTopicsMap.get(ec); // concurrent as well

            if (!eventTopics.isEmpty()) {
                CompletableFuture.supplyAsync(() -> {
                    boolean atLeastOneSent = false;
                    for (String topic : eventToTopicsMap.get(ec)) {
                        if (doActualSend(topic, dp)) {
                            if (this.latestEventsSent != null) this.latestEventsSent.add(dp.hashCode());
                            atLeastOneSent = true;
                        } else {
                            System.err.println("Failed to send event " + ec.getCanonicalName() + " to topic " + topic);
                        }
                    }
                    return atLeastOneSent;
                }).thenAccept((result) -> {
                    if (onCompletion != null) {
                        onCompletion.accept(result, event);
                    }
                });
            }
        } else if (onCompletion != null) {
            onCompletion.accept(Boolean.FALSE, event);
        }
    }

    @Override
    public boolean send(IEvent event) {
        if (!prepareToSend(event)) return false;

        @SuppressWarnings("unchecked")
        Class<? extends IEvent> ec = event.getClass();

        @SuppressWarnings("unchecked")
        ConcurrentMap<Class<? extends IEvent>, Set<String>> eventToTopicsMap =
                this.retrieveConfig(DispatchingType.PRODUCE).getEventToTopicsMap();

        @SuppressWarnings("unchecked")
        Set<String> eventTopics = eventToTopicsMap.get(ec); // concurrent as well

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

    /**
     * Executes a task within certain time or timeouts.
     * @param task The task to be executed.
     * @param timeoutAfter The time interval after a timeout to be resulted.
     * @param taskScheduler The task scheduler executor service used to detect the timeout.
     * @param <T> The result type returned by this CompletableFuture's get method.
     * @return Instance of CompletableFuture that will execute within a certain time or timeout.
     */
    public static <T> CompletableFuture<T> executeWithin(CompletableFuture<T> task, Duration timeoutAfter,
                                                         ScheduledExecutorService taskScheduler) {
        final CompletableFuture<T> timedOutTask = failAfter(timeoutAfter, taskScheduler);
        return task.applyToEither(timedOutTask, Function.identity());
    }

    /**
     * Creates CompletableFuture that will certainly fail by throwing internally TimeoutException. Used for creating
     * futures with timeouts.
     * @param afterDuration The duration after the TimeoutException (ExecutionException) will be raised and the task
     *                      will fail.
     * @param taskScheduler The task scheduler executor service to be used to run the timing out task.
     * @param <T> The result type returned by this CompletableFuture's get method.
     * @return A new CompletableFuture instance.
     */
    protected static <T> CompletableFuture<T> failAfter(Duration afterDuration, ScheduledExecutorService taskScheduler) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        taskScheduler.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + afterDuration);
            return promise.completeExceptionally(ex);
        }, afterDuration.toMillis(), MILLISECONDS);
        return promise;
    }

    /**
     * Creates CompletableFuture from Callable task, that gets executed immediately through existing
     * ScheduledExecutorService.
     * @param task The task to be executed.
     * @param taskScheduler The task scheduler executor service to be used to run the timing out task.
     * @param <T> The result type returned by this CompletableFuture's get method.
     * @return A new CompletableFuture instance.
     */
    public static <T> CompletableFuture<T> scheduleNow(Callable<T> task, ScheduledExecutorService taskScheduler) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        taskScheduler.schedule(() -> promise.complete(task.call()), 0, MILLISECONDS);
        return promise;
    }
}
