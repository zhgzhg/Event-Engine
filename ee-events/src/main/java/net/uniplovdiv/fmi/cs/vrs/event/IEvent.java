package net.uniplovdiv.fmi.cs.vrs.event;

import net.uniplovdiv.fmi.cs.vrs.event.parameters.IEventsContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParametersComparisonResult;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.*;
import net.uniplovdiv.fmi.cs.vrs.event.location.EventLocation;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.ParametersContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcome;

import java.lang.Comparable;
import java.lang.Long;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;


/**
 * Generalized {@link Event} interface.
 */
public interface IEvent extends Comparable<IEvent> {

    /**
     * String suggestion of the virtual Event field name that is containing the particular IEvent class human-readable
     * name string.
     */
    String ___EVENT_TYPE_CLASS_NAME = "__event_type_class_name";

    /**
     * String suggestion of the virtual field name that is containing the particular IEvent class serial version uid
     * number.
     */
    String ___SV_UID_FIELD_NAME = "serialVersionUID";

    /**
     * Returns the current event's identifier.
     * @return The identifier of the current events.
     */
    long getId();

    /**
     * Sets the current event's identifier.
     * @param id The identifier to be set.
     */
    void setId(long id);

    /**
     * Returns the Unix epoch timestamp in microseconds resolution of when the events has occurred.
     * @return Unix epoch timestamp in microseconds resolution.
     */
    long getTimestampMs();

    /**
     * Sets the Unix epoch timestamp in milliseconds resolution of when the events has occurred.
     * @param timestampMs Unix epoch timestamp in milliseconds resolution.
     */
    void setTimestampMs(long timestampMs);

    /**
     * Refreshes the timestamp of the current event by updating it to the current time.
     */
    default void refreshTimestampMs() {
        setTimestampMs(System.currentTimeMillis());
    }

    /**
     * Sets the Unix epoch timestamp in milliseconds resolution of when the events has occurred. As a source a
     * {@link ZonedDateTime} instance is used, but because of that the resolution will be rounded to seconds.
     * @param zdt The zoned date time instance whose value will be used.
     * @throws NullPointerException if zdt is null
     */
    default void setTimestampMsFromZonedDateTime(ZonedDateTime zdt) {
        setTimestampMs(zdt.toEpochSecond() * 1000);
    }

    /**
     * Returns {@link ZonedDateTime} according to system's default time zone of when the events has occurred.
     * @return New initialized {@link ZonedDateTime} instance.
     */
    default ZonedDateTime getZonedDateTime() {
        long timestampMs = getTimestampMs();
        LocalDateTime ldt = LocalDateTime.ofEpochSecond((int)(timestampMs / 1000), (int)(timestampMs % 1000), ZoneOffset.UTC);
        return ldt.toInstant(ZoneOffset.UTC).atZone(ZoneId.systemDefault());
    }

    /**
     * Returns the priority of the event.
     * @return An integer. The lower the number the higher the priority and vice versa.
     */
    int getPriority();

    /**
     * Sets the priority of the event. The lower the number the higher the priority and vice versa.
     * @param priority The desired priority number.
     */
    void setPriority(int priority);

    /**
     * Returns the current events description.
     * @return A string with events description.
     */
    String getDescription();

    /**
     * Sets the current events description.
     * @param description The description of the events.
     */
    void setDescription(String description);

    /**
     * Returns the category to which a particular event instance is part of. If the method has not been overridden then
     * the category of the parent class will be used. The most root category is defined as "events".
     * The returned information is used to provide a common grouping criteria during event dispatching.
     * @return A nonempty category string.
     */
    default String getCategory() {
        return "events";
    }

    /**
     * Returns additional describing the events dynamic parameters.
     * @return A Map of strings with event's dynamicParameters.
     */
    ParametersContainer getDynamicParameters();

    /**
     * Sets map with additional describing the events dynamic parameters.
     * @param parameters The map containing additional events dynamic parameters.
     */
    void setDynamicParameters(ParametersContainer parameters);

    /**
     * Returns the native, embedded to this class parameters (normally a copy of their values) and also the dynamic
     * parameters in the current instance by reference - all of that in a new ParametersContainer structure.
     * If there is key collision between the embedded and the dynamic parameters, the dynamic version will be ignored.
     *
     * @return Initialized container with the values of all embedded and dynamic for the current class parameters.
     *
     * @throws NoSuchMethodException During unsuccessful attempt to convert primitive data types to a reference ones.
     * Also if the initialization of any reference type fails, because of missing/unreachable copy constructor for the
     * specified type, or because the type is abstract or an interface whose implementation cannot be determined.
     * @throws IllegalAccessException During unsuccessful attempt to obtain information about a specific class field.
     * @throws InstantiationException During unsuccessful attempt to data primitive types to a reference ones.
     * @throws InvocationTargetException During unsuccessful attempt to data primitive types to a reference ones.
     */
    ParametersContainer getWithEmbeddedParameters() throws NoSuchMethodException, IllegalAccessException,
            InstantiationException, InvocationTargetException;

