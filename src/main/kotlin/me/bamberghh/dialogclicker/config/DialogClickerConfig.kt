package me.bamberghh.dialogclicker.config

import eu.midnightdust.lib.config.MidnightConfig

class DialogClickerConfig: MidnightConfig() {
    companion object {
        const val GENERAL = "general"
        const val DEBUG = "debug"

        @Entry(category = GENERAL) @JvmField var isModEnabled: Boolean = true
        @Entry(category = GENERAL) @JvmField var shouldApplyPrevActions: Boolean = true
        @Entry(category = DEBUG) @JvmField var shouldLogReceivedDialogSNBT: Boolean = false
        @Entry(category = DEBUG) @JvmField var shouldLogReceivedDialogJSON: Boolean = false
    }
}

