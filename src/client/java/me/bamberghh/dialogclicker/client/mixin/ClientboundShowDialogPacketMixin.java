package me.bamberghh.dialogclicker.client.mixin;

import com.mojang.serialization.JsonOps;
import me.bamberghh.dialogclicker.DialogClicker;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.Dialog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundShowDialogPacket.class)
public class ClientboundShowDialogPacketMixin {
    @Shadow
    @Final
    private Holder<Dialog> dialog;

    @Inject(method = "Lnet/minecraft/network/protocol/common/ClientboundShowDialogPacket;handle(Lnet/minecraft/network/protocol/common/ClientCommonPacketListener;)V", at = @At("HEAD"))
    private void handle(ClientCommonPacketListener listener, CallbackInfo ci) {
//        final var tagResult = Dialog.CODEC.encodeStart(NbtOps.INSTANCE, dialog);
//        if (tagResult.isError()) {
//            return;
//        }
//        final var tag = tagResult.result().get();
        // TODO: only print this if enabled in config
        final var tagResult = Dialog.CODEC.encodeStart(JsonOps.INSTANCE, dialog);
        if (tagResult.result().isEmpty()) {
            return;
        }
        final var tag = tagResult.result().get();
        DialogClicker.LOGGER.info("ClientboundShowDialogPacketMixin: {}", tag);
    }
}
