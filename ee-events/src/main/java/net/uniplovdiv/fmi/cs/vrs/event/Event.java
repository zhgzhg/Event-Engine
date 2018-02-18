package net.uniplovdiv.fmi.cs.vrs.event;

import net.uniplovdiv.fmi.cs.vrs.event.annotations.EmbeddedParameter;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ComparableArrayList;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.EventsContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.IEventsContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ParametersContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.IParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParametersComparisonResult;
import net.uniplovdiv.fmi.cs.vrs.event.location.EventLocation;

import org.apache.commons.lang3.ClassUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;


/**
 * General event type basis. For any new event types it is recommended to inherit somehow this class.
 * Any inheritors should override hashCode() and equals() methods if define additional embedded parameters.
 */
public class Event implements IEvent, Serializable {
    private static final long serialVersionUID = -71441414431666125L;
    private static volatile long eventIdAccumulator = 0;

    /*
     * A string with the human-readable name of the particular IEvent class. Useful for de/serialization of events
     * from/in a format that is not the standard Java one (for e.g. JSON).
     */
    //public final String __event_type_class_name;

    /**
     * Constants of human-readable, uniquely identified embedded parameter names, that can be used as keys in some
     * container structures.
     */
    public abstract class ParamNames {
        /**
         * Embedded parameter name for event's id.
         */
        public static final String ID = "event_id";

        /**
         * Embedded parameter name for event's timestamp.
         */
        public static final String TIMESTAMP = "event_timestamp_ms";

        /**
         * Embedded parameter name for event's priority.
         */
        public static final String PRIORITY = "event_priority";

        /**
         * Embedded parameter name for event's location.
         */
        public static final String LOCATION = "event_location";

        /**
         * Embedded parameter name for event's description.
         */
        public static final String DESCRIPTION = "event_description";

        /**
         * Embedded parameter name for event's sub-events.
         */
        public static final String SUBEVENTS = "event_subevents";
    }

    /**
     * Unique numeric identifier of the event. Usually is generated automatically when the a new instance of the
     * {@link Event} class has been created.
     */
    @EmbeddedParameter(ParamNames.ID)
    protected long id;

    /**
     * Unix epoch timestamp with milliseconds resolution of the time when the event has occurred
     * (instance been created).
     */
    @EmbeddedParameter(ParamNames.TIMESTAMP)
    protected long timestampMs;

    /**
     * Priority of the event. Used for sorting (compareTo) purposes. The lower number means higher priority. The higher
     * number makes the priority lower.
     */
    @EmbeddedParameter(ParamNames.PRIORITY)
    protected int priority;

    /**
     * The location of occurred event.
     */
    @EmbeddedParameter(ParamNames.LOCATION)
    protected EventLocation eventLocation;

    /**
     * Description of the event bringing some additional information in text form.
     */
    @EmbeddedParameter(ParamNames.DESCRIPTION)
    protected String description;

    /**
     * Stores all dynamic parameters added to the event during run-time execution.
     */
    protected ParametersContainer dynamicParameters;

    /**
     * Stores any subevents that are intended to be part of the current event (as its parameters).
     */
    @EmbeddedParameter(value = ParamNames.SUBEVENTS, wrapper = EventsContainer.class)
    protected IEventsContainer subEvents;

    /**
     * Constructor.
     */
    public Event() {
        //this.__event_type_class_name = this.getClass().getCanonicalName();
        this.priority = 0;
        this.description = "";
        this.dynamicParameters = constructDynamicParamsStruct();
        this.eventLocation = new EventLocation();
        this.subEvents = constructSubEventsStructure();
    }

    /**
     * Copy constructor.
     * @param e The event from which to create a new copy instance.
     */
    public Event(Event e) {
        this();
        if (e == null) return;
        setId(e.id);
        this.timestampMs = e.timestampMs;
        this.priority = e.priority;
        this.description = e.description;
        this.dynamicParameters = constructDynamicParamsStruct(e.dynamicParameters);
        this.eventLocation = new EventLocation(e.eventLocation);
        this.subEvents = constructSubEventsStructure(e.subEvents);
    }

    /**
     * Instantiates dynamicParameters structure.
     * @return New empty structure.
     */
    private ParametersContainer constructDynamicParamsStruct() {
        return new ParametersContainer();
    }

