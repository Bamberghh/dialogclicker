package me.bamberghh.dialogclicker.client.mixin;

import me.bamberghh.dialogclicker.client.DialogControlSetChanges;
import me.bamberghh.dialogclicker.client.DialogScreenInterface;
import net.minecraft.client.gui.screens.dialog.DialogControlSet;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.dialog.action.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(DialogControlSet.class)
public class DialogControlSetMixin {
    @Shadow @Final private DialogScreen<?> screen;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(method = "bindAction", at = @At("RETURN"))
    private void bindActionMixin(Optional<Action> maybeAction, CallbackInfoReturnable<Supplier<Optional<ClickEvent>>> cir) {
        DialogControlSetChanges.INSTANCE.bindActionMixin((DialogScreenInterface) screen, maybeAction, cir.getReturnValue());
    }
}
