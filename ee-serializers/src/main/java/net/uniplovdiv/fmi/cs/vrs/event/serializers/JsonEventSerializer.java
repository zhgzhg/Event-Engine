package net.uniplovdiv.fmi.cs.vrs.event.serializers;

import com.google.gson.*;

import net.uniplovdiv.fmi.cs.vrs.event.Event;
import net.uniplovdiv.fmi.cs.vrs.event.IEvent;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.EventsContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.IEventsContainer;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.IParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcome;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParameterComparisonOutcomeTemplate;
import net.uniplovdiv.fmi.cs.vrs.event.parameters.comparison.ParametersComparisonResult;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.ClassesIEventScanner;
import net.uniplovdiv.fmi.cs.vrs.event.serializers.engine.RuntimeTypeAdapterFactory;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Provides serialization and deserialization logic from/to JSON format serialization representation and objects
 * implementing IEvent. All of the serialized objects must contain explicit information with serialVersionUID and
 * the name of the representing class (the second one is automatically covered from the Event class).
 */
public class JsonEventSerializer implements IEventSerializer {

    private JavaEventSerializer javaEventSerializer;
    private RuntimeTypeAdapterFactory<IEvent> iEventRuntimeTypeAdapterFactory;
    @SuppressWarnings("FieldCanBeLocal")
    private RuntimeTypeAdapterFactory<IParameterComparisonOutcome> iParameterComparisonOutcomeRuntimeTypeAdapterFactory;
    private GsonBuilder gsonBuilder;
    private Gson gson;
    private Charset encoding;
    private ClassesIEventScanner packageScanner;
    private boolean attemptAutomaticClassRegistration;

    //<editor-fold desc="Experiments with Gson generic type adapters">
    /*
     * GSON generic type wrapper adapter.
     * @param <T> The current data type for an adapter will be created.
     * @param <S> The superior type whose existing adapter to be used.
     *
    class GenericTypeWrapperAdapter<T> extends TypeAdapter<T> {
        private Gson gson;
        private Class<T> wrapper;
        private final Class<? super T> superior;

        public GenericTypeWrapperAdapter(Class<T> wrapper) {
            gson = new Gson();
            this.wrapper = wrapper;
            this.superior = wrapper.getSuperclass();
        }

        @Override
        public void write(JsonWriter jsonWriter, T t) throws IOException {
            TypeAdapter<? super T> adapter = gson.getAdapter(superior);
            adapter.write(jsonWriter, t);
        }

        @Override
        public T read(JsonReader jsonReader) throws IOException {
            T result = null;
            TypeAdapter<?> adapter = gson.getAdapter(superior);
            Object data = adapter.read(jsonReader);
            if (data != null) {
                try {
                    Constructor<T> ctor = wrapper.getDeclaredConstructor(superior);
                    result = ctor.newInstance(data);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    return result;
                }
            }
            return result;
        }
    }*/

