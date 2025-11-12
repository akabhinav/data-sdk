package io.dataverse.example;

import io.dataverse.api.Entity;

/**
 * Example User entity.
 *
 * <p>This is a simple domain object demonstrating how to create entities for DataVerse SDK.
 *
 * @since 1.0.0
 */
public class User implements Entity<String> {

  private String id;
  private String email;
  private String name;
  private String status;
  private int age;

  /** Default constructor required for deserialization. */
  public User() {}

  /**
   * Constructs a new user.
   *
   * @param email the user's email
   * @param name the user's name
   * @param age the user's age
   */
  public User(String email, String name, int age) {
    this.email = email;
    this.name = name;
    this.age = age;
    this.status = "ACTIVE";
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public String toString() {
    return String.format(
        "User{id='%s', email='%s', name='%s', status='%s', age=%d}",
        id, email, name, status, age);
  }
}
