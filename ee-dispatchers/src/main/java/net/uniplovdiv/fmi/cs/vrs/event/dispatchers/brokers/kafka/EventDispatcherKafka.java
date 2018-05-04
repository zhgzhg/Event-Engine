package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.kafka;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.AbstractBrokerConfigFactory;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.AbstractEventDispatcher;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers.DispatchingType;
import net.uniplovdiv.fmi.cs.vrs.event.dispatchers.encapsulation.DataPacket;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;


/**
 * Dispatches IEvent instances using Apache Kafka.
 */
public class EventDispatcherKafka extends AbstractEventDispatcher {
    protected static final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(
            Math.min(6, Runtime.getRuntime().availableProcessors()),
            new BasicThreadFactory.Builder()
                    .namingPattern("event-dispatcher-kafka-%d")
                    .daemon(true)
                    .priority(Thread.MAX_PRIORITY)
                    .build()
    );

    protected ConfigurationFactoryKafka configFactoryConsumer;
    protected ConfigurationFactoryKafka configFactoryProducer;

    protected KafkaConsumer<String, DataPacket> consumer;
    protected KafkaProducer<String, DataPacket> producer;

    // create meta header used when kafka sends messages to identify who is the sender
    protected final Header clientIdHeader;

    /**
     * Constructor - the most complete one.
     * @param config The configuration settings that also include Kafka configuration options. Cannot be null.
     * @param latestEventsRememberCapacity The capacity of remembered latest events worked with. Must be 0 or a positive
     *                                     number. Used to prevent same event duplications coming from different
     *                                     channels.
     * @param doNotReceiveEventsFromSameSource Events sent from the same source as the current one will be totally
     *                                         ignored. Differentiation is made by the identifier set in the config and
     *                                         the available meta information in the received Kafka records.
     *                                         This is a more strict limitation compared to latestEventsRememberCapacity
     *                                         since the last one is restricted to the limited amount of run-time data.
     * @param packagesWithEvents Array of full package names containing custom IEvent implementations. Useful during
     *                           event de/serialization to increase performance and prevent exceptions. Can be null.
     * @throws NullPointerException If config is null.
     * @throws IllegalArgumentException If latestEventsRememberCapacity is negative number.
     */
    public EventDispatcherKafka(ConfigurationFactoryKafka config, int latestEventsRememberCapacity,
                                boolean doNotReceiveEventsFromSameSource, String[] packagesWithEvents) {
        super(config, latestEventsRememberCapacity, doNotReceiveEventsFromSameSource, packagesWithEvents);
        this.clientIdHeader = new RecordHeader(
                CLIENT_ID_HEADER_KEY,
                config.getMainConfiguration(null).getProperty(config.getClientIdKey())
                        .getBytes(StandardCharsets.ISO_8859_1)
        );

        this.configFactoryProducer = this.configFactoryConsumer = null;
        DispatchingType dispatchingType = config.getDispatchingType();

        Properties props = config.getMainConfiguration(null);
        if (dispatchingType.equals(DispatchingType.PRODUCE)) {
            // PRODUCER only
            this.configFactoryProducer = new ConfigurationFactoryKafka(config);
            this.producer = new KafkaProducer<>(props);
        } else {
            // CONSUMER
            this.configFactoryConsumer = new ConfigurationFactoryKafka(config);
            this.consumer = new KafkaConsumer<>(props);
            this.consumer.subscribe(config.getTopics());
            if (dispatchingType.equals(DispatchingType.CONSUME_PRODUCE)) {
                // PRODUCER too
                this.configFactoryProducer = new ConfigurationFactoryKafka(config);
                this.producer = new KafkaProducer<>(props);
            }
        }
    }

    /**
     * Constructor.
     * @param config The configuration settings that also include Kafka configuration options. Cannot be null.
     *               The used latestEventsRememberCapacity is 15, doNotReceiveEventsFromSameSource is set to true and
     *               and no additional packages with events are specified.
     * @throws NullPointerException If config is null.
     */
    public EventDispatcherKafka(ConfigurationFactoryKafka config) {
        this(config, 15, true, null);
    }

