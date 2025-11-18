package io.dataverse.core.mapping;

import java.lang.annotation.*;

/**
 * Annotation-based entity mapping definitions.
 *
 * <p>Feature #26: Annotation-Based Mapping - Java annotations for entity-table mapping
 * <p>Feature #27: Convention-over-Configuration - Automatic mapping based on naming conventions
 *
 * @since 1.0.0
 */
public class EntityMapping {

    /**
     * Marks a class as a database entity.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Entity {
        /**
         * Table name. If empty, derived from class name.
         */
        String table() default "";

        /**
         * Schema name.
         */
        String schema() default "";

        /**
         * Catalog name.
         */
        String catalog() default "";
    }

    /**
     * Maps a field to a database column.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Column {
        /**
         * Column name. If empty, derived from field name.
         */
        String name() default "";

        /**
         * Whether the column is nullable.
         */
        boolean nullable() default true;

        /**
         * Whether the column is unique.
         */
        boolean unique() default false;

        /**
         * Column length for string types.
         */
        int length() default 255;

        /**
         * Precision for decimal types.
         */
        int precision() default 0;

        /**
         * Scale for decimal types.
         */
        int scale() default 0;

        /**
         * Whether this column is insertable.
         */
        boolean insertable() default true;

        /**
         * Whether this column is updatable.
         */
        boolean updatable() default true;

        /**
         * Column definition (DDL).
         */
        String columnDefinition() default "";
    }

    /**
     * Marks a field as the primary key.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Id {
    }

    /**
     * Specifies generation strategy for primary keys.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface GeneratedValue {
        GenerationType strategy() default GenerationType.AUTO;
        String generator() default "";
    }

    /**
     * Primary key generation strategies.
     */
    public enum GenerationType {
        AUTO, IDENTITY, SEQUENCE, UUID, TABLE
    }

    /**
     * Defines a sequence generator.
     */
    @Target({ElementType.TYPE, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SequenceGenerator {
        String name();
        String sequenceName() default "";
        int initialValue() default 1;
        int allocationSize() default 50;
    }

    /**
     * Marks a field as transient (not persisted).
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Transient {
    }

    /**
     * Marks a field for version-based optimistic locking.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Version {
    }

    /**
     * Embedded object mapping.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Embedded {
    }

    /**
     * Marks a class as embeddable.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Embeddable {
    }

    /**
     * One-to-One relationship.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OneToOne {
        Class<?> targetEntity() default void.class;
        FetchType fetch() default FetchType.EAGER;
        CascadeType[] cascade() default {};
        String mappedBy() default "";
        boolean optional() default true;
    }

    /**
     * One-to-Many relationship.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OneToMany {
        Class<?> targetEntity() default void.class;
        FetchType fetch() default FetchType.LAZY;
        CascadeType[] cascade() default {};
        String mappedBy() default "";
        boolean orphanRemoval() default false;
    }

    /**
     * Many-to-One relationship.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManyToOne {
        Class<?> targetEntity() default void.class;
        FetchType fetch() default FetchType.EAGER;
        CascadeType[] cascade() default {};
        boolean optional() default true;
    }

    /**
     * Many-to-Many relationship.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManyToMany {
        Class<?> targetEntity() default void.class;
        FetchType fetch() default FetchType.LAZY;
        CascadeType[] cascade() default {};
        String mappedBy() default "";
    }

    /**
     * Join column specification.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JoinColumn {
        String name() default "";
        String referencedColumnName() default "";
        boolean nullable() default true;
        boolean insertable() default true;
        boolean updatable() default true;
    }

    /**
     * Join table for many-to-many relationships.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JoinTable {
        String name() default "";
        JoinColumn[] joinColumns() default {};
        JoinColumn[] inverseJoinColumns() default {};
    }

    /**
     * Fetch type for relationships.
     */
    public enum FetchType {
        LAZY, EAGER
    }

    /**
     * Cascade types for relationships.
     */
    public enum CascadeType {
        ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH
    }

    /**
     * Maps an enum field.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enumerated {
        EnumType value() default EnumType.ORDINAL;
    }

    /**
     * Enum mapping strategies.
     */
    public enum EnumType {
        ORDINAL, STRING
    }

    /**
     * Temporal type mapping.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Temporal {
        TemporalType value();
    }

    /**
     * Temporal types.
     */
    public enum TemporalType {
        DATE, TIME, TIMESTAMP
    }

    /**
     * LOB (Large Object) mapping.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Lob {
    }

    /**
     * JSON column mapping.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JsonColumn {
        String columnDefinition() default "jsonb";
    }

    /**
     * Array column mapping.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ArrayColumn {
        String elementType() default "varchar";
    }

    /**
     * Soft delete support.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SoftDelete {
        String column() default "deleted";
        String deletedValue() default "true";
    }

    /**
     * Audit field - created timestamp.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CreatedAt {
    }

    /**
     * Audit field - updated timestamp.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UpdatedAt {
    }

    /**
     * Audit field - created by.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CreatedBy {
    }

    /**
     * Audit field - updated by.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UpdatedBy {
    }

    /**
     * Field-level encryption.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Encrypted {
        String algorithm() default "AES/GCM/NoPadding";
        String keyId() default "";
    }

    /**
     * Composite key specification.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IdClass {
        Class<?> value();
    }

    /**
     * Embedded ID for composite keys.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EmbeddedId {
    }

    /**
     * Inheritance strategy.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Inheritance {
        InheritanceType strategy() default InheritanceType.SINGLE_TABLE;
    }

    /**
     * Inheritance strategies.
     */
    public enum InheritanceType {
        SINGLE_TABLE, JOINED, TABLE_PER_CLASS
    }

    /**
     * Discriminator column for inheritance.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DiscriminatorColumn {
        String name() default "DTYPE";
        DiscriminatorType discriminatorType() default DiscriminatorType.STRING;
        int length() default 31;
    }

    /**
     * Discriminator value for inheritance.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DiscriminatorValue {
        String value();
    }

    /**
     * Discriminator types.
     */
    public enum DiscriminatorType {
        STRING, CHAR, INTEGER
    }

    /**
     * Custom type converter.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Convert {
        Class<? extends AttributeConverter<?, ?>> converter();
        boolean disableConversion() default false;
    }

    /**
     * Attribute converter interface.
     */
    public interface AttributeConverter<X, Y> {
        Y convertToDatabaseColumn(X attribute);
        X convertToEntityAttribute(Y dbData);
    }

    /**
     * Index definition.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Indexes.class)
    public @interface Index {
        String name() default "";
        String[] columnList();
        boolean unique() default false;
    }

    /**
     * Container for multiple indexes.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Indexes {
        Index[] value();
    }

    /**
     * Named query definition.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(NamedQueries.class)
    public @interface NamedQuery {
        String name();
        String query();
    }

    /**
     * Container for multiple named queries.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NamedQueries {
        NamedQuery[] value();
    }
}
