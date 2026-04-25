package com.colewinfield.liftingtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogDao {

    @Query("SELECT COUNT(*) FROM catalog_lifts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lifts: List<CatalogLiftEntity>)

    @Query("SELECT * FROM catalog_lifts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CatalogLiftEntity?

    /**
     * Lifts whose [primaryMuscle] matches, ordered alphabetically by name. Used for the
     * "Same muscle" tier of swap suggestions.
     */
    @Query("SELECT * FROM catalog_lifts WHERE primaryMuscle = :muscle ORDER BY name")
    suspend fun byPrimaryMuscle(muscle: String): List<CatalogLiftEntity>

    /**
     * Lifts that mention [muscle] in their secondary-muscles list. Stored as a single TEXT
     * column joined by U+001F, so we match via LIKE on the normalised lowercase token. Slow
     * scan but the table is ~900 rows, fine for a personal app.
     */
    @Query(
        "SELECT * FROM catalog_lifts " +
            "WHERE secondaryMuscles LIKE :likeQuery " +
            "AND primaryMuscle != :muscle " +
            "ORDER BY name"
    )
    suspend fun bySecondaryMuscle(muscle: String, likeQuery: String): List<CatalogLiftEntity>

    @Query("SELECT * FROM catalog_lifts WHERE equipment = :equipment ORDER BY name")
    suspend fun byEquipment(equipment: String): List<CatalogLiftEntity>

    @Query("SELECT * FROM catalog_lifts ORDER BY name LIMIT :limit OFFSET :offset")
    suspend fun page(offset: Int, limit: Int): List<CatalogLiftEntity>

    /**
     * Search by free-text name match (LIKE %q%). Caller passes the full pattern (e.g. "%squat%").
     * Cap [limit] keeps the bottom-sheet list responsive even on broad queries.
     */
    @Query(
        "SELECT * FROM catalog_lifts " +
            "WHERE name LIKE :pattern OR primaryMuscle LIKE :pattern " +
            "ORDER BY name " +
            "LIMIT :limit"
    )
    suspend fun search(pattern: String, limit: Int): List<CatalogLiftEntity>
}
