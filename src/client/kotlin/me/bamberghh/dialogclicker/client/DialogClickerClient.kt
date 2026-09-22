package me.bamberghh.dialogclicker.client

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.bamberghh.dialogclicker.DialogClicker
import me.bamberghh.dialogclicker.config.DialogClickerConfig
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
data class ActionValue(val closeAction: Optional<ClickEvent>, val afterAction: DialogAction) {
	companion object {
		val CODEC = RecordCodecBuilder.create<ActionValue> { i -> i.group(
			ClickEvent.CODEC.optionalFieldOf("closeAction").forGetter(ActionValue::closeAction),
			DialogAction.CODEC.fieldOf("afterAction").forGetter(ActionValue::afterAction)
		).apply(i, ::ActionValue)
		}
	}
}

typealias ActionValuesForType = MutableMap<String, MutableMap<Component, MutableList<ActionValue>>>

class ActionValuesRoot private constructor(
	val world: ActionValuesForType,
	val server: ActionValuesForType,
) {
	companion object {
		val FOR_TYPE_CODEC: Codec<ActionValuesForType> = run {
			val actionValuesCodec = mapPairListListCodec(
				ComponentSerialization.CODEC,
				ActionValue.CODEC
			)
			Codec.unboundedMap(Codec.STRING, actionValuesCodec).xmap(
				{ map -> HashMap(map) },
				{ map -> map }
			)
		}

		val CODEC = RecordCodecBuilder.create<ActionValuesRoot> { i -> i.group(
			FOR_TYPE_CODEC.fieldOf("world").forGetter(ActionValuesRoot::world),
			FOR_TYPE_CODEC.fieldOf("server").forGetter(ActionValuesRoot::server)
		).apply(i, ::ActionValuesRoot)
		}

		fun load(file: File): ActionValuesRoot {
			val nbt = try {
				NbtIo.readCompressed(file.toPath(), NbtAccounter.defaultQuota())
			} catch (_: NoSuchFileException) {
				return ActionValuesRoot(HashMap(), HashMap())
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
	var actionValuesRoot: ActionValuesRoot? = null
	var modDirectory: File? = null
	var actionsFile: File? = null

	override fun onInitializeClient() {
		val gameDirectory = Minecraft.getInstance().gameDirectory
		val modDirectory = gameDirectory.resolve(".dialogclicker")
		val actionsFile = modDirectory.resolve("actions.nbt")
		this.modDirectory = modDirectory
		this.actionsFile = actionsFile
		actionValuesRoot = ActionValuesRoot.load(actionsFile)
	}

	private fun getActionValuesAndKeyForType(minecraft: Minecraft): Pair<ActionValuesForType, String>? {
		val actionValuesRoot = actionValuesRoot!!
		val currentServer = minecraft.currentServer
		return if (currentServer != null) {
			Pair(actionValuesRoot.server, currentServer.ip)
		} else {
			val integratedServer = minecraft.singleplayerServer
			if (integratedServer == null) {
				DialogClicker.LOGGER.error("Couldn't save a dialog action: No integrated server loaded")
				return null
			}
			val levelName = integratedServer.worldData.levelName
			Pair(actionValuesRoot.world, levelName)
		}
	}

	private fun getDialogActionValues(minecraft: Minecraft): MutableMap<Component, List<ActionValue>>? {
		val (actionValuesForType, key) = getActionValuesAndKeyForType(minecraft) ?: return null
		@Suppress("UNCHECKED_CAST")
		return actionValuesForType.computeIfAbsent(key) { HashMap() } as MutableMap<Component, List<ActionValue>>?
	}

	fun saveActions(minecraft: Minecraft, externalTitle: Component, actionValues: List<ActionValue>) {
        val dialogActionValues = getDialogActionValues(minecraft) ?: return
		if (!actionValues.isEmpty()) {
			dialogActionValues[externalTitle] = actionValues
		} else {
			dialogActionValues.remove(externalTitle)
			if (dialogActionValues.isEmpty()) {
				val (actionValuesForType, key) = getActionValuesAndKeyForType(minecraft) ?: return
				actionValuesForType.remove(key)
			}
		}
		if (DialogClickerConfig.shouldLogActionSaving) {
			DialogClicker.LOGGER.info("Actions for dialog {} saved: {}", externalTitle, actionValues)
		}
		actionValuesRoot!!.save(modDirectory!!, actionsFile!!)
	}

	fun loadActions(minecraft: Minecraft, externalTitle: Component): List<ActionValue> {
		val actionValues = getDialogActionValues(minecraft)?.get(externalTitle) ?: return listOf()
		if (DialogClickerConfig.shouldLogActionLoading) {
			DialogClicker.LOGGER.info("Actions for dialog {} loaded: {}", externalTitle, actionValues)
		}
		return actionValues
	}
}