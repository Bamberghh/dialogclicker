package me.bamberghh.dialogclicker.client.mixin;

import me.bamberghh.dialogclicker.client.DialogScreenMixinImpl;
import me.bamberghh.dialogclicker.client.DialogScreenInterface;
import me.bamberghh.dialogclicker.client.ActionKey;
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
public abstract class DialogScreenMixin extends Screen implements DialogScreenInterface {
	@Unique private final DialogScreenMixinImpl impl = new DialogScreenMixinImpl((DialogScreen<?>) (Object) this);

	protected DialogScreenMixin(Component title) {
		super(title);
	}

	@Override
	public @NonNull List<ActionKey> dialogclicker_getCurrentActionKeys() {
		return impl.getCurrentActionKeys();
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void constructorRETURN(Screen previousScreen, Dialog dialog, DialogConnectionAccess connectionAccess, CallbackInfo ci) {
		impl.constructorRETURN(previousScreen, dialog, connectionAccess, ci);
	}

    @Inject(method = "init", at = @At("HEAD"))
    private void initHEAD(CallbackInfo ci) {
        impl.initHEAD(ci);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void initRETURN(CallbackInfo ci) {
        impl.initRETURN(ci);
    }

	@Inject(method = "repositionElements", at = @At("HEAD"))
	private void repositionElementsHEAD(CallbackInfo ci) {
		impl.repositionElementsHEAD(ci);
	}

	@Inject(method = "createTitleWithWarningButton", at = @At("RETURN"), cancellable = true)
	private void createTitleWithWarningButtonRETURN(CallbackInfoReturnable<LayoutElement> cir) {
		impl.createTitleWithWarningButtonRETURN(cir);
	}

	@Inject(method = "runAction(Ljava/util/Optional;Lnet/minecraft/server/dialog/DialogAction;)V", at = @At("HEAD"))
	private void runActionHEAD(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<ClickEvent> closeAction, DialogAction afterAction, CallbackInfo ci) {
		impl.runActionHEAD(closeAction, afterAction, ci);
	}
}