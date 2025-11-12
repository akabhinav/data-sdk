package io.dataverse.core.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for method name parsing.
 *
 * @since 1.0.0
 */
@DisplayName("Method Name Parser Tests")
class MethodNameParserTest {

  @Test
  @DisplayName("Should parse simple findBy method")
  void shouldParseSimpleFindBy() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsername");

    // Then
    assertThat(parsed.getAction()).isEqualTo(MethodNameParser.QueryAction.FIND);
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.EQUALS);
  }

  @Test
  @DisplayName("Should parse findBy with AND condition")
  void shouldParseFindByWithAnd() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameAndEmail");

    // Then
    assertThat(parsed.getConditions()).hasSize(2);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getLogic()).isEqualTo("AND");
    assertThat(parsed.getConditions().get(1).getProperty()).isEqualTo("email");
    assertThat(parsed.getConditions().get(1).getLogic()).isEqualTo("AND");
  }

  @Test
  @DisplayName("Should parse findBy with OR condition")
  void shouldParseFindByWithOr() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameOrEmail");

    // Then
    assertThat(parsed.getConditions()).hasSize(2);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(1).getProperty()).isEqualTo("email");
    assertThat(parsed.getConditions().get(1).getLogic()).isEqualTo("OR");
  }

  @Test
  @DisplayName("Should parse findBy with GreaterThan operator")
  void shouldParseFindByGreaterThan() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByAgeGreaterThan");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("age");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.GREATER_THAN);
  }

  @Test
  @DisplayName("Should parse findBy with LessThan operator")
  void shouldParseFindByLessThan() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByAgeLessThan");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("age");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.LESS_THAN);
  }

  @Test
  @DisplayName("Should parse findBy with Like operator")
  void shouldParseFindByLike() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameLike");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.LIKE);
  }

  @Test
  @DisplayName("Should parse findBy with StartingWith operator")
  void shouldParseFindByStartingWith() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameStartingWith");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.STARTING_WITH);
  }

  @Test
  @DisplayName("Should parse findBy with EndingWith operator")
  void shouldParseFindByEndingWith() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameEndingWith");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.ENDING_WITH);
  }

  @Test
  @DisplayName("Should parse findBy with Containing operator")
  void shouldParseFindByContaining() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameContaining");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.CONTAINING);
  }

  @Test
  @DisplayName("Should parse findBy with Between operator")
  void shouldParseFindByBetween() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByAgeBetween");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("age");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.BETWEEN);
  }

  @Test
  @DisplayName("Should parse findBy with IsNull operator")
  void shouldParseFindByIsNull() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByEmailIsNull");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("email");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.IS_NULL);
  }

  @Test
  @DisplayName("Should parse findBy with IsNotNull operator")
  void shouldParseFindByIsNotNull() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByEmailIsNotNull");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("email");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.IS_NOT_NULL);
  }

  @Test
  @DisplayName("Should parse findBy with True operator")
  void shouldParseFindByTrue() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByActiveTrue");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("active");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.TRUE);
  }

  @Test
  @DisplayName("Should parse findBy with False operator")
  void shouldParseFindByFalse() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByActiveFalse");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("active");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.FALSE);
  }

  @Test
  @DisplayName("Should parse findBy with In operator")
  void shouldParseFindByIn() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByUsernameIn");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.IN);
  }

  @Test
  @DisplayName("Should parse findBy with OrderBy")
  void shouldParseFindByWithOrderBy() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByActiveOrderByUsernameAsc");

    // Then
    assertThat(parsed.getConditions()).hasSize(1);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("active");
    assertThat(parsed.getOrderBy()).hasSize(1);
    assertThat(parsed.getOrderBy().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getOrderBy().get(0).getDirection()).isEqualTo("ASC");
  }

  @Test
  @DisplayName("Should parse findBy with OrderBy descending")
  void shouldParseFindByWithOrderByDesc() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByActiveTrueOrderByCreatedAtDesc");

    // Then
    assertThat(parsed.getOrderBy()).hasSize(1);
    assertThat(parsed.getOrderBy().get(0).getProperty()).isEqualTo("createdAt");
    assertThat(parsed.getOrderBy().get(0).getDirection()).isEqualTo("DESC");
  }

  @Test
  @DisplayName("Should parse findBy with multiple OrderBy")
  void shouldParseFindByWithMultipleOrderBy() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("findByActiveOrderByUsernameAscAgeDesc");

    // Then
    assertThat(parsed.getOrderBy()).hasSize(2);
    assertThat(parsed.getOrderBy().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getOrderBy().get(0).getDirection()).isEqualTo("ASC");
    assertThat(parsed.getOrderBy().get(1).getProperty()).isEqualTo("age");
    assertThat(parsed.getOrderBy().get(1).getDirection()).isEqualTo("DESC");
  }

  @Test
  @DisplayName("Should parse countBy method")
  void shouldParseCountBy() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("countByActiveTrue");

    // Then
    assertThat(parsed.getAction()).isEqualTo(MethodNameParser.QueryAction.COUNT);
    assertThat(parsed.getConditions()).hasSize(1);
  }

  @Test
  @DisplayName("Should parse existsBy method")
  void shouldParseExistsBy() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("existsByUsername");

    // Then
    assertThat(parsed.getAction()).isEqualTo(MethodNameParser.QueryAction.EXISTS);
    assertThat(parsed.getConditions()).hasSize(1);
  }

  @Test
  @DisplayName("Should parse deleteBy method")
  void shouldParseDeleteBy() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse("deleteByUsername");

    // Then
    assertThat(parsed.getAction()).isEqualTo(MethodNameParser.QueryAction.DELETE);
    assertThat(parsed.getConditions()).hasSize(1);
  }

  @Test
  @DisplayName("Should parse complex method with multiple conditions and operators")
  void shouldParseComplexMethod() {
    // When
    MethodNameParser.ParsedMethod parsed = MethodNameParser.parse(
        "findByUsernameContainingAndAgeGreaterThanOrEmailIsNullOrderByUsernameAsc"
    );

    // Then
    assertThat(parsed.getConditions()).hasSize(3);
    assertThat(parsed.getConditions().get(0).getProperty()).isEqualTo("username");
    assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo(MethodNameParser.Operator.CONTAINING);
    assertThat(parsed.getConditions().get(1).getProperty()).isEqualTo("age");
    assertThat(parsed.getConditions().get(1).getOperator()).isEqualTo(MethodNameParser.Operator.GREATER_THAN);
    assertThat(parsed.getConditions().get(2).getProperty()).isEqualTo("email");
    assertThat(parsed.getConditions().get(2).getOperator()).isEqualTo(MethodNameParser.Operator.IS_NULL);
    assertThat(parsed.getOrderBy()).hasSize(1);
  }

  @Test
  @DisplayName("Should throw exception for invalid method name")
  void shouldThrowExceptionForInvalidMethodName() {
    // When/Then
    assertThrows(IllegalArgumentException.class, () -> {
      MethodNameParser.parse("invalidMethodName");
    });
  }

  @Test
  @DisplayName("Should throw exception for method without By")
  void shouldThrowExceptionForMethodWithoutBy() {
    // When/Then
    assertThrows(IllegalArgumentException.class, () -> {
      MethodNameParser.parse("findUsername");
    });
  }
}
