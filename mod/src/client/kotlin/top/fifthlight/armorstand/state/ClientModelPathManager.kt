package top.fifthlight.armorstand.state

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.ArmorStandClient
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.manage.ModelManager
import top.fifthlight.armorstand.util.ModelHash
import java.nio.file.Path
import java.util.*

object ClientModelPathManager {
    private val client = MinecraftClient.getInstance()
    var selfPath: Path? = null
        private set
    private val selfUuid: UUID?
        get() = client.player?.uuid
    private val modelPaths = mutableMapOf<UUID, Path>()

    fun initialize() {
        ArmorStand.instance.scope.launch {
            ConfigHolder.config
                .map { it.modelPath }
                .distinctUntilChanged()
                .collect { selfPath = it }
        }
        ArmorStand.instance.scope.launch {
            ModelManager.lastScanTime.filterNotNull().collectLatest {
                modelPaths.clear()
                ModelHashManager.getModelHashes().forEach { (uuid, hash) ->
                    update(uuid, hash)
                }
            }
        }
    }

    suspend fun update(uuid: UUID, hash: ModelHash?) {
        if (hash == null) {
            modelPaths.remove(uuid)
            return
        }
        val path = ModelManager.getModel(hash)?.path
        if (path != null) {
            modelPaths[uuid] = path
        } else {
            modelPaths.remove(uuid)
        }
    }

    fun getPath(uuid: UUID) = if (uuid == selfUuid) {
        selfPath
    } else if (ArmorStandClient.debug) {
        modelPaths[uuid] ?: selfPath
    } else {
        modelPaths[uuid]
    }
}