    /**
     * Instantiates new dynamicParameters structure as a copy from an existing one.
     * @param p The instance whose data will be copied into the returned new instance.
     * @return New empty structure.
     */
    private ParametersContainer constructDynamicParamsStruct(ParametersContainer p) {
        if (p == null) return new ParametersContainer();
        return new ParametersContainer(p);
    }

    /**
     * Instantiates subevents structure.
     * @return New empty structure.
     */
    private IEventsContainer constructSubEventsStructure() {
        //return Collections.synchronizedSortedMap(new TreeMap<Long, IEvent>());
        return new EventsContainer();
    }

    /**
     * Instantiates subevents structure as a copy from an existing one.
     * @param se The instance whose data will be copied into the returned new instance.
     * @return New (possibly empty) structure.
     */
    private IEventsContainer constructSubEventsStructure(IEventsContainer se) {
        if (se == null) return constructSubEventsStructure();
        //return Collections.synchronizedSortedMap(new TreeMap<>(se));
        return new EventsContainer((EventsContainer)se);
    }

    /**
     * Constructor.
     * @param id A unique event identifier.
     * @param timestampMs The time occurrence in milliseconds resolution when the events has been created.
     */
    public Event(long id, long timestampMs) {
        this();
        setId(id);
        this.timestampMs = timestampMs;
    }

    /**
     * Constructor.
     * @param id A unique event identifier.
     * @param timestampMs The time occurrence in milliseconds resolution when the events has been created.
     * @param priority The priority of the event. The lower number means higher priority and vice versa.
     */
    public Event(long id, long timestampMs, int priority) {
        this(id, timestampMs);
        this.priority = priority;
    }

    /**
     * Adjusts the internal accumulator of the id generator. Use this method cautiously since it will affect globally
     * any generated after that events, so there is a risk of id collisions. Some affected methods will be
     * makeInstance() and reserveNewId().
     * This method is useful if any previously processed events are stored somewhere (for e.g. database) including their
     * ids and you want to resume after application restart the id generation from the last n+1 id.
     * @param newStartingValue The identifier value to use as a new basis for generation or reserving ids of events. The
     *                         specified value will be used as a first id value for the next id generation operation.                         operation.
     */
    public static synchronized void adjustIdentifierAccumulator(long newStartingValue) {
        eventIdAccumulator = newStartingValue;
    }

    /**
     * Generates a new unique for the class and its inheritors events id.
     * @return The new events id.
     */
    protected static synchronized long generateEventId() {
        return eventIdAccumulator++;
    }

    /**
     * Generates a new unique Event which is guaranteed to be unique relative to any previous instances created using
     * this method.
     * @param <T> Generic data type that must be or inherit the Event class.
     * @param _for The class for which the instance to be created. It must inherit Event class.
     * @param callers The instances wrapping the target class that are required in order to be initialized.
     *                They must follow from the most top one to the most inner one.
     *                Not nested or static classes do not require any.
     * @return New unique Event relative to the others created with via this method or null in case of failure.
     */
    public static <T extends Event> T makeInstance(Class<T> _for, Object... callers) {
        T t = null;
        if (_for == null) return null;

        Class<?>[] arguments = null;
        if (callers != null && callers.length > 0) {
            arguments = new Class<?>[callers.length];
            for (int i = 0; i < callers.length; ++i) {
                arguments[i] = (callers[i] != null ? callers[i].getClass() : null );
            }
        }

        Constructor<T> ctor;
        try {
            ctor = _for.getConstructor(arguments);
            t = ctor.newInstance(callers);
        } catch (Throwable ex) {
            try {
                ctor = _for.getDeclaredConstructor(arguments);
                ctor.setAccessible(true);
                t = ctor.newInstance(callers);
            } catch (Throwable ex2) {
                ex.printStackTrace(System.err);
                System.err.println("-----------------------");
                ex2.printStackTrace(System.err);
            }
        }

        if (t != null) {
            try {
                t.setId(generateEventId());
                t.setTimestampMs(System.currentTimeMillis());
            } catch (Throwable ex) {
                ex.printStackTrace(System.err);
            }
        }
        return t;
    }

