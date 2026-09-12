package androidx.datastore.preferences.core

import androidx.datastore.core.DataStore

open class Preferences {
    class Key<T>(val name: String)

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>): T? = null
}

class MutablePreferences : Preferences() {
    operator fun <T> set(key: Key<T>, value: T) = Unit
}

fun stringPreferencesKey(name: String): Preferences.Key<String> = Preferences.Key(name)
fun floatPreferencesKey(name: String): Preferences.Key<Float> = Preferences.Key(name)
fun intPreferencesKey(name: String): Preferences.Key<Int> = Preferences.Key(name)

suspend fun DataStore<Preferences>.edit(
    transform: suspend (MutablePreferences) -> Unit,
): Preferences = error("bouchon de vérification")
