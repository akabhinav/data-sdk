package io.dataverse.example.springboot.model;

import io.dataverse.api.Entity;

/**
 * Product entity.
 *
 * @since 1.0.0
 */
public class Product implements Entity<String> {

  private String id;
  private String name;
  private String category;
  private double price;
  private int stock;
  private boolean available;

  public Product() {
  }

  public Product(String name, String category, double price, int stock) {
    this.name = name;
    this.category = category;
    this.price = price;
    this.stock = stock;
    this.available = stock > 0;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public int getStock() {
    return stock;
  }

  public void setStock(int stock) {
    this.stock = stock;
    this.available = stock > 0;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }
}
