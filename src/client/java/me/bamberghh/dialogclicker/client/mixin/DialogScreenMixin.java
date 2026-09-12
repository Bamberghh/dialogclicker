package me.bamberghh.dialogclicker.client.mixin;

import me.bamberghh.dialogclicker.DialogClicker;
import me.bamberghh.dialogclicker.client.DialogClickerClient;
import me.bamberghh.dialogclicker.client.RememberedAction;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(DialogScreen.class)
public abstract class DialogScreenMixin<T extends Dialog> extends Screen {
	@Shadow
	@Final
	private T dialog;

	@Shadow
	public abstract void runAction(Optional<ClickEvent> closeAction, DialogAction afterAction);

	@Unique
	private boolean triedLoadingAction = false;
	@Unique
	private Checkbox rememberCheckbox;
	@Unique
	private boolean shouldRememberAction = false;

	protected DialogScreenMixin(Component title) {
		super(title);
	}

	@Unique
	private void createRememberButton() {
		if (this.rememberCheckbox != null) {
			return;
		}
		this.rememberCheckbox = Checkbox
				.builder(Component.translatable("menu.dialogclicker.button_remember"), this.font)
				.onValueChange((_, value) -> shouldRememberAction = value)
				.build();
		this.rememberCheckbox.setTooltip(Tooltip.create(Component.translatable("menu.dialogclicker.button_remember.tooltip")));
		this.rememberCheckbox.setTabOrderGroup(-10);
		this.addRenderableWidget(this.rememberCheckbox);
	}

	@Inject(method = "createTitleWithWarningButton", at = @At("RETURN"))
	private void createTitleWithWarningButton(CallbackInfoReturnable<LayoutElement> cir) {
		LinearLayout layout = (LinearLayout) cir.getReturnValue();
		createRememberButton();
		layout.addChild(this.rememberCheckbox);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo info) {
		createRememberButton();
		if (!triedLoadingAction) {
			triedLoadingAction = true;
			Component externalTitle = dialog.common().computeExternalTitle();
			final var action = DialogClickerClient.INSTANCE.loadAction(minecraft, externalTitle);
			DialogClicker.LOGGER.info("loadAction {}", action);
			if (action != null) {
				runAction(action.closeAction(), action.afterAction());
			}
		}
	}

	@Inject(method = "runAction(Ljava/util/Optional;Lnet/minecraft/server/dialog/DialogAction;)V", at = @At("HEAD"))
	private void runActionMixin(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ClickEvent> closeAction, DialogAction afterAction, CallbackInfo ci) {
		if (!shouldRememberAction) {
			return;
		}
		Component externalTitle = dialog.common().computeExternalTitle();
		DialogClickerClient.INSTANCE.saveAction(minecraft, externalTitle, new RememberedAction(closeAction, afterAction));
	}
}