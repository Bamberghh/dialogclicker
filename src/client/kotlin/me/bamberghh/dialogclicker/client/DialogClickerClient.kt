package me.bamberghh.dialogclicker.client

import me.bamberghh.dialogclicker.DialogClicker
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Dialog
import net.minecraft.server.dialog.DialogAction
import java.util.Optional

@JvmRecord
data class RememberedAction(val closeAction: Optional<ClickEvent>, val afterAction: DialogAction)

typealias RememberedActionsForType = HashMap<String, HashMap<Component, RememberedAction>>

class RememberedActionsRoot private constructor(
	val world: RememberedActionsForType,
	val server: RememberedActionsForType,
) {
	companion object {
		fun load(): RememberedActionsRoot {
			return RememberedActionsRoot(HashMap(), HashMap())
		}
	}
}

object DialogClickerClient : ClientModInitializer {
	var rememberedActionsRoot: RememberedActionsRoot? = null

	override fun onInitializeClient() {
		rememberedActionsRoot = RememberedActionsRoot.load();
	}

	private fun getRememberedActions(minecraft: Minecraft): HashMap<Component, RememberedAction>? {
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
	}

	fun loadAction(minecraft: Minecraft, externalTitle: Component): RememberedAction? {
		val rememberedActions = getRememberedActions(minecraft) ?: return null
		return rememberedActions[externalTitle]
	}
}