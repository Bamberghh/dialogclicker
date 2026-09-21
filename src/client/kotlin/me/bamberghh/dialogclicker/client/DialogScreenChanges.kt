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

class DialogScreenChanges(val self: DialogScreen<*>) {
    companion object {
        private const val LAYOUT_SPACING = 10
        private const val LAYOUT_MARGIN_TOP = 13
        private const val LAYOUT_MARGIN_RIGHT = 4
    }

    val savedActionKeys: MutableList<SavedActionKey> = ArrayList()
    private var prevActions: List<SavedAction> = listOf()
    private val currentActions: MutableList<SavedAction> = ArrayList()
    private var shouldCheckSavedActions = true
    private var shouldApplySavedActions = true
    private var shouldSaveActions = false
    private var centerLayout: LinearLayout? = null
    private var rightLayout: LinearLayout? = null
    private var headerLayout: FrameLayout? = null
    private var shouldSaveActionsCheckbox: Checkbox? = null
    private var eraseSavedActionsButton: Button? = null
    private var errors: MutableComponent = Component.empty()
    private var errorsNotification: StringWidget = StringWidget(errors, self.font)

    private fun createShouldSaveActionsCheckbox() {
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

    private fun createEraseSavedActionsButton() {
        eraseSavedActionsButton = null
        if (!DialogClickerConfig.isModEnabled || (prevActions.isEmpty() && currentActions.isEmpty())) {
            return
        }
        val eraseSavedActionsButton = Button
            .builder(Component.translatable("dialogclicker.menu.button_erase")) {
                prevActions = listOf()
                currentActions.clear()
                saveActions(listOf())
                (self as ScreenAccessor).dialogclicker_rebuildWidgets()
            }
            .size(90, 17)
            .build()
        eraseSavedActionsButton.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_erase.tooltip")))
        eraseSavedActionsButton.tabOrderGroup = -10
        this.eraseSavedActionsButton = eraseSavedActionsButton
    }

    private fun saveActions(savedActions: List<SavedAction>) {
        val externalTitle = (self as DialogScreenAccessor).dialogclicker_getDialog().common().computeExternalTitle()
        DialogClickerClient.saveActions(
            (self as ScreenAccessor).dialogclicker_getMinecraft(),
            externalTitle,
            savedActions
        )
    }

    private fun shouldSaveActions(): Boolean {
        return DialogClickerConfig.isModEnabled && shouldSaveActions
    }

    private fun shouldCheckSavedActions(): Boolean {
        return DialogClickerConfig.isModEnabled && DialogClickerConfig.shouldApplySavedActions && shouldCheckSavedActions
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

    private fun alignmentForCenterWithRight(w: Int, aW: Int, bW: Int): Float {
        return if (w >= (aW + 2 * bW))
            0.5f
        else max(0f, 1f - bW.toFloat() / (w - aW))
    }

    fun createTitleWithWarningButtonRETURN(cir: CallbackInfoReturnable<LayoutElement?>) {
        val centerLayout = cir.getReturnValue() as LinearLayout
        this.centerLayout = centerLayout

        createShouldSaveActionsCheckbox()
        createEraseSavedActionsButton()

        val rightLayout = LinearLayout.vertical()
        rightLayout.defaultCellSetting().alignHorizontallyRight()
        rightLayout.spacing(LAYOUT_SPACING)
        shouldSaveActionsCheckbox?.let { rightLayout.addChild(it) }
        eraseSavedActionsButton?.let { rightLayout.addChild(it) }
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

    fun constructorRETURN(
        @Suppress("unused") previousScreen: Screen?,
        dialog: Dialog,
        @Suppress("unused") connectionAccess: DialogConnectionAccess?,
        @Suppress("unused") ci: CallbackInfo?
    ) {
        val externalTitle = dialog.common().computeExternalTitle()
        prevActions = loadActions((self as ScreenAccessor).dialogclicker_getMinecraft(), externalTitle)
        errorsNotification.visible = false;
    }

    fun initHEAD(@Suppress("unused") ci: CallbackInfo) {
        // Need to do this because the layout gets initialized in DialogScreen's constructor
        (self as DialogScreenAccessor).dialogclicker_setLayout(HeaderAndFooterLayout(self))
        errorsNotification.message = Component.empty()
    }

    fun initRETURN(@Suppress("unused") ci: CallbackInfo) {
        // Not in the constructor because in case of closing the dialog, its
        // screen immediately gets set after the constructor in the call stack
        if (shouldCheckSavedActions()) {
            shouldCheckSavedActions = false
            errors = Component.empty()
            for (prevAction in prevActions) {
                if (!checkCloseAction(prevAction.closeAction)) {
                    errorsNotification.message = Component.literal("Saved action is outdated").withColor(TextColor.RED)
                    errorsNotification.setTooltip(Tooltip.create(errors))
                    errorsNotification.visible = true
                    (self as ScreenAccessor).dialogclicker_repositionElements()
                    return
                }
            }
            errorsNotification.message = Component.empty()
            errorsNotification.setTooltip(null)
            errorsNotification.visible = false
            (self as ScreenAccessor).dialogclicker_repositionElements()
        }
        if (shouldApplySavedActions()) {
            shouldApplySavedActions = false
            for (prevAction in prevActions) {
                self.runAction(prevAction.closeAction, prevAction.afterAction)
            }
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
        eraseSavedActionsButton?.let { rightLayout.addChild(it) }
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

    fun runActionHEAD(
        closeAction: Optional<ClickEvent>,
        afterAction: DialogAction,
        @Suppress("unused") ci: CallbackInfo
    ) {
        if (!shouldSaveActions()) {
            return
        }
        currentActions.add(SavedAction(closeAction, afterAction))
        saveActions(currentActions)
        if (prevActions.isEmpty() && currentActions.size == 1) {
            (self as ScreenAccessor).dialogclicker_rebuildWidgets()
        }
    }
}