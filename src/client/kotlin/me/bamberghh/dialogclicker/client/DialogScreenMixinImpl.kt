package me.bamberghh.dialogclicker.client

import me.bamberghh.dialogclicker.DialogClicker
import me.bamberghh.dialogclicker.client.DialogClickerClient.loadActions
import me.bamberghh.dialogclicker.client.mixin.accessor.DialogScreenAccessor
import me.bamberghh.dialogclicker.client.mixin.accessor.ScreenAccessor
import me.bamberghh.dialogclicker.config.DialogClickerConfig
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.layouts.*
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess
import net.minecraft.client.gui.screens.dialog.DialogScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor
import net.minecraft.server.dialog.Dialog
import net.minecraft.server.dialog.DialogAction
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.*
import kotlin.math.max

class DialogScreenMixinImpl(val self: DialogScreen<*>) {
    companion object {
        private const val LAYOUT_SPACING = 10
        private const val LAYOUT_MARGIN_TOP = 13
        private const val LAYOUT_MARGIN_RIGHT = 4
    }

    val currentActionKeys: MutableList<ActionKey> = ArrayList()
    private var prevActions: List<ActionValue> = listOf()
    private val currentActions: MutableList<ActionValue> = ArrayList()
    private var errors: MutableComponent = Component.empty()
    private var shouldCheckPrevActions = true
    private var shouldApplyPrevActions = true
    private var shouldSaveCurrentActions = false

    private var centerLayout: LinearLayout? = null
    private var rightLayout: LinearLayout? = null
    private var headerLayout: FrameLayout? = null
    private var shouldSaveActionsCheckbox: Checkbox? = null
    private var eraseActionsButton: Button? = null
    private var errorsNotification: StringWidget = StringWidget(errors, self.font)

    private fun createShouldSaveActionsCheckbox() {
        if (!DialogClickerConfig.isModEnabled) {
            return
        }
        val shouldSaveActionsCheckbox = Checkbox
            .builder(Component.translatable("dialogclicker.menu.button_save"), self.font)
            .selected(shouldSaveCurrentActions)
            .onValueChange { _, value -> shouldSaveCurrentActions = value }
            .build()
        shouldSaveActionsCheckbox.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_save.tooltip")))
        shouldSaveActionsCheckbox.tabOrderGroup = -10
        this.shouldSaveActionsCheckbox = shouldSaveActionsCheckbox
    }

    private fun createEraseActionsButton() {
        this.eraseActionsButton = null
        if (!DialogClickerConfig.isModEnabled || (prevActions.isEmpty() && currentActions.isEmpty())) {
            return
        }
        val eraseActionsButton = Button
            .builder(Component.translatable("dialogclicker.menu.button_erase")) {
                errorsClear()
                prevActions = listOf()
                currentActions.clear()
                saveActions(listOf())
                (self as ScreenAccessor).dialogclicker_rebuildWidgets()
            }
            .size(90, 17)
            .build()
        eraseActionsButton.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_erase.tooltip")))
        eraseActionsButton.tabOrderGroup = -10
        this.eraseActionsButton = eraseActionsButton
    }

    private fun alignmentForCenterWithRight(w: Int, aW: Int, bW: Int): Float {
        return if (w >= (aW + 2 * bW))
            0.5f
        else max(0f, 1f - bW.toFloat() / (w - aW))
    }

    private fun shouldSaveActions(): Boolean {
        return DialogClickerConfig.isModEnabled && shouldSaveCurrentActions
    }

    private fun saveActions(actionValues: List<ActionValue>) {
        val externalTitle = (self as DialogScreenAccessor).dialogclicker_getDialog().common().computeExternalTitle()
        DialogClickerClient.saveActions(
            (self as ScreenAccessor).dialogclicker_getMinecraft(),
            externalTitle,
            actionValues
        )
    }

    private fun shouldCheckPrevActions(): Boolean {
        return DialogClickerConfig.isModEnabled && DialogClickerConfig.shouldApplyPrevActions && shouldCheckPrevActions
    }

    private fun checkPrevActions() {
        shouldCheckPrevActions = false
        errorsClear()
        for (prevAction in prevActions) {
            if (!checkCloseAction(prevAction.closeAction)) {
                shouldApplyPrevActions = false
                errorsUpdate(Component.translatable("dialogclicker.menu.error.outdated_saved_actions"))
                return
            }
        }
        (self as ScreenAccessor).dialogclicker_repositionElements()
    }

    private fun shouldApplyPrevActions(): Boolean {
        return DialogClickerConfig.isModEnabled && DialogClickerConfig.shouldApplyPrevActions && shouldApplyPrevActions
    }

    private fun applyPrevActions() {
        val shouldSaveCurrentActionsPrev = shouldSaveCurrentActions
        shouldSaveCurrentActions = false // otherwise they may be saved again by the mixin
        for (prevAction in prevActions) {
            self.runAction(prevAction.closeAction, prevAction.afterAction)
        }
        shouldSaveCurrentActions = shouldSaveCurrentActionsPrev
    }

