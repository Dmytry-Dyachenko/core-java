/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.base;

import java.lang.reflect.Type;
import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.StringifierRegistry.getStringifier;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static org.spine3.util.Exceptions.conversionArgumentException;

/**
 * Utility class for working with {@code Stringifier}s.
 *
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
public class Stringifiers {

    private Stringifiers() {
        // Disable instantiation of this utility class.
    }

    /**
     * Converts the passed value to the string representation.
     *
     * <p>Use this method for converting non-generic objects. For generic objects,
     * please use {@link #toString(Object, Type)}.
     *
     * @param object the object to convert
     * @param <T>    the type of the object
     * @return the string representation of the passed object
     */
    public static <T> String toString(T object) {
        checkNotNull(object);
        return toString(object, object.getClass());
    }

    /**
     * Converts the passed value to the string representation.
     *
     * <p>This method must be used of the passed object is a generic type.
     *
     * @param <T>     the type of the object to convert
     * @param object  to object to convert
     * @param typeOfT the type of the passed object
     * @return the string representation of the passed object
     * @throws MissingStringifierException if passed value cannot be converted
     */
    public static <T> String toString(T object, Type typeOfT) {
        checkNotNull(object);
        checkNotNull(typeOfT);

        final Stringifier<T> stringifier = getStringifier(typeOfT);
        final String result = stringifier.convert(object);
        return result;
    }

    /**
     * Parses string to the appropriate value.
     *
     * @param <T>     the type of the value to return
     * @param str     the string to convert
     * @param typeOfT the type into which to convert the string
     * @return the parsed value from string
     * @throws MissingStringifierException if passed value cannot be converted
     */
    public static <T> T fromString(String str, Type typeOfT) {
        checkNotNull(str);
        checkNotNull(typeOfT);

        final Stringifier<T> stringifier = getStringifier(typeOfT);
        final T result = stringifier.reverse()
                                    .convert(str);
        return result;
    }

    /**
     * Obtains {@code Stringifier} for map.
     *
     * @param keyClass   the class of keys are maintained by this map
     * @param valueClass the class  of mapped values
     * @param <K>        the type of keys are maintained by this map
     * @param <V>        the type of mapped values
     * @return the stringifier for the map
     */
    public static <K, V> Stringifier<Map<K, V>> mapStringifier(Class<K> keyClass,
                                                               Class<V> valueClass) {
        checkNotNull(keyClass);
        checkNotNull(valueClass);
        final Stringifier<Map<K, V>> mapStringifier = new MapStringifier<>(keyClass, valueClass);
        return mapStringifier;
    }

    /**
     * @param keyClass   the class of keys are maintained by this map
     * @param valueClass the class  of mapped values
     * @param delimiter  the delimiter for the passed map elements via string
     * @param <K>        the type of keys are maintained by this map
     * @param <V>        the type of mapped values
     * @return the stringifier for the map
     */
    public static <K, V> Stringifier<Map<K, V>> mapStringifier(Class<K> keyClass,
                                                               Class<V> valueClass,
                                                               String delimiter) {
        checkNotNull(keyClass);
        checkNotNull(valueClass);
        checkNotNull(delimiter);
        final Stringifier<Map<K, V>> mapStringifier =
                new MapStringifier<>(keyClass, valueClass, delimiter);
        return mapStringifier;
    protected static class MapStringifier<K, V> extends Stringifier<Map<K, V>> {

        private static final String KEY_VALUE_DELIMITER = ESCAPE_SEQUENCE + ':';

        /**
         * The delimiter for the passed elements in the {@code String} representation,
         * {@code DEFAULT_ELEMENTS_DELIMITER} by default.
         */
        private final String delimiter;
        private final Class<K> keyClass;
        private final Class<V> valueClass;

        public MapStringifier(Class<K> keyClass, Class<V> valueClass) {
            super();
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.delimiter = ESCAPE_SEQUENCE + DEFAULT_ELEMENTS_DELIMITER;
        }

        /**
         * That constructor should be used when need to use
         * a custom delimiter of the elements during conversion.
         *
         * @param keyClass   the class of the key elements
         * @param valueClass the class of the value elements
         * @param delimiter  the delimiter for the passed elements via string
         */
        public MapStringifier(Class<K> keyClass, Class<V> valueClass, String delimiter) {
            super();
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.delimiter = ESCAPE_SEQUENCE + delimiter;
        }

        @Override
        protected String doForward(Map<K, V> map) {
            final String result = map.toString();
            return result;
        }

        @Override
        protected Map<K, V> doBackward(String s) {
            final String[] buckets = s.split(Pattern.quote(delimiter));
            final Map<K, V> resultMap = newHashMap();

            for (String bucket : buckets) {
                saveConvertedBucket(resultMap, bucket);
            }
            Ints.stringConverter();
            return resultMap;
        }

        private Map<K, V> saveConvertedBucket(Map<K, V> resultMap, String element) {
            final String[] keyValue = element.split(Pattern.quote(KEY_VALUE_DELIMITER));
            checkKeyValue(keyValue);

            final String key = keyValue[0];
            final String value = keyValue[1];

            try {
                final K convertedKey = Stringifiers.convert(keyClass, key);
                final V convertedValue = Stringifiers.convert(valueClass, value);
                resultMap.put(convertedKey, convertedValue);
                return resultMap;
            } catch (Throwable ignored) {
                throw conversionArgumentException(CONVERSION_EXCEPTION);
            }
        }

        private static void checkKeyValue(String[] keyValue) {
            if (keyValue.length != 2) {
                final String exMessage =
                        "Illegal key - value format, key value should be " +
                        "separated with single `:` character";
                throw conversionArgumentException(exMessage);
            }
        }
    }

    /**
     * The stringifier for the {@code List} classes.
     *
     * <p> The converter for the type of the elements in the list
     * should be registered in the {@code StringifierRegistry} class
     * for the correct usage of the {@code ListStringifier} converter.
     *
     * @param <T> the type of the elements in the list.
     */
    protected static class ListStringifier<T> extends Stringifier<List<T>> {

        private final Class<T> listGenericClass;

        /**
         * The delimiter for the passed elements in the {@code String} representation,
         * {@code DEFAULT_DELIMITER} by default.
         */
        private final String delimiter;

        public ListStringifier(Class<T> listGenericClass) {
            super();
            this.delimiter = ESCAPE_SEQUENCE + DEFAULT_ELEMENTS_DELIMITER;
            this.listGenericClass = listGenericClass;
        }

        /**
         * That constructor should be used when need to use
         * a custom delimiter during conversion.
         *
         * @param listGenericClass the class of the list elements
         * @param delimiter        the delimiter for the passed elements via string
         */
        public ListStringifier(Class<T> listGenericClass, String delimiter) {
            super();
            this.listGenericClass = listGenericClass;
            this.delimiter = ESCAPE_SEQUENCE + delimiter;
        }

        @Override
        protected String doForward(List<T> list) {
            final String result = list.toString();
            return result;
        }

        @Override
        protected List<T> doBackward(String s) {
            final String[] elements = s.split(delimiter);

            final List<T> result = newArrayList();
            for (String element : elements) {
                final T convertedValue = Stringifiers.convert(listGenericClass, element);
                result.add(convertedValue);
            }
            return result;
        }
    }
}
