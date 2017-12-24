package net.uniplovdiv.fmi.cs.vrs.event.location;

import net.uniplovdiv.fmi.cs.vrs.event.IEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of the location of a particular events.
 */
public class EventLocation implements Serializable, Comparable<EventLocation> {
    private static final long serialVersionUID = -6982256911466131721L;

    private Double longitudeDegrees;
    private Double latitudeDegrees;
    private Double altitudeMeters;
    private Float accuracyMeters;
    private String address;
    private long occurrenceMediumType;
    private Map<String, String> extraData;

    /**
     * Constructor.
     */
    public EventLocation() {
        longitudeDegrees = latitudeDegrees = null;
        altitudeMeters = null;
        accuracyMeters = null;
        address = "";
        occurrenceMediumType = EventLocationOccurrenceMedium.prepare(EventLocationOccurrenceMedium.UNKNOWN);
        extraData = constructExtraDataStructure();
    }

    /**
     * Copy constructor.
     * @param el The variable whose data is going to be entirely copied.
     */
    public EventLocation(EventLocation el) {
        this();
        if (el == null) return;
        if (el.longitudeDegrees != null) longitudeDegrees = el.longitudeDegrees;
        if (el.latitudeDegrees != null) latitudeDegrees = el.latitudeDegrees;
        if (el.altitudeMeters != null) altitudeMeters = el.altitudeMeters;
        if (el.accuracyMeters != null) accuracyMeters = el.accuracyMeters;
        if (el.address != null && !el.address.isEmpty()) address = el.address;
        occurrenceMediumType = el.occurrenceMediumType;
        if (el.extraData != null && el.extraData.size() > 0) extraData.putAll(el.extraData);
    }

    /**
     * Creates new extraData structure.
     * @return The new empty structure.
     */
    private Map<String, String> constructExtraDataStructure() {
        return Collections.synchronizedMap(new HashMap<String, String>());
    }

    /**
     * Constructor.
     * @param longitudeDegrees The longitude in degrees of the specified location. Can be null.
     * @param latitudeDegrees The latitude in degrees of the specified location. Can be null.
     * @param altitudeMeters  The altitude in meters of the specified location. Can be null.
     * @param accuracyMeters The accuracy in meters of the specified location. Can be null.
     * @param address Address as a string that can represent the location. Can be null.
     * @param occurrenceMediumType Flag prepared using EventLocationOccurrenceMedium.prepare() method.
     * @param extraData Additional information for the location. Can be null.
     */
    public EventLocation(Double longitudeDegrees, Double latitudeDegrees, Double altitudeMeters, Float accuracyMeters,
                         String address, long occurrenceMediumType, Map<String, String> extraData) {
        this.longitudeDegrees = longitudeDegrees;
        this.latitudeDegrees = latitudeDegrees;
        this.altitudeMeters = altitudeMeters;
        this.accuracyMeters = accuracyMeters;
        this.address = address;
        this.occurrenceMediumType = occurrenceMediumType;
        this.extraData = extraData;
    }

    /**
     * Checks if any of the event parameters are set to something different from the default values.
     * @param eventLocation The EventLocation instance that will be checked.
     * @return True if there is something different from the default values in this class, otherwise false.
     */
    public static boolean isDefined(EventLocation eventLocation) {
        if (eventLocation == null) return false;
        if (eventLocation.getAddress() != null && !eventLocation.getAddress().isEmpty()) return true;
        if (eventLocation.getLatitudeDegrees() != null && !eventLocation.getLatitudeDegrees().isInfinite()) return true;
        if (eventLocation.getLongitudeDegrees() != null && !eventLocation.getLongitudeDegrees().isInfinite()) return true;
        if (eventLocation.getAltitudeMeters() != null && !eventLocation.getAltitudeMeters().isInfinite()) return true;
        //noinspection SimplifiableIfStatement
        if (eventLocation.hasExtraData()) return true;

        return (!EventLocationOccurrenceMedium.test(
                EventLocationOccurrenceMedium.UNKNOWN, eventLocation.getOccurrenceMediumType())
        );
    }

