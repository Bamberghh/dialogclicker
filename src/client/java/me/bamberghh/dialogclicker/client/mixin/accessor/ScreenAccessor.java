package me.bamberghh.dialogclicker.client.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("minecraft")
    Minecraft dialogclicker_getMinecraft();
    @Invoker("rebuildWidgets")
    void dialogclicker_rebuildWidgets();
    @Invoker("repositionElements")
    void dialogclicker_repositionElements();
    @Invoker("addRenderableOnly")
    <T extends Renderable> T dialogclicker_addRenderableOnly(final T renderable);
}

