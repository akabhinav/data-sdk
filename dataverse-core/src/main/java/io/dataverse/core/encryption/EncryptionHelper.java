package io.dataverse.core.encryption;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for automatic field-level encryption and decryption.
 *
 * <p>Uses reflection to find @Encrypted fields and automatically encrypt/decrypt them.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class EncryptionHelper {

  /**
   * Checks if an entity class has any encrypted fields.
   *
   * @param entityClass the entity class
   * @return true if the class has @Encrypted fields
   */
  public static boolean hasEncryptedFields(Class<?> entityClass) {
    for (Field field : getAllFields(entityClass)) {
      if (field.isAnnotationPresent(Encrypted.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Encrypts all @Encrypted fields in an entity.
   *
   * <p>Modifies the entity in-place by replacing plaintext values with encrypted values.
   *
   * @param entity the entity to encrypt
   * @param encryptionProvider the encryption provider
   * @param <T> the entity type
   */
  public static <T> void encryptFields(T entity, EncryptionProvider encryptionProvider) {
    if (entity == null || encryptionProvider == null) {
      return;
    }

    for (Field field : getAllFields(entity.getClass())) {
      Encrypted annotation = field.getAnnotation(Encrypted.class);
      if (annotation == null) {
        continue;
      }

      // Only encrypt String fields
      if (field.getType() != String.class) {
        throw new EncryptionException(
            "Field " + field.getName() + " is annotated with @Encrypted but is not a String. " +
            "Only String fields can be encrypted."
        );
      }

      try {
        field.setAccessible(true);
        String plaintext = (String) field.get(entity);

        if (plaintext != null && !plaintext.isEmpty()) {
          String keyId = annotation.keyId().isEmpty() ? null : annotation.keyId();
          String ciphertext = encryptionProvider.encrypt(plaintext, keyId, annotation.deterministic());
          field.set(entity, ciphertext);
        }
      } catch (IllegalAccessException e) {
        throw new EncryptionException("Failed to encrypt field: " + field.getName(), e);
      }
    }
  }

  /**
   * Decrypts all @Encrypted fields in an entity.
   *
   * <p>Modifies the entity in-place by replacing encrypted values with plaintext values.
   *
   * @param entity the entity to decrypt
   * @param encryptionProvider the encryption provider
   * @param <T> the entity type
   */
  public static <T> void decryptFields(T entity, EncryptionProvider encryptionProvider) {
    if (entity == null || encryptionProvider == null) {
      return;
    }

    for (Field field : getAllFields(entity.getClass())) {
      Encrypted annotation = field.getAnnotation(Encrypted.class);
      if (annotation == null) {
        continue;
      }

      try {
        field.setAccessible(true);
        String ciphertext = (String) field.get(entity);

        if (ciphertext != null && !ciphertext.isEmpty()) {
          String keyId = annotation.keyId().isEmpty() ? null : annotation.keyId();
          String plaintext = encryptionProvider.decrypt(ciphertext, keyId);
          field.set(entity, plaintext);
        }
      } catch (IllegalAccessException e) {
        throw new EncryptionException("Failed to decrypt field: " + field.getName(), e);
      }
    }
  }

  /**
   * Gets all fields including inherited fields from superclasses.
   *
   * @param clazz the class to inspect
   * @return list of all fields
   */
  private static List<Field> getAllFields(Class<?> clazz) {
    List<Field> fields = new ArrayList<>();
    Class<?> current = clazz;

    while (current != null && current != Object.class) {
      for (Field field : current.getDeclaredFields()) {
        fields.add(field);
      }
      current = current.getSuperclass();
    }

    return fields;
  }

  /**
   * Creates a deep copy of an entity with encrypted fields.
   *
   * <p>Useful for creating encrypted versions without modifying the original entity.
   *
   * @param entity the entity to copy and encrypt
   * @param encryptionProvider the encryption provider
   * @param <T> the entity type
   * @return a new entity instance with encrypted fields
   */
  public static <T> T encryptCopy(T entity, EncryptionProvider encryptionProvider) {
    if (entity == null) {
      return null;
    }

    try {
      // Create a shallow copy
      @SuppressWarnings("unchecked")
      T copy = (T) entity.getClass().getDeclaredConstructor().newInstance();

      // Copy all field values
      for (Field field : getAllFields(entity.getClass())) {
        field.setAccessible(true);
        Object value = field.get(entity);
        field.set(copy, value);
      }

      // Encrypt the copy
      encryptFields(copy, encryptionProvider);
      return copy;
    } catch (Exception e) {
      throw new EncryptionException("Failed to create encrypted copy", e);
    }
  }

  /**
   * Creates a deep copy of an entity with decrypted fields.
   *
   * <p>Useful for creating decrypted versions without modifying the original entity.
   *
   * @param entity the entity to copy and decrypt
   * @param encryptionProvider the encryption provider
   * @param <T> the entity type
   * @return a new entity instance with decrypted fields
   */
  public static <T> T decryptCopy(T entity, EncryptionProvider encryptionProvider) {
    if (entity == null) {
      return null;
    }

    try {
      // Create a shallow copy
      @SuppressWarnings("unchecked")
      T copy = (T) entity.getClass().getDeclaredConstructor().newInstance();

      // Copy all field values
      for (Field field : getAllFields(entity.getClass())) {
        field.setAccessible(true);
        Object value = field.get(entity);
        field.set(copy, value);
      }

      // Decrypt the copy
      decryptFields(copy, encryptionProvider);
      return copy;
    } catch (Exception e) {
      throw new EncryptionException("Failed to create decrypted copy", e);
    }
  }
}
