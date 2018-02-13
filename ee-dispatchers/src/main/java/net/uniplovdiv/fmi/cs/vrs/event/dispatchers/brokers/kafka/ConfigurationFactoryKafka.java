package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.kafka;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.AbstractBrokerConfigFactory;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.DispatchingType;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataEncodingMechanism;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;

/**
 * Configuration factory for Kafka. It accepts initial configuration options that are merged with an internal additional
 * ones provided by the factory. Optionally upon instantiation more options can be provided and even overwrite some
 * previous. Any configuration parameters can overwrite the internal ones.
 * Take note that making two separate instances even with the same input can result differences for some configuration
 * parameters (if not overwritten) like for e.g. 'group.id' for which the default approach is every time to randomly
 * generate a new one.
 */
public class ConfigurationFactoryKafka extends AbstractBrokerConfigFactory<Properties, Properties> {

    private Properties configuration;

    /**
     * Constructor. Provides the basic configuration parameters.
     * @param cfg The Kafka configuration parameters that are required. At least "bootstrap.servers" is required.
     *            Setting "auto.offset.reset" is also recommended. Depending on the dispatchingType the factory will
     *            initialize with random UUID values "group.id" and "client.id". Once generated it is recommended to
     *            persist them in order on the next start of your application to continue reading the events from the
     *            offset it has previously reached.
     * @param dataEncodingMechanismType The serialization mechanism to be used when dispatching events in producer
     *                                   mode. Can be set null which will result defaulting to the Java one. Also see
     *                                   dispatchingType parameter.
     * @param dispatchingType The role that will be taken during event dispatching. See {@link DispatchingType}.
     *                        If it's set to null then it will default to {@link DispatchingType#CONSUME_PRODUCE}.
     * @param topics The topics (event data "channels") to interact with. If it is set to null then it will default to
     *               the category returned by {@link IEvent#getCategory()} method.
     * @param topicToEventsMap Additional map for extending the provided definitions of which event to which topic
     *                         should be distributed/read from.
     *                         For a key must be used a value present in the topics parameter. The corresponding value
     *                         needs to contain the set of event classes to be associated with the particular topic.
     *                         This parameter can also be set to null. In that case the distribution of events will be
     *                         done automatically based on {@link IEvent#getCategory()} method and the filtering
     *                         provided by the topics parameter.
     * @throws NullPointerException - If cfg is null.
     * @throws IllegalArgumentException - If topicToEventsMap contains topic keys that are not present in the topics
     *                                    parameter.
     */
    public ConfigurationFactoryKafka(Properties cfg, DataEncodingMechanism dataEncodingMechanismType,
                                     DispatchingType dispatchingType, Set<String> topics,
                                     Map<String, Set<Class<? extends IEvent>>> topicToEventsMap) {
        super(dataEncodingMechanismType, dispatchingType, topics, topicToEventsMap);
        this.configuration = new Properties();
        this.configuration.putAll(cfg);
    }

    /**
     * Copy constructor.
     * @param cfg The configuration factory to create copy from.
     * @throws NullPointerException - If null cfg is provided.
     */
    public ConfigurationFactoryKafka(ConfigurationFactoryKafka cfg) {
        super(cfg);
        this.configuration = new Properties();
        this.configuration.putAll(cfg.configuration);
    }

    /**
     * Returns a new prepared configuration object while merging any data provided in the input.
     * @param input An optional input that can be null and provides additional Kafka configuration settings.
     * @return An initialized configuration object.
     */
    @Override
    public Properties getMainConfiguration(Properties input) {
        Properties result = new Properties();

        if (!this.dispatchingType.equals(DispatchingType.CONSUME)) {
            result.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        }
        if (!this.dispatchingType.equals(DispatchingType.PRODUCE)) {
            result.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        }
        String serdeName = DataPacketKafka.class.getCanonicalName();
        if (!this.dispatchingType.equals(DispatchingType.CONSUME)) {
            result.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serdeName);
            result.put(ProducerConfig.CLIENT_ID_CONFIG, this.identifier2.toString());
        }
        if (!this.dispatchingType.equals(DispatchingType.PRODUCE)) {
            result.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, serdeName);
            result.put(ConsumerConfig.GROUP_ID_CONFIG, this.identifier1.toString());
            result.put(ConsumerConfig.CLIENT_ID_CONFIG, this.identifier2.toString());
            result.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 150);
        }

        result.putAll(this.configuration);
        if (input != null) {
            result.putAll(input);
        }
        return result;
    }

    /**
     * Returns the configuration entry string key pointing to the identifier of the producer/consumer.
     * @return An initialized string mentioning the configuration entry containing the client id.
     */
    public String getClientIdKey() {
        if (!this.dispatchingType.equals(DispatchingType.PRODUCE)) {
            return ConsumerConfig.CLIENT_ID_CONFIG;
        }
        return ProducerConfig.CLIENT_ID_CONFIG;
    }
}
