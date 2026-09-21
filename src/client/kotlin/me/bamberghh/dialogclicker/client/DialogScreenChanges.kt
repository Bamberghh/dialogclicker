package me.bamberghh.dialogclicker.client

import me.bamberghh.dialogclicker.DialogClicker
import me.bamberghh.dialogclicker.client.DialogClickerClient.loadActions
import me.bamberghh.dialogclicker.client.mixin.accessor.DialogScreenAccessor
import me.bamberghh.dialogclicker.client.mixin.accessor.ScreenAccessor
import me.bamberghh.dialogclicker.config.DialogClickerConfig
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.layouts.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess
import net.minecraft.client.gui.screens.dialog.DialogScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.server.dialog.Dialog
import net.minecraft.server.dialog.DialogAction
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*
import kotlin.math.max

class DialogScreenChanges {
    companion object {
        private const val LAYOUT_SPACING = 10
        private const val LAYOUT_MARGIN_TOP = 3
        private const val LAYOUT_MARGIN_RIGHT = 4
    }

    val savedActionKeys: MutableList<SavedActionKey> = ArrayList()
    private var prevActions: List<SavedAction> = listOf()
    private val currentActions: MutableList<SavedAction> = ArrayList()
    private var shouldApplySavedActions = true
    private var shouldSaveActions = false
    private var oldHeaderLayout: LinearLayout? = null
    private var widgetsLayout: LinearLayout? = null
    private var newHeaderLayout: FrameLayout? = null
    private var shouldSaveActionsCheckbox: Checkbox? = null
    private var eraseSavedActionsButton: Button? = null

    private fun createShouldSaveActionsCheckbox(self: DialogScreen<*>) {
        if (!DialogClickerConfig.isModEnabled) {
            return
        }
        val shouldSaveActionsCheckbox = Checkbox
            .builder(Component.translatable("dialogclicker.menu.button_save"), self.font)
            .selected(shouldSaveActions)
            .onValueChange { _, value -> shouldSaveActions = value }
            .build()
        shouldSaveActionsCheckbox.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_save.tooltip")))
        shouldSaveActionsCheckbox.tabOrderGroup = -10
        this.shouldSaveActionsCheckbox = shouldSaveActionsCheckbox
    }

    private fun createEraseSavedActionsButton(self: DialogScreen<*>) {
        eraseSavedActionsButton = null
        if (!DialogClickerConfig.isModEnabled || (prevActions.isEmpty() && currentActions.isEmpty())) {
            return
        }
        val eraseSavedActionsButton = Button
            .builder(Component.translatable("dialogclicker.menu.button_erase")) {
                prevActions = listOf()
                currentActions.clear()
                saveActions(self, listOf())
                (self as ScreenAccessor).`dialogclicker$rebuildWidgets`()
            }
            .size(90, 17)
            .build()
        eraseSavedActionsButton.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_erase.tooltip")))
        eraseSavedActionsButton.tabOrderGroup = -10
        this.eraseSavedActionsButton = eraseSavedActionsButton
    }

    private fun saveActions(self: DialogScreen<*>, savedActions: List<SavedAction>) {
        val externalTitle = (self as DialogScreenAccessor).`dialogclicker$getDialog`().common().computeExternalTitle()
        DialogClickerClient.saveActions(
            (self as ScreenAccessor).`dialogclicker$getMinecraft`(),
            externalTitle,
            savedActions
        )
    }

    private fun shouldSaveActions(): Boolean {
        return DialogClickerConfig.isModEnabled && shouldSaveActions
    }

    private fun shouldApplySavedActions(): Boolean {
        return DialogClickerConfig.isModEnabled && DialogClickerConfig.shouldApplySavedActions && shouldApplySavedActions
    }

    private fun checkCloseAction(maybeCloseAction: Optional<ClickEvent>): Boolean {
        if (maybeCloseAction.isEmpty) {
            return true
        }
        val closeAction = maybeCloseAction.get()
        val matchesShallow = ArrayList<SavedActionKey>()
        for (savedActionKey in savedActionKeys) {
            if (savedActionKey.doesMatchClickEventShallow(closeAction)) {
                matchesShallow.add(savedActionKey)
            }
        }
        val matchErrors: MutableList<String>
        if (matchesShallow.isEmpty()) {
            matchErrors = mutableListOf("no similar actions found")
        } else {
            matchErrors = ArrayList()
            for (savedActionKey in matchesShallow) {
                val error = savedActionKey.doesMatchClickEvent(closeAction) ?: return true
                matchErrors.add(error)
            }
        }
        if (DialogClicker.LOGGER.isWarnEnabled) {
            DialogClicker.LOGGER.warn("Saved action is outdated: {}", matchErrors.joinToString("; "))
        }
        return false
    }

