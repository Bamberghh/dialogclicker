package me.bamberghh.dialogclicker.config

import eu.midnightdust.lib.config.MidnightConfig

class DialogClickerConfig: MidnightConfig() {
    companion object {
        @Entry @JvmField var printReceivedDialogSNBT: Boolean = false
        @Entry @JvmField var printReceivedDialogJSON: Boolean = false
    }
}

