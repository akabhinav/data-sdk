package io.dataverse.core.graphql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of a GraphQL query execution.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * GraphQLResult result = executor.execute(query, variables);
 *
 * if (result.hasErrors()) {
 *     for (GraphQLError error : result.getErrors()) {
 *         System.err.println(error.getMessage());
 *     }
 * } else {
 *     Map<String, Object> data = result.getData();
 *     System.out.println(data);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class GraphQLResult {

  private final Map<String, Object> data;
  private final List<GraphQLError> errors;

  private GraphQLResult(Map<String, Object> data, List<GraphQLError> errors) {
    this.data = data;
    this.errors = errors;
  }

  /**
   * Creates successful result.
   *
   * @param data result data
   * @return result
   */
  public static GraphQLResult success(Map<String, Object> data) {
    return new GraphQLResult(data, Collections.emptyList());
  }

  /**
   * Creates error result.
   *
   * @param errors errors
   * @return result
   */
  public static GraphQLResult error(List<GraphQLError> errors) {
    return new GraphQLResult(null, errors);
  }

  /**
   * Creates error result with single error.
   *
   * @param message error message
   * @return result
   */
  public static GraphQLResult error(String message) {
    List<GraphQLError> errors = List.of(new GraphQLError(message));
    return new GraphQLResult(null, errors);
  }

  /**
   * Gets result data.
   *
   * @return data map
   */
  public Map<String, Object> getData() {
    return data;
  }

  /**
   * Gets errors.
   *
   * @return errors list
   */
  public List<GraphQLError> getErrors() {
    return errors;
  }

  /**
   * Checks if result has errors.
   *
   * @return true if has errors
   */
  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }

  /**
   * Checks if result is successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return !hasErrors();
  }

  /**
   * GraphQL error.
   */
  public static class GraphQLError {
    private final String message;
    private final List<Location> locations;

    public GraphQLError(String message) {
      this(message, Collections.emptyList());
    }

    public GraphQLError(String message, List<Location> locations) {
      this.message = message;
      this.locations = locations;
    }

    public String getMessage() {
      return message;
    }

    public List<Location> getLocations() {
      return locations;
    }

    @Override
    public String toString() {
      return "GraphQLError{message='" + message + "'}";
    }
  }

  /**
   * Error location.
   */
  public static class Location {
    private final int line;
    private final int column;

    public Location(int line, int column) {
      this.line = line;
      this.column = column;
    }

    public int getLine() {
      return line;
    }

    public int getColumn() {
      return column;
    }
  }
}