    private fun checkCloseAction(maybeCloseAction: Optional<ClickEvent>): Boolean {
        if (maybeCloseAction.isEmpty) {
            return true
        }
        val closeAction = maybeCloseAction.get()
        val matchesShallow = ArrayList<ActionKey>()
        for (currentActionKey in currentActionKeys) {
            if (currentActionKey.doesMatchClickEventShallow(closeAction)) {
                matchesShallow.add(currentActionKey)
            }
        }
        val matchErrors: MutableList<String>
        if (matchesShallow.isEmpty()) {
            matchErrors = mutableListOf("no similar actions found")
        } else {
            matchErrors = ArrayList()
            for (currentActionKey in matchesShallow) {
                val error = currentActionKey.doesMatchClickEvent(closeAction) ?: return true
                matchErrors.add(error)
            }
        }
        for (matchError in matchErrors) {
            if (errors.siblings.isNotEmpty()) {
                errors.append(Component.literal("\n"))
            }
            errors.append(matchError)
        }
        if (DialogClicker.LOGGER.isWarnEnabled) {
            DialogClicker.LOGGER.warn("Saved action is outdated: {}", matchErrors.joinToString("; "))
        }
        return false
    }

    private fun errorsClear() {
        errors.siblings.clear()
        errorsNotification.message = errors
        errorsNotification.setTooltip(null)
    }

    private fun errorsUpdate(briefMessage: MutableComponent) {
        errorsNotification.message = briefMessage.withColor(TextColor.RED)
        errorsNotification.setTooltip(Tooltip.create(errors))
        (self as ScreenAccessor).dialogclicker_repositionElements()
    }

    fun constructorRETURN(
        @Suppress("unused") previousScreen: Screen?,
        dialog: Dialog,
        @Suppress("unused") connectionAccess: DialogConnectionAccess?,
        @Suppress("unused") ci: CallbackInfo?
    ) {
        val externalTitle = dialog.common().computeExternalTitle()
        prevActions = loadActions((self as ScreenAccessor).dialogclicker_getMinecraft(), externalTitle)
    }

    fun initHEAD(@Suppress("unused") ci: CallbackInfo) {
        // Need to do this because the layout gets initialized in DialogScreen's constructor
        (self as DialogScreenAccessor).dialogclicker_setLayout(HeaderAndFooterLayout(self))
        errorsNotification.message = Component.empty()
    }

    fun initRETURN(@Suppress("unused") ci: CallbackInfo) {
        // Not in the constructor because in case of closing the dialog, its
        // screen immediately gets set after the constructor in the call stack
        // Also the currentActionKeys need to be initialized

        // In case of a non-closing dialog action, the same dialog gets initialized twice when applying actions
        (self as ScreenAccessor).dialogclicker_setInitialized(true)
        if (shouldCheckPrevActions()) {
            shouldCheckPrevActions = false
            checkPrevActions()
        }
        if (shouldApplyPrevActions()) {
            shouldApplyPrevActions = false
            applyPrevActions()
        }
    }

    fun repositionElementsHEAD(@Suppress("unused") ci: CallbackInfo) {
        val centerLayout = centerLayout
        val rightLayout = rightLayout
        val headerLayout = headerLayout
        if (centerLayout == null || rightLayout == null || headerLayout == null) {
            return
        }
        centerLayout.arrangeElements()
        rightLayout.removeChildren()
        shouldSaveActionsCheckbox?.let { rightLayout.addChild(it) }
        eraseActionsButton?.let { rightLayout.addChild(it) }
        if (errors.siblings.isNotEmpty()) {
            rightLayout.addChild(errorsNotification)
        }
        rightLayout.arrangeElements()
        val alignment = alignmentForCenterWithRight(
            self.width,
            centerLayout.width,
            LAYOUT_SPACING + rightLayout.width + LAYOUT_MARGIN_RIGHT
        )
        headerLayout.removeChildren()
        headerLayout.setMinWidth(self.width)
        headerLayout.addChild(centerLayout) { it.alignHorizontally(alignment) }
        headerLayout.addChild(rightLayout) {
            it.paddingRight(LAYOUT_MARGIN_RIGHT)
            it.alignHorizontallyRight()
        }
    }

    fun createTitleWithWarningButtonRETURN(cir: CallbackInfoReturnable<LayoutElement?>) {
        val centerLayout = cir.getReturnValue() as LinearLayout
        this.centerLayout = centerLayout

        createShouldSaveActionsCheckbox()
        createEraseActionsButton()

        val rightLayout = LinearLayout.vertical()
        rightLayout.defaultCellSetting().alignHorizontallyRight()
        rightLayout.spacing(LAYOUT_SPACING)
        shouldSaveActionsCheckbox?.let { rightLayout.addChild(it) }
        eraseActionsButton?.let { rightLayout.addChild(it) }
        rightLayout.addChild(errorsNotification)
        this.rightLayout = rightLayout

        val headerLayout = FrameLayout(self.width, 0)
        headerLayout.defaultChildLayoutSetting().alignVerticallyTop().paddingTop(LAYOUT_MARGIN_TOP)
        headerLayout.addChild(centerLayout)
        headerLayout.addChild(rightLayout)
        this.headerLayout = headerLayout

        (self as ScreenAccessor).dialogclicker_addRenderableOnly(
            ColoredWidgetWrapper(
                headerLayout,
                colorAlpha = 0xFF / 4,
            )
        )

        cir.setReturnValue(headerLayout)
    }

    fun runActionHEAD(
        closeAction: Optional<ClickEvent>,
        afterAction: DialogAction,
        @Suppress("unused") ci: CallbackInfo
    ) {
        if (!shouldSaveActions()) {
            return
        }
        val needRebuilding = prevActions.isEmpty() && currentActions.isEmpty() || errors.siblings.isNotEmpty()
        errorsClear()
        currentActions.add(ActionValue(closeAction, afterAction))
        saveActions(currentActions)
        if (needRebuilding) {
            (self as ScreenAccessor).dialogclicker_rebuildWidgets()
        }
    }
}