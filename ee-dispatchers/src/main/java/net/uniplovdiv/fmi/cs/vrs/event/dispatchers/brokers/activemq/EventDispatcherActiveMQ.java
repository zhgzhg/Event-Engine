package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.activemq;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.AbstractBrokerConfigFactory;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.AbstractEventDispatcher;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.DispatchingType;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataPacket;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Dispatches IEvent instances using Apache ActiveMQ.
 */
public class EventDispatcherActiveMQ extends AbstractEventDispatcher {

    protected ConfigurationFactoryActiveMQ configFactory;
    protected Connection connection;
    protected Session session;

    protected MessageProducer producer;
    protected Consumer consumer;

    protected boolean isRetroactive;

    /**
     * Wrapper of MessageConsumer allowing seamless multiple topics per "one" consumer.
     */
    class Consumer {
        private List<MessageConsumer> consumers;
        private String clientId;
        private boolean isRetroactive;

        /**
         * Constructor. Creates a durable consumer associated with one or more topics.
         * @param topics The topics from which to receive data.
         * @param beRetroactive Tries to gather the oldest available possible data upon connect. Useful especially for
         *                      consumers connecting for the first time to a particular topic.
         * @throws JMSException If the consumer cannot be constructed because of an error preventing the JMS conditions
         *                      to be satisfied.
         * @throws IllegalArgumentException If topics is null.
         */
        public Consumer(Set<String> topics, boolean beRetroactive) throws JMSException {
            this.consumers = null;
            this.isRetroactive = beRetroactive;

            if (topics == null || topics.isEmpty()) {
                throw new IllegalArgumentException("Empty set of topics!");
            }
            if (session == null) {
                throw new IllegalArgumentException("Uninitialized session!");
            }

            this.consumers = new ArrayList<>(topics.size());

            this.clientId = configFactory.getMainConfiguration(null).getProperty(configFactory.getClientIdKey());
            for (String topic : topics) {
                MessageConsumer c = session.createDurableSubscriber(
                        session.createTopic(topic + (isRetroactive ? "?consumer.retroactive=true" : "")),
                        this.clientId + ":" + topic, null,
                        doNotReceiveEventsFromSameSource
                );
                this.consumers.add(c);
            }
        }

        /**
         * Receives data from the associated topics.
         * @param timeoutMs The time in milliseconds to wait before to give up.
         * @return Initialized byte array if there's data or null if none.
         * @throws JMSException If the receiving process fails due to JMS problems.
         */
        public List<byte[]> receive(int timeoutMs) throws JMSException {
            if (this.consumers == null || this.consumers.size() <= 0) {
                return null;
            }

            byte[] buffer = new byte[1024];
            int length;
            List<byte[]> result = new ArrayList<>(this.consumers.size());

            for (MessageConsumer mc : this.consumers) {
                Message m = mc.receive(timeoutMs);
                if (m != null && m instanceof BytesMessage) {
                    // the filtering for data from the same source is done in the consumer - no need since ActiveMQ does that
                    //if (doNotReceiveEventsFromSameSource) {
                    //    String source = m.getStringProperty(CLIENT_ID_HEADER_KEY);
                    //    if (source != null && source.equals(this.clientId)) {
                    //        continue;
                    //    }
                    //}
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    BytesMessage bm = (BytesMessage)m;
                    while ((length = bm.readBytes(buffer)) > 0) {
                        data.write(buffer, 0, length);
                    }
                    result.add(data.toByteArray());
                }
            }

            return result;
        }

        /**
         * Close any opened consumer connections, thus the current instance would not be able to receive anymore data.
         * Calling this method is mandatory if the application needs to be closed gracefully.
         */
        public void close() {
            if (this.consumers != null && !this.consumers.isEmpty()) {
                for (Iterator<MessageConsumer> it = this.consumers.iterator(); it.hasNext(); ) {
                    MessageConsumer mc = it.next();
                    try {
                        mc.close();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                    it.remove();
                }
                this.consumers = null;
            }
        }

        /**
         * Returns whether the consumer is in retroactive mode or not. See {@link #Consumer}.
         * @return True if the mode is active otherwise false.
         */
        public boolean isRetroactive() {
            return this.isRetroactive;
        }
    }

