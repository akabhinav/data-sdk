package io.dataverse.core.mapping;

import io.dataverse.core.mapping.EntityMapping.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Runtime entity metadata extracted from annotations and conventions.
 *
 * <p>Feature #27: Convention-over-Configuration - Automatic mapping based on naming conventions
 *
 * @since 1.0.0
 */
public class EntityMetadata {

    private final Class<?> entityClass;
    private final String tableName;
    private final String schema;
    private final FieldMetadata idField;
    private final Map<String, FieldMetadata> fields;
    private final Map<String, RelationshipMetadata> relationships;
    private final InheritanceType inheritanceType;
    private final String discriminatorColumn;
    private final String discriminatorValue;
    private final boolean softDelete;
    private final String softDeleteColumn;

    private EntityMetadata(Builder builder) {
        this.entityClass = builder.entityClass;
        this.tableName = builder.tableName;
        this.schema = builder.schema;
        this.idField = builder.idField;
        this.fields = Map.copyOf(builder.fields);
        this.relationships = Map.copyOf(builder.relationships);
        this.inheritanceType = builder.inheritanceType;
        this.discriminatorColumn = builder.discriminatorColumn;
        this.discriminatorValue = builder.discriminatorValue;
        this.softDelete = builder.softDelete;
        this.softDeleteColumn = builder.softDeleteColumn;
    }

    /**
     * Build metadata from an entity class.
     */
    public static EntityMetadata fromClass(Class<?> entityClass) {
        Builder builder = new Builder(entityClass);

        // Extract table name
        Entity entityAnn = entityClass.getAnnotation(Entity.class);
        if (entityAnn != null && !entityAnn.table().isEmpty()) {
            builder.tableName(entityAnn.table());
            builder.schema(entityAnn.schema());
        } else {
            builder.tableName(toSnakeCase(entityClass.getSimpleName()));
        }

        // Check for soft delete
        SoftDelete softDeleteAnn = entityClass.getAnnotation(SoftDelete.class);
        if (softDeleteAnn != null) {
            builder.softDelete(true, softDeleteAnn.column());
        }

        // Check for inheritance
        Inheritance inheritanceAnn = entityClass.getAnnotation(Inheritance.class);
        if (inheritanceAnn != null) {
            builder.inheritanceType(inheritanceAnn.strategy());
        }

        DiscriminatorColumn discColAnn = entityClass.getAnnotation(DiscriminatorColumn.class);
        if (discColAnn != null) {
            builder.discriminatorColumn(discColAnn.name());
        }

        DiscriminatorValue discValAnn = entityClass.getAnnotation(DiscriminatorValue.class);
        if (discValAnn != null) {
            builder.discriminatorValue(discValAnn.value());
        }

        // Process fields
        for (Field field : getAllFields(entityClass)) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            field.setAccessible(true);
            FieldMetadata fieldMeta = buildFieldMetadata(field);

            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                builder.idField(fieldMeta);
            }

            builder.field(fieldMeta.fieldName(), fieldMeta);

