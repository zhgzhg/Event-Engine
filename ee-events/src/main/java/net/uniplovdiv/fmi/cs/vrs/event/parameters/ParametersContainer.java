package net.uniplovdiv.fmi.cs.vrs.event.parameters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Container for (usually event's) dynamic parameters. It is required that the contained value must be a serializable
 * object.
 * Although the container permits storing inside IEvent instances it's strongly recommended not to do so, because any of
 * the automatic functionality regarding internal event and subevent manipulation and processing will deliberately skip
 * this structure.
 */
public class ParametersContainer extends HashMap<String, Object> implements Map<String, Object>, Serializable {
    private static final long serialVersionUID = -1808818111111926468L;
    private boolean checkForSerializationAbility = true;

    /**
     * Constructor.
     */
    public ParametersContainer() {
        super();
    }

    /**
     * Copy constructor.
     * @param p The ParametersContainer from which to create a copy.
     */
    public ParametersContainer(ParametersContainer p) {
        super(p);
    }

    /**
     * Toggles on/off the run-time serialization checks.
     * @param check Set to true if checks are desired. For every new instance this is true by default.
     */
    public void toggleSerializationChecks(boolean check) {
        this.checkForSerializationAbility = check;
    }

    /**
     * Checks if the object that is usually put into the map as value is serializable.
     * @param value The object to be checked.
     * @throws IllegalArgumentException if the object is not serializable
     */
    private void checkIsSerializable(Object value) {
        if (checkForSerializationAbility && value != null) {
            if (!(value instanceof Serializable)) {
                throw new IllegalArgumentException("Value objects put to + " + this.getClass().getName()
                        + " must be serializable!");
            }
        }
    }

    @Override
    public Object put(String key, Object value) {
        checkIsSerializable(value);
        return super.put(key, value);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        checkIsSerializable(value);
        return super.putIfAbsent(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        if (m != null && !m.isEmpty()) {
            Iterator<? extends Entry<? extends String, ?>> iterator = m.entrySet().iterator();
            checkIsSerializable(iterator.next().getValue());
            super.putAll(m);
        }
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        checkIsSerializable(newValue);
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {
        checkIsSerializable(value);
        return super.replace(key, value);
    }
}