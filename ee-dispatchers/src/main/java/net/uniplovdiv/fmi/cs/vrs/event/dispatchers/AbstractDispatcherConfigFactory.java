package net.uniplovdiv.fmi.cs.vrs.event.dispatchers;

/**
 * An abstract dispatcher configuration factory that every more concrete one needs to extend.
 * @param <T> The input that needs to be sent to the factory in order to produce configuration.
 * @param <R> The configuration object that will be returned by the factory.
 */
public abstract class AbstractDispatcherConfigFactory<T, R> {

    /**
     * Returns a prepared configuration object of type R by taking an input of type T.
     * @param input The input required by the factory in order to produce the object.
     * @return An initialized configuration object.
     */
    public abstract R getMainConfiguration(T input);
}
