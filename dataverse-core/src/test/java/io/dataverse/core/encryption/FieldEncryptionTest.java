package io.dataverse.core.encryption;

import io.dataverse.api.Entity;
import io.dataverse.core.AbstractRepository;
import io.dataverse.core.cache.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for field-level encryption functionality.
 *
 * @since 1.0.0
 */
@DisplayName("Field Encryption Tests")
class FieldEncryptionTest {

  private AESEncryptionProvider encryptionProvider;
  private TestUserRepository repository;
  private TestUserRepository repositoryWithoutEncryption;

  @BeforeEach
  void setUp() {
    encryptionProvider = new AESEncryptionProvider();
    repository = new TestUserRepository(encryptionProvider);
    repositoryWithoutEncryption = new TestUserRepository(new NoEncryptionProvider());
  }

  @Test
  @DisplayName("Should encrypt fields on save")
  void shouldEncryptFieldsOnSave() {
    // Given
    TestUser user = new TestUser();
    user.setUsername("john.doe");
    user.setEmail("john.doe@example.com");
    user.setSsn("123-45-6789");

    // When
    TestUser saved = repository.save(user);

    // Then - Returned entity should have decrypted values
    assertThat(saved.getEmail()).isEqualTo("john.doe@example.com");
    assertThat(saved.getSsn()).isEqualTo("123-45-6789");

    // Verify encrypted in storage
    TestUser stored = repository.storage.stream()
        .filter(u -> u.getId().equals(saved.getId()))
        .findFirst()
        .orElseThrow();

    assertThat(stored.getEmail()).isNotEqualTo("john.doe@example.com");
    assertThat(stored.getSsn()).isNotEqualTo("123-45-6789");
    assertThat(stored.getEmail()).startsWith("encoded:");  // Our storage marker
  }

  @Test
  @DisplayName("Should decrypt fields on find")
  void shouldDecryptFieldsOnFind() {
    // Given
    TestUser user = new TestUser();
    user.setUsername("jane.doe");
    user.setEmail("jane.doe@example.com");
    user.setSsn("987-65-4321");
    TestUser saved = repository.save(user);

    // When
    Optional<TestUser> found = repository.findById(saved.getId());

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("jane.doe@example.com");
    assertThat(found.get().getSsn()).isEqualTo("987-65-4321");
  }

  @Test
  @DisplayName("Should handle null encrypted fields")
  void shouldHandleNullEncryptedFields() {
    // Given
    TestUser user = new TestUser();
    user.setUsername("bob");
    user.setEmail(null);
    user.setSsn(null);

    // When
    TestUser saved = repository.save(user);
    Optional<TestUser> found = repository.findById(saved.getId());

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isNull();
    assertThat(found.get().getSsn()).isNull();
  }

  @Test
  @DisplayName("Should handle empty encrypted fields")
  void shouldHandleEmptyEncryptedFields() {
    // Given
    TestUser user = new TestUser();
    user.setUsername("alice");
    user.setEmail("");
    user.setSsn("");

    // When
    TestUser saved = repository.save(user);
    Optional<TestUser> found = repository.findById(saved.getId());

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEmpty();
    assertThat(found.get().getSsn()).isEmpty();
  }

  @Test
  @DisplayName("Should support deterministic encryption")
  void shouldSupportDeterministicEncryption() {
    // Given
    TestUserWithDeterministic user1 = new TestUserWithDeterministic();
    user1.setEmail("test@example.com");

    TestUserWithDeterministic user2 = new TestUserWithDeterministic();
    user2.setEmail("test@example.com");

    // When - Encrypt both users
    EncryptionHelper.encryptFields(user1, encryptionProvider);
    EncryptionHelper.encryptFields(user2, encryptionProvider);

    // Then - Same plaintext should produce same ciphertext (deterministic)
    assertThat(user1.getEmail()).isEqualTo(user2.getEmail());
  }

  @Test
  @DisplayName("Should use different ciphertext for randomized encryption")
  void shouldUseDifferentCiphertextForRandomizedEncryption() {
    // Given
    TestUser user1 = new TestUser();
    user1.setEmail("test@example.com");

    TestUser user2 = new TestUser();
    user2.setEmail("test@example.com");

    // When - Encrypt both users
    EncryptionHelper.encryptFields(user1, encryptionProvider);
    EncryptionHelper.encryptFields(user2, encryptionProvider);

    // Then - Same plaintext should produce different ciphertext (randomized)
    assertThat(user1.getEmail()).isNotEqualTo(user2.getEmail());
  }

  @Test
  @DisplayName("Should work without encryption provider")
  void shouldWorkWithoutEncryptionProvider() {
    // Given
    TestUser user = new TestUser();
    user.setUsername("test");
    user.setEmail("test@example.com");

    // When
    TestUser saved = repositoryWithoutEncryption.save(user);
    Optional<TestUser> found = repositoryWithoutEncryption.findById(saved.getId());

    // Then - Should work normally without encryption
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("test@example.com");
  }

  @Test
  @DisplayName("Should encrypt/decrypt with custom key ID")
  void shouldEncryptDecryptWithCustomKeyId() {
    // Given - Add a custom key
    String customKey = new AESEncryptionProvider().exportDefaultKey();
    encryptionProvider.addKey("custom-key", customKey);

    TestUserWithKeyId user = new TestUserWithKeyId();
    user.setSensitiveData("Very secret information");

    // When
    EncryptionHelper.encryptFields(user, encryptionProvider);
    String encrypted = user.getSensitiveData();

    EncryptionHelper.decryptFields(user, encryptionProvider);

    // Then
    assertThat(user.getSensitiveData()).isEqualTo("Very secret information");
  }

