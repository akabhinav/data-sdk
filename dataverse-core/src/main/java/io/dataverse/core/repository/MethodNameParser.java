package io.dataverse.core.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses repository method names into query structures.
 *
 * <p>Converts Spring Data style method names into executable queries.
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MethodNameParser {

  private static final Pattern METHOD_PATTERN = Pattern.compile(
      "^(find|findAll|count|exists|delete)By(.+?)(?:OrderBy(.+?))?$"
  );

  private static final List<String> OPERATORS = Arrays.asList(
      "GreaterThanEqual", "LessThanEqual", "GreaterThan", "LessThan",
      "StartingWith", "EndingWith", "Containing", "NotContaining",
      "Like", "NotLike", "Between", "IsNull", "IsNotNull",
      "True", "False", "In", "NotIn"
  );

  /**
   * Parses a repository method name into a ParsedMethod.
   *
   * @param methodName the method name to parse
   * @return the parsed method structure
   * @throws IllegalArgumentException if method name is invalid
   */
  public static ParsedMethod parse(String methodName) {
    Matcher matcher = METHOD_PATTERN.matcher(methodName);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "Invalid method name: " + methodName +
          ". Must follow pattern: [find|findAll|count|exists|delete]By[Property][Operator]"
      );
    }

    String action = matcher.group(1);
    String criteria = matcher.group(2);
    String orderBy = matcher.group(3);

    ParsedMethod parsed = new ParsedMethod();
    parsed.setAction(QueryAction.fromString(action));
    parsed.setConditions(parseCriteria(criteria));

    if (orderBy != null) {
      parsed.setOrderBy(parseOrderBy(orderBy));
    }

    return parsed;
  }

  /**
   * Parses the criteria portion of a method name (e.g., "UsernameAndAgeGreaterThan").
   */
  private static List<Condition> parseCriteria(String criteria) {
    List<Condition> conditions = new ArrayList<>();

    // Split by And/Or (but preserve them)
    String[] parts = criteria.split("(?=(And|Or))");

    String currentLogic = "AND";
    StringBuilder currentProperty = new StringBuilder();

    for (String part : parts) {
      if (part.equals("And")) {
        currentLogic = "AND";
        continue;
      } else if (part.equals("Or")) {
        currentLogic = "OR";
        continue;
      }

      // Remove leading "And" or "Or"
      String cleaned = part.replaceFirst("^(And|Or)", "");
      if (cleaned.isEmpty()) continue;

      // Find operator in this part
      Operator operator = null;
      String property = cleaned;

      for (String op : OPERATORS) {
        if (cleaned.endsWith(op)) {
          operator = Operator.fromString(op);
          property = cleaned.substring(0, cleaned.length() - op.length());
          break;
        }
      }

      if (operator == null) {
        operator = Operator.EQUALS;
      }

      // Convert property from PascalCase to camelCase
      property = toCamelCase(property);

      Condition condition = new Condition();
      condition.setProperty(property);
      condition.setOperator(operator);
      condition.setLogic(currentLogic);
      conditions.add(condition);
    }

    return conditions;
  }

  /**
   * Parses the OrderBy portion (e.g., "UsernameAscAgeDesc").
   */
  private static List<OrderBy> parseOrderBy(String orderBy) {
    List<OrderBy> orderByList = new ArrayList<>();

    // Split by property and direction
    String remaining = orderBy;
    while (!remaining.isEmpty()) {
      // Try to find a property followed by Asc or Desc
      int ascIndex = remaining.indexOf("Asc");
      int descIndex = remaining.indexOf("Desc");

      if (ascIndex == -1 && descIndex == -1) {
        // Default to ascending
        OrderBy order = new OrderBy();
        order.setProperty(toCamelCase(remaining));
        order.setDirection("ASC");
        orderByList.add(order);
        break;
      }

      boolean isAsc = (descIndex == -1) || (ascIndex != -1 && ascIndex < descIndex);
      int dirIndex = isAsc ? ascIndex : descIndex;
      int dirLength = isAsc ? 3 : 4;

      String property = remaining.substring(0, dirIndex);
      OrderBy order = new OrderBy();
      order.setProperty(toCamelCase(property));
      order.setDirection(isAsc ? "ASC" : "DESC");
      orderByList.add(order);

      remaining = remaining.substring(dirIndex + dirLength);
    }

    return orderByList;
  }

  /**
   * Converts PascalCase to camelCase.
   */
  private static String toCamelCase(String pascalCase) {
    if (pascalCase == null || pascalCase.isEmpty()) {
      return pascalCase;
    }
    return Character.toLowerCase(pascalCase.charAt(0)) + pascalCase.substring(1);
  }

  /**
   * Represents a parsed method structure.
   */
  public static class ParsedMethod {
    private QueryAction action;
    private List<Condition> conditions;
    private List<OrderBy> orderBy;

    public QueryAction getAction() { return action; }
    public void setAction(QueryAction action) { this.action = action; }
    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }
    public List<OrderBy> getOrderBy() { return orderBy; }
    public void setOrderBy(List<OrderBy> orderBy) { this.orderBy = orderBy; }
  }

  /**
   * Represents a query condition.
   */
  public static class Condition {
    private String property;
    private Operator operator;
    private String logic;  // AND or OR

    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }
    public String getLogic() { return logic; }
    public void setLogic(String logic) { this.logic = logic; }
  }

  /**
   * Represents an ORDER BY clause.
   */
  public static class OrderBy {
    private String property;
    private String direction;  // ASC or DESC

    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
  }

  /**
   * Query action enumeration.
   */
  public enum QueryAction {
    FIND, FIND_ALL, COUNT, EXISTS, DELETE;

    public static QueryAction fromString(String action) {
      return switch (action.toLowerCase()) {
        case "find" -> FIND;
        case "findall" -> FIND_ALL;
        case "count" -> COUNT;
        case "exists" -> EXISTS;
        case "delete" -> DELETE;
        default -> throw new IllegalArgumentException("Unknown action: " + action);
      };
    }
  }

  /**
   * Query operator enumeration.
   */
  public enum Operator {
    EQUALS,
    GREATER_THAN, GREATER_THAN_EQUAL,
    LESS_THAN, LESS_THAN_EQUAL,
    LIKE, NOT_LIKE,
    STARTING_WITH, ENDING_WITH, CONTAINING, NOT_CONTAINING,
    BETWEEN,
    IS_NULL, IS_NOT_NULL,
    TRUE, FALSE,
    IN, NOT_IN;

    public static Operator fromString(String op) {
      return switch (op) {
        case "GreaterThan" -> GREATER_THAN;
        case "GreaterThanEqual" -> GREATER_THAN_EQUAL;
        case "LessThan" -> LESS_THAN;
        case "LessThanEqual" -> LESS_THAN_EQUAL;
        case "Like" -> LIKE;
        case "NotLike" -> NOT_LIKE;
        case "StartingWith" -> STARTING_WITH;
        case "EndingWith" -> ENDING_WITH;
        case "Containing" -> CONTAINING;
        case "NotContaining" -> NOT_CONTAINING;
        case "Between" -> BETWEEN;
        case "IsNull" -> IS_NULL;
        case "IsNotNull" -> IS_NOT_NULL;
        case "True" -> TRUE;
        case "False" -> FALSE;
        case "In" -> IN;
        case "NotIn" -> NOT_IN;
        default -> EQUALS;
      };
    }
  }
}
