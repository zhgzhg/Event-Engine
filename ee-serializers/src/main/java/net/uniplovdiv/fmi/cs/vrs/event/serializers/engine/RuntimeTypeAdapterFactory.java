package net.uniplovdiv.fmi.cs.vrs.event.serializers.engine;

/*
 * Copyright (C) 2011 Google Inc.
 * Copyright (C) 2017 zhgzhg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Adapts values whose runtime type may differ from their declaration type. This
 * is necessary when a field's type is not the same type that GSON should create
 * when deserializing that field. For example, consider these types:
 * <pre>   {@code
 *   abstract class Shape {
 *     int x;
 *     int y;
 *   }
 *   class Circle extends Shape {
 *     int radius;
 *   }
 *   class Rectangle extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Diamond extends Shape {
 *     int width;
 *     int height;
 *   }
 *   class Drawing {
 *     Shape bottomShape;
 *     Shape topShape;
 *   }
 * }</pre>
 * <p>Without additional type information, the serialized JSON is ambiguous. Is
 * the bottom shape in this drawing a rectangle or a diamond? <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * This class addresses this problem by adding type information to the
 * serialized JSON and honoring that type information when the JSON is
 * deserialized: <pre>   {@code
 *   {
 *     "bottomShape": {
 *       "type": "Diamond",
 *       "width": 10,
 *       "height": 5,
 *       "x": 0,
 *       "y": 0
 *     },
 *     "topShape": {
 *       "type": "Circle",
 *       "radius": 2,
 *       "x": 4,
 *       "y": 1
 *     }
 *   }}</pre>
 * Both the type field name ({@code "type"}) and the type labels ({@code
 * "Rectangle"}) are configurable.
 *
 * <h3>Registering Types</h3>
 * Create a {@code RuntimeTypeAdapterFactory} by passing the base type and type field
 * name to the {@link #of} factory method. If you don't supply an explicit type
 * field name, {@code "type"} will be used. <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory
 *       = RuntimeTypeAdapterFactory.of(Shape.class, "type");
 * }</pre>
 * Next register all of your subtypes. Every subtype must be explicitly
 * registered. This protects your application from injection attacks. If you
 * don't supply an explicit type label, the type's simple name will be used.
 * <pre>   {@code
 *   shapeAdapter.registerSubtype(Rectangle.class, "Rectangle");
 *   shapeAdapter.registerSubtype(Circle.class, "Circle");
 *   shapeAdapter.registerSubtype(Diamond.class, "Diamond");
 * }</pre>
 * Finally, register the type adapter factory in your application's GSON builder:
 * <pre>   {@code
 *   Gson gson = new GsonBuilder()
 *       .registerTypeAdapterFactory(shapeAdapterFactory)
 *       .create();
 * }</pre>
 * Like {@code GsonBuilder}, this API supports chaining: <pre>   {@code
 *   RuntimeTypeAdapterFactory<Shape> shapeAdapterFactory = RuntimeTypeAdapterFactory.of(Shape.class)
 *       .registerSubtype(Rectangle.class)
 *       .registerSubtype(Circle.class)
 *       .registerSubtype(Diamond.class);
 * }</pre>
 * @param <T> The class object of the type that will be registered.
 */
