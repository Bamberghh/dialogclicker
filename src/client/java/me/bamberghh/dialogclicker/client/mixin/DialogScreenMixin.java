package me.bamberghh.dialogclicker.client.mixin;

import me.bamberghh.dialogclicker.client.DialogScreenChanges;
import me.bamberghh.dialogclicker.client.DialogScreenInterface;
import me.bamberghh.dialogclicker.client.SavedActionKey;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(DialogScreen.class)
public abstract class DialogScreenMixin<T extends Dialog> extends Screen implements DialogScreenInterface {
	@Unique private final DialogScreenChanges changes = new DialogScreenChanges();

	protected DialogScreenMixin(Component title) {
		super(title);
	}

	@Unique
	private DialogScreen<?> asOriginal() {
		return (DialogScreen<?>) (Object) this;
	}

	@Override
	public @NonNull List<SavedActionKey> dialogclicker$getSavedActionKeys() {
		return changes.getSavedActionKeys();
	}

	@Inject(method = "createTitleWithWarningButton", at = @At("RETURN"), cancellable = true)
	private void createTitleWithWarningButtonRETURN(CallbackInfoReturnable<LayoutElement> cir) {
		changes.createTitleWithWarningButtonRETURN(asOriginal(), cir);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void constructorRETURN(Screen previousScreen, Dialog dialog, DialogConnectionAccess connectionAccess, CallbackInfo ci) {
		changes.constructorRETURN(asOriginal(), previousScreen, dialog, connectionAccess, ci);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void initHEAD(CallbackInfo ci) {
		changes.initHEAD(asOriginal(), ci);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void initRETURN(CallbackInfo ci) {
		changes.initRETURN(asOriginal(), ci);
	}

	@Inject(method = "repositionElements", at = @At("HEAD"))
	private void repositionElementsHEAD(CallbackInfo ci) {
		changes.repositionElementsHEAD(asOriginal(), ci);
	}

	@Inject(method = "runAction(Ljava/util/Optional;Lnet/minecraft/server/dialog/DialogAction;)V", at = @At("HEAD"))
	private void runActionHEAD(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ClickEvent> closeAction, DialogAction afterAction, CallbackInfo ci) {
		changes.runActionHEAD(asOriginal(), closeAction, afterAction, ci);
	}
}