    /**
     * Creates new instance with the same settings as of the current one.
     * @return New EventDispatcherKafka instance with effective configuration as the one of the current instance.
     */
    public EventDispatcherKafka makeNewWithSameConfig() {
        if (this.configFactoryProducer != null && this.configFactoryConsumer != null) {
            Properties props = this.configFactoryConsumer.getMainConfiguration(
                    this.configFactoryProducer.getMainConfiguration(null));
            ConfigurationFactoryKafka cfk = new ConfigurationFactoryKafka(
                    props,
                    this.configFactoryProducer.getDataEncodingMechanismType(),
                    DispatchingType.CONSUME_PRODUCE,
                    this.configFactoryConsumer.getTopics(),
                    this.configFactoryProducer.getTopicToEventsMap()
            );
            EventDispatcherKafka eventDispatcherKafka = new EventDispatcherKafka(cfk, this.latestEventsRememberCapacity,
                    this.doNotReceiveEventsFromSameSource, this.packagesWithEvents);
            if (eventDispatcherKafka.latestEventsSent != null) {
                eventDispatcherKafka.latestEventsSent.addAll(this.latestEventsSent);
            }
            if (eventDispatcherKafka.latestEventsReceived != null) {
                eventDispatcherKafka.latestEventsReceived.addAll(this.latestEventsReceived);
            }
            return eventDispatcherKafka;
        }
        if (this.configFactoryProducer != null) {
            EventDispatcherKafka eventDispatcherKafka = new EventDispatcherKafka(this.configFactoryProducer,
                    this.latestEventsRememberCapacity, this.doNotReceiveEventsFromSameSource, this.packagesWithEvents);
            if (eventDispatcherKafka.latestEventsSent != null) {
                eventDispatcherKafka.latestEventsSent.addAll(this.latestEventsSent);
            }
            return eventDispatcherKafka;
        }

        EventDispatcherKafka edk = new EventDispatcherKafka(this.configFactoryConsumer,
                this.latestEventsRememberCapacity, this.doNotReceiveEventsFromSameSource, this.packagesWithEvents);
        if (edk.latestEventsReceived != null) {
            edk.latestEventsReceived.addAll(this.latestEventsReceived);
        }
        return edk;
    }

    @Override
    protected AbstractBrokerConfigFactory retrieveConfig(DispatchingType dt) {
        switch (dt) {
            case CONSUME:
                return this.configFactoryConsumer;

            case PRODUCE:
                return this.configFactoryProducer;

            default:
                return null;
        }
    }

    @Override
    protected boolean doPreSendChecks() {
        return !(this.producer == null || this.configFactoryProducer == null);
    }

    @Override
    protected synchronized boolean doActualSend(String topic, DataPacket dp) {
        try {
            ProducerRecord<String, DataPacket> pr = new ProducerRecord<>(topic, null, dp);
            pr.headers().add(this.clientIdHeader);
            this.producer.send(pr).get(); // wait for the data to be sent
            if (this.latestEventsSent != null) this.latestEventsSent.add(dp.hashCode());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    @Override
    protected boolean doPreReceiveChecks() {
        return !(this.consumer == null || this.configFactoryConsumer == null);
    }

    /**
     * Executes a task within certain time or timeouts.
     * @param task The task to be executed.
     * @param timeoutAfter The time interval after a timeout to be resulted.
     * @param <T> The result type returned by this CompletableFuture's get method.
     * @return Instance of CompletableFuture that will execute within a certain time or timeout.
     */
    protected static <T> CompletableFuture<T> executeWithin(CompletableFuture<T> task, Duration timeoutAfter) {
        final CompletableFuture<T> timedOutTask =
                AbstractEventDispatcher.failAfter(timeoutAfter, EventDispatcherKafka.taskScheduler);
        return task.applyToEither(timedOutTask, Function.identity());
    }

    @Override
    protected List<DataPacket> doActualReceive(long timeout) {
        Callable<List<DataPacket>> task = () -> {
            ConsumerRecords<String, DataPacket> consumerRecords = null;
            try {
                // Timeout documentation is misleading. It's for any data in the buffer previously. The actual timeout
                // needs to be tweaked via request.timeout.ms, session.timeout.ms and fetch.max.wait.ms
                // Also poll() always blocks indefinitely if there's no connection.
                consumerRecords = consumer.poll(timeout);
            } catch (WakeupException we) {
                we.printStackTrace(System.err);
            }

            if (consumerRecords == null || consumerRecords.isEmpty()) return null;

            List<DataPacket> result = new ArrayList<>(consumerRecords.count());

            for (ConsumerRecord<String, DataPacket> cr : consumerRecords) {
                // prevent receiving data sent by us with more strict approach based on metadata inside the record
                if (this.doNotReceiveEventsFromSameSource) {
                    Header h = cr.headers().lastHeader(this.clientIdHeader.key());
                    if (h != null && h.value() != null
                            && Arrays.equals(h.value(), this.clientIdHeader.value())) {
                        continue;
                    }
                }
                result.add(cr.value());
            }

            return (result.isEmpty() ? null : result);
        };

        List<DataPacket> result = null;
        try {
            result = executeWithin(AbstractEventDispatcher.scheduleNow(task, EventDispatcherKafka.taskScheduler),
                    Duration.ofMillis(timeout * 2))
                    .exceptionally((t) -> {
                        System.err.println("Poll timeout reached! Interrupting the receiving of events...");
                        consumer.wakeup();
                        return null;
                    })
                    .get();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

        return result;
    }

    @Override
    public List<IEvent> receive() {
        return this.receive(650);
    }

    @Override
    public List<IEvent> receive(long timeout) {
        List<IEvent> result = super.receive(timeout);
        if (!(result == null || result.isEmpty())) {
            this.consumer.commitSync();
        }
        return result;
    }

    @Override
    public void close() {
        if (this.consumer != null) {
            this.consumer.close(60, TimeUnit.SECONDS);
            this.consumer = null;
        }
        if (this.producer != null) {
            this.producer.close(60, TimeUnit.SECONDS);
            this.producer = null;
        }
    }
}
