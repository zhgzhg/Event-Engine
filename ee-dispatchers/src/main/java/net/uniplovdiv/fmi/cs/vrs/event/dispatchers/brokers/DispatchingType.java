package net.uniplovdiv.fmi.cs.vrs.event.dispatchers.brokers;

/**
 * The possible dispatching operations that will be executed.
 */
public enum DispatchingType {
    /**
     * Only consumer (data receiver).
     */
    CONSUME,

    /**
     * Only producer (data sender).
     */
    PRODUCE,

    /**
     * Combines both {@link #CONSUME} and {@link #PRODUCE} types.
     */
    CONSUME_PRODUCE
}