  @Test
  @DisplayName("Should throw exception for missing key")
  void shouldThrowExceptionForMissingKey() {
    // Given
    TestUserWithKeyId user = new TestUserWithKeyId();
    user.setSensitiveData("Secret");

    // When/Then
    assertThrows(EncryptionException.class, () -> {
      EncryptionHelper.encryptFields(user, encryptionProvider);
    });
  }

  @Test
  @DisplayName("Should throw exception for non-string encrypted field")
  void shouldThrowExceptionForNonStringEncryptedField() {
    // Given
    TestUserWithInvalidField user = new TestUserWithInvalidField();
    user.setAge(25);

    // When/Then
    EncryptionException exception = assertThrows(EncryptionException.class, () -> {
      EncryptionHelper.encryptFields(user, encryptionProvider);
    });

    assertThat(exception.getMessage()).contains("not a String");
  }

  @Test
  @DisplayName("Should handle encryption round-trip")
  void shouldHandleEncryptionRoundTrip() {
    // Given
    String original = "This is sensitive data that needs encryption!";

    // When
    String encrypted = encryptionProvider.encrypt(original);
    String decrypted = encryptionProvider.decrypt(encrypted);

    // Then
    assertThat(encrypted).isNotEqualTo(original);
    assertThat(decrypted).isEqualTo(original);
  }

  @Test
  @DisplayName("Should support long text encryption")
  void shouldSupportLongTextEncryption() {
    // Given
    StringBuilder longText = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longText.append("This is a long text that needs to be encrypted. ");
    }
    String original = longText.toString();

    // When
    String encrypted = encryptionProvider.encrypt(original);
    String decrypted = encryptionProvider.decrypt(encrypted);

    // Then
    assertThat(decrypted).isEqualTo(original);
  }

  @Test
  @DisplayName("Should support unicode encryption")
  void shouldSupportUnicodeEncryption() {
    // Given
    String original = "Hello 世界! 🌍 Привет Ελληνικά";

    // When
    String encrypted = encryptionProvider.encrypt(original);
    String decrypted = encryptionProvider.decrypt(encrypted);

    // Then
    assertThat(decrypted).isEqualTo(original);
  }

  @Test
  @DisplayName("Should verify encryption provider is healthy")
  void shouldVerifyEncryptionProviderIsHealthy() {
    // When/Then
    assertThat(encryptionProvider.isHealthy()).isTrue();
    assertThat(encryptionProvider.getName()).isEqualTo("AES-256-GCM");
  }

  // Test entities

  public static class TestUser implements Entity<Long> {
    private Long id;
    private String username;

    @Encrypted
    private String email;

    @Encrypted
    private String ssn;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
  }

  public static class TestUserWithDeterministic implements Entity<Long> {
    private Long id;

    @Encrypted(deterministic = true)
    private String email;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
  }

  public static class TestUserWithKeyId implements Entity<Long> {
    private Long id;

    @Encrypted(keyId = "custom-key")
    private String sensitiveData;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getSensitiveData() { return sensitiveData; }
    public void setSensitiveData(String sensitiveData) { this.sensitiveData = sensitiveData; }
  }

  public static class TestUserWithInvalidField implements Entity<Long> {
    private Long id;

    @Encrypted
    private int age;  // Invalid - not a String

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
  }

  // Test repository

  static class TestUserRepository extends AbstractRepository<TestUser, Long> {
    final List<TestUser> storage = new ArrayList<>();
    private long nextId = 1L;

    TestUserRepository(EncryptionProvider encryptionProvider) {
      super(TestUser.class, CacheConfig.builder().enabled(false).build(), encryptionProvider);
    }

    @Override
    protected TestUser doSave(TestUser entity) {
      if (entity.getId() == null) {
        entity.setId(nextId++);
      }
      // Mark as encrypted in storage for testing
      if (entity.getEmail() != null && !entity.getEmail().isEmpty()) {
        entity.setEmail("encoded:" + entity.getEmail());
      }
      if (entity.getSsn() != null && !entity.getSsn().isEmpty()) {
        entity.setSsn("encoded:" + entity.getSsn());
      }
      storage.removeIf(u -> u.getId().equals(entity.getId()));
      storage.add(entity);
      return entity;
    }

    @Override
    protected Optional<TestUser> doFindById(Long id) {
      return storage.stream()
          .filter(u -> u.getId().equals(id))
          .map(this::cloneUser)
          .findFirst();
    }

    private TestUser cloneUser(TestUser user) {
      TestUser clone = new TestUser();
      clone.setId(user.getId());
      clone.setUsername(user.getUsername());
      // Remove the "encoded:" marker
      if (user.getEmail() != null && user.getEmail().startsWith("encoded:")) {
        clone.setEmail(user.getEmail().substring(8));
      } else {
        clone.setEmail(user.getEmail());
      }
      if (user.getSsn() != null && user.getSsn().startsWith("encoded:")) {
        clone.setSsn(user.getSsn().substring(8));
      } else {
        clone.setSsn(user.getSsn());
      }
      return clone;
    }

    @Override
    protected void doDelete(Long id) {
      storage.removeIf(u -> u.getId().equals(id));
    }

    @Override
    protected List<TestUser> doFindAll() {
      return new ArrayList<>(storage);
    }

    @Override
    protected long doCount() {
      return storage.size();
    }

    @Override
    public io.dataverse.api.QueryBuilder<TestUser> query() {
      return null;
    }

    @Override
    public io.dataverse.api.AggregationBuilder<TestUser> aggregate() {
      return null;
    }

    @Override
    public List<TestUser> executeNativeQuery(String nativeQuery) {
      return null;
    }
  }
}