    /* TODO - thing whether it should even exist. look at http://openjdk.java.net/jeps/259 https://stackoverflow.com/questions/1696551/how-to-get-the-name-of-the-calling-class-in-java
     * @param automaticCallerDetermination If set to true the callers argument will be ignored and the library will
     *                                     automatically try to determine the needed information. On Java 8 and older
     *                                     versions there can be a performance penalty.
     *
     * Generates a new unique Event which is guaranteed to be unique relative to any previous instances created using
     * this method. If the class happens to be nested and is not a static one this method will try to automatically
     * find the needed instances in order to instantiate the class. On Java 8 and older versions there can be a
     * performance penalty.
     * @param <T> Generic data type that must be or inherit the Event class.
     * @param _for The class for which the instance to be created. It must inherit Event class.
     * @return New unique Event relative to the others created with via this method or null in case of failure.
     *
    public static <T extends Event> T makeInstanceWithDeepLook(Class<T> _for) {
    }*/

    /**
     * Retrieves all fields of concrete IEvent instance class and those above in the hierarchy.
     * @param event The event instance for which the fields to be retrieved.
     * @return Stack of Field arrays where at the top will be those that belong to the most senior class.
     * @throws NullPointerException If event param is null.
     */
    private static Stack<Field[]> getAllIEventFields(IEvent event) {
        Stack<Field[]> allFields = new Stack<>();
        Class c = event.getClass();
        do {
            Field[] f = c.getDeclaredFields();
            if (f != null && f.length > 0) {
                allFields.push(f);
            }
            c = c.getSuperclass();
        } while (c != null);
        return allFields;
    }