    /*abstract class CustomizedTypeAdapterFactory<C> implements TypeAdapterFactory {
        private final Class<C> customizedClass;

        public CustomizedTypeAdapterFactory(Class<C> customizedClass) {
            this.customizedClass = customizedClass;
        }

        @SuppressWarnings("unchecked") // we use a runtime check to guarantee that 'C' and 'T' are equal
        public final <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            return type.getRawType() == customizedClass
                    ? (TypeAdapter<T>) customizeMyClassAdapter(gson, (TypeToken<C>) type)
                    : null;
        }

        private TypeAdapter<C> customizeMyClassAdapter(Gson gson, TypeToken<C> type) {
            final TypeAdapter<C> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            return new TypeAdapter<C>() {

                @Override
                public void write(JsonWriter out, C value) throws IOException {
                    JsonElement tree = delegate.toJsonTree(value);
                    beforeWrite(value, tree);
                    elementAdapter.write(out, tree);
                }
                @Override
                public C read(JsonReader in) throws IOException {
                    JsonElement tree = elementAdapter.read(in);
                    afterRead(tree);
                    return delegate.fromJsonTree(tree);
                }
            };
        }

        /**
         * Override this to muck with {@code toSerialize} before it is written to
         * the outgoing JSON stream.
         *
        protected void beforeWrite(C source, JsonElement toSerialize) {
        }

        /**
         * Override this to muck with {@code deserialized} before it parsed into
         * the application type.
         *
        protected void afterRead(JsonElement deserialized) {
        }
    }

    private class IEventTypeAdapterFactory extends CustomizedTypeAdapterFactory<IEvent> {
        private IEventTypeAdapterFactory() {
            super(IEvent.class);
        }

        @Override
        protected void beforeWrite(IEvent source, JsonElement toSerialize) {
            //JsonObject custom = toSerialize.getAsJsonObject();
            //custom.add("$event_type_class", new JsonPrimitive(source.getClass().getTypeName()));
        }

        @Override protected void afterRead(JsonElement deserialized) {
            //JsonObject custom = deserialized.getAsJsonObject();
            //custom.remove("$event_type_class");
        }
    }

    class IEventInstanceCreator implements InstanceCreator<IEvent> {
        @Override
        public IEvent createInstance(Type type) {
            return Event.makeInstance(Event.class);
        }
    }
    */
    //</editor-fold>

    /**
     * IEventsContainer instance creator class for GSON.
     */
    class IEventsContainerInstanceCreator implements InstanceCreator<IEventsContainer> {
        @Override
        public IEventsContainer createInstance(Type type) {
            return new EventsContainer();
        }
    }

