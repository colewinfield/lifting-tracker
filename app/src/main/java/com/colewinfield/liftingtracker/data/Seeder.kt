package com.colewinfield.liftingtracker.data

import com.colewinfield.liftingtracker.data.db.PerformedSetEntity
import com.colewinfield.liftingtracker.data.db.ProgramDao
import com.colewinfield.liftingtracker.data.db.SessionDao
import com.colewinfield.liftingtracker.data.db.SessionEntity
import com.colewinfield.liftingtracker.data.db.toEntity
import java.util.UUID
import java.util.concurrent.TimeUnit

object Seeder {

    suspend fun seed(programDao: ProgramDao, sessionDao: SessionDao) {
        seedProgram(programDao)
        seedHistory(sessionDao)
    }

    private suspend fun seedProgram(dao: ProgramDao) {
        val program = SampleData.program
        dao.insertProgram(program.toEntity())
        dao.insertDays(
            program.days.mapIndexed { idx, day ->
                day.toEntity(programId = program.id, orderIndex = idx)
            }
        )
        dao.insertLifts(
            program.days.flatMap { day ->
                day.lifts.mapIndexed { idx, lift ->
                    lift.toEntity(dayId = day.id, orderIndex = idx)
                }
            }
        )
        dao.insertAlternatives(
            SampleData.alternatives.values.flatten().map { it.toEntity() }
        )
    }

    /**
     * Seeds historic SampleData.history entries as concrete sessions/performed_sets so the
     * Today screen's "last week" query hits real DB rows instead of a parallel in-memory store.
     * Sessions are dated relative to "now" using the cyclic week math: week N is (currentWeek - N)
     * weeks ago when N < current, otherwise (cycleLength + currentWeek - N) weeks ago for entries
     * from the prior cycle (e.g. week 9 deload before week 1).
     */
    private suspend fun seedHistory(sessionDao: SessionDao) {
        val program = SampleData.program
        val currentWeek = SampleData.defaultCurrentWeek
        val cycleLength = program.cycleLength

        val liftToDay: Map<String, String> = program.days
            .flatMap { day -> day.lifts.map { it.id to day.id } }
            .toMap()

        // Group entries by (week, dayId) so multiple lifts performed on the same day share
        // a SessionEntity row.
        val grouped: Map<Pair<Int, String>, List<Pair<String, HistoryEntry>>> =
            SampleData.history
                .flatMap { (liftId, entries) ->
                    entries.mapNotNull { entry ->
                        val dayId = liftToDay[liftId] ?: return@mapNotNull null
                        Triple(entry.week, dayId, liftId to entry)
                    }
                }
                .groupBy({ (week, dayId, _) -> week to dayId }, { it.third })

        val now = System.currentTimeMillis()
        val msPerWeek = TimeUnit.DAYS.toMillis(7)

        grouped.forEach { (key, liftEntries) ->
            val (week, dayId) = key
            val weeksBack = if (week < currentWeek) currentWeek - week
            else cycleLength + currentWeek - week
            val sessionDate = now - weeksBack * msPerWeek
            val sessionId = UUID.randomUUID().toString()

            sessionDao.insertSession(
                SessionEntity(
                    id = sessionId,
                    dayId = dayId,
                    weekNumber = week,
                    date = sessionDate,
                    finishedAt = sessionDate,
                )
            )

            liftEntries.forEach { (liftId, entry) ->
                entry.sets.forEachIndexed { idx, set ->
                    sessionDao.insertPerformedSet(
                        PerformedSetEntity(
                            sessionId = sessionId,
                            liftId = liftId,
                            setIndex = idx + 1,
                            weight = set.weight,
                            reps = set.reps,
                            whoopsy = false,
                            rpe = null,
                            done = true,
                            completedAt = sessionDate,
                        )
                    )
                }
            }
        }
    }
}