    /**
     * Constructor - the most complete one.
     * @param config The configuration settings that also include ActiveMQ configuration options. Cannot be null.
     * @param latestEventsRememberCapacity The capacity of remembered latest events worked with. Must be 0 or a positive
     *                                     number. Used to prevent same event duplications coming from different
     *                                     channels.
     * @param doNotReceiveEventsFromSameSource Events sent from the same source as the current one will be totally
     *                                         ignored. Differentiation is made by the identifier set in the config and
     *                                         the available meta information in the received ActiveMQ records.
     *                                         This is a more strict limitation compared to latestEventsRememberCapacity
     *                                         since the last one is restricted to the limited amount of run-time data.
     * @param beRetroactive Tries to gather the oldest available possible data upon connect. Useful especially for
     *                      consumers connecting for the first time to a particular topic.
     * @param packagesWithEvents Array of full package names containing custom IEvent implementations. Useful during
     *                           event de/serialization to increase performance and prevent exceptions. Can be null.
     * @throws NullPointerException If config is null.
     * @throws IllegalArgumentException If latestEventsRememberCapacity is negative number.
     * @throws NamingException If ActiveMQ cannot be initialized for some reason.
     * @throws JMSException If ActiveMQ cannot be initialized for some reason.
     */
    public EventDispatcherActiveMQ(ConfigurationFactoryActiveMQ config, int latestEventsRememberCapacity,
                                   boolean doNotReceiveEventsFromSameSource, boolean beRetroactive,
                                   String[] packagesWithEvents) throws
            NamingException, JMSException {
        super(config, latestEventsRememberCapacity, doNotReceiveEventsFromSameSource, packagesWithEvents);
        this.isRetroactive = beRetroactive;

        this.configFactory = new ConfigurationFactoryActiveMQ(config);

        DispatchingType dispatchingType = config.getDispatchingType();
        Properties props = config.getMainConfiguration(null);

        Context ctx = new InitialContext(props);
        ConnectionFactory cf = (ConnectionFactory)ctx.lookup("ConnectionFactory");
        this.connection = cf.createConnection();
        this.connection.setClientID(props.getProperty(configFactory.getClientIdKey()));
        this.connection.start();

        boolean isTransacted = false;
        //noinspection ConstantConditions
        this.session = this.connection.createSession(isTransacted, Session.AUTO_ACKNOWLEDGE);

        if (!dispatchingType.equals(DispatchingType.CONSUME)) {
            this.producer = this.session.createProducer(null);
            this.producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        }
        if (!dispatchingType.equals(DispatchingType.PRODUCE)) {
            this.consumer = new Consumer(config.getTopics(), beRetroactive);
        }
    }

    /**
     * Constructor.
     * @param config The configuration settings that also include ActiveMQ configuration options. Cannot be null.
     *               The used latestEventsRememberCapacity is 15, doNotReceiveEventsFromSameSource is set to true,
     *               beRetroactive is set to true and no additional packages with events are specified.
     * @throws NullPointerException If config is null.
     * @throws NamingException If ActiveMQ cannot be initialized for some reason.
     * @throws JMSException If ActiveMQ cannot be initialized for some reason.
     */
    public EventDispatcherActiveMQ(ConfigurationFactoryActiveMQ config) throws NamingException, JMSException {
        this(config, 15, true, true, null);
    }

    @Override
    public AbstractEventDispatcher makeNewWithSameConfig() {
        EventDispatcherActiveMQ eda = null;
        try {
            eda = new EventDispatcherActiveMQ(this.configFactory, this.latestEventsRememberCapacity,
                    this.doNotReceiveEventsFromSameSource, this.isRetroactive, this.packagesWithEvents);
            DispatchingType dt = this.configFactory.getDispatchingType();
            if (!dt.equals(DispatchingType.CONSUME) && eda.latestEventsSent != null && this.latestEventsSent != null) {
                eda.latestEventsSent.addAll(this.latestEventsSent);
            }
            if (!dt.equals(DispatchingType.PRODUCE) && eda.latestEventsReceived != null
                    && this.latestEventsReceived != null) {
                eda.latestEventsReceived.addAll(this.latestEventsReceived);
            }
        } catch (NamingException | JMSException ex) {
            ex.printStackTrace(System.err);
        }
        return eda;
    }

    @Override
    protected AbstractBrokerConfigFactory retrieveConfig(DispatchingType dt) {
        return this.configFactory;
    }

    @Override
    protected boolean doPreSendChecks() {
        return !(this.producer == null || this.configFactory == null);
    }

    @Override
    protected synchronized boolean doActualSend(String topic, DataPacket dp) {
        try {
            BytesMessage msg = session.createBytesMessage();
            msg.writeBytes(dp.toBytes());
            msg.setStringProperty(
                    CLIENT_ID_HEADER_KEY,
                    configFactory.getMainConfiguration(null).getProperty(configFactory.getClientIdKey())
            );
            this.producer.send(this.session.createTopic(topic), msg);
        } catch (JMSException ex) {
            ex.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    @Override
    protected boolean doPreReceiveChecks() {
        return !(this.consumer == null || this.configFactory == null);
    }

    @Override
    protected List<DataPacket> doActualReceive(long timeout) {
        List<byte[]> consumerRecords = null;
        try {
            // the filtering for data from the same source is done in the consumer
            consumerRecords = this.consumer.receive((int)timeout);
        } catch (JMSException we) {
            we.printStackTrace(System.err);
        }

        if (consumerRecords == null || consumerRecords.isEmpty()) return null;

        List<DataPacket> result = new ArrayList<>(consumerRecords.size());

        for (byte[] cr : consumerRecords) {
            try {
                DataPacket dp = new DataPacket(cr);
                result.add(dp);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        if (result.isEmpty()) return null;
        return result;
    }

    @Override
    public List<IEvent> receive() {
        return this.receive(250);
    }

    @Override
    public void close() {
        if (this.producer != null) {
            try {
                this.producer.close();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            this.producer = null;
        }
        if (this.consumer != null) {
            this.consumer.close();
            this.consumer = null;
        }
        if (this.session != null) {
            try {
                this.session.close();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            this.session = null;
        }
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            this.connection = null;
        }
    }
}
