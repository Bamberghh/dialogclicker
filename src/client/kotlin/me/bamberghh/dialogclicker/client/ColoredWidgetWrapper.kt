package me.bamberghh.dialogclicker.client

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents
import net.minecraft.util.Mth
import java.util.function.Consumer
import kotlin.random.Random

class ColoredWidgetWrapper(private var element: LayoutElement?, val seed: Int, val sizeFactor: Float, val padding: Int) :
    AbstractWidget(
        element?.x ?: 0,
        element?.y ?: 0,
        element?.width ?: 0,
        element?.height ?: 0,
        (element as? AbstractWidget)?.message ?: CommonComponents.EMPTY
    ), Layout {
    override fun setX(x: Int) {
//        super.setX(x)
        element?.x = x
    }

    override fun setY(y: Int) {
//        super.setY(x)
        element?.y = y
    }

    override fun getX(): Int {
        return element?.x ?: 0
    }

    override fun getY(): Int {
        return element?.y ?: 0
    }

    override fun getWidth(): Int {
        return element?.width ?: 0
    }

    override fun getHeight(): Int {
        return element?.height ?: 0
    }

    override fun getRectangle(): ScreenRectangle {
        return element?.rectangle ?: ScreenRectangle.empty()
    }

    override fun setPosition(x: Int, y: Int) {
//        super<AbstractWidget>.setPosition(x, y)
        element?.setPosition(x, y)
    }

    override fun visitWidgets(widgetVisitor: Consumer<AbstractWidget>) {
        super<AbstractWidget>.visitWidgets(widgetVisitor)
        element?.visitWidgets(widgetVisitor)
    }

    override fun visitChildren(layoutElementVisitor: Consumer<LayoutElement>) {
        val element = element
        if (element is Layout) {
            layoutElementVisitor.accept(element)
//            element.visitChildren(layoutElementVisitor)
        } else {
            element?.visitWidgets { layoutElementVisitor.accept(it) }
        }
    }

    override fun removeChildren() {
        element = null
    }

    fun setChild(child: LayoutElement?) {
        this.element = child
    }

    override fun extractWidgetRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        a: Float
    ) {
        fun drawElement(random: Random, isWidget: Boolean, element: LayoutElement) {
            val xm = element.x + 0.5f * element.width
            val ym = element.y + 0.5f * element.height
            val wm = (padding + 0.5f * sizeFactor * element.width).coerceAtLeast(0.5f)
            val hm = (padding + 0.5f * sizeFactor * element.height).coerceAtLeast(0.5f)
            val x0 = xm - wm
            val y0 = ym - hm
            val x1 = xm + wm
            val y1 = ym + hm
            val hue = random.nextFloat()
            graphics.fill(
                RenderPipelines.GUI,
                x0.toInt(),
                y0.toInt(),
                x1.toInt(),
                y1.toInt(),
                Mth.hsvToArgb(
                    hue,
                    if (isWidget) 3/4f + 1/4f*random.nextFloat() else 1/4f + 1/4f*random.nextFloat(),
                    if (isWidget) 7/8f + 1/8f*random.nextFloat() else 1/8f + 1/8f*random.nextFloat(),
                    0xFF
                ),
            )
        }
        fun visitLayouts(random: Random, root: Layout) {
            root.visitChildren { element ->
                if (element is Layout) {
                    drawElement(random, false, element)
                    visitLayouts(random, element)
                }
            }
        }
        val random = Random(seed)
        visitLayouts(random, this)
        visitWidgets { widget ->
            if (widget != this) {
                drawElement(random, true, widget)
            }
        }
        (element as? AbstractWidget)?.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
    }
}
