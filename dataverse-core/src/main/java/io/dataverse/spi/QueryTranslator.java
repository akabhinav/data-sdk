package io.dataverse.spi;

/**
 * Service Provider Interface for translating generic queries to data source-specific queries.
 *
 * <p>QueryTranslator converts abstract query representations (built using QueryBuilder)
 * into native query formats understood by the underlying data source. This enables the
 * same query API to work across different databases.
 *
 * <p><strong>Example Translations:</strong>
 * <ul>
 *   <li>DynamoDB: Converts to FilterExpression, KeyConditionExpression</li>
 *   <li>MongoDB: Converts to BSON query documents</li>
 *   <li>SQL: Converts to SQL WHERE clauses</li>
 *   <li>Elasticsearch: Converts to Query DSL</li>
 * </ul>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface QueryTranslator {

  /**
   * Translates a generic query to a native query.
   *
   * @param query the generic query representation, must not be {@code null}
   * @return the translated native query, never {@code null}
   * @throws QueryTranslationException if translation fails
   * @throws IllegalArgumentException if query is {@code null}
   */
  NativeQuery translate(Query query);

  /**
   * Validates that a query can be translated for this data source.
   *
   * @param query the query to validate, must not be {@code null}
   * @return {@code true} if the query can be translated, {@code false} otherwise
   * @throws IllegalArgumentException if query is {@code null}
   */
  boolean supports(Query query);

  /**
   * Returns the native query language name.
   *
   * <p>Examples: "DynamoDB FilterExpression", "MongoDB Query", "SQL", "Elasticsearch DSL"
   *
   * @return the query language name, never {@code null}
   */
  String getQueryLanguage();

  /**
   * Generic query representation (data source-agnostic).
   */
  interface Query {
    // Query structure (populated by QueryBuilder implementation)
  }

  /**
   * Native query representation (data source-specific).
   */
  interface NativeQuery {
    /**
     * Returns the native query object in data source-specific format.
     *
     * @return the native query object
     */
    Object getNativeQueryObject();

    /**
     * Returns the query as a string (for logging/debugging).
     *
     * @return the query string representation
     */
    String toQueryString();
  }
}
