package me.bamberghh.dialogclicker.client

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.components.WidgetTooltipHolder
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import java.util.function.Consumer
import kotlin.random.Random

class ColoredWidgetWrapper(
    var element: LayoutElement? = null,
    val colorSeed: Int = 0,
    val colorAlpha: Int = 0xFF,
    val colorDarkerLayouts: Boolean = false,
    val sizeFactor: Float = 1f,
    val padding: Int = 0
) : GuiEventListener, Renderable, NarratableEntry, Layout {
    private var tooltip: WidgetTooltipHolder = WidgetTooltipHolder()
    private var isFocused = false
    private var isHovered = false

    override fun setX(x: Int) {
        element?.x = x
    }

    override fun setY(y: Int) {
        element?.y = y
    }

    override fun getX(): Int = element?.x ?: 0
    override fun getY(): Int = element?.y ?: 0
    override fun getWidth(): Int = element?.width ?: 0
    override fun getHeight(): Int = element?.height ?: 0
    override fun getRectangle(): ScreenRectangle = element?.rectangle ?: ScreenRectangle.empty()

    override fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    override fun isFocused(): Boolean = isFocused

    override fun setPosition(x: Int, y: Int) {
        element?.setPosition(x, y)
    }

    override fun visitWidgets(widgetVisitor: Consumer<AbstractWidget>) {
        element?.visitWidgets(widgetVisitor)
    }

    override fun visitChildren(layoutElementVisitor: Consumer<LayoutElement>) {
        element?.let { layoutElementVisitor.accept(it) }
    }

    override fun removeChildren() {
        element = null
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        a: Float
    ) {
        isHovered = graphics.containsPointInScissor(mouseX, mouseY) && rectangle.containsPoint(mouseX, mouseY)
        val elementsText = Component.empty()
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
            val color = Mth.hsvToArgb(
                hue,
                if (isWidget) 3 / 4f + 1 / 4f * random.nextFloat() else 1 / 4f + 1 / 4f * random.nextFloat(),
                if (isWidget) 7 / 8f + 1 / 8f * random.nextFloat() else 1 / 8f + 1 / 8f * random.nextFloat(),
                colorAlpha
            )
            if (element.rectangle.containsPoint(mouseX, mouseY)) {
                var name = element.javaClass.typeName
                name = name.removePrefix("net.minecraft.client.gui.")
                name = name.removePrefix("layouts.")
                name = name.removePrefix("components.")
                if (!elementsText.siblings.isEmpty()) {
                    elementsText.append("\n");
                }
                elementsText.append(
                    Component.literal(name).withColor(
                        Mth.hsvToArgb(
                            hue,
                            if (isWidget) 1f else 1 / 3f,
                            if (isWidget) 1f else 1 / 3f,
                            0xFF
                        )
                    )
                )
            }
            graphics.fill(
                RenderPipelines.GUI,
                x0.toInt(),
                y0.toInt(),
                x1.toInt(),
                y1.toInt(),
                color,
            )
        }

        fun visitLayouts(random: Random, root: Layout) {
            root.visitChildren { element ->
                if (element is Layout) {
                    drawElement(random, !colorDarkerLayouts, element)
                    visitLayouts(random, element)
                }
            }
        }

        val random = Random(colorSeed)
        visitLayouts(random, this)
        visitWidgets { widget ->
            if (widget != this) {
                drawElement(random, true, widget)
            }
        }
        (element as? AbstractWidget)?.extractRenderState(graphics, mouseX, mouseY, a)
        tooltip.set(Tooltip.create(elementsText))
        tooltip.refreshTooltipForNextRenderPass(
            graphics,
            mouseX,
            mouseY,
            isHovered,
            isFocused,
            this.getRectangle()
        )
    }

    override fun narrationPriority(): NarratableEntry.NarrationPriority = NarratableEntry.NarrationPriority.NONE

    override fun updateNarration(output: NarrationElementOutput) {
    }
}
