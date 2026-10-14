package com.motsmeles.game.data

import android.content.res.AssetManager
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.ports.LevelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Niveaux embarqués : assets « levels/ » (source : data/levels du dépôt). Aucun accès réseau. */
class AssetLevelRepository(private val assets: AssetManager) : LevelRepository {
    override suspend fun loadCatalog(): LevelCatalog = withContext(Dispatchers.IO) {
        LevelJsonParser.parseCatalog { name ->
            assets.open("levels/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }
}
