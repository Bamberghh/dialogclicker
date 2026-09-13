package me.bamberghh.dialogclicker

import eu.midnightdust.lib.config.MidnightConfig
import me.bamberghh.dialogclicker.config.DialogClickerConfig
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
//import me.bamberghh.dialogclicker.config.DialogClickerConfig

object DialogClicker : ModInitializer {
	const val MOD_ID: String = "dialogclicker"

	@JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

	@JvmField
	val CONFIG: DialogClickerConfig = DialogClickerConfig()

	override fun onInitialize() {
		MidnightConfig.init(MOD_ID, DialogClickerConfig::class.java)
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
