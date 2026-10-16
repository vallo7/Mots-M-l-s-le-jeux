package com.motsmeles.game.data

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.motsmeles.game.BuildConfig
import com.motsmeles.game.domain.ports.ProgressRepository
import com.motsmeles.game.domain.ports.SettingsRepository
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private class JsonSerializer<T>(
    override val defaultValue: T,
    private val decode: (String) -> T,
    private val encode: (T) -> String,
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T = try {
        decode(input.readBytes().toString(Charsets.UTF_8))
    } catch (e: PersistedFormatException) {
        throw CorruptionException("Fichier illisible", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(encode(t).toByteArray(Charsets.UTF_8))
    }
}

/**
 * Progression : DataStore (écritures atomiques) + copie de secours `.bak` du dernier état valide.
 * Un fichier corrompu est conservé en `.corrupt` puis remplacé par la copie de secours (ou l'état initial).
 */
class DataStoreProgressRepository(private val file: File, scope: CoroutineScope) : ProgressRepository {
    private val backup = File(file.parentFile, file.name + ".bak")

    private val store: DataStore<ProgressState> = DataStoreFactory.create(
        serializer = JsonSerializer(ProgressState(), { ProgressJson.decode(it) }, { ProgressJson.encode(it) }),
        corruptionHandler = ReplaceFileCorruptionHandler { error ->
            if (BuildConfig.DEBUG) Log.w(TAG, "Progression corrompue : ${error.message}")
            runCatching { file.copyTo(File(file.path + ".corrupt"), overwrite = true) }
            runCatching { ProgressJson.decode(backup.readText(Charsets.UTF_8)) }.getOrDefault(ProgressState())
        },
        scope = scope,
        produceFile = { file },
    )

    override suspend fun read(): ProgressState = store.data.first()

    override suspend fun update(transform: (ProgressState) -> ProgressState): ProgressState {
        val updated = store.updateData { transform(it) }
        writeBackup(updated)
        return updated
    }

    private suspend fun writeBackup(state: ProgressState) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(backup.path + ".tmp")
            tmp.writeText(ProgressJson.encode(state), Charsets.UTF_8)
            if (!tmp.renameTo(backup)) tmp.delete()
        }
    }

    private companion object { const val TAG = "Progress" }
}

class DataStoreSettingsRepository(file: File, scope: CoroutineScope) : SettingsRepository {
    private val store: DataStore<AppSettings> = DataStoreFactory.create(
        serializer = JsonSerializer(AppSettings.DEFAULT, { SettingsJson.decode(it) }, { SettingsJson.encode(it) }),
        corruptionHandler = ReplaceFileCorruptionHandler { AppSettings.DEFAULT },
        scope = scope,
        produceFile = { file },
    )

    override suspend fun read(): AppSettings = store.data.first()
    override suspend fun update(transform: (AppSettings) -> AppSettings): AppSettings = store.updateData { transform(it) }
}
