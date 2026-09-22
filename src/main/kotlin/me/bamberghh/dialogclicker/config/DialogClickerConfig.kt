package me.bamberghh.dialogclicker.config

import eu.midnightdust.lib.config.MidnightConfig
import me.bamberghh.dialogclicker.DialogClicker

class DialogClickerConfig: MidnightConfig() {
    companion object {
        const val GENERAL = "general"
        const val DEBUG = "debug"

        @Entry(category = GENERAL) @JvmField var isModEnabled: Boolean = true
        @Entry(category = GENERAL) @Condition(requiredOption = "isModEnabled") @JvmField var shouldApplyPrevActions: Boolean = true

        @Entry(category = DEBUG) @JvmField var shouldLog: Boolean = true
        @Entry(category = DEBUG) @Condition(requiredOption = "shouldLog") @JvmField var shouldLogActionSaving: Boolean = true
        @Entry(category = DEBUG) @Condition(requiredOption = "shouldLog") @JvmField var shouldLogActionLoading: Boolean = true
        @Entry(category = DEBUG) @Condition(requiredOption = "shouldLog") @JvmField var shouldLogReceivedDialogSNBT: Boolean = false
        @Entry(category = DEBUG) @Condition(requiredOption = "shouldLog") @JvmField var shouldLogReceivedDialogJSON: Boolean = false
    }

    override fun writeChanges() {
        super.writeChanges()
        DialogClicker.onConfigChanged()
    }
}