    /**
     * Returns the native, embedded to this class parameters (normally a copy of their values) in a
     * {@link ParametersContainer}.
     *
     * @return Initialized container with the values of all of the embedded for the current class parameters.
     *
     * @throws NoSuchMethodException During unsuccessful attempt to convert primitive data  types to a reference ones.
     * Also if the initialization of any reference type fails, because of missing/unreachable copy constructor for the
     * specified type, or because the type is abstract or an interface whose implementation cannot be determined.
     * @throws IllegalAccessException During unsuccessful attempt to obtain information about a specific class field.
     * @throws InstantiationException During unsuccessful attempt to data primitive types to a reference ones.
     * @throws InvocationTargetException During unsuccessful attempt to data primitive types to a reference ones.
     */
    ParametersContainer getEmbeddedParameters() throws NoSuchMethodException, IllegalAccessException,
            InstantiationException, InvocationTargetException;

    /**
     * Returns the EventLocation of the current events.
     * @return An initiated EventLocation that might contain null values.
     */
    EventLocation getEventLocation();

    /**
     * Sets the {@link EventLocation} of the current events.
     * @param eventLocation Instance of EventLocation.
     */
    void setEventLocation(EventLocation eventLocation);

    /**
     * Checks if the current event has happened earlier compared to another events.
     * @param event The event to which the current event is compared.
     * @return True if the current event has happened earlier, otherwise false (or if the event parameter is null).
     */
    default boolean isEarlierThan(IEvent event) {
        return (event != null) && (getTimestampMs() < event.getTimestampMs());
    }

    /**
     * Checks if the current event has happened later compared to another events.
     * @param event The event to which the current event is compared.
     * @return True if the current event has happened later, otherwise false (or if the event parameter is null).
     */
    default boolean isLaterThan(IEvent event) {
        return (event != null) && (getTimestampMs() > event.getTimestampMs());
    }

    /**
     * Checks if the current event has happened at the same time compared to another events.
     * @param event The event to which the current event is compared.
     * @return True if the current event has happened at the same as events parameter, otherwise false (or if the event
     * parameter is null).
     */
    default boolean isAtTheSameTimeAs(IEvent event) {
        return (event != null) && (getTimestampMs() == event.getTimestampMs());
    }

    /**
     * Checks if the current event's priority is higher compared to another event.
     * @param event The event to which the current event is compared.
     * @return True if the current event's priority is higher, otherwise false (or if the event parameter is null).
     */
    default boolean isHigherPriorityThan(IEvent event) {
        return (event != null) && (getPriority() < event.getPriority());
    }

    /**
     * Checks if the current event's priority is lower compared to another event.
     * @param event The event to which the current event is compared.
     * @return True if the current event's priority is lower, otherwise false (or if the event parameter is null).
     */
    default boolean isLowerPriorityThan(IEvent event) {
        return (event != null) && (getPriority() > event.getPriority());
    }

    /**
     * Checks if the current event's priority is the same compared to another event.
     * @param event The event to which the current event is compared.
     * @return True if the current event's priority is the same, otherwise false (or if the event parameter is null).
     */
    default boolean isSamePriorityAs(IEvent event) {
        return (event != null) && (getPriority() == event.getPriority());
    }

    /**
     * Check whether the current instance has other events subinstances inside.
     * @return True if there are nested IEvents inside otherwise false.
     */
    default boolean hasSubEvents() {
        IEventsContainer subEvents = getSubEvents();
        return (subEvents != null) && (!subEvents.isEmpty());
    }

    /**
     * Check whether the current instance has a description set inside.
     * @return True if it's present otherwise false.
     */
    default boolean hasDescription() {
        String description = getDescription();
        return (description != null) && (!description.isEmpty());
    }

    /**
     * Check whether the current instance has a additional dynamicParameters set inside.
     * @return True if it's present otherwise false.
     */
    default boolean hasDynamicParameters() {
        ParametersContainer dynamicParameters = getDynamicParameters();
        return (dynamicParameters != null) && (dynamicParameters.size() > 0);
    }

    /**
     * Check whether the current instance has any location parameters set inside.
     * @return True if it's present otherwise false.
     */
    default boolean hasLocation() {
        EventLocation eventLocation = getEventLocation();
        return EventLocation.isDefined(eventLocation);
    }

    /**
     * Returns the subevents contained in the current instance.
     * @return The SortedMap with id key and {@link IEvent} value that can also be null.
     */
    IEventsContainer getSubEvents();

