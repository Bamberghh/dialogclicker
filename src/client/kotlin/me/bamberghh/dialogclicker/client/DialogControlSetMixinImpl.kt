package me.bamberghh.dialogclicker.client

import net.minecraft.network.chat.ClickEvent
import net.minecraft.server.dialog.action.Action
import net.minecraft.server.dialog.action.CommandTemplate
import net.minecraft.server.dialog.action.CustomAll
import java.util.*
import java.util.function.Supplier

object DialogControlSetMixinImpl {
    fun bindActionMixin(
        dialogScreenInterface: DialogScreenInterface,
        maybeAction: Optional<Action>,
        clickEventSupplier: Supplier<Optional<ClickEvent>>
    ) {
        if (maybeAction.isEmpty) {
            return
        }
        val key = when (val action = maybeAction.get()) {
            is CustomAll -> ActionKeyCustomAll.fromCustomAll(action)
            is CommandTemplate -> ActionKeyCommandTemplate.fromCommandTemplate(action)
            else -> {
                val maybeClickEvent = clickEventSupplier.get()
                if (maybeClickEvent.isEmpty) {
                    return
                }
                val clickEvent = maybeClickEvent.get()
                ActionKeyStaticAction(clickEvent)
            }
        }
        dialogScreenInterface.dialogclicker_getCurrentActionKeys().add(key)
    }
}