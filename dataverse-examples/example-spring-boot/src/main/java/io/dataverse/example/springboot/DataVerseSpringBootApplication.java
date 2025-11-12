package io.dataverse.example.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application demonstrating DataVerse SDK integration.
 *
 * <p>This application showcases:
 * <ul>
 *   <li>Auto-configuration of DataVerse adapters</li>
 *   <li>YAML-based configuration</li>
 *   <li>REST API using DataVerse repositories</li>
 *   <li>Health checks via Spring Boot Actuator</li>
 *   <li>Multi-adapter usage (DynamoDB, MongoDB, Redis)</li>
 * </ul>
 *
 * <p>Run the application:
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <p>Access endpoints:
 * <ul>
 *   <li>API: http://localhost:8080/api/products</li>
 *   <li>Health: http://localhost:8080/actuator/health</li>
 *   <li>DataVerse Health: http://localhost:8080/actuator/health/dataverse</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
@SpringBootApplication
public class DataVerseSpringBootApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataVerseSpringBootApplication.class, args);
  }
}