public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
    private final Class<?> baseType;
    private final String typeFieldName;
    private final Map<String, Class<?>> labelToSubtype = new LinkedHashMap<String, Class<?>>();
    private final Map<Class<?>, String> subtypeToLabel = new LinkedHashMap<Class<?>, String>();
    private final RuntimeFieldInjector runtimeFieldInjector;
    private final RuntimeFieldRemover runtimeFieldRemover;

    /**
     * Interface used for dynamic injection of fields into {@link JsonObject} during runtime.
     */
    @FunctionalInterface
    public interface RuntimeFieldInjector<R> {
        void inject(JsonObject into, R value);
    }

    /**
     * Interface used for dynamic removal of fields from {@link JsonObject} during runtime.
     */
    @FunctionalInterface
    public interface RuntimeFieldRemover {
        void remove(JsonObject from, String typeClassName, Map<String, Class<?>> labelToSubtype) throws IOException;
    }


    private RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName,
                                      RuntimeFieldInjector runtimeFieldInjector,
                                      RuntimeFieldRemover runtimeFieldRemover) {
        if (typeFieldName == null || baseType == null) {
            throw new NullPointerException();
        }
        this.baseType = baseType;
        this.typeFieldName = typeFieldName;
        this.runtimeFieldInjector = runtimeFieldInjector;
        this.runtimeFieldRemover = runtimeFieldRemover;
    }

    /**
     * Creates a new runtime type adapter using for {@code baseType} using {@code
     * typeFieldName} as the type field name. Type field names are case sensitive.
     * @param <T> The class object of the base type.
     * @param baseType The class representing base data type
     * @param typeFieldName The field name used to designate (contain) the data type.
     * @param runtimeFieldInjector Additional field injector executed during serialization of the object.
     * @param runtimeFieldRemover Additional field remover executed during deserialization of the object.
     * @return Concrete instance of the factory.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName,
                                                      RuntimeFieldInjector runtimeFieldInjector,
                                                      RuntimeFieldRemover runtimeFieldRemover) {
        return new RuntimeTypeAdapterFactory<T>(baseType, typeFieldName, runtimeFieldInjector, runtimeFieldRemover);
    }

    /**
     * Creates a new runtime type adapter using for {@code baseType} using {@code
     * typeFieldName} as the type field name. Type field names are case sensitive.
     * @param <T> The class object of the base type.
     * @param baseType The class representing base data type
     * @param typeFieldName The field name used to designate (contain) the data type.
     * @return Concrete instance of the factory.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<T>(baseType, typeFieldName, null, null);
    }

    /**
     * Creates a new runtime type adapter for {@code baseType} using {@code "type"} as
     * the type field name.
     * @param <T> The class object of the base type.
     * @param baseType The class representing base data type
     * @return Concrete instance of the factory.
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType) {
        return new RuntimeTypeAdapterFactory<T>(baseType, "type", null, null);
    }

    /**
     * Registers {@code type} identified by {@code label}. Labels are case
     * sensitive.
     * @param type The subtype to be registered.
     * @param label Label used for the type for e.g. full class name.
     * @return Concrete instance of the factory.
     * @throws IllegalArgumentException if either {@code type} or {@code label}
     *     have already been registered on this type adapter.
     */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
        if (type == null || label == null) {
            throw new NullPointerException("Type: " + (type != null ? type.toString() : " null") + ", label: " + label);
        }
        /*if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
            throw new IllegalArgumentException("types and labels must be unique");
        }
        labelToSubtype.put(label, type);
        subtypeToLabel.put(type, label);*/
        if (!subtypeToLabel.containsKey(type) && !labelToSubtype.containsKey(label)) {
            labelToSubtype.put(label, type);
            subtypeToLabel.put(type, label);
        }
        return this;
    }

    /**
     * Registers {@code type} identified by its {@link Class#getCanonicalName canonical
     * name}. Labels are case sensitive.
     * @param type The subtype to be registered.
     * @return Concrete instance of the factory.
     * @throws IllegalArgumentException if either {@code type} or its simple name
     *     have already been registered on this type adapter.
     */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type) {
        return registerSubtype(type, type.getCanonicalName());
    }

    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
        //if (type.getRawType() != baseType) {
        if (null == type || !baseType.isAssignableFrom(type.getRawType())) {
            return null;
        }

        final Map<String, TypeAdapter<?>> labelToDelegate
                = new LinkedHashMap<String, TypeAdapter<?>>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate
                = new LinkedHashMap<Class<?>, TypeAdapter<?>>();
        for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            labelToDelegate.put(entry.getKey(), delegate);
            subtypeToDelegate.put(entry.getValue(), delegate);
        }

        return new TypeAdapter<R>() {
            @Override public R read(JsonReader in) throws IOException {
                JsonElement jsonElement = Streams.parse(in);
                JsonElement labelJsonElement = jsonElement.getAsJsonObject().remove(typeFieldName);
                if (labelJsonElement == null) {
                    throw new JsonParseException("cannot deserialize " + baseType
                            + " because it does not define a field named " + typeFieldName);
                }

                String label = labelJsonElement.getAsString();

                if (runtimeFieldRemover != null) {
                    runtimeFieldRemover.remove(jsonElement.getAsJsonObject(), label, labelToSubtype);
                }

                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                        TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
                if (delegate == null) {
                    throw new JsonParseException("cannot deserialize " + baseType + " subtype named "
                            + label + "; did you forget to register a subtype?");
                }

                if (labelToSubtype.get(label).isEnum()) {
                    //String enumVal = jsonElement.getAsJsonObject().getAsJsonPrimitive("enum_value").getAsString();
                    JsonReader jr = new JsonReader(new StringReader(jsonElement.toString()));
                    jr.beginObject();
                    String key = jr.nextName();
                    if (!"enum_value".equals(key)) {
                        throw new JsonParseException("cannot deserialize " + baseType + " enum subtype named "
                                + label + " because is missing an enum_value key");
                    }
                    return delegate.read(jr);
                }

                return delegate.fromJsonTree(jsonElement);
            }

            @SuppressWarnings("unchecked")
            @Override public void write(JsonWriter out, R value) throws IOException {
                Class<?> srcType = value.getClass();
                String label = subtypeToLabel.get(srcType);

                @SuppressWarnings("unchecked") // registration requires that subtype extends T
                        TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
                if (delegate == null) {
                    throw new JsonParseException("cannot serialize " + srcType.getName()
                            + "; did you forget to register a subtype?");
                }
                JsonObject clone = new JsonObject();
                clone.add(typeFieldName, new JsonPrimitive(label));

                if (!srcType.isEnum()) {
                    JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();
                    if (jsonObject.has(typeFieldName)) {
                        throw new JsonParseException("cannot serialize " + srcType.getName()
                                + " because it already defines a field named " + typeFieldName);
                    }
                    for (Map.Entry<String, JsonElement> e : jsonObject.entrySet()) {
                        clone.add(e.getKey(), e.getValue());
                    }
                } else {
                    clone.add("enum_value", new JsonPrimitive(value.toString()));
                }
                if (runtimeFieldInjector != null) {
                    //noinspection unchecked
                    runtimeFieldInjector.inject(clone, value);
                }
                Streams.write(clone, out);
            }
        }.nullSafe();
    }
}

