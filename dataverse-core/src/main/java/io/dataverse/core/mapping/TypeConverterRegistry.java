package io.dataverse.core.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for custom type converters.
 *
 * <p>Feature #31: Custom Type Converters - Register custom converters for complex types
 * <p>Feature #32: JSON Column Support - Native mapping of JSON columns
 * <p>Feature #33: Array/Collection Mapping - Map database arrays to Java collections
 * <p>Feature #34: Enum Support - Automatic enum to string/int conversion
 *
 * @since 1.0.0
 */
public class TypeConverterRegistry {

    private static final TypeConverterRegistry INSTANCE = new TypeConverterRegistry();

    private final Map<TypePair, TypeConverter<?, ?>> converters = new ConcurrentHashMap<>();
    private final Map<Class<?>, JsonSerializer<?>> jsonSerializers = new ConcurrentHashMap<>();

    private TypeConverterRegistry() {
        registerDefaultConverters();
    }

    public static TypeConverterRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a custom type converter.
     */
    public <J, D> void register(Class<J> javaType, Class<D> dbType, TypeConverter<J, D> converter) {
        converters.put(new TypePair(javaType, dbType), converter);
    }

    /**
     * Register a JSON serializer for a type.
     */
    public <T> void registerJsonSerializer(Class<T> type, JsonSerializer<T> serializer) {
        jsonSerializers.put(type, serializer);
    }

    /**
     * Get a converter for the given types.
     */
    @SuppressWarnings("unchecked")
    public <J, D> Optional<TypeConverter<J, D>> getConverter(Class<J> javaType, Class<D> dbType) {
        TypeConverter<?, ?> converter = converters.get(new TypePair(javaType, dbType));
        return Optional.ofNullable((TypeConverter<J, D>) converter);
    }

    /**
     * Convert a database value to Java type.
     */
    @SuppressWarnings("unchecked")
    public <T> T convertToJava(Object dbValue, Class<T> targetType) {
        if (dbValue == null) {
            return null;
        }

        Class<?> sourceType = dbValue.getClass();

        // Check for exact match
        if (targetType.isAssignableFrom(sourceType)) {
            return (T) dbValue;
        }

        // Check for registered converter
        TypeConverter<?, ?> converter = converters.get(new TypePair(targetType, sourceType));
        if (converter != null) {
            return (T) ((TypeConverter<T, Object>) converter).toJava(dbValue);
        }

        // Handle enums
        if (targetType.isEnum()) {
            return convertToEnum(dbValue, (Class<T>) targetType);
        }

        // Handle common conversions
        return convertBasicType(dbValue, targetType);
    }

    /**
     * Convert a Java value to database type.
     */
    @SuppressWarnings("unchecked")
    public Object convertToDatabase(Object javaValue, Class<?> targetDbType) {
        if (javaValue == null) {
            return null;
        }

        Class<?> sourceType = javaValue.getClass();

        // Check for registered converter
        TypeConverter<?, ?> converter = converters.get(new TypePair(sourceType, targetDbType));
        if (converter != null) {
            return ((TypeConverter<Object, Object>) converter).toDatabase(javaValue);
        }

        // Handle enums
        if (javaValue instanceof Enum) {
            return convertEnumToDatabase((Enum<?>) javaValue, targetDbType);
        }

        return javaValue;
    }

    /**
     * Serialize an object to JSON string.
     */
    @SuppressWarnings("unchecked")
    public <T> String toJson(T object) {
        if (object == null) {
            return null;
        }

        JsonSerializer<T> serializer = (JsonSerializer<T>) jsonSerializers.get(object.getClass());
        if (serializer != null) {
            return serializer.serialize(object);
        }

        // Default simple serialization (would use Jackson in production)
        return simpleToJson(object);
    }

    /**
     * Deserialize a JSON string to object.
     */
    @SuppressWarnings("unchecked")
    public <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        JsonSerializer<T> serializer = (JsonSerializer<T>) jsonSerializers.get(type);
        if (serializer != null) {
            return serializer.deserialize(json);
        }

