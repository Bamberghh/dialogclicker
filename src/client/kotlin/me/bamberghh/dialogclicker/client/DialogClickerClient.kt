package me.bamberghh.dialogclicker.client

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.bamberghh.dialogclicker.DialogClicker
import me.bamberghh.dialogclicker.client.DialogClickerClient.actionsFile
import me.bamberghh.dialogclicker.mapPairListCodec
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
import kotlin.collections.toMutableMap

@JvmRecord
data class RememberedAction(val closeAction: Optional<ClickEvent>, val afterAction: DialogAction) {
	companion object {
		val CODEC = RecordCodecBuilder.create<RememberedAction> { i -> i.group(
			ClickEvent.CODEC.optionalFieldOf("closeAction").forGetter(RememberedAction::closeAction),
			DialogAction.CODEC.fieldOf("afterAction").forGetter(RememberedAction::afterAction)
		).apply(i, ::RememberedAction)
		}
	}
}

typealias RememberedActionsForType = MutableMap<String, MutableMap<Component, RememberedAction>>

class RememberedActionsRoot private constructor(
	val world: RememberedActionsForType,
	val server: RememberedActionsForType,
) {
	companion object {
		val FOR_TYPE_CODEC: Codec<RememberedActionsForType> = run {
			val rememberedActionsCodec = mapPairListCodec(ComponentSerialization.CODEC, RememberedAction.CODEC)
			Codec.unboundedMap(Codec.STRING, rememberedActionsCodec).xmap(
				{ map -> map.toMutableMap() },
				{ map -> map }
			)
		}

		val CODEC = RecordCodecBuilder.create<RememberedActionsRoot> { i -> i.group(
			FOR_TYPE_CODEC.fieldOf("world").forGetter(RememberedActionsRoot::world),
			FOR_TYPE_CODEC.fieldOf("server").forGetter(RememberedActionsRoot::server)
		).apply(i, ::RememberedActionsRoot)
		}

		fun load(file: File): RememberedActionsRoot {
			val nbt = try {
				NbtIo.readCompressed(file.toPath(), NbtAccounter.defaultQuota())
			} catch (_: NoSuchFileException) {
				return RememberedActionsRoot(HashMap(), HashMap())
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
	var rememberedActionsRoot: RememberedActionsRoot? = null
	var modDirectory: File? = null
	var actionsFile: File? = null

	override fun onInitializeClient() {
		val gameDirectory = Minecraft.getInstance().gameDirectory
		val modDirectory = gameDirectory.resolve(".dialogclicker")
		val actionsFile = modDirectory.resolve("actions.nbt")
		this.modDirectory = modDirectory
		this.actionsFile = actionsFile
		rememberedActionsRoot = RememberedActionsRoot.load(actionsFile);
	}

	private fun getRememberedActions(minecraft: Minecraft): MutableMap<Component, RememberedAction>? {
		val rememberedActionsRoot = rememberedActionsRoot!!
		val currentServer = minecraft.currentServer
		return if (currentServer != null) {
			rememberedActionsRoot.server.computeIfAbsent(currentServer.ip) { HashMap() }
		} else {
			val integratedServer = minecraft.singleplayerServer
			if (integratedServer == null) {
				DialogClicker.LOGGER.error("Couldn't save a dialog action: No integrated server loaded")
				return null
			}
			val levelName = integratedServer.worldData.levelName
			rememberedActionsRoot.world.computeIfAbsent(levelName) { HashMap() }
		}
	}

	fun saveAction(minecraft: Minecraft, externalTitle: Component, action: RememberedAction) {
        val rememberedActions = getRememberedActions(minecraft) ?: return
		rememberedActions[externalTitle] = action
		DialogClicker.LOGGER.info("saveAction {}", rememberedActions)
		rememberedActionsRoot!!.save(modDirectory!!, actionsFile!!)
	}

	fun loadAction(minecraft: Minecraft, externalTitle: Component): RememberedAction? {
		val rememberedActions = getRememberedActions(minecraft) ?: return null
		return rememberedActions[externalTitle]
	}
}