package net.blay09.mods.eirairc.client.gui.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;

public class GuiLinkButton extends GuiButton {

    private final FontRenderer fontRenderer;

    public GuiLinkButton(int id, int x, int y, FontRenderer fontRenderer, String displayString) {
        super(id, x, y, displayString);
        this.fontRenderer = fontRenderer;

        width = fontRenderer.getStringWidth(displayString);
        height = fontRenderer.FONT_HEIGHT;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partial) {
        if (this.visible) {
            boolean hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            int textColor = 14737632;
            if (packedFGColour != 0) {
                textColor = packedFGColour;
            } else if (!this.enabled) {
                textColor = 10526880;
            } else if (hovered) {
                textColor = 16777120;
            }
            drawCenteredString(fontRenderer, displayString, x + width / 2, y + (height - 8) / 2, textColor);
        }
    }
}
