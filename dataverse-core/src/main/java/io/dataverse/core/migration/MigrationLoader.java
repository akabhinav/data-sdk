package io.dataverse.core.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads migration scripts from filesystem or classpath.
 *
 * <p>Supports migration file naming convention:
 * <pre>
 * V{version}__{description}.sql
 *
 * Examples:
 * V001__create_users_table.sql
 * V002__add_email_index.sql
 * V003__add_orders_table.sql
 * </pre>
 *
 * <p>Rollback scripts use the same naming with .rollback suffix:
 * <pre>
 * V001__create_users_table.rollback.sql
 * </pre>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Load from classpath
 * MigrationLoader loader = new MigrationLoader();
 * List<Migration> migrations = loader.loadFromClasspath("db/migrations");
 *
 * // Load from filesystem
 * List<Migration> migrations = loader.loadFromDirectory("/path/to/migrations");
 *
 * // Add to migration manager
 * MigrationManager manager = new MigrationManager(executor);
 * manager.addMigrations(migrations);
 * }</pre>
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public class MigrationLoader {

  private static final Logger logger = System.getLogger(MigrationLoader.class.getName());

  private static final Pattern MIGRATION_PATTERN = Pattern.compile(
      "V(\\d+)__([a-zA-Z0-9_]+)\\.sql"
  );

  private static final Pattern ROLLBACK_PATTERN = Pattern.compile(
      "V(\\d+)__([a-zA-Z0-9_]+)\\.rollback\\.sql"
  );

  /**
   * Loads migrations from classpath.
   *
   * @param resourcePath the classpath resource path
   * @return list of migrations
   */
  public List<Migration> loadFromClasspath(String resourcePath) {
    logger.log(Level.INFO, "Loading migrations from classpath: " + resourcePath);

    List<Migration> migrations = new ArrayList<>();

    try {
      // Get resource URL
      var resources = Thread.currentThread()
          .getContextClassLoader()
          .getResources(resourcePath);

      while (resources.hasMoreElements()) {
        var url = resources.nextElement();
        logger.log(Level.DEBUG, "Found resource: " + url);

        // For now, just log - actual implementation would enumerate files
        // This is a simplified version for the core module
      }

    } catch (IOException e) {
      throw new MigrationException("Failed to load migrations from classpath", e);
    }

    return migrations;
  }

  /**
   * Loads migrations from a filesystem directory.
   *
   * @param directoryPath the directory path
   * @return list of migrations
   */
  public List<Migration> loadFromDirectory(String directoryPath) {
    return loadFromDirectory(Paths.get(directoryPath));
  }

  /**
   * Loads migrations from a filesystem directory.
   *
   * @param directory the directory path
   * @return list of migrations
   */
  public List<Migration> loadFromDirectory(Path directory) {
    logger.log(Level.INFO, "Loading migrations from directory: " + directory);

    if (!Files.exists(directory)) {
      logger.log(Level.WARNING, "Migration directory does not exist: " + directory);
      return Collections.emptyList();
    }

    if (!Files.isDirectory(directory)) {
      throw new MigrationException("Path is not a directory: " + directory);
    }

    List<Migration> migrations = new ArrayList<>();

    try (Stream<Path> files = Files.list(directory)) {
      files.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".sql"))
          .filter(path -> !path.toString().contains(".rollback."))
          .forEach(path -> {
            try {
              Migration migration = loadMigrationFile(path, directory);
              if (migration != null) {
                migrations.add(migration);
              }
            } catch (Exception e) {
              logger.log(Level.ERROR, "Failed to load migration: " + path, e);
            }
          });
    } catch (IOException e) {
      throw new MigrationException("Failed to load migrations from directory", e);
    }

    Collections.sort(migrations, (m1, m2) ->
        Integer.compare(m1.getVersion(), m2.getVersion()));

    logger.log(Level.INFO, "Loaded " + migrations.size() + " migrations");

    return migrations;
  }

  /**
   * Loads a single migration file.
   *
   * @param file the migration file
   * @param directory the migrations directory
   * @return the migration, or null if file doesn't match pattern
   */
  private Migration loadMigrationFile(Path file, Path directory) throws IOException {
    String filename = file.getFileName().toString();
    Matcher matcher = MIGRATION_PATTERN.matcher(filename);

    if (!matcher.matches()) {
      logger.log(Level.DEBUG, "Skipping file (doesn't match pattern): " + filename);
      return null;
    }

    int version = Integer.parseInt(matcher.group(1));
    String description = matcher.group(2).replace('_', ' ');

    logger.log(Level.DEBUG, "Loading migration V" + version + ": " + description);

    // Read migration script
    String script = readFile(file);

    // Try to find rollback script
    String rollbackFilename = "V" + String.format("%03d", version) + "__" +
        matcher.group(2) + ".rollback.sql";
    Path rollbackPath = directory.resolve(rollbackFilename);

    String rollbackScript = null;
    if (Files.exists(rollbackPath)) {
      logger.log(Level.DEBUG, "Found rollback script for V" + version);
      rollbackScript = readFile(rollbackPath);
    }

    return Migration.builder()
        .version(version)
        .description(description)
        .script(script)
        .rollbackScript(rollbackScript)
        .type(Migration.MigrationType.SQL)
        .build();
  }

  /**
   * Reads a file into a string.
   *
   * @param path the file path
   * @return file contents
   */
  private String readFile(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  /**
   * Loads a migration script from a resource.
   *
   * @param resourcePath the resource path
   * @return script content
   */
  public String loadResource(String resourcePath) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new MigrationException("Resource not found: " + resourcePath);
      }

      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(is, StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append('\n');
        }
        return sb.toString();
      }
    } catch (IOException e) {
      throw new MigrationException("Failed to load resource: " + resourcePath, e);
    }
  }

  /**
   * Parses a migration filename to extract version and description.
   *
   * @param filename the filename
   * @return array with [version, description], or null if invalid
   */
  public static String[] parseFilename(String filename) {
    Matcher matcher = MIGRATION_PATTERN.matcher(filename);
    if (matcher.matches()) {
      String version = matcher.group(1);
      String description = matcher.group(2).replace('_', ' ');
      return new String[]{version, description};
    }
    return null;
  }

  /**
   * Checks if a filename is a valid migration filename.
   *
   * @param filename the filename
   * @return true if valid
   */
  public static boolean isValidMigrationFilename(String filename) {
    return MIGRATION_PATTERN.matcher(filename).matches();
  }

  /**
   * Checks if a filename is a rollback script.
   *
   * @param filename the filename
   * @return true if rollback script
   */
  public static boolean isRollbackFilename(String filename) {
    return ROLLBACK_PATTERN.matcher(filename).matches();
  }
}
