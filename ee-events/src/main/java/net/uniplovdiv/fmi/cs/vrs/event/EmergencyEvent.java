package net.uniplovdiv.fmi.cs.vrs.event;


/**
 * Emergence type event basis. Emergence events by default have a higher priority (-1).
 * Any inheritors should override hashCode() and equals() methods if define additional embedded parameters.
 */
public class EmergencyEvent extends DomainEvent {
    private static final long serialVersionUID = 3734983523888272118L;

    /**
     * Constructor.
     */
    public EmergencyEvent() {
        super();
        this.priority = -1;
    }

    /**
     * Copy constructor. The priority of the event will be adjusted accordingly.
     * @param e The event from which to create a new copy instance.
     */
    public EmergencyEvent(DomainEvent e) {
        super(e);
        if (!(e instanceof DomainEvent)) { // just in case
            this.priority = -1;
        }
    }

    /**
     * Copy constructor. The priority of the event will be adjusted accordingly.
     * @param e The event from which to create a new copy instance.
     */
    public EmergencyEvent(Event e) {
        super(e);
        if (!(e instanceof EmergencyEvent)) { // just in case
            this.priority = -1;
        }
    }

    /**
     * Copy constructor.
     * @param e The event from which to create a new copy instance.
     */
    public EmergencyEvent(EmergencyEvent e) {
        super(e);
    }

    /**
     * Constructor.
     * @param id A unique event identifier.
     * @param timestampMs The time occurrence in milliseconds resolution when the events has been created.
     */
    public EmergencyEvent(long id, long timestampMs) {
        super(id, timestampMs);
        this.priority = -1;
    }

    /**
     * Constructor.
     * @param id A unique event identifier.
     * @param timestampMs The time occurrence in milliseconds resolution when the events has been created.
     * @param priority The priority of the event. The lower number means higher priority and vice versa. In the current
     *                 case the priority should be lower than 0 (for e.g. -1).
     */
    public EmergencyEvent(long id, long timestampMs, int priority) {
        super(id, timestampMs, priority);
    }

    @Override
    public String getCategory() {
        return "emergency-events";
    }
}