    // <editor-fold desc="Sample code for additional de/serializers">
    /*
     * Serializers and deserializers for all classes that implement {@link IParameterComparisonOutcome} for GSON.
     *
    public class IParameterComparisonOutcomeSerializer implements JsonSerializer<IParameterComparisonOutcome>,
            JsonDeserializer<IParameterComparisonOutcome> {
        public static final String CLASS_META_KEY = ParameterComparisonOutcome.___RESULT_TYPE_CLASS_NAME;
        private final Class<IParameterComparisonOutcome> IPARAMETERCOMPARISONOUTCOME_INTERFACE =
                IParameterComparisonOutcome.class;
        private final Class<ParameterComparisonOutcome> ENUM_PARAMETERCOMPARISONOUTCOME_CLASS =
                ParameterComparisonOutcome.class;
        public static final String SV_UID_FIELD_NAME = ParametersComparisonResult.___SV_UID_FIELD_NAME;

        @Override
        public JsonElement serialize(IParameterComparisonOutcome src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonElement element = null;
            if (src == null) {
                return element;
            }

            Class<? extends IParameterComparisonOutcome> clazz = src.getClass();
            if (clazz.equals(IPARAMETERCOMPARISONOUTCOME_INTERFACE)) { // highly improbable
                clazz = ENUM_PARAMETERCOMPARISONOUTCOME_CLASS;
                System.err.println("Class object of IParameterComparisonOutcome specified directly instead of class" +
                        "implementing the interface! Falling back to ParameterComparisonOutcome.class!");
            }

            element = context.serialize(src, clazz);

            if (!clazz.equals(ENUM_PARAMETERCOMPARISONOUTCOME_CLASS)) {
                try {
                    long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                    if (serialUid == 0) {
                        serialUid = javaEventSerializer.computeSerialVersionUidFromEventHelper(src);
                    }
                    element.getAsJsonObject().addProperty(SV_UID_FIELD_NAME, serialUid);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            element.getAsJsonObject().addProperty(CLASS_META_KEY, getClassFullName(src.getClass()));
            return element;
        }

        @Override
        public IParameterComparisonOutcome deserialize(JsonElement jsonElement, Type typeOfT,
                                                       JsonDeserializationContext context)
                throws JsonParseException {
            IParameterComparisonOutcome result = null;
            JsonObject object = jsonElement.getAsJsonObject();
            if (object.has(CLASS_META_KEY)) {
                Class<?> clazz = null;
                String className = object.get(CLASS_META_KEY).getAsString();
                try {
                    clazz = Class.forName(className);
                    if (!clazz.equals(ENUM_PARAMETERCOMPARISONOUTCOME_CLASS)) {
                        long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                        if (object.has(SV_UID_FIELD_NAME)) {
                            long serializedSerialUid = object.get(SV_UID_FIELD_NAME).getAsLong();
                            if (serializedSerialUid != serialUid) {
                                throw new IllegalClassFormatException(SV_UID_FIELD_NAME + "mismatch - original "
                                        + serialUid + ", serialized " + serializedSerialUid + " - class "
                                        + className);
                            }
                        } else {
                            throw new IllegalClassFormatException("Missing field " + SV_UID_FIELD_NAME
                                    + " for serialized class " + className);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Cannot deserialize " + className +
                            ". Falling back to ParameterComparisonOutcome.class. Cause: ");
                    e.printStackTrace(System.err);
                    clazz = ENUM_PARAMETERCOMPARISONOUTCOME_CLASS;
                }
                object.remove(CLASS_META_KEY);
                object.remove(SV_UID_FIELD_NAME);
                result = context.deserialize(jsonElement, clazz);
            }
            return result;
        }
    }
*/
    /*
     * Serializers and deserializers for class {@link ParameterComparisonOutcomeTemplate}.
     *
    public class ParameterComparisonOutcomeTemplateSerializer implements
            JsonSerializer<ParameterComparisonOutcomeTemplate>, JsonDeserializer<ParameterComparisonOutcomeTemplate> {

        public static final String CLASS_META_KEY = ParameterComparisonOutcome.___RESULT_TYPE_CLASS_NAME;
        private final Class<ParameterComparisonOutcomeTemplate> PARAMETERCOMPARISONOUTCOMETEMPLATE_CLASS =
                ParameterComparisonOutcomeTemplate.class;
        public static final String SV_UID_FIELD_NAME = ParametersComparisonResult.___SV_UID_FIELD_NAME;

        @Override
        public JsonElement serialize(ParameterComparisonOutcomeTemplate src, Type typeOfSrc,
                JsonSerializationContext context) {
            JsonElement element = null;
            if (src == null) {
                return element;
            }

            Class<? extends ParameterComparisonOutcomeTemplate> clazz = src.getClass();
            element = context.serialize(src, clazz);

            try {
                long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                if (serialUid == 0) {
                    serialUid = javaEventSerializer.computeSerialVersionUidFromEventHelper(src);
                }
                element.getAsJsonObject().addProperty(SV_UID_FIELD_NAME, serialUid);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            element.getAsJsonObject().addProperty(CLASS_META_KEY, getClassFullName(src.getClass()));
            return element;
        }

        @Override
        public ParameterComparisonOutcomeTemplate deserialize(JsonElement jsonElement, Type typeOfT,
                JsonDeserializationContext context)
            throws JsonParseException {
            ParameterComparisonOutcomeTemplate result = null;
            JsonObject object = jsonElement.getAsJsonObject();

            if (object.has(CLASS_META_KEY)) {
                Class<?> clazz = null;
                String className = object.get(CLASS_META_KEY).getAsString();
                try {
                    clazz = Class.forName(className);
                    if (!clazz.equals(PARAMETERCOMPARISONOUTCOMETEMPLATE_CLASS)) {
                        long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                        if (object.has(SV_UID_FIELD_NAME)) {
                            long serializedSerialUid = object.get(SV_UID_FIELD_NAME).getAsLong();
                            if (serializedSerialUid != serialUid) {
                                throw new IllegalClassFormatException(SV_UID_FIELD_NAME + "mismatch - original "
                                        + serialUid + ", serialized " + serializedSerialUid + " - class "
                                        + className);
                            }
                        } else {
                            throw new IllegalClassFormatException("Missing field " + SV_UID_FIELD_NAME
                                    + " for serialized class " + className);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Cannot deserialize " + className +
                            ". Cause: ");
                    e.printStackTrace(System.err);
                    return result;
                }
                object.remove(CLASS_META_KEY);
                object.remove(SV_UID_FIELD_NAME);
                result = context.deserialize(jsonElement, clazz);
            }
            return result;
        }
    }
*/
    //</editor-fold>

