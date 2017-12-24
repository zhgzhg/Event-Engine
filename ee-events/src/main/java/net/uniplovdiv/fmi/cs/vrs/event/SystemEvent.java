package net.uniplovdiv.fmi.cs.vrs.event;


/**
 * System type event basis. Any inheritors should override hashCode() and equals() methods if define additional
 * embedded parameters.
 */
public class SystemEvent extends Event {
    private static final long serialVersionUID = 2926275522197125279L;

    /**
     * Constructor.
     */
    public SystemEvent() {
        super();
    }

    /**
     * Constructor.
     * @param id A unique events identifier.
     * @param timestampMs The time occurrence in milliseconds resolution when the events has been created.
     */
    public SystemEvent(long id, long timestampMs) {
        super(id, timestampMs);
    }

    /**
     * Constructor.
     * @param id A unique event identifier.
     * @param timestampMs The time occurrence in milliseconds resolution when the events has been created.
     * @param priority The priority of the event. The lower number means higher priority and vice versa. In the current
     *                 case the priority should be lower than 0 (for e.g. -1).
     */
    public SystemEvent(long id, long timestampMs, int priority) {
        super(id, timestampMs, priority);
    }

    /**
     * Copy constructor.
     * @param e The event whose data to use to create the new instance.
     */
    public SystemEvent(Event e) {
        super(e);
    }

    @Override
    public String getCategory() {
        return "system-events";
    }
}