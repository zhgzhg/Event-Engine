package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers;

import net.uniplovdiv.fmi.cs.vrs.event.Event;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.AbstractDispatcherConfigFactory;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataEncodingMechanism;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.ClassesIEventScanner;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Abstract event broker configuration functional basis.
 * @param <T> The input that needs to be sent to the factory in order to produce configuration.
 * @param <R> The configuration object that will be returned by the factory.
 */
public abstract class AbstractBrokerConfigFactory<T, R> extends AbstractDispatcherConfigFactory<T, R> {

    protected DataEncodingMechanism dataEncodingMechanismType;
    protected DispatchingType dispatchingType;
    protected Set<String> topics;
    protected ConcurrentMap<String, Set<Class<? extends IEvent>>> topicToEventsMap;
    protected ConcurrentMap<Class<? extends IEvent>, Set<String>> eventToTopicsMap;
    protected UUID identifier1;
    protected UUID identifier2;

    /**
     * Constructor. Provides the basic configuration parameters.
     * @param dataEncodingMechanismType The serialization mechanism to be used when dispatching events in producer
     *                                   mode. Can be set null which will result defaulting to the Java one. Also see
     *                                   dispatchingType parameter.
     * @param dispatchingType The role that will be taken during event dispatching. See {@link DispatchingType}.
     *                        If it's set to null then it will default to {@link DispatchingType#CONSUME_PRODUCE}.
     * @param topics The topics (event data "channels") to interact with. If it is set to null then it will default to
     *               the category returned by {@link Event#getCategory()} method.
     * @param topicToEventsMap Additional map for extending the provided definitions of which event to which topic
     *                         should be distributed/read from.
     *                         For a key must be used a value present in the topics parameter. The corresponding value
     *                         needs to contain the set of event classes to be associated with the particular topic.
     *                         This parameter can also be set to null. In that case the distribution of events will be
     *                         done automatically based on {@link IEvent#getCategory()} method and the filtering
     *                         provided by the topics parameter.
     * @throws NullPointerException If cfg is null.
     * @throws IllegalArgumentException If topicToEventsMap contains topic keys that are not present in the topics
     *                                  parameter or if the provided event encoding mechanism is not supported.
     */
    public AbstractBrokerConfigFactory(DataEncodingMechanism dataEncodingMechanismType,
                                       DispatchingType dispatchingType, Set<String> topics,
                                       Map<String, Set<Class<? extends IEvent>>> topicToEventsMap) {
        if (dataEncodingMechanismType == DataEncodingMechanism.BASE32) {
            throw new IllegalArgumentException("Not supported event encoding mechanism "
                    + dataEncodingMechanismType.name());
        }

        this.dataEncodingMechanismType = (
                dataEncodingMechanismType != null && !dataEncodingMechanismType.equals(DataEncodingMechanism.UNKNOWN)
                ?
                        dataEncodingMechanismType
                :
                DataEncodingMechanism.JAVA
        );

        this.dispatchingType = (dispatchingType != null ? dispatchingType : DispatchingType.CONSUME_PRODUCE);

        this.topicToEventsMap = new ConcurrentHashMap<>();
        this.eventToTopicsMap = new ConcurrentHashMap<>();

        {
            String rootCat = new Event().getCategory();
            Class<? extends IEvent> ec = Event.class;

            if (topics == null || topics.isEmpty()) {
                this.topics = ConcurrentHashMap.newKeySet();
                this.topics.add(rootCat);
                this.topicToEventsMap.put(rootCat, ConcurrentHashMap.newKeySet());
                this.topicToEventsMap.get(rootCat).add(ec);
                this.eventToTopicsMap.put(ec, ConcurrentHashMap.newKeySet());
                this.eventToTopicsMap.get(ec).add(rootCat);
            } else {
                this.topics = ConcurrentHashMap.newKeySet(topics.size() + 1);
                this.topics.addAll(topics);
                if (!this.dispatchingType.equals(DispatchingType.CONSUME)) {
                    this.topics.add(rootCat);
                    this.topicToEventsMap.put(rootCat, ConcurrentHashMap.newKeySet());
                    this.topicToEventsMap.get(rootCat).add(ec);
                    this.eventToTopicsMap.put(ec, ConcurrentHashMap.newKeySet());
                    this.eventToTopicsMap.get(ec).add(rootCat);
                }
            }
        }

        // generate the UUID 1
        this.identifier1 = UUID.randomUUID();

        if (topicToEventsMap != null && !topicToEventsMap.isEmpty()) {
            List<String> missingTopics = topicToEventsMap.keySet().stream().filter((o) -> !this.topics.contains(o))
                    .collect(Collectors.toList());
            if (missingTopics != null && !missingTopics.isEmpty()) {
                throw new IllegalArgumentException(
                        "The following topic mapping keys are not found as topics in the configuration: "
                                + missingTopics);
            }

            for (Map.Entry<String, Set<Class<? extends IEvent>>> entry : topicToEventsMap.entrySet()) {
                Set<Class<? extends IEvent>> specifiedClasses = entry.getValue();
                String k = entry.getKey();
                Set<Class<? extends IEvent>> classes = this.topicToEventsMap.get(k);
                if (classes == null) {
                    classes = ConcurrentHashMap.newKeySet();
                    this.topicToEventsMap.put(k, classes);
                }
                classes.addAll(specifiedClasses);

                // build the reversed map
                for (Class<? extends IEvent> cls : classes) {
                    Set<String> _topics = this.eventToTopicsMap.get(cls);
                    if (_topics == null) {
                        _topics = ConcurrentHashMap.newKeySet();
                        this.eventToTopicsMap.put(cls, _topics);
                    }
                    _topics.add(k);
                }
            }
        }

        // Scan the default event classes to map additional topics to events. This is helpful for producers, but not
        // necessarily for consumers since they may be interested only in a particular topic, while the producers must
        // emit events at least in the most top group.
        if ((topics == null && this.dispatchingType.equals(DispatchingType.CONSUME))
                || !this.dispatchingType.equals(DispatchingType.CONSUME)) {
            ClassesIEventScanner ces = new ClassesIEventScanner(Event.class.getPackage().getName());
            Set<Class<? extends IEvent>> scanned = ces.scan();
            for (Class<? extends IEvent> cls : scanned) {
                Class<?> _cls = cls;
                do {
                    IEvent event = null;
                    try {
                        @SuppressWarnings("unchecked")
                        Constructor<? extends IEvent> ctor =
                                (Constructor<? extends IEvent>) _cls.getDeclaredConstructor();
                        event = ctor.newInstance();
                    } catch (Exception ex) {
                        break;
                    }

                    String eventCat = event.getCategory();
                    if (!this.topics.contains(eventCat)) {
                        // consumers by default should be subscribed only to "events" topic
                        // or the explicitly stated topics
                        if (this.dispatchingType.equals(DispatchingType.PRODUCE)) {
                            this.topics.add(eventCat);
                        }
                        this.topicToEventsMap.putIfAbsent(eventCat, ConcurrentHashMap.newKeySet());
                    }

                    this.topicToEventsMap.get(eventCat).add(cls);
                    Set<String> _topics = this.eventToTopicsMap.get(cls);
                    if (_topics == null) {
                        _topics = ConcurrentHashMap.newKeySet();
                        this.eventToTopicsMap.put(cls, _topics);
                    }
                    _topics.add(eventCat);

                    _cls = _cls.getSuperclass();
                } while (_cls != null);
            }
        }

        // make the prepared collections unmodifiable
        //this.topics = Collections.unmodifiableSet(this.topics);
        //for (Map.Entry<String, Set<Class<?>>> entry : this.topicToEventsMap.entrySet()) {
        //    entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        //}
        //this.topicToEventsMap = Collections.unmodifiableMap(this.topicToEventsMap);
        //for (Map.Entry<Class<?>, Set<String>> entry : this.eventToTopicsMap.entrySet()) {
        //    entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        //}
        //this.eventToTopicsMap = Collections.unmodifiableMap(this.eventToTopicsMap);

        // generate the UUID 2
        this.identifier2 = UUID.randomUUID();
    }

