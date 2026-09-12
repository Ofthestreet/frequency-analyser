package com.ofthestreet.frequencyanalyzer.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ofthestreet.frequencyanalyzer.analysis.Algorithm
import com.ofthestreet.frequencyanalyzer.music.OctaveIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "analyzer_settings")

/** Persistance des réglages. Les valeurs inconnues (mise à jour, fichier abîmé) retombent sur les défauts. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ALGORITHM = stringPreferencesKey("algorithm")
        val RANGE = stringPreferencesKey("range")
        val THRESHOLD = floatPreferencesKey("noise_threshold_db")
        val NOTATION = stringPreferencesKey("notation")
        val OCTAVE_INDEX = stringPreferencesKey("octave_index")
        val CLEF = stringPreferencesKey("clef")
        val REFERENCE = floatPreferencesKey("reference_a4")
        val HISTORY = intPreferencesKey("history_seconds")
    }

    val settings: Flow<AnalyzerSettings> = context.dataStore.data.map { preferences ->
        val defaults = AnalyzerSettings()
        AnalyzerSettings(
            algorithm = preferences[Keys.ALGORITHM].toEnum(defaults.algorithm),
            range = preferences[Keys.RANGE].toEnum(defaults.range),
            noiseThresholdDb = preferences[Keys.THRESHOLD] ?: defaults.noiseThresholdDb,
            notation = preferences[Keys.NOTATION].toEnum(defaults.notation),
            octaveIndex = preferences[Keys.OCTAVE_INDEX].toEnum(defaults.octaveIndex),
            clefMode = preferences[Keys.CLEF].toEnum(defaults.clefMode),
            referenceA4 = preferences[Keys.REFERENCE] ?: defaults.referenceA4,
            historySeconds = preferences[Keys.HISTORY] ?: defaults.historySeconds,
        )
    }

    suspend fun save(settings: AnalyzerSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ALGORITHM] = settings.algorithm.name
            preferences[Keys.RANGE] = settings.range.name
            preferences[Keys.THRESHOLD] = settings.noiseThresholdDb
            preferences[Keys.NOTATION] = settings.notation.name
            preferences[Keys.OCTAVE_INDEX] = settings.octaveIndex.name
            preferences[Keys.CLEF] = settings.clefMode.name
            preferences[Keys.REFERENCE] = settings.referenceA4
            preferences[Keys.HISTORY] = settings.historySeconds
        }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback
