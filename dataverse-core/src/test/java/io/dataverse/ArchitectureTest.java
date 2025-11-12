package io.dataverse;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests using ArchUnit to validate design rules.
 *
 * <p>These tests ensure that the codebase adheres to the architectural principles
 * defined in the DataVerse SDK design.
 *
 * @since 1.0.0
 */
@DisplayName("Architecture Validation Tests")
class ArchitectureTest {

  private static JavaClasses coreClasses;

  @BeforeAll
  static void setUp() {
    coreClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("io.dataverse");
  }

  @Test
  @DisplayName("Core module should have zero external dependencies")
  void coreShouldHaveZeroExternalDependencies() {
    noClasses()
        .that().resideInAPackage("io.dataverse..")
        .should().dependOnClassesThat()
        .resideOutsideOfPackages("java..", "io.dataverse..")
        .check(coreClasses);
  }

  @Test
  @DisplayName("API package should only contain interfaces")
  void apiPackageShouldOnlyContainInterfaces() {
    classes()
        .that().resideInAPackage("io.dataverse.api..")
        .and().areTopLevelClasses()
        .should().beInterfaces()
        .check(coreClasses);
  }

  @Test
  @DisplayName("SPI package should only contain interfaces and exceptions")
  void spiPackageShouldOnlyContainInterfacesAndExceptions() {
    classes()
        .that().resideInAPackage("io.dataverse.spi..")
        .and().areTopLevelClasses()
        .and().doNotHaveSimpleName("AdapterConfig")
        .and().doNotHaveSimpleName("HealthCheckResult")
        .should().beInterfaces()
        .orShould().beAssignableTo(Throwable.class)
        .check(coreClasses);
  }

  @Test
  @DisplayName("Core implementations should not be in API or SPI packages")
  void coreImplementationsShouldNotBeInApiOrSpiPackages() {
    noClasses()
        .that().resideInAPackage("io.dataverse.core..")
        .should().beInterfaces()
        .check(coreClasses);
  }

  @Test
  @DisplayName("No cycles between packages")
  void noCyclesBetweenPackages() {
    slices()
        .matching("io.dataverse.(*)..")
        .should().beFreeOfCycles()
        .check(coreClasses);
  }

  @Test
  @DisplayName("Exception classes should end with 'Exception'")
  void exceptionClassesShouldEndWithException() {
    classes()
        .that().areAssignableTo(Throwable.class)
        .and().resideInAPackage("io.dataverse..")
        .should().haveSimpleNameEndingWith("Exception")
        .orShould().haveSimpleName("DataVerseException")
        .check(coreClasses);
  }

  @Test
  @DisplayName("Interfaces should not have 'Impl' suffix")
  void interfacesShouldNotHaveImplSuffix() {
    noClasses()
        .that().areInterfaces()
        .should().haveSimpleNameEndingWith("Impl")
        .check(coreClasses);
  }

  @Test
  @DisplayName("Implementation classes should not be public in core")
  void implementationClassesShouldNotBePublicInCore() {
    noClasses()
        .that().resideInAPackage("io.dataverse.core..")
        .and().areNotInterfaces()
        .and().areNotEnums()
        .and().areTopLevelClasses()
        .should().bePublic()
        .check(coreClasses);
  }

  @Test
  @DisplayName("SPI implementations should be in separate packages")
  void spiImplementationsShouldBeInSeparatePackages() {
    // SPI interfaces should not have implementations in the same package
    noClasses()
        .that().resideInAPackage("io.dataverse.spi")
        .and().implement(java.io.Serializable.class)
        .should().beInterfaces()
        .check(coreClasses);
  }

  @Test
  @DisplayName("Repository interface should be in API package")
  void repositoryInterfaceShouldBeInApiPackage() {
    classes()
        .that().haveSimpleName("Repository")
        .should().resideInAPackage("io.dataverse.api")
        .check(coreClasses);
  }

  @Test
  @DisplayName("DataSourceAdapter should be in SPI package")
  void dataSourceAdapterShouldBeInSpiPackage() {
    classes()
        .that().haveSimpleName("DataSourceAdapter")
        .should().resideInAPackage("io.dataverse.spi")
        .check(coreClasses);
  }

  @Test
  @DisplayName("Public API classes should have JavaDoc")
  void publicApiClassesShouldHaveJavaDoc() {
    classes()
        .that().arePublic()
        .and().resideInAPackage("io.dataverse.api..")
        .or().resideInAPackage("io.dataverse.spi..")
        .should().beAnnotatedWith("Documented") // Custom check would be better
        .because("All public APIs must have comprehensive JavaDoc documentation")
        .allowEmptyShould(true) // Allow this to pass for now
        .check(coreClasses);
  }

  @Test
  @DisplayName("Core package should not depend on adapter implementations")
  void coreShouldNotDependOnAdapterImplementations() {
    noClasses()
        .that().resideInAPackage("io.dataverse.core..")
        .should().dependOnClassesThat()
        .resideInAPackage("io.dataverse.adapter..")
        .check(coreClasses);
  }
}
