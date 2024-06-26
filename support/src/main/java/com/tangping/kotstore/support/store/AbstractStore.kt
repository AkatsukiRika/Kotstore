package com.tangping.kotstore.support.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.tangping.kotstore.support.KotStoreModel
import com.tangping.kotstore.support.UPDATE_TIME_SUFFIX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class AbstractStore<T: Any?> : ReadWriteProperty<KotStoreModel, T> {
    abstract val key: String
    abstract val default: T
    abstract val syncSave: Boolean

    abstract fun getPreferencesKey(): Preferences.Key<T>

    override operator fun getValue(thisRef: KotStoreModel, property: KProperty<*>): T {
        val preferencesKey = getPreferencesKey()
        var value = default
        runBlocking {
            thisRef.dataStore.data.first {
                value = it[preferencesKey] ?: default
                true
            }
        }
        return value
    }

    override operator fun setValue(thisRef: KotStoreModel, property: KProperty<*>, value: T) {
        saveToStore(thisRef.dataStore, thisRef.scope, getPreferencesKey(), value)
    }

    private fun saveToStore(
        dataStore: DataStore<Preferences>,
        scope: CoroutineScope,
        preferencesKey: Preferences.Key<T>,
        value: T
    ) {
        if (syncSave) {
            runBlocking {
                dataStore.edit { mutablePreferences ->
                    mutablePreferences[preferencesKey] = value
                    mutablePreferences[longPreferencesKey(key + "_$UPDATE_TIME_SUFFIX")] = System.currentTimeMillis()
                }
            }
        } else {
            scope.launch {
                dataStore.edit { mutablePreferences ->
                    mutablePreferences[preferencesKey] = value
                    mutablePreferences[longPreferencesKey(key + "_$UPDATE_TIME_SUFFIX")] = System.currentTimeMillis()
                }
            }
        }
    }
}