    /**
     * Serializers and deserializers for all classes that implement {@link IEvent} interface.
     */
    public class IEventImplementorsSerializer implements JsonSerializer<IEvent>, JsonDeserializer<IEvent> {
        /**
         * The JSON field name containing the canonical serialized class name.
         */
        public static final String CLASS_META_KEY = IEvent.___EVENT_TYPE_CLASS_NAME;
        private final Class<IEvent> IEVENT_INTERFACE_CLASS = IEvent.class;

        /**
         * The JSON field name containing the serial version uid of the serialized class.
         */
        public static final String SV_UID_FIELD_NAME = IEvent.___SV_UID_FIELD_NAME;

        @Override
        public JsonElement serialize(IEvent src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }

            Class<? extends IEvent> clazz = src.getClass();
            if (clazz.equals(IEVENT_INTERFACE_CLASS)) { // highly improbable
                clazz = Event.class;
                System.err.println("Class object of IEvent specified directly instead of class implementing the "
                        + "interface! Falling back to Event.class!");
            }

            JsonElement element = context.serialize(src, clazz);
            try {
                long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                if (serialUid == 0) {
                    serialUid = javaEventSerializer.computeSerialVersionUidFromEvent(src);
                }
                element.getAsJsonObject().addProperty(SV_UID_FIELD_NAME, serialUid);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            element.getAsJsonObject().addProperty(CLASS_META_KEY, getClassFullName(src.getClass()));
            return element;
        }

