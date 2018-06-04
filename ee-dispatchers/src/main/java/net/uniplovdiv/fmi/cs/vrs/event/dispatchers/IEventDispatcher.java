package net.uniplovdiv.fmi.cs.vrs.event.dispatchers;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Dispatcher interface for events.
 */
public interface IEventDispatcher {

    /**
     * Returns if the current dispatcher instance is connected or not. In case of mixed consumer-producer mode the
     * result will be true only if there's a connection for both modes. The result lag from the actual connectivity
     * status!
     * @return True if there is an active connection, otherwise false.
     */
    boolean isConnected();

    /**
     * Sends an event to any subscribed receivers. This is usually done in a synchronous manner.
     * @param event The event to be sent. Must not be null.
     * @return True on success otherwise false.
     */
    boolean send(IEvent event);

    /**
     * Sends an event to any subscribed receivers. This is usually done in asynchronous manner.
     * @param event The event to be sent. Must not be null.
     * @param onCompletion A BiConsumer accepting as first parameter Boolean value indicating whether the sending
     *                     succeeded and a second parameter the event instance for which the operation has completed.
     *                     This parameter can be null in which case the method will still be executed asynchronously.
     */
    void send(IEvent event, BiConsumer<Boolean, IEvent> onCompletion);

    /**
     * Receives new events to which the dispatcher has been subscribed.
     * @return Nonempty initialized list of events on success. Null or an empty list if no events were received.
     */
    List<IEvent> receive();

    /**
     * Receives new events to which the dispatcher has been subscribed.
     * @param timeout The time to wait before giving up on receiving data.
     * @return Nonempty initialized list of events on success. Null or an empty list if no events were received.
     */
    List<IEvent> receive(long timeout);

    /**
     * Closes any opened dispatcher connections making it unable to send or receive anymore data. Calling this method is
     * recommended and sometimes even mandatory in order the application to be able to exit.
     */
    void close();
}