    /**
     * Walks recursively the current and the nested inside events. Applies an "operation" function to them. Optionally
     * the returned result from the operation can be further manipulated and even accumulated using a result manipulator
     * function.
     * Note 1 - the "operation" and "resultManipulator" functions can receive null values so they must be handled
     * accordingly!
     * Note 2 - there is no guarantee for the iteration order of the events and those nested within them!
     *
     * @param inside The event instance from which the walking to begin.
     * @param extraArg Any additional argument to be passed to the "operation" function.
     * @param operation A function to be applied to every single event. The signature of function needs to be:
     *                  any_return_type (IEvent, any_argument_type)
     * @param resultManipulator A function that will be applied after "operation". Can be null. Useful to transform the
     *                          current result. Its signature needs to be: any_return_type (any_return_type)
     * @param <U> Additional parameter passed to the "operation" function.
     * @param <R> The return type of the "operation" function.
     *
     * @throws IllegalAccessException In case that a particular event field cannot be reached.
     * @return The final R value produced after walking all of the available events.
     */
    public static <U, R> R walkEventsInside(IEvent inside, U extraArg, BiFunction<IEvent, U, R> operation,
                                            UnaryOperator<R> resultManipulator) throws IllegalAccessException {
        R result = operation.apply(inside, extraArg);
        if (resultManipulator != null) {
            result = resultManipulator.apply(result);
        }
        if (inside == null) return result;

        IEventsContainer subEvents = inside.getSubEvents();
        if (subEvents != null && !subEvents.isEmpty()) {
            for (IEvent subEvent : subEvents.values()) {
                result = walkEventsInside(subEvent, extraArg, operation, resultManipulator);
            }
        }

        Stack<Field[]> allFields = getAllIEventFields(inside);

        Class<? extends Annotation> targetAnnotation = EmbeddedParameter.class;
        Class<?> ieventClass = IEvent.class;

        while (!allFields.empty()) {
            for (Field f : allFields.pop()) {

                if (f.isAnnotationPresent(targetAnnotation)) {
                    EmbeddedParameter ep = (EmbeddedParameter) f.getAnnotation(targetAnnotation);
                    Class<?> dataType = f.getType();
                    if (!ieventClass.isAssignableFrom(dataType)) continue;
                    f.setAccessible(true);
                    IEvent embeddedEvent = (IEvent) f.get(inside);
                    result = walkEventsInside(embeddedEvent, extraArg, operation, resultManipulator);

                    if (embeddedEvent != null) {
                        subEvents = embeddedEvent.getSubEvents();
                        if (subEvents != null && !subEvents.isEmpty()) {
                            for (IEvent subEvent : subEvents.values()) {
                                result = walkEventsInside(subEvent, extraArg, operation, resultManipulator);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Reserves a new, unique identifier for a given event and optionally for any nested subevents. This method is
     * useful to "adjust" the identifiers of events that come from outside and thus the id generation logic is based on
     * an outside algorithm and unknown state. This method accounts for those differences and prevents identifier
     * collisions.
     * @param event The event for which the id reserving and adjusting to be performed.
     * @param recursive Indicates whether to perform the process in a recursive manner so any nested events inside will
     *                  be affected too. If cleared, this flag significantly reduces the execution time.
     * @throws IllegalStateException If adjustment of any subevents causes an infinite loop.
     * @throws IllegalAccessException In case that a particular event cannot be accessed.
     */
    public static void reserveNewId(IEvent event, boolean recursive) throws IllegalAccessException {
        if (event == null) return;
        if (!recursive) {
            event.setId(generateEventId());
            return;
        }

        // Collect the events into flat structures

        IEventsContainer allGatheredEventsFlat = new EventsContainer();
        List<IEventsContainer> allSubeventsContainers = new ArrayList<>();

        walkEventsInside(event, null, (IEvent theEvent, Void extraArg) -> {
            if (theEvent != null) {
                allGatheredEventsFlat.put(theEvent.getId(), theEvent);
                IEventsContainer subEvents = theEvent.getSubEvents();
                if (subEvents != null) {
                    allSubeventsContainers.add(subEvents);
                }
            }
            return null;
        }, null);

        // Regenerate the id of all events
        for (IEvent ec : allGatheredEventsFlat.values()) {
            ec.setId(generateEventId());
        }

        // Change the keys for all dynamic events
        for (IEventsContainer ec : allSubeventsContainers) {
            TreeSet<Long> keysToAdjust = new TreeSet<>();
            for (Map.Entry<Long, IEvent> ent : ec.entrySet()) {
                if (!ent.getKey().equals(ent.getValue().getId())) {
                    keysToAdjust.add(ent.getKey());
                }
            }

            for (Iterator<Long> it = keysToAdjust.descendingIterator(); it.hasNext(); ) {
                Long k = it.next();
                IEvent ev = ec.remove(k);
                int i = 0;
                while (ev != null && i < 15) {
                    IEvent putEv = ec.put(ev.getId(), ev);
                    if (putEv != ev && putEv != null) {
                        ev = putEv;
                    } else {
                        ev = null;
                    }
                    ++i;
                }
                if (i == 15) {
                    throw new IllegalStateException("Infinite loop while adjusting event's subevents keys. "
                            + "Problematic event id " + (ev != null ? ev.getId() : "- null event"));
                }
            }
        }

    }

    /**
     * Recursively reserves a new, unique identifier for a given event and any nested subevents. This method is useful
     * to "adjust" the identifiers of events that come from outside and thus the id generation logic is based on an
     * outside algorithm and unknown state. This method accounts for those differences and prevents identifier
     * collisions.
     * @param event The event for which the id reserving and adjusting to be performed.
     * @throws IllegalStateException If adjustment of any subevents causes an infinite loop.
     * @throws IllegalAccessException In case that a particular event cannot be accessed.
     */
    public static void reserveNewId(IEvent event) throws IllegalAccessException {
        reserveNewId(event, true);
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getTimestampMs() {
        return this.timestampMs;
    }

    @Override
    public void setTimestampMs(long timestampMs) {
        this.timestampMs = timestampMs;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ParametersContainer getDynamicParameters() {
        return this.dynamicParameters;
    }

    @Override
    public void setDynamicParameters(ParametersContainer parameters) {
        this.dynamicParameters = parameters;
    }

    @Override
    public EventLocation getEventLocation() {
        return this.eventLocation;
    }

    @Override
    public void setEventLocation(EventLocation eventLocation) {
        this.eventLocation = eventLocation;
    }

    @Override
    public IEventsContainer getSubEvents() {
        return this.subEvents;
    }

    @Override
    public void setSubEvents(IEventsContainer subEvents) {
        this.subEvents = subEvents;
    }

    /**
     * Check if this events equals another object.
     * @param obj The object to check if it's equal to the current one.
     * @return True if equal or false if are not equal of are from a different data type.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof IEvent && (this.getClass() == obj.getClass())) {
            IEvent _obj = (IEvent)obj;
            return (id == _obj.getId() && timestampMs == _obj.getTimestampMs() && priority == _obj.getPriority()
                    && IEvent.safeEquals(description, _obj.getDescription())
                    && IEvent.safeEquals(eventLocation,_obj.getEventLocation())
                    && IEvent.safeEquals(dynamicParameters, _obj.getDynamicParameters())
                    && IEvent.safeEquals(subEvents, _obj.getSubEvents()));
        }
        return false;
    }

    /**
     * Calculates a hash code for events.
     * @return The hash code of the current events instance.
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + getClass().getName().hashCode();
        result = 31 * result + (int)(id ^ (id >>> 32));
        result = 31 * result + (int)(timestampMs ^ (timestampMs >>> 32));
        result = 31 * result + priority;
        result = 31 * result + ((description != null) ? description.hashCode() : 0);
        result = 31 * result + ((dynamicParameters != null) ? dynamicParameters.hashCode() : 0);
        result = 31 * result + ((eventLocation != null) ? eventLocation.hashCode() : 0);
        result = 31 * result + ((subEvents != null) ? subEvents.hashCode() : 0);
        return result;
    }

    /**
     * Compares the current event to another one based on the priority, or in case of equality the time when they have
     * occurred or based on the event identifiers in case of time equality. This is the exact exact evaluation sequence
     * performed during event comparison.
     * @param event The events to which to compare the current one.
     * @return In the current one has happened earlier -1, later 1 or 0 if both events occurred at the same time.
     * @throws NullPointerException Thrown if events parameter is null.
     */
    @Override
    public int compareTo(IEvent event) throws NullPointerException {
        if (event != null) {
            if (this.priority < event.getPriority()) {
                return -1;
            } else if (this.priority > event.getPriority()) {
                return 1;
            } else if (this.timestampMs < event.getTimestampMs()) {
                return -1;
            } else if (this.timestampMs > event.getTimestampMs()) {
                return 1;
            } else if (this.id < event.getId()) {
                return -1;
            } else {
                return (this.id > event.getId() ? 1 : 0);
            }
        } else {
            throw new NullPointerException("Supplied null event for comparison!");
        }
    }

    /**
     * Searches for specific value using initialized iterator and when it's found removes it from the collection.
     * @param it The iterator that will be searched.
     * @param forValue The value that is searched for.
     * @param <T> Generic type of the element/value that is searched for and returned.
     * @return Returns the element containing the value or null if nothing is found.
     */
    private static <T> T destructiveSearchIterator(Iterator<T> it, T forValue) {
        T result = null;
        while (it.hasNext()) {
            T v = it.next();
            if (v == forValue || v.equals(forValue)) {
                result = v;
                it.remove();
                break;
            }
        }
        return result;
    }

     /**
      * Searches for specific value inside a Set and when it's found removes it from the collection.
      * @param s The Set that will be searched for.
      * @param relation The container of the objects whose keys are inside 's'.
      * @param forValueContainer The container whose value will be searched for.
      * @param currentValueRetriever The function that will be used to retrieve the value from "forValueContainer" and
      *                              from "relation" in order to be compared during the search.
      * @param <V> Generic data type of the value that's being searched for.
      * @return The value that was found or null.
      */
    private static <V> Long destructiveSearchSet(Set<Long> s, IEventsContainer relation, IEvent forValueContainer,
                                                      Function<IEvent, V> currentValueRetriever) {
        Long result = null;
        List<Long> resultCandidates = new LinkedList<>();

        Iterator<Long> it = s.iterator();

        while (it.hasNext()) {
            Long v = it.next();

            V _src = currentValueRetriever.apply(forValueContainer);
            V _rel = currentValueRetriever.apply(relation.get(v));

            if (_src.equals(_rel)) {
                resultCandidates.add(v);
                break;
            }
        }

        if (resultCandidates.size() > 0) {
            // try to chose 1 from the candidates by class object type match.
            for (Long v : resultCandidates) {
                if (forValueContainer.getClass().equals(relation.get(v).getClass())) {
                    result = v;
                    break;
                }
            }

            // if match via object class type failed then just pick the first candidate
            if (result == null) {
                result = resultCandidates.get(0);
            }
            s.remove(result); // the "destructive" part
        }

        return result;
    }

    /**
     * Asymmetrically compare symmetric subevents as "inner" part of a concrete events. The comparison is based on their
     * parameters. It is not historical comparison.
     *
     * During the comparison of more than 1 sub-event, a 1:1 mapping is attempted to be performed using the event
     * identifier, or if the former one fails the timestamp, or if the former one fails the event description or as a
     * last resort direct mapping by insertion order.
     *
     * When timestamp or event description mapping produce more than 1 candidate with an additional filtering will be
     * picked that one whose event class type matches the one coming from the "source" parameter. If no such match is
     * found then the first available candidate will be chosen for the mapping.
     *
     * @param source The base parameter (comparison template).
     * @param relation The comparison "according to" parameter.
     * @return Results with the comparison results.
     */
    private static IParameterComparisonOutcome compareSubEventsParameters(IEventsContainer source,
                                                                          IEventsContainer relation)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (source.size() == 0 && relation.size() == 0) {
            return ParameterComparisonOutcome.EQUAL;
        }
        if (source.size() == 0 || relation.size() == 0) {
            if (source.size() > 0) {
                return ParameterComparisonOutcome.NOTCOMPARED;
            } else {
                return ParameterComparisonOutcome.UNKNOWN;
            }
        }

        ParametersComparisonResult result = new ParametersComparisonResult();

        LinkedHashSet<Long> srcKeys = new LinkedHashSet<>(source.keySet());
        LinkedHashSet<Long> relKeys = new LinkedHashSet<>(relation.keySet());

        Iterator<Long> srcKeysIter = srcKeys.iterator();
        while (srcKeysIter.hasNext()) {
            final Long k = srcKeysIter.next();

            // maps directly by object or by Long value equality
            Long ek = destructiveSearchIterator(relKeys.iterator(), k);

            // Try to map by timestamp.
            if (ek == null) {
                ek = destructiveSearchSet(relKeys, relation, source.get(k), IEvent::getTimestampMs);

                // Try to map by event description.
                if (ek == null) {
                    ek = destructiveSearchSet(relKeys, relation, source.get(k), IEvent::getDescription);

                    // Map directly by insertion order
                    if (ek == null) {
                        Iterator<Long> relKeysIterator = relKeys.iterator();
                        if (relKeysIterator.hasNext()) {
                            ek = relKeysIterator.next();
                            relKeysIterator.remove();
                        }
                    }
                }
            }

            // The corresponding event key in the relation cannot be found. This results NOTCOMPARED.
            if (ek == null) { // handle impossible comparison case
                result.put(k.toString(), ParameterComparisonOutcome.NOTCOMPARED);
            } else {
                result.put(k.toString(), compareParameters(source.get(k).getWithEmbeddedParameters(),
                        relation.get(ek).getWithEmbeddedParameters()));
            }
        }

        // For the remaining in relation events who were not compared to the source,
        // because they are more in count flag them as UNKNOWN comparison.
        for (Iterator<Long> it = relKeys.iterator(); it.hasNext(); ) {
            result.put(it.next().toString(), ParameterComparisonOutcome.UNKNOWN);
        }

        return result;
    }

    /**
     * Asymmetrically compares dynamicParameters using compareTo method.
     * @param source The base parameter (comparison template).
     * @param relation The comparison "according to" parameter.
     * @return Container with the comparison results.
     * @throws NullPointerException If empty source or relation are provided.
     */
    @SuppressWarnings("unchecked")
    public static ParametersComparisonResult compareParameters(ParametersContainer source,
                                                                ParametersContainer relation) {
        ParametersComparisonResult result = new ParametersComparisonResult();
        for (String keyParam : source.keySet()) {
            if (relation.containsKey(keyParam)) {
                result.put(keyParam, ParameterComparisonOutcome.INCOMPARABLE);

                // handle null values as well as the same instances
                if (source.get(keyParam) == relation.get(keyParam)) {
                    result.put(keyParam, ParameterComparisonOutcome.EQUAL);
                    continue;
                }

                try {
                    boolean nestedParamComparisonCandidate1 = (source.get(keyParam) instanceof ParametersContainer);
                    boolean nestedParamComparisonCandidate2 = (relation.get(keyParam) instanceof ParametersContainer);
                    if (nestedParamComparisonCandidate1 || nestedParamComparisonCandidate2) {
                        // special comparison on the nested inside ParametersContainer types shall be done
                        // if both parameters are of the same "nested" type
                        if (nestedParamComparisonCandidate1 && nestedParamComparisonCandidate2) {
                            ParametersContainer srcPar = (ParametersContainer) source.get(keyParam);
                            ParametersContainer relPar = (ParametersContainer) relation.get(keyParam);
                            result.put(keyParam, compareParameters(srcPar, relPar));
                        } else {
                            result.put(keyParam, ParameterComparisonOutcome.INCOMPARABLE);
                        }
                        continue;
                    }

                    /*Object rawSourceParam = source.get(keyParam);
                    Object rawRelationParam = relation.get(keyParam);

                    if (rawSourceParam == null || rawRelationParam == null) {
                        continue; // the result is INCOMPARABLE since at this point one of those is null
                    }*/

                    Comparable<Object> sourceParam = (Comparable<Object>) source.get(keyParam);
                    Comparable<Object> relationParam = (Comparable<Object>) relation.get(keyParam);

                    if (sourceParam == null || relationParam == null) {
                        continue; // the result is INCOMPARABLE since at this point one of those is null
                    }

                    int comparisonResult = sourceParam.compareTo(relationParam);
                    result.put(keyParam, ParameterComparisonOutcome.fromInteger(comparisonResult));

                    if (comparisonResult != 0) {
                        boolean nestedComparisonCandidate1 = (source.get(keyParam) instanceof IEventsContainer);
                        boolean nestedComparisonCandidate2 = (relation.get(keyParam) instanceof IEventsContainer);

                        if (nestedComparisonCandidate1 || nestedComparisonCandidate2) {
                            // special comparison on the nested inside types shall be done
                            // if both parameters are of the same "nested" type
                            if (nestedComparisonCandidate1 && nestedComparisonCandidate2) {
                                IEventsContainer srcPar = (IEventsContainer) source.get(keyParam);
                                IEventsContainer relPar = (IEventsContainer) relation.get(keyParam);
                                result.put(keyParam, compareSubEventsParameters(srcPar, relPar));
                            } else {
                                result.put(keyParam, ParameterComparisonOutcome.INCOMPARABLE);
                            }
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("Comparison exception " + t.getMessage() + " for " + keyParam);
                    t.printStackTrace(System.err);
                }
            } else {
                // the relation does not contain the source parameter, but the original has it, so
                // the Origin CANNOT be compared to the Relation
                result.put(keyParam, ParameterComparisonOutcome.NOTCOMPARED);
            }
        }

        for (String keyParamRelation : relation.keySet()) {
            if (!source.containsKey(keyParamRelation)) {
                // the Relation has a parameter that is NOT present in the Origin
                // BUT we compare Origin to Relation, so the comparison result is UNKNOWN
                result.put(keyParamRelation, ParameterComparisonOutcome.UNKNOWN);
            }
        }
        return result;
    }

    @Override
    public synchronized ParametersComparisonResult compareParametersTo(ParametersContainer to) throws
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return compareParameters(this.getWithEmbeddedParameters(), to);
    }

    @Override
    public synchronized ParametersComparisonResult compareParametersTo(IEvent to) throws
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return compareParametersTo(to.getWithEmbeddedParameters());
    }


    /**
     * Converts a multidimensional array of any data to a flat List of objects. If the array consists of a primitive
     * types their boxed analogues will be put inside the resulting list.
     * @param array The array that we want to convert.
     * @param resultingList The resulting List that will be filled. It must be initialized by the programmer.
     * @throws NoSuchMethodException During unsuccessful attempt to create a boxed type of a primitive one.
     * @throws IllegalAccessException During unsuccessful attempt to obtain information about the array or its specific
     * element.
     * @throws InstantiationException During unsuccessful attempt to create a boxed type of a primitive one.
     * @throws InvocationTargetException During unsuccessful attempt to create a boxed type of a primitive one.
     */
    private void multidimArrayToFlatList(Object array, List<Object> resultingList) throws
            NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (array == null) return;

        int size = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < size; i++) {
            Object value = java.lang.reflect.Array.get(array, i);
            Class<?> valueType = value.getClass();
            if (valueType.isArray()) {
                multidimArrayToFlatList(value, resultingList);
            } else { // not an array; process it
                if (!valueType.isPrimitive()) {
                    resultingList.add(value);
                } else {
                    resultingList.add(ClassUtils.primitiveToWrapper(valueType).getConstructor(valueType)
                            .newInstance(value));
                }
            }
        }
    }


    @Override
    public ParametersContainer getWithEmbeddedParameters() throws NoSuchMethodException, IllegalAccessException,
            InstantiationException, InvocationTargetException {

        ParametersContainer pc = this.getEmbeddedParameters();
        if (pc == null) {
            pc = new ParametersContainer();
        }
        for (Map.Entry<String, Object> ee : this.dynamicParameters.entrySet()) {
            if (!pc.containsKey(ee.getKey())) {
                pc.put(ee.getKey(), ee.getValue());
            }
        }
        return pc;
    }


    @Override
    public ParametersContainer getEmbeddedParameters() throws NoSuchMethodException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        Stack<Field[]> allFields = getAllIEventFields(this);

        ParametersContainer pc = new ParametersContainer();
        Class<? extends Annotation> targetAnnotation = EmbeddedParameter.class;

        while (!allFields.empty()) {
            for (Field f : allFields.pop()) {

                if (f.isAnnotationPresent(targetAnnotation)) {
                    EmbeddedParameter ep = (EmbeddedParameter) f.getAnnotation(targetAnnotation);
                    f.setAccessible(true);

                    Class<?> dataType = f.getType();
                    if (!dataType.isArray()) {
                        if (!dataType.isPrimitive()) {
                            Object originalVal = f.get(this);
                            if (originalVal != null) {

                                if (originalVal instanceof IEvent) { // Handle Events as embedded parameters
                                    ParametersContainer _pc = null;
                                    try {
                                        _pc = ((IEvent)originalVal).getEmbeddedParameters();
                                    } catch (Exception e) {
                                        System.err.println("Failed to get the embedded parameters of an embedded event '"
                                                + ep.value() + "' as parameter - " + e.getMessage());
                                        e.printStackTrace(System.err);
                                    }
                                    pc.put(ep.value(), _pc);
                                } else {

                                    // Handle other data types as embedded parameter

                                    if (dataType.isInterface() || Modifier.isAbstract(dataType.getModifiers())) {
                                        Constructor<?> ctor;
                                        try {
                                            ctor = originalVal.getClass().getDeclaredConstructor(dataType);
                                            ctor.setAccessible(true);
                                            pc.put(ep.value(), ctor.newInstance(originalVal)); // use Copy constructor
                                        } catch (NoSuchMethodException exception) {
                                            dataType = originalVal.getClass();
                                            ctor = dataType.getConstructor(dataType);
                                            ctor.setAccessible(true); // handle inner static friendly classes and their public ctors
                                            pc.put(ep.value(), ctor.newInstance(originalVal)); // use Copy constructor
                                        }
                                    } else {
                                        Constructor<?> ctor;
                                        if (ClassUtils.isPrimitiveWrapper(dataType)) { // handle primitive wrappers
                                            try {
                                                ctor = dataType.getConstructor(ClassUtils.wrapperToPrimitive(dataType));
                                                ctor.setAccessible(true); // handle inner static friendly classes and their public ctors
                                            } catch (NoSuchMethodException ex) {
                                                ctor = dataType.getDeclaredConstructor(dataType); // handle private constructors
                                                ctor.setAccessible(true);
                                            }
                                        } else {
                                            try {
                                                ctor = dataType.getConstructor(dataType);
                                                ctor.setAccessible(true); // handle inner static friendly classes and their public ctors
                                            } catch (NoSuchMethodException ex) {
                                                ctor = dataType.getDeclaredConstructor(dataType); // handle private constructors
                                                ctor.setAccessible(true);
                                            }
                                        }
                                        pc.put(ep.value(), ctor.newInstance(originalVal)); // use Copy constructor
                                        //pc.put(ep.value(), originalVal); // use reference
                                    }

                                }
                            } else {
                                pc.put(ep.value(), null);
                            }
                        } else {
                            pc.put(ep.value(), ClassUtils.primitiveToWrapper(dataType).getConstructor(dataType)
                                    .newInstance(f.get(this)));
                        }
                    } else {
                        List<Object> collection = new ComparableArrayList<Object>() {
                            private static final long serialVersionUID = -4707654819449935291L;
                        };
                        multidimArrayToFlatList(f.get(this), collection);
                        pc.put(ep.value(), collection);
                    }
                }
            }
        }

        return pc;
    }


    @Override
    public String toString() {
        String s = "";
        try {
            s = getWithEmbeddedParameters().toString();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return s;
    }

    @Override
    public void addSubEvent(Long keyId, IEvent event) throws UnsupportedOperationException, ClassCastException,
            IllegalArgumentException, NullPointerException {
        if (this == event) {
            throw new IllegalArgumentException("Infinite recursion of event having itself as a subevent!");
        }

        if (this.subEvents == null) {
            this.subEvents = constructSubEventsStructure();
        }

        IEvent previousEvent = this.subEvents.get(keyId);
        if (previousEvent == null || previousEvent == event) {
            this.subEvents.put(keyId, event);
        } else {
            throw new IllegalArgumentException("Event identifier collision during subevent addition attempt!");
        }
    }

    @Override
    public IEvent removeSubEvent(Long id) {
        if (this.subEvents == null) {
            this.subEvents = constructSubEventsStructure();
            return null;
        }
        return this.subEvents.remove(id);
    }

    @Override
    public IEvent getSubEvent(Long id) {
        if (this.subEvents == null) {
            this.subEvents = constructSubEventsStructure();
            return null;
        }
        return this.subEvents.get(id);
    }
}