        // Would use Jackson in production
        throw new UnsupportedOperationException("No JSON serializer registered for: " + type);
    }

    /**
     * Convert a database array to Java collection.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> arrayToList(Object dbArray, Class<T> elementType) {
        if (dbArray == null) {
            return Collections.emptyList();
        }

        if (dbArray instanceof java.sql.Array sqlArray) {
            try {
                Object[] array = (Object[]) sqlArray.getArray();
                List<T> result = new ArrayList<>(array.length);
                for (Object element : array) {
                    result.add(convertToJava(element, elementType));
                }
                return result;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to convert array", e);
            }
        }

        if (dbArray.getClass().isArray()) {
            Object[] array = (Object[]) dbArray;
            List<T> result = new ArrayList<>(array.length);
            for (Object element : array) {
                result.add(convertToJava(element, elementType));
            }
            return result;
        }

        throw new IllegalArgumentException("Cannot convert to array: " + dbArray.getClass());
    }

    /**
     * Read a value from ResultSet with type conversion.
     */
    public <T> T readFromResultSet(ResultSet rs, String columnName, Class<T> type) throws SQLException {
        Object value = rs.getObject(columnName);
        return convertToJava(value, type);
    }

    private void registerDefaultConverters() {
        // String conversions
        register(UUID.class, String.class, new TypeConverter<>() {
            @Override
            public String toDatabase(UUID value) {
                return value.toString();
            }

            @Override
            public UUID toJava(String dbValue) {
                return UUID.fromString(dbValue);
            }
        });

        // Date/Time conversions
        register(LocalDate.class, java.sql.Date.class, new TypeConverter<>() {
            @Override
            public java.sql.Date toDatabase(LocalDate value) {
                return java.sql.Date.valueOf(value);
            }

            @Override
            public LocalDate toJava(java.sql.Date dbValue) {
                return dbValue.toLocalDate();
            }
        });

        register(LocalDateTime.class, java.sql.Timestamp.class, new TypeConverter<>() {
            @Override
            public java.sql.Timestamp toDatabase(LocalDateTime value) {
                return java.sql.Timestamp.valueOf(value);
            }

            @Override
            public LocalDateTime toJava(java.sql.Timestamp dbValue) {
                return dbValue.toLocalDateTime();
            }
        });

        register(LocalTime.class, java.sql.Time.class, new TypeConverter<>() {
            @Override
            public java.sql.Time toDatabase(LocalTime value) {
                return java.sql.Time.valueOf(value);
            }

            @Override
            public LocalTime toJava(java.sql.Time dbValue) {
                return dbValue.toLocalTime();
            }
        });

        register(Instant.class, java.sql.Timestamp.class, new TypeConverter<>() {
            @Override
            public java.sql.Timestamp toDatabase(Instant value) {
                return java.sql.Timestamp.from(value);
            }

            @Override
            public Instant toJava(java.sql.Timestamp dbValue) {
                return dbValue.toInstant();
            }
        });

        // Duration
        register(Duration.class, Long.class, new TypeConverter<>() {
            @Override
            public Long toDatabase(Duration value) {
                return value.toMillis();
            }

            @Override
            public Duration toJava(Long dbValue) {
                return Duration.ofMillis(dbValue);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T convertToEnum(Object dbValue, Class<T> enumType) {
        if (dbValue instanceof String) {
            return (T) Enum.valueOf((Class<Enum>) enumType, (String) dbValue);
        } else if (dbValue instanceof Number) {
            int ordinal = ((Number) dbValue).intValue();
            Object[] constants = enumType.getEnumConstants();
            if (ordinal >= 0 && ordinal < constants.length) {
                return (T) constants[ordinal];
            }
        }
        throw new IllegalArgumentException("Cannot convert to enum: " + dbValue);
    }

    private Object convertEnumToDatabase(Enum<?> enumValue, Class<?> targetType) {
        if (targetType == String.class) {
            return enumValue.name();
        } else if (Number.class.isAssignableFrom(targetType)) {
            return enumValue.ordinal();
        }
        return enumValue.name();
    }

    @SuppressWarnings("unchecked")
    private <T> T convertBasicType(Object value, Class<T> targetType) {
        if (targetType == String.class) {
            return (T) value.toString();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) return (T) value;
            if (value instanceof Number) return (T) Boolean.valueOf(((Number) value).intValue() != 0);
            return (T) Boolean.valueOf(value.toString());
        }

        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to " + targetType);
    }

    private String simpleToJson(Object object) {
        // Very simple serialization - would use Jackson in production
        if (object instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) object;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(simpleToJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (object instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Collection<?>) object) {
                if (!first) sb.append(",");
                sb.append(simpleToJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        } else if (object instanceof String) {
            return "\"" + object + "\"";
        } else {
            return String.valueOf(object);
        }
    }

    /**
     * Type converter interface.
     */
    public interface TypeConverter<J, D> {
        D toDatabase(J javaValue);
        J toJava(D dbValue);
    }

    /**
     * JSON serializer interface.
     */
    public interface JsonSerializer<T> {
        String serialize(T object);
        T deserialize(String json);
    }

    /**
     * Type pair key for converter lookup.
     */
    private record TypePair(Class<?> javaType, Class<?> dbType) {}
}
