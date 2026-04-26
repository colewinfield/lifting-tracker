package com.colewinfield.liftingtracker.data

/**
 * Domain projection of a row in the read-only `catalog_lifts` table (sourced from
 * yuhonas/free-exercise-db). The Swap-flow uses [Alternative] because it carries an overlap
 * percentage and is rendered alongside curated swaps; the picker flow (Exercise DB browser)
 * never has overlap, so it uses this thinner type instead.
 *
 * - [primaryMuscle] / [equipment] are the normalised lowercase tokens from the catalog table
 *   (e.g. `quadriceps`, `body only`). Use [displayMuscle] / [displayEquipment] for UI.
 * - [mechanic] (compound / isolation / null) is exposed so the browser can hint at CNS load
 *   when the JSX renders an effort dot — catalog rows have no real effort field.
 */
data class CatalogLift(
    val id: String,
    val name: String,
    val primaryMuscle: String,
    val displayMuscle: String,
    val equipment: String,
    val displayEquipment: String,
    val mechanic: String?,
)
