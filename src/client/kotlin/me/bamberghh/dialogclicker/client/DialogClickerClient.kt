package me.bamberghh.dialogclicker.client

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.bamberghh.dialogclicker.DialogClicker
import me.bamberghh.dialogclicker.mapPairListListCodec
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.dialog.DialogAction
import java.io.File
import java.nio.file.NoSuchFileException
import java.util.*

@JvmRecord
data class SavedAction(val closeAction: Optional<ClickEvent>, val afterAction: DialogAction) {
	companion object {
		val CODEC = RecordCodecBuilder.create<SavedAction> { i -> i.group(
			ClickEvent.CODEC.optionalFieldOf("closeAction").forGetter(SavedAction::closeAction),
			DialogAction.CODEC.fieldOf("afterAction").forGetter(SavedAction::afterAction)
		).apply(i, ::SavedAction)
		}
	}
}

typealias SavedActionsForType = MutableMap<String, MutableMap<Component, MutableList<SavedAction>>>

class SavedActionsRoot private constructor(
	val world: SavedActionsForType,
	val server: SavedActionsForType,
) {
	companion object {
		val FOR_TYPE_CODEC: Codec<SavedActionsForType> = run {
			val savedActionsCodec = mapPairListListCodec(
				ComponentSerialization.CODEC,
				SavedAction.CODEC
			)
			Codec.unboundedMap(Codec.STRING, savedActionsCodec).xmap(
				{ map -> HashMap(map) },
				{ map -> map }
			)
		}

		val CODEC = RecordCodecBuilder.create<SavedActionsRoot> { i -> i.group(
			FOR_TYPE_CODEC.fieldOf("world").forGetter(SavedActionsRoot::world),
			FOR_TYPE_CODEC.fieldOf("server").forGetter(SavedActionsRoot::server)
		).apply(i, ::SavedActionsRoot)
		}

		fun load(file: File): SavedActionsRoot {
			val nbt = try {
				NbtIo.readCompressed(file.toPath(), NbtAccounter.defaultQuota())
			} catch (_: NoSuchFileException) {
				return SavedActionsRoot(HashMap(), HashMap())
			}
			val result = CODEC.decode(NbtOps.INSTANCE, nbt)
			return result.orThrow.first
		}
	}

	fun save(modDirectory: File, file: File) {
		val encodeResult = CODEC.encodeStart(NbtOps.INSTANCE, this)
		val result = encodeResult.orThrow as CompoundTag
		modDirectory.mkdir()
		NbtIo.writeCompressed(result, file.toPath())
	}
}

object DialogClickerClient : ClientModInitializer {
	var savedActionsRoot: SavedActionsRoot? = null
	var modDirectory: File? = null
	var actionsFile: File? = null

	override fun onInitializeClient() {
		val gameDirectory = Minecraft.getInstance().gameDirectory
		val modDirectory = gameDirectory.resolve(".dialogclicker")
		val actionsFile = modDirectory.resolve("actions.nbt")
		this.modDirectory = modDirectory
		this.actionsFile = actionsFile
		savedActionsRoot = SavedActionsRoot.load(actionsFile)
	}

	private fun getSavedActionsAndKeyForType(minecraft: Minecraft): Pair<SavedActionsForType, String>? {
		val savedActionsRoot = savedActionsRoot!!
		val currentServer = minecraft.currentServer
		return if (currentServer != null) {
			Pair(savedActionsRoot.server, currentServer.ip)
		} else {
			val integratedServer = minecraft.singleplayerServer
			if (integratedServer == null) {
				DialogClicker.LOGGER.error("Couldn't save a dialog action: No integrated server loaded")
				return null
			}
			val levelName = integratedServer.worldData.levelName
			Pair(savedActionsRoot.world, levelName)
		}
	}

	private fun getSavedActions(minecraft: Minecraft): MutableMap<Component, List<SavedAction>>? {
		val (savedActionsForType, key) = getSavedActionsAndKeyForType(minecraft) ?: return null
		@Suppress("UNCHECKED_CAST")
		return savedActionsForType.computeIfAbsent(key) { HashMap() } as MutableMap<Component, List<SavedAction>>?
	}

	fun saveActions(minecraft: Minecraft, externalTitle: Component, action: List<SavedAction>?) {
        val savedActions = getSavedActions(minecraft) ?: return
		if (action != null) {
			savedActions[externalTitle] = action
		} else {
			savedActions.remove(externalTitle)
			if (savedActions.isEmpty()) {
				val (savedActionsForType, key) = getSavedActionsAndKeyForType(minecraft) ?: return
				savedActionsForType.remove(key)
			}
		}
		DialogClicker.LOGGER.info("saveActions {}", savedActions)
		savedActionsRoot!!.save(modDirectory!!, actionsFile!!)
	}

	fun loadActions(minecraft: Minecraft, externalTitle: Component): List<SavedAction>? {
		val savedActions = getSavedActions(minecraft) ?: return null
		return savedActions[externalTitle]
	}
}