            // Process relationships
            if (field.isAnnotationPresent(OneToOne.class) ||
                field.isAnnotationPresent(OneToMany.class) ||
                field.isAnnotationPresent(ManyToOne.class) ||
                field.isAnnotationPresent(ManyToMany.class)) {

                RelationshipMetadata relMeta = buildRelationshipMetadata(field);
                builder.relationship(field.getName(), relMeta);
            }
        }

        return builder.build();
    }

    private static FieldMetadata buildFieldMetadata(Field field) {
        String columnName;
        Column columnAnn = field.getAnnotation(Column.class);
        if (columnAnn != null && !columnAnn.name().isEmpty()) {
            columnName = columnAnn.name();
        } else {
            columnName = toSnakeCase(field.getName());
        }

        FieldMetadata.Builder builder = FieldMetadata.builder()
            .fieldName(field.getName())
            .columnName(columnName)
            .fieldType(field.getType())
            .field(field);

        if (columnAnn != null) {
            builder.nullable(columnAnn.nullable())
                .unique(columnAnn.unique())
                .length(columnAnn.length())
                .insertable(columnAnn.insertable())
                .updatable(columnAnn.updatable());
        }

        if (field.isAnnotationPresent(Id.class)) {
            builder.primaryKey(true);
        }

        if (field.isAnnotationPresent(Version.class)) {
            builder.version(true);
        }

        if (field.isAnnotationPresent(GeneratedValue.class)) {
            GeneratedValue genVal = field.getAnnotation(GeneratedValue.class);
            builder.generationType(genVal.strategy());
        }

        if (field.isAnnotationPresent(Enumerated.class)) {
            Enumerated enumAnn = field.getAnnotation(Enumerated.class);
            builder.enumType(enumAnn.value());
        }

        if (field.isAnnotationPresent(CreatedAt.class)) {
            builder.createdAt(true);
        }

        if (field.isAnnotationPresent(UpdatedAt.class)) {
            builder.updatedAt(true);
        }

        if (field.isAnnotationPresent(Encrypted.class)) {
            builder.encrypted(true);
        }

        if (field.isAnnotationPresent(JsonColumn.class)) {
            builder.jsonColumn(true);
        }

        return builder.build();
    }

    private static RelationshipMetadata buildRelationshipMetadata(Field field) {
        RelationshipType type;
        FetchType fetchType = FetchType.LAZY;
        String mappedBy = "";
        CascadeType[] cascades = {};

        if (field.isAnnotationPresent(OneToOne.class)) {
            OneToOne ann = field.getAnnotation(OneToOne.class);
            type = RelationshipType.ONE_TO_ONE;
            fetchType = ann.fetch();
            mappedBy = ann.mappedBy();
            cascades = ann.cascade();
        } else if (field.isAnnotationPresent(OneToMany.class)) {
            OneToMany ann = field.getAnnotation(OneToMany.class);
            type = RelationshipType.ONE_TO_MANY;
            fetchType = ann.fetch();
            mappedBy = ann.mappedBy();
            cascades = ann.cascade();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            ManyToOne ann = field.getAnnotation(ManyToOne.class);
            type = RelationshipType.MANY_TO_ONE;
            fetchType = ann.fetch();
            cascades = ann.cascade();
        } else {
            ManyToMany ann = field.getAnnotation(ManyToMany.class);
            type = RelationshipType.MANY_TO_MANY;
            fetchType = ann.fetch();
            mappedBy = ann.mappedBy();
            cascades = ann.cascade();
        }

        String joinColumn = "";
        JoinColumn joinAnn = field.getAnnotation(JoinColumn.class);
        if (joinAnn != null) {
            joinColumn = joinAnn.name();
        }

        return new RelationshipMetadata(
            field.getName(),
            type,
            field.getType(),
            fetchType,
            mappedBy,
            joinColumn,
            Set.of(cascades)
        );
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // Getters
    public Class<?> getEntityClass() { return entityClass; }
    public String getTableName() { return tableName; }
    public String getSchema() { return schema; }
    public FieldMetadata getIdField() { return idField; }
    public Map<String, FieldMetadata> getFields() { return fields; }
    public Map<String, RelationshipMetadata> getRelationships() { return relationships; }
    public boolean isSoftDelete() { return softDelete; }
    public String getSoftDeleteColumn() { return softDeleteColumn; }

    public String getQualifiedTableName() {
        if (schema != null && !schema.isEmpty()) {
            return schema + "." + tableName;
        }
        return tableName;
    }

    public Collection<FieldMetadata> getInsertableFields() {
        return fields.values().stream()
            .filter(FieldMetadata::isInsertable)
            .toList();
    }

    public Collection<FieldMetadata> getUpdatableFields() {
        return fields.values().stream()
            .filter(FieldMetadata::isUpdatable)
            .toList();
    }

    /**
     * Builder for EntityMetadata.
     */
    public static class Builder {
        private final Class<?> entityClass;
        private String tableName;
        private String schema = "";
        private FieldMetadata idField;
        private final Map<String, FieldMetadata> fields = new LinkedHashMap<>();
        private final Map<String, RelationshipMetadata> relationships = new LinkedHashMap<>();
        private InheritanceType inheritanceType;
        private String discriminatorColumn;
        private String discriminatorValue;
        private boolean softDelete = false;
        private String softDeleteColumn;

        public Builder(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder idField(FieldMetadata idField) {
            this.idField = idField;
            return this;
        }

        public Builder field(String name, FieldMetadata field) {
            this.fields.put(name, field);
            return this;
        }

        public Builder relationship(String name, RelationshipMetadata rel) {
            this.relationships.put(name, rel);
            return this;
        }

        public Builder inheritanceType(InheritanceType type) {
            this.inheritanceType = type;
            return this;
        }

        public Builder discriminatorColumn(String column) {
            this.discriminatorColumn = column;
            return this;
        }

        public Builder discriminatorValue(String value) {
            this.discriminatorValue = value;
            return this;
        }

        public Builder softDelete(boolean enabled, String column) {
            this.softDelete = enabled;
            this.softDeleteColumn = column;
            return this;
        }

        public EntityMetadata build() {
            return new EntityMetadata(this);
        }
    }

    /**
     * Field metadata.
     */
    public record FieldMetadata(
        String fieldName,
        String columnName,
        Class<?> fieldType,
        Field field,
        boolean primaryKey,
        boolean nullable,
        boolean unique,
        int length,
        boolean insertable,
        boolean updatable,
        boolean version,
        GenerationType generationType,
        EnumType enumType,
        boolean createdAt,
        boolean updatedAt,
        boolean encrypted,
        boolean jsonColumn
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String fieldName;
            private String columnName;
            private Class<?> fieldType;
            private Field field;
            private boolean primaryKey = false;
            private boolean nullable = true;
            private boolean unique = false;
            private int length = 255;
            private boolean insertable = true;
            private boolean updatable = true;
            private boolean version = false;
            private GenerationType generationType;
            private EnumType enumType;
            private boolean createdAt = false;
            private boolean updatedAt = false;
            private boolean encrypted = false;
            private boolean jsonColumn = false;

            public Builder fieldName(String fieldName) { this.fieldName = fieldName; return this; }
            public Builder columnName(String columnName) { this.columnName = columnName; return this; }
            public Builder fieldType(Class<?> fieldType) { this.fieldType = fieldType; return this; }
            public Builder field(Field field) { this.field = field; return this; }
            public Builder primaryKey(boolean primaryKey) { this.primaryKey = primaryKey; return this; }
            public Builder nullable(boolean nullable) { this.nullable = nullable; return this; }
            public Builder unique(boolean unique) { this.unique = unique; return this; }
            public Builder length(int length) { this.length = length; return this; }
            public Builder insertable(boolean insertable) { this.insertable = insertable; return this; }
            public Builder updatable(boolean updatable) { this.updatable = updatable; return this; }
            public Builder version(boolean version) { this.version = version; return this; }
            public Builder generationType(GenerationType type) { this.generationType = type; return this; }
            public Builder enumType(EnumType type) { this.enumType = type; return this; }
            public Builder createdAt(boolean createdAt) { this.createdAt = createdAt; return this; }
            public Builder updatedAt(boolean updatedAt) { this.updatedAt = updatedAt; return this; }
            public Builder encrypted(boolean encrypted) { this.encrypted = encrypted; return this; }
            public Builder jsonColumn(boolean jsonColumn) { this.jsonColumn = jsonColumn; return this; }

            public FieldMetadata build() {
                return new FieldMetadata(fieldName, columnName, fieldType, field, primaryKey,
                    nullable, unique, length, insertable, updatable, version, generationType,
                    enumType, createdAt, updatedAt, encrypted, jsonColumn);
            }
        }
    }

    /**
     * Relationship types.
     */
    public enum RelationshipType {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
    }

    /**
     * Relationship metadata.
     */
    public record RelationshipMetadata(
        String fieldName,
        RelationshipType type,
        Class<?> targetType,
        FetchType fetchType,
        String mappedBy,
        String joinColumn,
        Set<CascadeType> cascadeTypes
    ) {}
}