        @Override
        public IEvent deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) {
            IEvent event = null;
            JsonObject object = jsonElement.getAsJsonObject();
            if (object.has(CLASS_META_KEY)) {
                Class<?> clazz = null;
                String className = object.get(CLASS_META_KEY).getAsString();
                try {
                    clazz = Class.forName(className);
                    long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                    if (object.has(SV_UID_FIELD_NAME)) {
                        long serializedSerialUid = object.get(SV_UID_FIELD_NAME).getAsLong();
                        if (serializedSerialUid != serialUid) {
                            throw new IllegalClassFormatException(SV_UID_FIELD_NAME + "mismatch - original "
                                    + serialUid + ", serialized " + serializedSerialUid + " - class "
                                    + className);
                        }
                    } else {
                        throw new IllegalClassFormatException("Missing field " + SV_UID_FIELD_NAME
                                + " for serialized class " + className);
                    }

                } catch (Exception e) {
                    System.err.println("Cannot deserialize " + className + ". Falling back to Event. Cause: ");
                    e.printStackTrace(System.err);
                    System.err.println("------------");
                    clazz = Event.class;
                }
                object.remove(CLASS_META_KEY);
                object.remove(SV_UID_FIELD_NAME);
                event = context.deserialize(jsonElement, clazz);
            }
            return event;
        }
    }

    /**
     * Constructor. The default encoding used during serialization/deserialization is UTF-16.
     */
    public JsonEventSerializer() {
        this.attemptAutomaticClassRegistration = false;
        this.encoding = getDefaultEncoding();
        this.javaEventSerializer = new JavaEventSerializer();

        this.iEventRuntimeTypeAdapterFactory = RuntimeTypeAdapterFactory.of(IEvent.class,
                IEvent.___EVENT_TYPE_CLASS_NAME,
                (RuntimeTypeAdapterFactory.RuntimeFieldInjector<IEvent>) (into, value) -> {
                    if (into == null || value == null) return;
                    Class<?> clazz = value.getClass();
                    try {
                        long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                        if (serialUid == 0) {
                            serialUid = javaEventSerializer.computeSerialVersionUidFromEvent(value);
                        }
                        into.addProperty(IEvent.___SV_UID_FIELD_NAME, serialUid);
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                },
                (RuntimeTypeAdapterFactory.RuntimeFieldRemover) (from, typeClassName, labelToSubtype) -> {
                    if (from == null) return;
                    Class<?> clazz = labelToSubtype.getOrDefault(typeClassName, null);
                    if (clazz == null && isAttemptAutomaticClassRegistration()) {
                        try {
                            clazz = Class.forName(typeClassName);
                            if (clazz != null && (IEvent.class.isAssignableFrom(clazz))) {
                                @SuppressWarnings("unchecked")
                                Class<? extends IEvent> cls = (Class<? extends IEvent>) clazz;
                                registerEvent(cls);
                                labelToSubtype.put(typeClassName, cls);
                            }
                        } catch (Exception e) {
                        }
                    }
                    try {
                        clazz = labelToSubtype.getOrDefault(typeClassName, Event.class); //Class.forName(typeClassName);
                        long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                        if (from.has(IEvent.___SV_UID_FIELD_NAME)) {
                            long serializedSerialUid = from.get(IEvent.___SV_UID_FIELD_NAME).getAsLong();
                            if (serializedSerialUid != serialUid) {
                                throw new IllegalClassFormatException(IEvent.___SV_UID_FIELD_NAME
                                        + " mismatch - original " + serialUid + ", serialized " + serializedSerialUid
                                        + " - class " + typeClassName);
                            }
                        } else {
                            throw new IllegalClassFormatException("Missing field " + IEvent.___SV_UID_FIELD_NAME
                                    + " for serialized class " + typeClassName);
                        }
                    } catch (Exception e) {
                        System.err.println("Cannot deserialize " + typeClassName + " using " + clazz + ". Cause: ");
                        e.printStackTrace(System.err);
                        System.err.println("--------------------");
                        throw new IOException(e);
                    }
                    from.remove(IEvent.___SV_UID_FIELD_NAME);
                }
        );

        this.iParameterComparisonOutcomeRuntimeTypeAdapterFactory = RuntimeTypeAdapterFactory.of(
                IParameterComparisonOutcome.class, ParameterComparisonOutcome.___RESULT_TYPE_CLASS_NAME,
                (RuntimeTypeAdapterFactory.RuntimeFieldInjector<IParameterComparisonOutcome>)(into, value) -> {
                    if (into == null || value == null) return;
                    Class<?> clazz = value.getClass();
                    try {
                        if (!clazz.equals(ParameterComparisonOutcome.class)) {
                            long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                            if (serialUid == 0) {
                                serialUid = javaEventSerializer.computeSerialVersionUidFromEventHelper(value);
                            }
                            into.addProperty(ParametersComparisonResult.___SV_UID_FIELD_NAME, serialUid);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                },
                (RuntimeTypeAdapterFactory.RuntimeFieldRemover) (from, typeClassName, labelToSubtype) -> {
                    if (from == null) return;
                    try {
                        Class<?> clazz = labelToSubtype.getOrDefault(typeClassName, ParameterComparisonOutcome.class); //Class.forName(typeClassName);
                        if (!clazz.equals(ParameterComparisonOutcome.class)) {
                            long serialUid = ObjectStreamClass.lookup(clazz).getSerialVersionUID();
                            if (from.has(ParametersComparisonResult.___SV_UID_FIELD_NAME)) {
                                long serializedSerialUid = from.get(ParametersComparisonResult.___SV_UID_FIELD_NAME)
                                        .getAsLong();
                                if (serializedSerialUid != serialUid) {
                                    throw new IllegalClassFormatException(IEvent.___SV_UID_FIELD_NAME
                                            + " mismatch - original " + serialUid + ", serialized "
                                            + serializedSerialUid + " - class " + typeClassName);
                                }
                            } else {
                                throw new IllegalClassFormatException("Missing field " + IEvent.___SV_UID_FIELD_NAME
                                        + " for serialized class " + typeClassName);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Cannot deserialize " + typeClassName + ". Cause: ");
                        e.printStackTrace(System.err);
                        throw new IOException(e);
                    }
                    from.remove(ParametersComparisonResult.___SV_UID_FIELD_NAME);
                }
        );
        this.iParameterComparisonOutcomeRuntimeTypeAdapterFactory.registerSubtype(ParameterComparisonOutcome.class,
                getClassFullName(ParameterComparisonOutcome.class));
        this.iParameterComparisonOutcomeRuntimeTypeAdapterFactory.registerSubtype(ParametersComparisonResult.class,
                getClassFullName(ParametersComparisonResult.class));

        this.packageScanner = new ClassesIEventScanner();
        Set<Class<? extends IEvent>> iEventImplementors = packageScanner.scan();
        for (Class<? extends IEvent> implementor : iEventImplementors) {
            this.iEventRuntimeTypeAdapterFactory.registerSubtype(implementor, getClassFullName(implementor));
        }

        this.gsonBuilder = new GsonBuilder().enableComplexMapKeySerialization()
                .registerTypeAdapter(IEventsContainer.class, new IEventsContainerInstanceCreator())
                .registerTypeAdapter(IEvent.class, new IEventImplementorsSerializer())
                //.registerTypeHierarchyAdapter(IParameterComparisonOutcome.class,
                //        new IParameterComparisonOutcomeSerializer())
                //.registerTypeAdapter(ParameterComparisonOutcomeTemplate.class,
                //        new ParameterComparisonOutcomeTemplateSerializer())
                .registerTypeAdapterFactory(iEventRuntimeTypeAdapterFactory)
                .registerTypeAdapterFactory(iParameterComparisonOutcomeRuntimeTypeAdapterFactory);
        /*IEventImplementorsSerializer iEventImplementorsSerializer = new IEventImplementorsSerializer();
        for (Class<? extends IEvent> implementor : iEventImplementors) {
            this.gsonBuilder.registerTypeAdapter(implementor, iEventImplementorsSerializer);
        }*/
        this.gson = this.gsonBuilder.create();
    }

    /**
     * Constructor.
     * @param encoding The encoding used during serialization/deserialization.
     */
    public JsonEventSerializer(final Charset encoding) {
        this();
        this.encoding = encoding;
    }

    /**
     * Constructor.
     * @param packagesWithEvents The packages that contain classes implementing {@link IEvent} interface.
     */
    public JsonEventSerializer(String... packagesWithEvents) {
        this();
        this.packageScanner = new ClassesIEventScanner(packagesWithEvents);
        Set<Class<? extends IEvent>> iEventImplementors = this.packageScanner.scan();
        for (Class<? extends IEvent> implementor : iEventImplementors) {
            this.iEventRuntimeTypeAdapterFactory.registerSubtype(implementor, getClassFullName(implementor));
        }
        this.gson = this.gsonBuilder.create();
    }

    /**
     * Constructor - the most complete one.
     * @param encoding The encoding used during serialization/deserialization.
     *                 The see preferred one take a look at {@link #getDefaultEncoding() getDefaultEncoding()} method.
     * @param classesImplementingEvents Any class objects that represent classes implementing {@link IEvent} interface.
     *                                  Can be null. Useful alternative of packagesWithEvents parameter on platforms
     *                                  where package scanning is not well supported for e.g. Android.
     * @param customDataTypeAdapters Data type adapters used for de/serialization of you own concrete data types,
     *                               that usually appear 'exotic' and not supported by standard. Can be null.
     *                               Each pair consists of class object and class instance that implements
     *                               GSON's JsonSerializer, JsonDeserializer, etc.
     * @param packagesWithEvents The packages that contain classes implementing {@link IEvent} interface.
     *                           Can be null or missing.
     */
    public JsonEventSerializer(final Charset encoding, Set<Class<? extends IEvent>> classesImplementingEvents,
                               Map<Type, Object> customDataTypeAdapters, String... packagesWithEvents) {
        this();
        this.encoding = encoding;
        this.packageScanner = new ClassesIEventScanner(packagesWithEvents);
        Set<Class<? extends IEvent>> iEventImplementors = this.packageScanner.scan();
        if (classesImplementingEvents != null) {
            iEventImplementors.addAll(classesImplementingEvents);
        }
        for (Class<? extends IEvent> implementor : iEventImplementors) {
            this.iEventRuntimeTypeAdapterFactory.registerSubtype(implementor, getClassFullName(implementor));
        }
        if (customDataTypeAdapters != null && !customDataTypeAdapters.isEmpty()) {
            for (Map.Entry<Type, Object> entry : customDataTypeAdapters.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    this.gsonBuilder = this.gsonBuilder.registerTypeAdapter(entry.getKey(), entry.getValue());
                }
            }
        }
        this.gson = this.gsonBuilder.create();
    }

    /**
     * Registers additional event classes.
     * @param event The event class to be registered.
     */
    public void registerEvent(Class<? extends IEvent> event) {
        if (event != null) {
            synchronized (this) {
                this.iEventRuntimeTypeAdapterFactory.registerSubtype(event, getClassFullName(event));
                this.gson = this.gsonBuilder.create();
            }
        }
    }

    /**
     * Registers additional event classes.
     * @param events The set of event classes to be registered.
     */
    public void registerEvent(Set<Class<? extends IEvent>> events) {
        if (events != null) {
            synchronized (this) {
                events.forEach(ev -> this.iEventRuntimeTypeAdapterFactory.registerSubtype(ev, getClassFullName(ev)));
                this.gson = this.gsonBuilder.create();
            }
        }
    }

    /**
     * Retrieves the status of the automatic class registration mode.
     * @return True if the mode is activated otherwise false.
     */
    public boolean isAttemptAutomaticClassRegistration() {
        return attemptAutomaticClassRegistration;
    }

    /**
     * Sets on or off the automatic class registration mode. This function by is a slower fallback alternative when a
     * class cannot be de/serialized and allows this class to automatically attempt to register the missing class
     * in case of a problem. However the performance might be reduced as well as the security of the application, thus
     * by default is turned off.
     * @param attemptAutomaticClassRegistration True to turn on the function. False to turn it off.
     */
    public void setAttemptAutomaticClassRegistration(boolean attemptAutomaticClassRegistration) {
        this.attemptAutomaticClassRegistration = attemptAutomaticClassRegistration;
    }

    /**
     * Returns the default encoding used during de/serialization from/to strings.
     * @return The used encoding instance.
     */
    @SuppressWarnings("SameReturnValue")
    public static Charset getDefaultEncoding() {
        return StandardCharsets.UTF_16;
    }

    private static String getClassFullName(Class c) {
        String result = null;
        if (c != null) {
            result = c.getCanonicalName();
            if (result == null) {
                result = c.getName();
            }
        }
        return result;
    }

    /**
     * Creates new String from byte data, based on the set encoding.
     * @param data The (serialized) data to use.
     * @return String containing the converted from byte[] type data.
     */
    public String stringFromBytes(byte[] data) {
        return new String(data, this.encoding);
    }

    /**
     * Creates new byte[] from String data, based on the set encoding.
     * @param data The (serialized) data to use.
     * @return Array of bytes containing the converted from String type data. Can be null if the provided param is null.
     */
    public byte[] bytesFromString(String data) {
        if (data != null) {
            return data.getBytes(this.encoding);
        }
        return null;
    }

    /**
     * Returns the encoding for String/byte data types used in this instance.
     * @return A constant describing the encoding/decoding charset using during the de/serialization process.
     */
    public final Charset getEncoding() {
        return this.encoding;
    }

    /**
     * Serializes class for which there is a serializer implementation to byte data.
     * @param object The original instantiated object to be serialized.
     * @param <T> The type of the object for which a serializer GSON utility exists.
     * @return The serialized byte representation of the object.
     * @throws IOException If an I/O error occurs while reading stream header
     * @throws SecurityException If untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException If serializedEvent is null
     */
    private <T> byte[] _serialize(T object) throws IOException {
        try {
            return this.gson.toJson(object).getBytes(this.encoding);
        } catch (RuntimeException ex) {
            Throwable t = ex.getCause();
            if (t != null && ((t = t.getCause()) != null) && t instanceof JsonParseException) {
                if (t.getCause() != null && ((t = t.getCause()) instanceof IOException)) {
                    throw (IOException) t;
                }
                throw new IOException(t);
            }
            throw new IOException(ex);
        }
    }

    @Override
    public byte[] serialize(IEvent event) throws IOException {
        try {
            return this._serialize(event);
        } catch (IOException ex) {
            registerEvent(event.getClass());
            return this._serialize(event);
        }
    }

    @Override
    public byte[] serializePCO(IParameterComparisonOutcome comparisonOutcome) throws IOException {
        return this._serialize(comparisonOutcome);
    }

    @Override
    public byte[] serializePCOT(ParameterComparisonOutcomeTemplate comparisonOutcomeTemplate) throws IOException {
        return this._serialize(comparisonOutcomeTemplate);
    }

    /**
     * Deserialize byte data representing object implementing object for which a corresponding de/serializer GSON
     * utility exists to an actual object instance.
     * @param serializedData The serialized object data which will be converted to an actual object.
     * @param representative The class object of the class that will be used to represent the deserialized data.
     * @return An initialized object.
     * @throws java.io.StreamCorruptedException If the provided data cannot form a stream header that is correct
     * @throws IOException If an I/O error occurs while reading stream header or if the resulting of the deserialization
     *                     object cannot be instantiated, because its class is not found in the system
     * @throws SecurityException If untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException If serializedEvent is null
     */
    private <R> R _deserialize(byte[] serializedData, Class<? extends R> representative) throws IOException {
        try {
            return this.gson.fromJson(new String(serializedData, this.encoding), representative);
        } catch (RuntimeException ex) {
            Throwable t = ex.getCause();
            if (t != null && ((t = t.getCause()) != null) && t instanceof JsonParseException) {
                if (t.getCause() != null && ((t = t.getCause()) instanceof IOException)) {
                    throw (IOException) t;
                }
                throw new IOException(t);
            }
            throw new IOException(ex);
        }
    }

    /**
     * Deserialize byte data representing object implementing IEvent to an actual object instance.
     * @param serializedEvent The serialized IEvent which will be converted to an actual object.
     * @param representative The class object of the class that will be used to represent the deserialized data.
     * @return An initialized object implementing IEvent.
     * @throws java.io.StreamCorruptedException If the provided data cannot form a stream header that is correct
     * @throws IOException If an I/O error occurs while reading stream header or if the resulting of the deserialization
     *                     object cannot be instantiated, because its class is not found in the system
     * @throws SecurityException If untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException If serializedEvent is null
     */
    public IEvent deserialize(byte[] serializedEvent, Class<? extends IEvent> representative) throws IOException {
        try {
            return this._deserialize(serializedEvent, representative);
        } catch (Exception e) {
            if (isAttemptAutomaticClassRegistration()) {
                // if not registered class is the case the auto registration should have fixed it. Retry...
                return this._deserialize(serializedEvent, representative);
            }
            throw e;
        }
    }

    /**
     * Deserialization, where the class representative will be Event. For better representation use the other version of
     * this method.
     * @param serializedEvent The serialized IEvent which will be converted to an actual object.
     * @return Event instance with the initialized data.
     * @throws IOException If an I/O error occurs while reading stream header or if the resulting of the deserialization
     *                     object cannot be instantiated, because its class is not found in the system
     * @throws SecurityException If untrusted subclass illegally overrides security-sensitive methods
     * @throws NullPointerException If serializedEvent or the encoding is null
     * @throws JsonSyntaxException If the conversion to JSON format fails
     */
    @Override
    public IEvent deserialize(byte[] serializedEvent) throws IOException {
        return this.deserialize(serializedEvent, IEvent.class);
    }

    @Override
    public IParameterComparisonOutcome deserializePCO(byte[] serializedComparisonOutcome) throws IOException {
        return this._deserialize(serializedComparisonOutcome, IParameterComparisonOutcome.class);
    }

    @Override
    public ParameterComparisonOutcomeTemplate deserializePCOT(byte[] serializedComparisonOutcomeTemplate) throws
            IOException {
        return this._deserialize(serializedComparisonOutcomeTemplate, ParameterComparisonOutcomeTemplate.class);
    }
}
