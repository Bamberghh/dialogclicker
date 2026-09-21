package me.bamberghh.dialogclicker.client

import net.minecraft.network.chat.ClickEvent
import net.minecraft.server.dialog.action.Action
import net.minecraft.server.dialog.action.CommandTemplate
import net.minecraft.server.dialog.action.CustomAll
import java.util.*
import java.util.function.Supplier

object DialogControlSetChanges {
    fun bindActionMixin(
        dialogScreenInterface: DialogScreenInterface,
        maybeAction: Optional<Action>,
        clickEventSupplier: Supplier<Optional<ClickEvent>>
    ) {
        if (maybeAction.isEmpty) {
            return
        }
        val key = when (val action = maybeAction.get()) {
            is CustomAll -> SavedActionKeyCustomAll.fromCustomAll(action)
            is CommandTemplate -> SavedActionKeyCommandTemplate.fromCommandTemplate(action)
            else -> {
                val maybeClickEvent = clickEventSupplier.get()
                if (maybeClickEvent.isEmpty) {
                    return
                }
                val clickEvent = maybeClickEvent.get()
                SavedActionKeyStaticAction(clickEvent)
            }
        }
        dialogScreenInterface.dialogclicker_getSavedActionKeys().add(key)
    }
}