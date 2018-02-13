package net.uniplovdiv.fmi.cs.vrs.event.location;

/**
 * Enumerator of the location type of an events location
 */
public enum EventLocationOccurrenceMedium {
    /**
     * Unknown location of occurrence flag value.
     */
    UNKNOWN(0L),

    /**
     * Physical world location of occurrence flag value.
     */
    PHYSICAL(1L),
    
    /**
     * Virtual world location of occurrence flag value.
     */
    VIRTUAL(2L);

    private final long mediumType;

    EventLocationOccurrenceMedium(long mediumType) {
        this.mediumType = mediumType;
    }

    /**
     * Returns numerical flag representation of the location type.
     * @return The medium type flag.
     */
    public long getMediumType() {
        return mediumType;
    }

    /**
     * Tests a specific flag value for a specific events types.
     * @param eventLocationOccurrenceMedium The events location type you want to test if it has been set.
     * @param value The value that will be tested.
     * @return True of the location type has been set otherwise false.
     */
    public static boolean test(EventLocationOccurrenceMedium eventLocationOccurrenceMedium, long value) {
        if (eventLocationOccurrenceMedium != EventLocationOccurrenceMedium.UNKNOWN) {
            return Long.compareUnsigned(eventLocationOccurrenceMedium.getMediumType(),
                    (value & eventLocationOccurrenceMedium.getMediumType())) == 0;
        }
        return Long.compareUnsigned(eventLocationOccurrenceMedium.getMediumType(), value) == 0;
    }

    /**
     * Creates an events flag based on the combination of different EventLocationOccurrenceMedium types
     * @param eventLocationOccurrenceMedia ... The EventLocationOccurrenceMedium values you want to combine.
     * @return The prepared events flag.
     */
    public static long prepare(EventLocationOccurrenceMedium... eventLocationOccurrenceMedia) {
        long result = 0;
        for (EventLocationOccurrenceMedium elom : eventLocationOccurrenceMedia) {
            result |= elom.getMediumType();
        }
        return result;
    }
}
