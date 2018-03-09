package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.activemq;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.AbstractBrokerConfigFactory;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.DispatchingType;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataEncodingMechanism;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jndi.ActiveMQInitialContextFactory;

import javax.naming.Context;
import java.util.*;

/**
 * Basic configuration factory for ActiveMQ providing only the most general options. It accepts initial configuration
 * options that are merged with an internal additional ones provided by the factory. Optionally upon instantiation more
 * options can be provided and even overwrite some previous. Any configuration parameters can overwrite the internal
 * ones. Take note that making two separate instances even with the same input can result differences for some
 * configuration parameters (if not overwritten) like for e.g. 'jms.clientID' for which the default approach is every
 * time to randomly generate a new one.
 * The supported configuration options match the name of the available set methods inside
 * {@link ActiveMQConnectionFactory}. Very basic connectivity options are also described in JNDI documentation and
 * constants like those inside {@link Context} and in ActiveMQ's sample file in GitHub -
 * <a href="https://github.com/apache/activemq/blob/master/activemq-unit-tests/src/test/resources/jndi.properties">jndi.properties</a>
 */
public class ConfigurationFactoryActiveMQ extends AbstractBrokerConfigFactory<Properties, Properties> {

    private Properties configuration;

    /**
     * Constructor. Provides the basic configuration parameters.
     * @param cfg The ActiveMQ configuration parameters that are required. Recommended to set are parameters like
     *            jms.userName", "jms.password", "jms.clientID". If the last one is not specified the factory will
     *            initialize it with a random UUID value. Once generated it is recommended to persist it in order on the
     *            next start of your application to continue reading the events from the offset it has been previously
     *            reached.
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
     * @throws NullPointerException If cfg is null.
     * @throws IllegalArgumentException If topicToEventsMap contains topic keys that are not present in the topics
     *                                    parameter.
     */
    public ConfigurationFactoryActiveMQ(Properties cfg, DataEncodingMechanism dataEncodingMechanismType,
                                        DispatchingType dispatchingType, Set<String> topics,
                                        Map<String, Set<Class<? extends IEvent>>> topicToEventsMap) {
        super(dataEncodingMechanismType, dispatchingType, topics, topicToEventsMap);
        this.configuration = new Properties();
        this.configuration.put(Context.INITIAL_CONTEXT_FACTORY, ActiveMQInitialContextFactory.class.getCanonicalName());
        this.configuration.put(Context.PROVIDER_URL, ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL);
        this.configuration.putAll(cfg);
    }

    /**
     * Copy constructor.
     * @param cfg The configuration factory to create copy from.
     * @throws NullPointerException - If null cfg is provided.
     */
    public ConfigurationFactoryActiveMQ(ConfigurationFactoryActiveMQ cfg) {
        super(cfg);
        this.configuration = new Properties();
        this.configuration.putAll(cfg.configuration);
    }

    @Override
    public Properties getMainConfiguration(Properties input) {
        Properties result = new Properties();

        String id = this.configuration.getProperty(this.getClientIdKey());
        if (id == null || id.isEmpty()) {
            this.configuration.put(this.getClientIdKey(), this.getIdentifier2().toString());
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
        return "jms.clientID";
    }
}
