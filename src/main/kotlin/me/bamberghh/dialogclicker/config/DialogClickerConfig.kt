package me.bamberghh.dialogclicker.config

import eu.midnightdust.lib.config.MidnightConfig

class DialogClickerConfig: MidnightConfig() {
    companion object {
        @Entry @JvmField var isModEnabled: Boolean = true
        @Entry @JvmField var shouldApplySavedActions: Boolean = true
        @Entry @JvmField var shouldPrintReceivedDialogSNBT: Boolean = false
        @Entry @JvmField var shouldPrintReceivedDialogJSON: Boolean = false
    }
}

