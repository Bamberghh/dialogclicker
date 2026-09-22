package me.bamberghh.dialogclicker

import eu.midnightdust.lib.config.MidnightConfig
import me.bamberghh.dialogclicker.config.DialogClickerConfig
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger

//import me.bamberghh.dialogclicker.config.DialogClickerConfig

object DialogClicker : ModInitializer {
	const val MOD_ID: String = "dialogclicker"

	private val modLogger: Logger = LoggerFactory.getLogger(MOD_ID)
	@JvmField
    var LOGGER: Logger = modLogger

	@Suppress("unused")
	@JvmField
	val CONFIG: DialogClickerConfig = DialogClickerConfig()

	override fun onInitialize() {
		MidnightConfig.init(MOD_ID, DialogClickerConfig::class.java)
		onConfigChanged()
	}

	fun onConfigChanged() {
		LOGGER = if (DialogClickerConfig.shouldLog) {
			modLogger
		} else {
			NOPLogger.NOP_LOGGER
		}
	}

	@Suppress("unused")
	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
