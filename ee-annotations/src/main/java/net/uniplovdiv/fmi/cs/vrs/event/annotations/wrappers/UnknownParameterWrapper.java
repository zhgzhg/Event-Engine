package net.uniplovdiv.fmi.cs.vrs.event.annotations.wrappers;

import net.uniplovdiv.fmi.cs.vrs.event.annotations.EmbeddedParameter;

/**
 * Dummy class representing unusable class giving a class object.
 * This class is used as a default "wrapper" value of annotation {@link EmbeddedParameter}.
 * It also fully satisfies the requirements of the {@link EmbeddedParameter} except that cannot be instantiated.
 * @see EmbeddedParameter
 */
public final class UnknownParameterWrapper implements Comparable<UnknownParameterWrapper> {
    /**
     * Default constructor.
     */
    private UnknownParameterWrapper() {
    }

    /**
     * Copy constructor.
     * @param dummy The UnknownParameterWrapper to be copied.
     */
    public UnknownParameterWrapper(UnknownParameterWrapper dummy) {
    }

    @Override
    public int compareTo(UnknownParameterWrapper o) {
        return 0;
    }
}