    private fun alignmentForCenterWithRight(w: Int, aW: Int, bW: Int): Float {
        return if (w >= (aW + 2 * bW))
            0.5f
        else max(0f, 1f - bW.toFloat() / (w - aW))
    }

    fun createTitleWithWarningButtonRETURN(self: DialogScreen<*>, cir: CallbackInfoReturnable<LayoutElement?>) {
        val oldHeaderLayout = cir.getReturnValue() as LinearLayout
        this.oldHeaderLayout = oldHeaderLayout

        createShouldSaveActionsCheckbox(self)
        createEraseSavedActionsButton(self)

        val widgetsLayout = LinearLayout.vertical()
        widgetsLayout.defaultCellSetting().alignHorizontallyRight()
        widgetsLayout.spacing(LAYOUT_SPACING)
        shouldSaveActionsCheckbox?.let { widgetsLayout.addChild(it) }
        eraseSavedActionsButton?.let { widgetsLayout.addChild(it) }
        this.widgetsLayout = widgetsLayout

        val newHeaderLayout = FrameLayout(self.width, 0)
        newHeaderLayout.addChild(oldHeaderLayout)
        newHeaderLayout.addChild(widgetsLayout)
        this.newHeaderLayout = newHeaderLayout

        (self as ScreenAccessor).`dialogclicker$addRenderableOnly`(
            ColoredWidgetWrapper(
                newHeaderLayout,
                colorAlpha = 0xFF / 3,
            )
        )

        cir.setReturnValue(newHeaderLayout)
    }

    fun constructorRETURN(
        self: DialogScreen<*>,
        @Suppress("unused") previousScreen: Screen?,
        dialog: Dialog,
        @Suppress("unused") connectionAccess: DialogConnectionAccess?,
        @Suppress("unused") ci: CallbackInfo?
    ) {
        val externalTitle = dialog.common().computeExternalTitle()
        prevActions = loadActions((self as ScreenAccessor).`dialogclicker$getMinecraft`(), externalTitle)
    }

    fun initHEAD(self: DialogScreen<*>, @Suppress("unused") ci: CallbackInfo) {
        // Need to do this because the layout gets initialized in DialogScreen's constructor
        (self as DialogScreenAccessor).`dialogclicker$setLayout`(HeaderAndFooterLayout(self))
    }

    fun initRETURN(self: DialogScreen<*>, @Suppress("unused") ci: CallbackInfo) {
        // Not in the constructor because in case of closing the dialog, its
        // screen immediately gets set after the constructor in the call stack
        if (shouldApplySavedActions()) {
            shouldApplySavedActions = false
            for (prevAction in prevActions) {
                if (!checkCloseAction(prevAction.closeAction)) {
                    return
                }
            }
            for (prevAction in prevActions) {
                self.runAction(prevAction.closeAction, prevAction.afterAction)
            }
        }
    }

    fun repositionElementsHEAD(self: DialogScreen<*>, @Suppress("unused") ci: CallbackInfo) {
        val oldHeaderLayout = oldHeaderLayout
        val widgetsLayout = widgetsLayout
        val newHeaderLayout = newHeaderLayout
        if (oldHeaderLayout == null || widgetsLayout == null || newHeaderLayout == null) {
            return
        }
        oldHeaderLayout.arrangeElements()
        widgetsLayout.arrangeElements()
        val alignment = alignmentForCenterWithRight(
            self.width,
            oldHeaderLayout.width,
            LAYOUT_SPACING + widgetsLayout.width + LAYOUT_MARGIN_TOP
        )
        newHeaderLayout.removeChildren()
        newHeaderLayout.setMinWidth(self.width)
        newHeaderLayout.addChild(oldHeaderLayout) { it.alignHorizontally(alignment) }
        newHeaderLayout.addChild(widgetsLayout) {
            it.paddingTop(LAYOUT_MARGIN_TOP)
            it.paddingRight(LAYOUT_MARGIN_RIGHT)
            it.alignHorizontallyRight()
        }
    }

    fun runActionHEAD(
        self: DialogScreen<*>,
        closeAction: Optional<ClickEvent>,
        afterAction: DialogAction,
        @Suppress("unused") ci: CallbackInfo
    ) {
        if (!shouldSaveActions()) {
            return
        }
        currentActions.add(SavedAction(closeAction, afterAction))
        saveActions(self, currentActions)
        if (prevActions.isEmpty() && currentActions.size == 1) {
            (self as ScreenAccessor).`dialogclicker$rebuildWidgets`()
        }
    }
}