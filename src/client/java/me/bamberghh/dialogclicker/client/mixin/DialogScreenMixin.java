package me.bamberghh.dialogclicker.client.mixin;

import me.bamberghh.dialogclicker.DialogClicker;
import me.bamberghh.dialogclicker.client.DialogClickerClient;
import me.bamberghh.dialogclicker.client.SavedAction;
import me.bamberghh.dialogclicker.config.DialogClickerConfig;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import org.jspecify.annotations.NonNull;
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
	@Shadow
	@Final
	private T dialog;
	@Shadow
	@Final
	private HeaderAndFooterLayout layout;
	@Shadow
	public abstract void runAction(Optional<ClickEvent> closeAction, DialogAction afterAction);

	@Unique
	private LinearLayout headerLayout = null;
	@Unique
	@NonNull
	private List<SavedAction> prevActions = List.of();
	@Unique
	private boolean prevActionsLoaded = false;
	@Unique
	private Checkbox shouldSaveActionsCheckbox = null;
	@Unique
	private Button eraseSavedActionsButton = null;
	@Unique
	@NonNull
	private final List<SavedAction> currentActions = new ArrayList<>();

	protected DialogScreenMixin(Component title) {
		super(title);
	}

	@Unique
	private void createShouldSaveActionsCheckbox() {
		if (!DialogClickerConfig.isModEnabled || shouldSaveActionsCheckbox != null) {
			return;
		}
		shouldSaveActionsCheckbox = Checkbox
				.builder(Component.translatable("dialogclicker.menu.button_save"), font)
				.build();
		shouldSaveActionsCheckbox.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_save.tooltip")));
		shouldSaveActionsCheckbox.setTabOrderGroup(-10);
		if (headerLayout != null) {
			headerLayout.addChild(shouldSaveActionsCheckbox);
		}
		addRenderableWidget(shouldSaveActionsCheckbox);
		layout.arrangeElements();
	}

	@Unique
	private void createEraseSavedActionsButton() {
		if (!DialogClickerConfig.isModEnabled || prevActions.isEmpty() || this.eraseSavedActionsButton != null) {
			return;
		}
		eraseSavedActionsButton = Button
				.builder(Component.translatable("dialogclicker.menu.button_erase"), _ -> {
					prevActions = List.of();
					currentActions.clear();
					saveActions(List.of());
				})
				.width(90)
				.build();
		eraseSavedActionsButton.setTooltip(Tooltip.create(Component.translatable("dialogclicker.menu.button_erase.tooltip")));
		eraseSavedActionsButton.setTabOrderGroup(-10);
		if (headerLayout != null) {
			headerLayout.addChild(eraseSavedActionsButton);
		}
		addRenderableWidget(eraseSavedActionsButton);
		layout.arrangeElements();
	}

	@Unique
	private void saveActions(@NonNull List<SavedAction> savedActions) {
		Component externalTitle = dialog.common().computeExternalTitle();
		DialogClickerClient.INSTANCE.saveActions(minecraft, externalTitle, savedActions);
	}

	@Unique
	private boolean shouldSaveActions() {
		return DialogClickerConfig.isModEnabled && shouldSaveActionsCheckbox != null && shouldSaveActionsCheckbox.selected();
	}

	@Unique
	private boolean shouldApplySavedActions() {
		return DialogClickerConfig.isModEnabled && DialogClickerConfig.shouldApplySavedActions;
	}

	@Inject(method = "createTitleWithWarningButton", at = @At("RETURN"))
	private void createTitleWithWarningButton(CallbackInfoReturnable<LayoutElement> cir) {
        headerLayout = (LinearLayout) cir.getReturnValue();
		createShouldSaveActionsCheckbox();
		createEraseSavedActionsButton();
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo info) {
		if (!prevActionsLoaded) {
			// TODO: move this into constructor
			Component externalTitle = dialog.common().computeExternalTitle();
			prevActions = DialogClickerClient.INSTANCE.loadActions(minecraft, externalTitle);
			prevActionsLoaded = true;
			DialogClicker.LOGGER.info("loadActions {}", prevActions);
			if (shouldApplySavedActions()) {
				for (var prevAction : prevActions) {
					runAction(prevAction.closeAction(), prevAction.afterAction());
				}
			}
		}
		createShouldSaveActionsCheckbox();
		createEraseSavedActionsButton();
	}

	@Inject(method = "runAction(Ljava/util/Optional;Lnet/minecraft/server/dialog/DialogAction;)V", at = @At("HEAD"))
	private void runActionMixin(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ClickEvent> closeAction, DialogAction afterAction, CallbackInfo ci) {
		if (!shouldSaveActions()) {
			return;
		}
		currentActions.add(new SavedAction(closeAction, afterAction));
		saveActions(currentActions);
	}
}