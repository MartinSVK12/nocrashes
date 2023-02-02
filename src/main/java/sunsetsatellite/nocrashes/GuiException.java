package sunsetsatellite.nocrashes;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiMainMenu;
import net.minecraft.src.GuiScreen;

public class GuiException extends GuiScreen {
    private RuntimeException e;

    public void updateScreen() {
    }

    public GuiException(RuntimeException exception){
        e = exception;
    }

    public void initGui() {
        this.controlList.clear();
        this.controlList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120 + 12, "Back to title screen"));
    }

    protected void actionPerformed(GuiButton guiButton1) {
        if(guiButton1.enabled) {
            if(guiButton1.id == 0) {
                this.mc.displayGuiScreen(new GuiMainMenu());
            }

        }
    }

    public void drawScreen(int i1, int i2, float f3) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, "An exception has occurred!", this.width / 2, this.height / 4 - 60 + 20, 0xFFFFFF);
        this.drawString(this.fontRenderer, "Minecraft ran into an unexpected problem:", this.width / 2 - 140, this.height / 4 - 60 + 60 + 0, 10526880);
        this.drawString(this.fontRenderer, "", this.width / 2 - 140, this.height / 4 - 60 + 60 + 18, 10526880);
        this.drawCenteredString(this.fontRenderer, e.toString(), this.width / 2, this.height / 4 - 60 + 60 + 27, 0xFFFFFF00);
        this.drawCenteredString(this.fontRenderer, 	"   at "+e.getStackTrace()[0].toString(), this.width /2, this.height / 4 - 60 + 60 + 35, 0xFFFFFF00);
        this.drawString(this.fontRenderer, "To prevent more problems, the current game has quit.", this.width / 2 - 140, this.height / 4 - 60 + 60 + 60, 10526880);
        this.drawString(this.fontRenderer, "NoCrashes (BTA) v1.0.0 by MartinSVK12",2,this.height - 12,10526880);
        super.drawScreen(i1, i2, f3);
    }
}
