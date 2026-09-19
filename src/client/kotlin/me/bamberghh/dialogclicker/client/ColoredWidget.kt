package me.bamberghh.dialogclicker.client

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents

class ColoredWidget(x: Int, y: Int, width: Int, height: Int, val color: Int) : AbstractWidget(x, y, width, height, CommonComponents.EMPTY) {
    override fun extractWidgetRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        a: Float
    ) {
        graphics.fill(
            RenderPipelines.GUI,
            x,
            y,
            x+width,
            y+height,
            color,
        )
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
    }
}