    /**
     * Checks if any of the event parameters are set to something different from the default values.
     * @return True if there is something different from the default values in this class, otherwise false.
     */
    public boolean isDefined() {
        return isDefined(this);
    }

    /**
     * Returns the longitude in degrees of the location.
     * @return An initialized value or null.
     */
    public Double getLongitudeDegrees() {
        return longitudeDegrees;
    }

    /**
     * Sets the longitude in degrees of the location.
     * @param longitudeDegrees An initialized value or null.
     */
    public void setLongitudeDegrees(Double longitudeDegrees) {
        this.longitudeDegrees = longitudeDegrees;
    }

    /**
     * Returns the latitude in degrees of the location.
     * @return An initialized value or null.
     */
    public Double getLatitudeDegrees() {
        return latitudeDegrees;
    }

    /**
     * Sets the latitude in degrees of the location.
     * @param latitudeDegrees An initialized value or null.
     */
    public void setLatitudeDegrees(Double latitudeDegrees) {
        this.latitudeDegrees = latitudeDegrees;
    }

    /**
     * Returns the altitude in meters of the location.
     * @return An initialized value or null.
     */
    public Double getAltitudeMeters() {
        return altitudeMeters;
    }

    /**
     * Sets the altitude in meters of the location.
     * @param  altitudeMeters An initialized value or null.
     */
    public void setAltitudeMeters(Double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    /**
     * Returns the accuracy in meters of the location.
     * @return An initialized value or null.
     */
    public Float getAccuracyMeters() {
        return accuracyMeters;
    }

    /**
     * Sets the accuracy in meters of the location.
     * @param accuracyMeters  An initialized value or null.
     */
    public void setAccuracyMeters(Float accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    /**
     * Returns the string address of the location.
     * @return An initialized value or null.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the string address of the location.
     * @param address An initialized value or null.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Returns the location occurrence medium flag for an events.
     * @return A long value that can be tested using EventLocationOccurrenceMedium.test() method.
     */
    public long getOccurrenceMediumType() {
        return occurrenceMediumType;
    }

    /**
     * Sets the location occurrence medium flag for an events.
     * @param occurrenceMediumType The flag prepared using EventLocationOccurrenceMedium.prepare() method.
     */
    public void setOccurrenceType(long occurrenceMediumType) {
        this.occurrenceMediumType = occurrenceMediumType;
    }

    /**
     * Returns additional key-value strings structure description data for the location.
     * @return An initialized value or null.
     */
    public Map<String, String> getExtraData() {
        return extraData;
    }

    /**
     * Sets additional key-value strings structure with description data for the location.
     * @param extraData Additional key-value data describing the location. The value can be also null.
     */
    public void setExtraData(Map<String, String> extraData) {
        this.extraData = extraData;
    }

    /**
     * Adds an additional key-value strings pair to structure with extra description data for the location.
     * @param key Key string identifier.
     * @param value The value that will be added to the structure and associated with the key parameter.
     * @return The previous value associated with the key or null.
     */
    public String addExtraData(String key, String value) {
        if (this.extraData == null) {
            this.extraData = constructExtraDataStructure();
        }
        return this.extraData.put(key, value);
    }

    /**
     * Returns a value string from key-value pair from the structure with extra description data for the location.
     * @param key The key of the value to return.
     * @return The value associated with the key or null.
     */
    public String getExtraData(String key) {
        if (this.extraData == null) {
            this.extraData = constructExtraDataStructure();
            return null;
        }
        return this.extraData.get(key);
    }

    /**
     * Removes a value string from key-value pair from the structure with extra description data for the location.
     * @param key The key of the value which will be removed.
     * @return The value associated with the key or null.
     */
    public String removeExtraData(String key) {
        if (this.extraData == null) {
            this.extraData = constructExtraDataStructure();
            return null;
        }
        return this.extraData.remove(key);
    }

    /**
     * Indicates whether there is any extra data currently set.
     * @return True if there is any extra data otherwise false.
     */
    public boolean hasExtraData() {
        return this.extraData != null && this.extraData.size() > 0;
    }

    @Override
    public String toString() {
        ArrayList<String> oc = new ArrayList<>();
        for (EventLocationOccurrenceMedium t : EventLocationOccurrenceMedium.values())  {
            if (EventLocationOccurrenceMedium.test(t, occurrenceMediumType)) {
                oc.add(t.name());
            }
        }
        return "{ longitudeDegrees=" + longitudeDegrees + ", latitudeDegrees=" +
                latitudeDegrees + ", altitudeMeters=" + altitudeMeters +
                ", accuracyMeters=" + accuracyMeters + ", address=" + address +
                ", occurrenceMediumType=" + oc + ", extraData=" + extraData + " }";
    }

    /**
     * Safe equals operation that will work with nulls.
     * @param a Source
     * @param b Reference
     * @param <T> The generic data type for which the equality check will be performed. It must also implement the
     *           Comparable interface.
     * @return The result equivalent of a.equals(b)
     */
    private static <T extends Comparable<T>> boolean safeEquals(T a, T b) {
        return (a == b) || (a != null && b != null && a.equals(b));
    }

    /**
     * Check if this events location equals another object. It also takes into account the accuracy which has to be
     * better to the one that the obj parameter has.
     * @param obj The object to check if it's equal to the current one.
     * @return True if equal or false if are not equal of are from a different data type.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (obj instanceof EventLocation) {
            EventLocation _obj = (EventLocation) obj;
            if (occurrenceMediumType != _obj.getOccurrenceMediumType()) {
                return false;
            }
            @SuppressWarnings("NumberEquality")
            boolean accuracyMatch = (accuracyMeters == _obj.getAccuracyMeters());
            if (!accuracyMatch) {
                if (accuracyMeters != null && _obj.getAccuracyMeters() != null  &&
                        _obj.getAccuracyMeters() > accuracyMeters) {
                    accuracyMatch = true;
                }
            }

            return (accuracyMatch
                    && safeEquals(longitudeDegrees, _obj.getLongitudeDegrees())
                    && safeEquals(latitudeDegrees, _obj.getLatitudeDegrees())
                    && safeEquals(altitudeMeters, _obj.getAltitudeMeters())
                    && safeEquals(address, _obj.getAddress())
                    && IEvent.safeEquals(extraData, _obj.getExtraData())
            );
        }
        return false;
    }

    /**
     * Calculates a hash code for events events location.
     * @return The hash code of the current events location instance.
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (longitudeDegrees == null ? 0 : longitudeDegrees.hashCode());
        result = 31 * result + (latitudeDegrees == null ? 0 : latitudeDegrees.hashCode());
        result = 31 * result + (altitudeMeters == null ? 0 : altitudeMeters.hashCode());
        result = 31 * result + (accuracyMeters == null ? 0 : accuracyMeters.hashCode());
        result = 31 * result + (address == null ? 0 : address.hashCode());
        result = 31 * result + (int)(occurrenceMediumType ^ (occurrenceMediumType >>> 32));
        result = 31 * result + (extraData == null ? 0 : extraData.hashCode());
        return result;
    }

    /**
     * This method is very limited in terms of comparison!
     * @param el The EventLocation to compare the current instance against.
     * @return In case of equality 0. If both locations are not defined well 1 or if the current one is well defined.
     * If el is well defined, but the current one is not then -1 is returned.
     */
    @Override
    public int compareTo(EventLocation el) {
        if (this.equals(el)) {
            return 0;
        }
        if (this.isDefined()) {
            return 1;
        }
        if (el.isDefined()) {
            return -1;
        }
        return 1;
    }
}
