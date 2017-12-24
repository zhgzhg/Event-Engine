package net.uniplovdiv.fmi.cs.vrs.event.dispatchers;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.util.List;

/**
 * Dispatcher interface for events.
 */
public interface IEventDispatcher {
    /**
     * Sends an event to any subscribed receivers.
     * @param event The event to be sent. Must not be null.
     * @return True on success otherwise false.
     */
    boolean send(IEvent event);

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

    //boolean hasAny();
}
