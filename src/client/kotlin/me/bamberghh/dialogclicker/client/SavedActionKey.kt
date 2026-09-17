package me.bamberghh.dialogclicker.client

import net.minecraft.commands.functions.CommandFunction
import net.minecraft.commands.functions.StringTemplate
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.resources.Identifier
import net.minecraft.server.dialog.action.CommandTemplate
import net.minecraft.server.dialog.action.CustomAll
import net.minecraft.server.dialog.action.StaticAction
import java.util.*

private fun stringTemplateToRegex(stringTemplate: StringTemplate): Regex {
    val builder = StringBuilder()

    builder.append("^")

    for (i in stringTemplate.variables.indices) {
        builder.append(Regex.escape(stringTemplate.segments[i])).append(".*")
        CommandFunction.checkCommandLineLength(builder)
    }

    if (stringTemplate.segments.size > stringTemplate.variables.size) {
        builder.append(Regex.escape(stringTemplate.segments.last()))
    }

    builder.append("$")

    return Regex(builder.toString())
}

sealed interface SavedActionKey {
    fun doesMatchClickEventShallow(clickEvent: ClickEvent): Boolean
    fun doesMatchClickEvent(clickEvent: ClickEvent): String?
}

data class SavedActionKeyCustomAll(val id: Identifier, val additions: Optional<CompoundTag>): SavedActionKey {
    companion object {
        fun fromCustomAll(customAll: CustomAll) =
            SavedActionKeyCustomAll(customAll.id, customAll.additions)
    }
    override fun doesMatchClickEventShallow(clickEvent: ClickEvent): Boolean {
        return clickEvent is ClickEvent.Custom
    }
    override fun doesMatchClickEvent(clickEvent: ClickEvent): String? {
        val custom = clickEvent as? ClickEvent.Custom ?: return "isn't \"${ClickEvent.Action.CUSTOM.name}\""
        if (custom.id != id) return "IDs don't match: ${custom.id} != $id"
        if (additions.isEmpty) return null
        if (custom.payload.isEmpty) return "payload is empty"
        val additions = additions.get()
        val payload = custom.payload.get()
        val payloadCompound = payload as? CompoundTag ?: return "payload isn't a compound"
        for ((key, additionTag) in additions.entrySet()) {
            val payloadTag =
                payloadCompound.get(key) ?: return "payload tag isn't present: additions.$key == $additionTag"
            if (payloadTag != additionTag) return "payload and additions tags don't match: .$key: $payloadTag != $additionTag"
        }
        return null
    }
}

data class SavedActionKeyCommandTemplate(val template: Regex): SavedActionKey {
    companion object {
        fun fromCommandTemplate(commandTemplate: CommandTemplate) =
            SavedActionKeyCommandTemplate(stringTemplateToRegex(commandTemplate.template.parsed))
    }
    override fun doesMatchClickEventShallow(clickEvent: ClickEvent): Boolean {
        return clickEvent is ClickEvent.RunCommand
    }
    override fun doesMatchClickEvent(clickEvent: ClickEvent): String? {
        val runCommand = clickEvent as? ClickEvent.RunCommand ?: return "isn't \"${ClickEvent.Action.RUN_COMMAND.name}\""
        if (!template.matches(runCommand.command)) return "command \"${runCommand.command}\" doesn't match the template \"$template\""
        return null
    }
}

data class SavedActionKeyStaticAction(val clickEvent: ClickEvent): SavedActionKey {
    companion object {
        fun fromStaticAction(staticAction: StaticAction) =
            SavedActionKeyStaticAction(staticAction.value)
    }
    override fun doesMatchClickEventShallow(clickEvent: ClickEvent): Boolean {
        return clickEvent.javaClass.isInstance(this.clickEvent)
    }
    override fun doesMatchClickEvent(clickEvent: ClickEvent): String? {
        if (this.clickEvent != clickEvent) return "click events don't match: ${this.clickEvent} != $clickEvent"
        return null
    }
}
