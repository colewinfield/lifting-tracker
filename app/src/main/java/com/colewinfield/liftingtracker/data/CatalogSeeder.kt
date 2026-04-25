package com.colewinfield.liftingtracker.data

import com.colewinfield.liftingtracker.data.db.CatalogDao
import com.colewinfield.liftingtracker.data.db.CatalogLiftEntity
import org.json.JSONArray
import java.io.InputStream

/**
 * Loads `assets/exercises.json` (free-exercise-db, Unlicense / public domain) into the
 * `catalog_lifts` table on first launch. ~870 lifts, parsed once with the built-in `org.json`
 * reader so we don't drag in kotlinx.serialization or Gson just for one read.
 */
object CatalogSeeder {

    /** Resource path inside the app's assets directory. */
    const val ASSET_PATH = "exercises.json"

    /** Insert in batches to keep individual SQL statements small. */
    private const val BATCH_SIZE = 200

    suspend fun seed(catalogDao: CatalogDao, openAsset: (String) -> InputStream) {
        if (catalogDao.count() > 0) return
        val json = openAsset(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(json)
        val entities = ArrayList<CatalogLiftEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val primaryArr = o.optJSONArray("primaryMuscles")
            val primaryMuscle = if (primaryArr != null && primaryArr.length() > 0) {
                normalizeMuscle(primaryArr.getString(0))
            } else ""
            val secondaryArr = o.optJSONArray("secondaryMuscles")
            val secondary = mutableListOf<String>()
            if (secondaryArr != null) {
                for (j in 0 until secondaryArr.length()) {
                    secondary += normalizeMuscle(secondaryArr.getString(j))
                }
            }
            entities += CatalogLiftEntity(
                id = o.optString("id").ifBlank { "lift-$i" },
                name = o.optString("name").ifBlank { "Unnamed lift" },
                primaryMuscle = primaryMuscle,
                secondaryMuscles = secondary,
                equipment = o.optString("equipment").lowercase().trim(),
                category = o.optString("category").lowercase().trim(),
                force = o.optStringOrNull("force"),
                mechanic = o.optStringOrNull("mechanic"),
                level = o.optString("level").lowercase().trim(),
            )
        }
        entities.chunked(BATCH_SIZE).forEach { catalogDao.insertAll(it) }
    }

    /**
     * Map free-form muscle names to the free-exercise-db taxonomy so program lifts (which
     * use shorthand like "Quads"/"Core") match catalog rows ("quadriceps"/"abdominals").
     */
    fun normalizeMuscle(raw: String): String {
        val lower = raw.trim().lowercase()
        return when (lower) {
            "quad", "quads" -> "quadriceps"
            "ham", "hams" -> "hamstrings"
            "abs", "core" -> "abdominals"
            "delts", "delt", "shoulder" -> "shoulders"
            "back" -> "lats"
            "upper back" -> "middle back"
            "tris" -> "triceps"
            "bis" -> "biceps"
            else -> lower
        }
    }
}

private fun org.json.JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    val s = optString(key, "")
    return s.ifBlank { null }?.lowercase()?.trim()
}
