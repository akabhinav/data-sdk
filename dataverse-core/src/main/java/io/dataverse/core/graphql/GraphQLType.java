package io.dataverse.core.graphql;

/**
 * GraphQL scalar types.
 *
 * <p>Standard scalar types from the GraphQL specification.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public enum GraphQLType {

  /**
   * ID scalar - unique identifier.
   */
  ID,

  /**
   * String scalar - UTF-8 character sequence.
   */
  STRING,

  /**
   * Int scalar - signed 32-bit integer.
   */
  INT,

  /**
   * Float scalar - signed double-precision floating-point.
   */
  FLOAT,

  /**
   * Boolean scalar - true or false.
   */
  BOOLEAN;

  /**
   * Maps Java type to GraphQL type.
   *
   * @param javaType the Java type
   * @return GraphQL type
   */
  public static GraphQLType fromJavaType(Class<?> javaType) {
    if (javaType == Long.class || javaType == long.class ||
        javaType == Integer.class || javaType == int.class ||
        javaType == Short.class || javaType == short.class ||
        javaType == Byte.class || javaType == byte.class) {
      return INT;
    }

    if (javaType == Double.class || javaType == double.class ||
        javaType == Float.class || javaType == float.class) {
      return FLOAT;
    }

    if (javaType == Boolean.class || javaType == boolean.class) {
      return BOOLEAN;
    }

    if (javaType == String.class) {
      return STRING;
    }

    // Default to ID for unknown types
    return ID;
  }

  @Override
  public String toString() {
    return switch (this) {
      case ID -> "ID";
      case STRING -> "String";
      case INT -> "Int";
      case FLOAT -> "Float";
      case BOOLEAN -> "Boolean";
    };
  }
}
