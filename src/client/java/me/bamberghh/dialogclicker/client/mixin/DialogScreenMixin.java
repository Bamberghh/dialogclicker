package me.bamberghh.dialogclicker.client.mixin;

import me.bamberghh.dialogclicker.client.DialogClickerClient;
import me.bamberghh.dialogclicker.client.SavedAction;
import me.bamberghh.dialogclicker.config.DialogClickerConfig;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(DialogScreen.class)
public abstract class DialogScreenMixin<T extends Dialog> extends Screen {
	@Shadow @Final private T dialog;
	@Shadow private HeaderAndFooterLayout layout;
	@Shadow public abstract void runAction(Optional<ClickEvent> closeAction, DialogAction afterAction);

	@Unique @NonNull private List<SavedAction> prevActions = List.of();
	@Unique @NonNull private final List<SavedAction> currentActions = new ArrayList<>();
	@Unique private boolean shouldApplySavedActions = true;
	@Unique private boolean shouldSaveActions = false;
	@Unique @Nullable private LinearLayout oldHeaderLayout = null;
	@Unique @Nullable private LinearLayout widgetsLayout = null;
	@Unique @Nullable private FrameLayout newHeaderLayout = null;
    @Unique @Nullable private Checkbox shouldSaveActionsCheckbox = null;
	@Unique @Nullable private Button eraseSavedActionsButton = null;

	protected DialogScreenMixin(Component title) {
		super(title);
	}

	@Unique
	private void createShouldSaveActionsCheckbox() {
		if (!DialogClickerConfig.isModEnabled) {
			return;
		}
		shouldSaveActionsCheckbox = Checkbox
				.builder(Component.translatable("dialogclicker.menu.button_save"), font)
				.selected(shouldSaveActions)
				.onValueChange((checkbox, value) -> {
					shouldSaveActions = value;
				})
				.build();
		shouldSaveActionsCheckbox.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_save.tooltip")));
		shouldSaveActionsCheckbox.setTabOrderGroup(-10);
	}

	@Unique
	private void createEraseSavedActionsButton() {
		eraseSavedActionsButton = null;
		if (!DialogClickerConfig.isModEnabled || (prevActions.isEmpty() && currentActions.isEmpty())) {
			return;
		}
		eraseSavedActionsButton = Button
				.builder(Component.translatable("dialogclicker.menu.button_erase"), _ -> {
					prevActions = List.of();
					currentActions.clear();
					saveActions(List.of());
					rebuildWidgets();
				})
				.size(90, 17)
				.build();
		eraseSavedActionsButton.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_erase.tooltip")));
		eraseSavedActionsButton.setTabOrderGroup(-10);
	}

	@Unique
	private void saveActions(@NonNull List<SavedAction> savedActions) {
		Component externalTitle = dialog.common().computeExternalTitle();
		DialogClickerClient.INSTANCE.saveActions(minecraft, externalTitle, savedActions);
	}

	@Unique
	private boolean shouldSaveActions() {
		return DialogClickerConfig.isModEnabled && shouldSaveActions;
	}

	@Unique
	private boolean shouldApplySavedActions() {
		return DialogClickerConfig.isModEnabled && DialogClickerConfig.shouldApplySavedActions && shouldApplySavedActions;
	}

	@Inject(method = "createTitleWithWarningButton", at = @At("RETURN"), cancellable = true)
	private void createTitleWithWarningButtonMixin(CallbackInfoReturnable<LayoutElement> cir) {
		var oldHeaderLayout = (LinearLayout) cir.getReturnValue();
		this.oldHeaderLayout = oldHeaderLayout;
		newHeaderLayout = new FrameLayout(width, 0);
		createShouldSaveActionsCheckbox();
		createEraseSavedActionsButton();
		widgetsLayout = LinearLayout.horizontal();
		widgetsLayout.spacing(10);
		if (shouldSaveActionsCheckbox != null) {
			widgetsLayout.addChild(shouldSaveActionsCheckbox);
		}
		if (eraseSavedActionsButton != null) {
			widgetsLayout.addChild(eraseSavedActionsButton);
		}
		newHeaderLayout.addChild(oldHeaderLayout);
		newHeaderLayout.addChild(widgetsLayout);
		cir.setReturnValue(newHeaderLayout);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void constructorMixin(Screen previousScreen, Dialog dialog, DialogConnectionAccess connectionAccess, CallbackInfo ci) {
		Component externalTitle = dialog.common().computeExternalTitle();
		prevActions = DialogClickerClient.INSTANCE.loadActions(minecraft, externalTitle);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void initMixin(CallbackInfo info) {
		// Need to do this because the layout gets initialized in DialogScreen's constructor
		layout = new HeaderAndFooterLayout(this);
		// Not in the constructor because in case of closing the dialog, its
		// screen immediately gets set after the constructor in the call stack
		if (shouldApplySavedActions()) {
			shouldApplySavedActions = false;
			for (var prevAction : prevActions) {
				runAction(prevAction.closeAction(), prevAction.afterAction());
			}
		}
	}

	@Unique
	private float alignmentForCenterWithRight(int aW, int bW) {
		return width >= (aW + 2*bW)
				? 0.5f
				: Math.max(0f, 1f - (float)bW/(width - aW));
	}

	@Inject(method = "repositionElements", at = @At("HEAD"))
	private void repositionElementsMixin(CallbackInfo info) {
		if (oldHeaderLayout == null || widgetsLayout == null || newHeaderLayout == null) {
			return;
		}
		oldHeaderLayout.arrangeElements();
		widgetsLayout.arrangeElements();
		// 10 from layout padding & 10 for margin
		float alignment = alignmentForCenterWithRight(oldHeaderLayout.getWidth(), widgetsLayout.getWidth() + 20);
		//noinspection DataFlowIssue
        newHeaderLayout.removeChildren();
		newHeaderLayout.setMinWidth(width);
		//noinspection DataFlowIssue
		newHeaderLayout.addChild(oldHeaderLayout, settings -> settings.alignHorizontally(alignment));
		//noinspection DataFlowIssue
		newHeaderLayout.addChild(widgetsLayout, settings -> {
			settings.paddingRight(10);
			settings.alignHorizontallyRight();
		});
	}

	@Inject(method = "runAction(Ljava/util/Optional;Lnet/minecraft/server/dialog/DialogAction;)V", at = @At("HEAD"))
	private void runActionMixin(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ClickEvent> closeAction, DialogAction afterAction, CallbackInfo ci) {
		if (!shouldSaveActions()) {
			return;
		}
		currentActions.add(new SavedAction(closeAction, afterAction));
		saveActions(currentActions);
		if (prevActions.isEmpty() && currentActions.size() == 1) {
			rebuildWidgets();
		}
	}
}