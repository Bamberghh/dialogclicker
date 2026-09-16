package me.bamberghh.dialogclicker.client.mixin;

import com.mojang.serialization.JsonOps;
import me.bamberghh.dialogclicker.DialogClicker;
import me.bamberghh.dialogclicker.config.DialogClickerConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.core.Holder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.dialog.Dialog;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
    @Inject(method = "showDialog(Lnet/minecraft/core/Holder;Lnet/minecraft/client/gui/screens/dialog/DialogConnectionAccess;Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void showDialogMixin(Holder<Dialog> dialog, DialogConnectionAccess connectionAccess, @Nullable Screen activeScreen, CallbackInfo ci) {
        if (!DialogClickerConfig.isModEnabled) {
            return;
        }
        if (DialogClickerConfig.shouldPrintReceivedDialogSNBT) {
            final var tagResult = Dialog.CODEC.encodeStart(NbtOps.INSTANCE, dialog);
            if (tagResult.result().isEmpty()) {
                return;
            }
            final var tag = tagResult.result().get();
            DialogClicker.LOGGER.info("Received dialog SNBT: {}", tag);
        }
        if (DialogClickerConfig.shouldPrintReceivedDialogJSON) {
            final var tagResult = Dialog.CODEC.encodeStart(JsonOps.INSTANCE, dialog);
            if (tagResult.result().isEmpty()) {
                return;
            }
            final var tag = tagResult.result().get();
            DialogClicker.LOGGER.info("Received dialog JSON: {}", tag);
        }
    }
}
