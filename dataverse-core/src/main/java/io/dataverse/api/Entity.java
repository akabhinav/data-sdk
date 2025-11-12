package io.dataverse.api;

import java.io.Serializable;

/**
 * Marker interface for all entities managed by DataVerse SDK.
 *
 * <p>Entities represent domain objects that can be persisted to and retrieved from data sources.
 * Each entity must have a unique identifier accessible via {@link #getId()}.
 *
 * <p>This interface provides the foundation for type-safe operations across all data sources.
 *
 * @param <ID> the type of the entity's identifier
 *
 * @since 1.0.0
 * @author DataVerse SDK Team
 */
public interface Entity<ID extends Serializable> {

  /**
   * Returns the unique identifier of this entity.
   *
   * @return the entity's identifier, never {@code null} for persisted entities
   */
  ID getId();

  /**
   * Sets the unique identifier of this entity.
   *
   * @param id the identifier to set
   */
  void setId(ID id);
}
