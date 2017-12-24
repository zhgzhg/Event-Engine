package net.uniplovdiv.fmi.cs.vrs.event.parameters;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.util.TreeMap;

/**
 * Implementation of IEventsContainer. Can be used to store events. Useful when subevents implementation is desired.
 */
public class EventsContainer extends TreeMap<Long, IEvent> implements IEventsContainer {
    private static final long serialVersionUID = 5167151805083093858L;

    /**
     * Constructor.
     */
    public EventsContainer() {
        super();
    }

    /**
     * Copy constructor.
     * @param ec The EventContainer from which to create a copy.
     */
    public EventsContainer(EventsContainer ec) {
        super(ec);
    }

    /**
     * Copy constructor.
     * @param ec The EventContainer from which to create a copy.
     */
    public EventsContainer(TreeMap<Long, IEvent> ec) { // this method is specially implemented in order to ease deserialization with gson library
        super(ec);
    }

    @Override
    public int compareTo(IEventsContainer o) {
        if (this == o) return 0;
        return 1;
    }
}
