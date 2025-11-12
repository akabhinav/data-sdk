package io.dataverse.core.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for binding named parameters in native queries.
 *
 * <p>Replaces named parameters (:paramName) with database-specific placeholders
 * (?, $1, etc.) and maintains parameter order for binding.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * String query = "SELECT * FROM users WHERE status = :status AND age > :age";
 * Map<String, Object> params = Map.of(
 *     "status", "ACTIVE",
 *     "age", 18
 * );
 *
 * ParameterBinder binder = new ParameterBinder(query, params);
 * String boundQuery = binder.getQueryWithPlaceholders();
 * // Result: "SELECT * FROM users WHERE status = ? AND age > ?"
 *
 * List<Object> orderedParams = binder.getOrderedParameterValues();
 * // Result: ["ACTIVE", 18]
 * }</pre>
 *
 * <p><strong>Supported Parameter Patterns:</strong>
 * <ul>
 *   <li>:paramName - Standard named parameter</li>
 *   <li>:param_name - Underscores allowed</li>
 *   <li>:paramName123 - Numbers allowed</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class ParameterBinder {

  private static final Pattern NAMED_PARAM_PATTERN = Pattern.compile(":([a-zA-Z][a-zA-Z0-9_]*)");

  private final String originalQuery;
  private final Map<String, Object> parameters;
  private String processedQuery;
  private List<String> parameterNames;
  private List<Object> orderedValues;

  /**
   * Creates a parameter binder.
   *
   * @param query the query with named parameters
   * @param parameters the parameter values
   */
  public ParameterBinder(String query, Map<String, Object> parameters) {
    this.originalQuery = query;
    this.parameters = new HashMap<>(parameters);
    this.parameterNames = new ArrayList<>();
    this.orderedValues = new ArrayList<>();
    processQuery();
  }

  /**
   * Gets the query with placeholders (?) instead of named parameters.
   *
   * @return the processed query
   */
  public String getQueryWithPlaceholders() {
    return processedQuery;
  }

  /**
   * Gets the query with numbered placeholders ($1, $2, etc.) for PostgreSQL.
   *
   * @return the processed query with numbered placeholders
   */
  public String getQueryWithNumberedPlaceholders() {
    String result = processedQuery;
    int index = 1;
    while (result.contains("?")) {
      result = result.replaceFirst("\\?", "\\$" + index);
      index++;
    }
    return result;
  }

  /**
   * Gets parameter names in order of appearance.
   *
   * @return ordered parameter names
   */
  public List<String> getParameterNames() {
    return List.copyOf(parameterNames);
  }

  /**
   * Gets parameter values in order of appearance.
   *
   * @return ordered parameter values
   */
  public List<Object> getOrderedParameterValues() {
    return List.copyOf(orderedValues);
  }

  /**
   * Gets the number of parameters.
   *
   * @return parameter count
   */
  public int getParameterCount() {
    return parameterNames.size();
  }

  /**
   * Validates that all parameters in the query have values.
   *
   * @throws IllegalArgumentException if missing parameters
   */
  public void validate() {
    for (String paramName : parameterNames) {
      if (!parameters.containsKey(paramName)) {
        throw new IllegalArgumentException(
            "Missing parameter value for: " + paramName);
      }
    }
  }

  /**
   * Processes the query to extract parameters and create placeholders.
   */
  private void processQuery() {
    StringBuilder queryBuilder = new StringBuilder();
    Matcher matcher = NAMED_PARAM_PATTERN.matcher(originalQuery);

    int lastEnd = 0;
    while (matcher.find()) {
      // Append text before this parameter
      queryBuilder.append(originalQuery, lastEnd, matcher.start());

      // Get parameter name
      String paramName = matcher.group(1);
      parameterNames.add(paramName);

      // Add parameter value to ordered list
      Object value = parameters.get(paramName);
      if (value == null && !parameters.containsKey(paramName)) {
        throw new IllegalArgumentException(
            "Missing parameter value for: " + paramName);
      }
      orderedValues.add(value);

      // Replace with placeholder
      queryBuilder.append("?");

      lastEnd = matcher.end();
    }

    // Append remaining text
    queryBuilder.append(originalQuery.substring(lastEnd));

    processedQuery = queryBuilder.toString();
  }

  /**
   * Creates a binder for a query without parameters.
   *
   * @param query the query
   * @return parameter binder
   */
  public static ParameterBinder empty(String query) {
    return new ParameterBinder(query, Map.of());
  }

  /**
   * Extracts all parameter names from a query.
   *
   * @param query the query with named parameters
   * @return list of parameter names
   */
  public static List<String> extractParameterNames(String query) {
    List<String> names = new ArrayList<>();
    Matcher matcher = NAMED_PARAM_PATTERN.matcher(query);
    while (matcher.find()) {
      String paramName = matcher.group(1);
      if (!names.contains(paramName)) {
        names.add(paramName);
      }
    }
    return names;
  }

  /**
   * Checks if a query contains named parameters.
   *
   * @param query the query to check
   * @return true if query has named parameters
   */
  public static boolean hasNamedParameters(String query) {
    return NAMED_PARAM_PATTERN.matcher(query).find();
  }
}
