package net.uniplovdiv.fmi.cs.vrs.event.parameters;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.io.Serializable;
import java.util.SortedMap;

/**
 * Container for events. Can be used as a sub-event container in a particular event.
 */
public interface IEventsContainer extends SortedMap<Long, IEvent>, Comparable<IEventsContainer>, Serializable {
}