    /**
     * Sets the subevents contained in the current instance.
     * @param subEvents Sorted map of subevents that will be contained with id key and {@link IEvent} value.
     */
    void setSubEvents(IEventsContainer subEvents);

    /**
     * Adds a new subevent to the internal subevents container. If the same event has already been added its existing
     * identifier is returned. In a case of collision an exception will be thrown.
     * @param event The subevent that will be added
     * @throws IllegalArgumentException If identifier collision or bad arguments are supplied.
     * @throws ClassCastException If identifier collision or bad arguments are supplied.
     * @throws IllegalArgumentException If identifier collision or bad arguments are supplied.
     * @throws NullPointerException If the corresponding dynamicParameters are null.
     * @return The identifier of the added events, based on the identifier inside events parameter.
     */
    default Long addSubEvent(IEvent event) throws UnsupportedOperationException, ClassCastException,
            IllegalArgumentException, NullPointerException {
        Long eid = event.getId();
        addSubEvent(eid, event);
        return eid;
    }

    /**
     * Adds a new subevent to the internal subevents container. In a case of collision an exception will be thrown.
     * @param keyId An unique key identifier used for that new subevent.
     * @param event The subevent that will be added.
     * @throws IllegalArgumentException If identifier collision or bad arguments are supplied.
     * @throws ClassCastException If identifier collision or bad arguments are supplied.
     * @throws IllegalArgumentException If identifier collision or bad arguments are supplied.
     * @throws NullPointerException If the corresponding dynamicParameters are null.
     */
    void addSubEvent(Long keyId, IEvent event) throws UnsupportedOperationException, ClassCastException,
            IllegalArgumentException, NullPointerException;

    /**
     * Removes an existing subevent from the current events.
     * @param event The existing events that has to be removed.
     * @return On success returns true otherwise false.
     */
    default boolean removeSubEvent(IEvent event) {
        IEventsContainer subEvents = getSubEvents();
        if (subEvents != null) {
            return subEvents.remove(event.getId(), event);
        }
        return true;
    }

    /**
     * Removes an existing subevent from the current events.
     * @param id The id of the existing events.
     * @return The {@link IEvent} instance that previously was associated with the passed identifier in the internal
     * subevent structure.
     */
    IEvent removeSubEvent(Long id);

    /**
     * Returns an existing subevent finding it by its identifier.
     * @param id The id of the existing events
     * @return An initialized {@link IEvent} if such is found otherwise null.
     */
    IEvent getSubEvent(Long id);

    /**
     * Asymmetrically compares the current event parameters using compareTo method to some other parameters.
     * The parameters contained in the current event are used as a source template, while the passed parameters are
     * compared to them. This covers both embedded and dynamic parameters.
     * @param to The parameters to which a comparison is going to be performed.
     * @return Results map with the comparison results.
     * @throws NullPointerException If the corresponding parameters are null.
     * @throws NoSuchMethodException During unsuccessful attempt to convert primitive data types to a reference ones.
     * Also if the initialization of any reference type fails, because of missing/unreachable copy constructor for the
     * specified type, or because the type is abstract or an interface whose implementation cannot be determined.
     * @throws IllegalAccessException During unsuccessful attempt to obtain information about a specific class field.
     * @throws InstantiationException During unsuccessful attempt to data primitive types to a reference ones.
     * @throws InvocationTargetException During unsuccessful attempt to data primitive types to a reference ones.
     * @see ParameterComparisonOutcome
     */
    ParametersComparisonResult compareParametersTo(ParametersContainer to) throws InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException;

    /**
     * Asymmetrically compares the current event parameters using compareTo method to the parameters of another event.
     * @param to The event to whose parameters a comparison is going to be performed.
     * @return Results map with the comparison results.
     * @throws NullPointerException If the corresponding parameters are null.
     * @throws NoSuchMethodException During unsuccessful attempt to convert primitive data types to a reference ones.
     * Also if the initialization of any reference type fails, because of missing/unreachable copy constructor for the
     * specified type, or because the type is abstract or an interface whose implementation cannot be determined.
     * @throws IllegalAccessException During unsuccessful attempt to obtain information about a specific class field.
     * @throws InstantiationException During unsuccessful attempt to data primitive types to a reference ones.
     * @throws InvocationTargetException During unsuccessful attempt to data primitive types to a reference ones.
     * @see ParameterComparisonOutcome
     */
    ParametersComparisonResult compareParametersTo(IEvent to) throws InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException;

    /**
     * Safe equals helper operation that will work with nulls.
     * @param a Source
     * @param b Reference
     * @param <T> The generic data type for which the equality check will be performed.
     * @return The result equivalent of a.equals(b)
     */
    static <T> boolean safeEquals(T a, T b) {
        return (a == b) || (a != null && b != null && a.equals(b));
    }
}