    /**
     * Copy constructor.
     * @param cfg The configuration factory to create copy from.
     * @throws NullPointerException If null cfg is provided.
     */
    public AbstractBrokerConfigFactory(AbstractBrokerConfigFactory<T,R> cfg) {
        if (cfg == null) throw new NullPointerException("Copying from null configuration factory!");

        this.identifier1 = cfg.identifier1;
        this.identifier2 = cfg.identifier2;
        this.dispatchingType = cfg.dispatchingType;

        this.dataEncodingMechanismType = cfg.dataEncodingMechanismType;

        this.topics = ConcurrentHashMap.newKeySet();
        this.topics.addAll(cfg.topics);

        this.topicToEventsMap = new ConcurrentHashMap<>(cfg.topicToEventsMap);
        this.eventToTopicsMap = new ConcurrentHashMap<>(cfg.eventToTopicsMap);
    }

    @Override
    public abstract R getMainConfiguration(T input);

    /**
     * Returns the configured type of serialization used for de/serializing of data.
     * @return An enumeration value of the used mechanism.
     */
    public DataEncodingMechanism getDataEncodingMechanismType() {
        return this.dataEncodingMechanismType;
    }

    /**
     * Returns the configured operation mode of the dispatcher.
     * @return An enumeration value of the used mode.
     */
    public DispatchingType getDispatchingType() {
        return this.dispatchingType;
    }

    /**
     * Returns a unique string identifier used for specific purposes.
     * @return A randomly generated UUID instance.
     */
    public UUID getIdentifier1() {
        return this.identifier1;
    }

    /**
     * Returns a unique string identifier used for specific purposes.
     * @return A randomly generated UUID instance.
     */
    public UUID getIdentifier2() {
        return this.identifier2;
    }

    /**
     * Returns the list of topics (information channels) desired for interaction.
     * @return A non null set of strings. Can be an empty one.
     */
    public Set<String> getTopics() {
        return this.topics;
    }

    /**
     * Returns an internal mapping structured used for quick determination which topic to which event class corresponds.
     * @return A non null set.
     */
    public ConcurrentMap<String, Set<Class<? extends IEvent>>> getTopicToEventsMap() {
        return this.topicToEventsMap;
    }

    /**
     * Returns an internal mapping structured used for quick determination which event class to which topic corresponds.
     * @return A non null set.
     */
    public ConcurrentMap<Class<? extends IEvent>, Set<String>> getEventToTopicsMap() {
        return this.eventToTopicsMap;
    }
}
