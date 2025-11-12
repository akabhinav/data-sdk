package io.dataverse.adapter.mongodb;

import io.dataverse.api.Entity;
import io.dataverse.api.QueryBuilder;
import io.dataverse.core.AbstractRepository;
import io.dataverse.core.query.DefaultQueryBuilder;
import io.dataverse.core.query.Query;
import io.dataverse.spi.ConnectionProvider;
import io.dataverse.spi.QueryTranslator;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of the Repository interface.
 *
 * <p>This class implements all CRUD operations using the MongoDB Java Driver.
 * It extends AbstractRepository to inherit common functionality.
 *
 * @param <T> the entity type
 * @param <ID> the identifier type
 * @since 1.0.0
 */
public class MongoDBRepository<T extends Entity<ID>, ID extends Serializable>
    extends AbstractRepository<T, ID> {

  private final ConnectionProvider<MongoDatabase> connectionProvider;
  private final QueryTranslator queryTranslator;
  private final String collectionName;
  private final String databaseName;

  public MongoDBRepository(
      Class<T> entityClass,
      ConnectionProvider<MongoDatabase> connectionProvider,
      QueryTranslator queryTranslator,
      String databaseName) {
    super(entityClass);
    this.connectionProvider = connectionProvider;
    this.queryTranslator = queryTranslator;
    this.databaseName = databaseName;
    this.collectionName = deriveCollectionName(entityClass);
  }

  /**
   * Derives the MongoDB collection name from the entity class.
   *
   * @param entityClass the entity class
   * @return the collection name
   */
  protected String deriveCollectionName(Class<T> entityClass) {
    // Convert CamelCase to lowercase plural (simple strategy)
    // User -> users, Order -> orders
    return entityClass.getSimpleName().toLowerCase() + "s";
  }

  /**
   * Gets the MongoDB collection for this repository.
   */
  private MongoCollection<Document> getCollection() {
    return connectionProvider.execute(db -> db.getCollection(collectionName));
  }

  @Override
  protected T doSave(T entity) {
    return connectionProvider.execute(db -> {
      try {
        MongoCollection<Document> collection = db.getCollection(collectionName);
        Document doc = convertToDocument(entity);

        if (entity.getId() == null) {
          // Insert new document
          collection.insertOne(doc);
          // MongoDB generates _id automatically
          entity.setId((ID) doc.get("_id").toString());
        } else {
          // Update existing document
          collection.replaceOne(
              Filters.eq("_id", entity.getId()),
              doc);
        }

        return entity;
      } catch (Exception e) {
        throw new RuntimeException("Failed to save entity to MongoDB", e);
      }
    });
  }

  @Override
  protected Optional<T> doFindById(ID id) {
    return connectionProvider.execute(db -> {
      try {
        MongoCollection<Document> collection = db.getCollection(collectionName);
        Document doc = collection.find(Filters.eq("_id", id)).first();

        if (doc != null) {
          return Optional.of(convertFromDocument(doc));
        } else {
          return Optional.empty();
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to find entity by ID in MongoDB", e);
      }
    });
  }

  @Override
  protected void doDelete(ID id) {
    connectionProvider.execute(db -> {
      try {
        MongoCollection<Document> collection = db.getCollection(collectionName);
        collection.deleteOne(Filters.eq("_id", id));
        return null;
      } catch (Exception e) {
        throw new RuntimeException("Failed to delete entity from MongoDB", e);
      }
    });
  }

  @Override
  protected List<T> doFindAll() {
    return connectionProvider.execute(db -> {
      try {
        MongoCollection<Document> collection = db.getCollection(collectionName);
        List<T> results = new ArrayList<>();

        collection.find().into(new ArrayList<>()).forEach(doc -> {
          results.add(convertFromDocument(doc));
        });

        return results;
      } catch (Exception e) {
        throw new RuntimeException("Failed to find all entities in MongoDB", e);
      }
    });
  }

  @Override
  protected long doCount() {
    return connectionProvider.execute(db -> {
      try {
        MongoCollection<Document> collection = db.getCollection(collectionName);
        return collection.countDocuments();
      } catch (Exception e) {
        throw new RuntimeException("Failed to count entities in MongoDB", e);
      }
    });
  }

  @Override
  public QueryBuilder<T> query() {
    return new DefaultQueryBuilder<>(this::executeQuery);
  }

  @Override
  public List<T> executeNativeQuery(String nativeQuery) {
    // Native MongoDB queries would be executed here
    throw new UnsupportedOperationException("Native queries not yet implemented for MongoDB");
  }

  /**
   * Executes a query built by the QueryBuilder.
   */
  private List<T> executeQuery(Query query) {
    return connectionProvider.execute(db -> {
      try {
        MongoCollection<Document> collection = db.getCollection(collectionName);

        // Translate query to MongoDB format
        var nativeQuery =
            (MongoDBQueryTranslator.MongoDBNativeQuery) queryTranslator.translate(query);

        // Build find operation
        var findIterable = collection.find(nativeQuery.getFilter());

        // Apply sorting
        if (!nativeQuery.getSort().isEmpty()) {
          findIterable = findIterable.sort(nativeQuery.getSort());
        }

        // Apply pagination
        if (nativeQuery.getOffset() != null) {
          findIterable = findIterable.skip(nativeQuery.getOffset());
        }
        if (nativeQuery.getLimit() != null) {
          findIterable = findIterable.limit(nativeQuery.getLimit());
        }

        // Execute and convert results
        List<T> results = new ArrayList<>();
        findIterable.into(new ArrayList<>()).forEach(doc -> {
          results.add(convertFromDocument(doc));
        });

        return results;
      } catch (Exception e) {
        throw new RuntimeException("Failed to execute query on MongoDB", e);
      }
    });
  }

  /**
   * Converts an entity to a MongoDB document.
   */
  protected Document convertToDocument(T entity) {
    // Simplified: In production, use Jackson or custom serialization
    Document doc = new Document();

    if (entity.getId() != null) {
      doc.put("_id", entity.getId());
    }

    // In production, iterate over all fields using reflection or use MongoDB POJO codec

    return doc;
  }

  /**
   * Converts a MongoDB document to an entity.
   */
  protected T convertFromDocument(Document doc) {
    // Simplified: In production, use proper deserialization
    try {
      T instance = entityClass.getDeclaredConstructor().newInstance();

      // Set ID
      if (doc.containsKey("_id")) {
        instance.setId((ID) doc.get("_id"));
      }

      // In production, set all fields using reflection or MongoDB POJO codec

      return instance;
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert MongoDB document to entity", e);
    }
  }

  @Override
  protected void shutdown() {
    super.shutdown();
    // Additional cleanup if needed
  }
}
