package sunsetsatellite.nocrashes.mixin;

import bta.ModLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MinecraftApplet;
import net.minecraft.src.*;
import net.minecraft.src.command.ClientCommandHandler;
import net.minecraft.src.helper.Time;
import net.minecraft.src.input.MouseInput;
import net.minecraft.src.input.controller.ControllerInput;
import net.minecraft.src.render.IRenderer;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import sunsetsatellite.nocrashes.GuiException;

import java.awt.*;

@Mixin(
        value = Minecraft.class,
        remap = false
)
public abstract class MinecraftMixin {
    @Shadow public PlayerController playerController;
    @Shadow private boolean fullscreen;
    @Shadow private Timer timer;
    @Shadow public World theWorld;
    @Shadow public EntityPlayerSP thePlayer;
    @Shadow public Canvas mcCanvas;
    @Shadow public volatile boolean isGamePaused;
    @Shadow public GuiScreen currentScreen;
    @Shadow public EntityRenderer entityRenderer;
    @Shadow private int ticksRan;
    @Shadow public GuiAchievement guiAchievement;
    @Shadow public boolean skipRenderWorld;
    @Shadow public GameSettings gameSettings;
    @Shadow protected MinecraftApplet mcApplet;
    @Shadow public SoundManager sndManager;
    @Shadow public volatile boolean running;
    @Shadow public String debug;
    @Shadow public String debugFPS;
    @Shadow long prevFrameTime;
    @Shadow @Final public GameResolution resolution;
    @Shadow @Final public ClientCommandHandler commandHandler;
    @Shadow public IRenderer render;
    @Shadow public ControllerInput controllerInput;
    @Shadow public MouseInput mouseInput;

    @Shadow public abstract void onMinecraftCrash(UnexpectedThrowable unexpectedthrowable);
    @Shadow public abstract void startGame() throws LWJGLException;

    @Shadow public abstract void displayGuiScreen(GuiScreen guiscreen);

    @Shadow protected abstract void checkGLError(String s);

    @Shadow public abstract void shutdownMinecraftApplet();

    @Shadow public abstract void func_28002_e();

    @Shadow protected abstract void screenshotListener();

    @Shadow protected abstract void displayDebugInfo(long l);

    @Shadow public abstract void shutdown();

    @Shadow public abstract boolean shadersActive();

    @Shadow public abstract void displayInGameMenu();

    @Shadow public abstract void toggleFullscreen();

    @Shadow protected abstract void resize();

    @Shadow public abstract boolean isMultiplayerWorld();

    @Shadow public abstract void changeWorld1(World world);

    @Shadow public abstract void runTick();

    /**
     * @author MartinSVK12
     * @reason Better crash handler
     */
    @Overwrite
    public void run() {
        this.running = true;

        try {
            this.startGame();
        } catch (Exception var17) {
            var17.printStackTrace();
            this.onMinecraftCrash(new UnexpectedThrowable("Failed to start game", var17));
            return;
        }

        try {
            long l = System.currentTimeMillis();
            int i = 0;

            while(this.running) {
                try {
                    Time.tick();
                    //noinspection removal
                    if (this.mcApplet != null && !this.mcApplet.isActive()) {
                        break;
                    }

                    AxisAlignedBB.clearBoundingBoxPool();
                    Vec3D.initialize();
                    if (this.mcCanvas == null && Display.isCloseRequested()) {
                        this.shutdown();
                    }

                    if (Display.isFullscreen() && !Display.isActive()) {
                        this.toggleFullscreen();
                    }

                    if (this.isGamePaused && this.theWorld != null) {
                        float f = this.timer.renderPartialTicks;
                        this.timer.updateTimer();
                        this.timer.renderPartialTicks = f;
                    } else {
                        this.timer.updateTimer();
                    }

                    long l1 = System.nanoTime();
                    this.mouseInput.update();
                    if (this.controllerInput != null && this.currentScreen != null) {
                        this.controllerInput.moveCursor();
                    }

                    for(int j = 0; j < this.timer.elapsedTicks; ++j) {
                        ++this.ticksRan;

                        try {
                            this.runTick();
                        } catch (MinecraftException var16) {
                            this.theWorld = null;
                            this.changeWorld1(null);
                            this.displayGuiScreen(new GuiConflictWarning());
                        }
                    }

                    long l2 = System.nanoTime() - l1;
                    this.checkGLError("Pre render");
                    RenderBlocks.fancyGrass = this.gameSettings.fancyGraphics.value;
                    this.sndManager.func_338_a(this.thePlayer, this.timer.renderPartialTicks);
                    GL11.glEnable(3553);
                    if (this.theWorld != null) {
                        this.theWorld.updatingLighting();
                    }

                    Display.update();
                    if (this.thePlayer != null && this.thePlayer.isEntityInsideOpaqueBlock()) {
                        this.gameSettings.thirdPersonView.value = 0;
                    }

                    if (this.shadersActive()) {
                        this.render.beginRenderGame();
                    }

                    GL11.glEnable(3008);
                    if (!this.skipRenderWorld) {
                        if (this.playerController != null) {
                            this.playerController.setPartialTime(this.timer.renderPartialTicks);
                        }

                        this.entityRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
                    }

                    if (this.gameSettings.pauseOnLostFocus.value && !Display.isActive() && this.currentScreen == null) {
                        this.displayInGameMenu();
                    }

                    if (this.gameSettings.showFrameTimes.value) {
                        this.displayDebugInfo(l2);
                    } else {
                        this.prevFrameTime = System.nanoTime();
                    }

                    this.guiAchievement.updateAchievementWindow();
                    if (this.shadersActive()) {
                        this.render.endRenderGame();
                    }

                    Thread.yield();
                    this.screenshotListener();
                    if (this.mcCanvas != null && !this.fullscreen && (this.mcCanvas.getWidth() != this.resolution.width || this.mcCanvas.getHeight() != this.resolution.height)) {
                        this.resize();
                    }

                    this.checkGLError("Post render");
                    ++i;

                    for(this.isGamePaused = !this.isMultiplayerWorld() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame(); System.currentTimeMillis() >= l + 1000L; i = 0) {
                        this.debug = i + " fps, " + WorldRenderer.chunksUpdated + " chunk updates";
                        this.debugFPS = i + " fps";
                        WorldRenderer.chunksUpdated = 0;
                        l += 1000L;
                    }
                } catch (MinecraftException var18) {
                    this.theWorld = null;
                    this.changeWorld1(null);
                    this.displayGuiScreen(new GuiConflictWarning());
                } catch (OutOfMemoryError var19) {
                    this.func_28002_e();
                    this.displayGuiScreen(new GuiErrorScreen());
                    System.gc();
                } catch (RuntimeException e){
                    e.printStackTrace();
                    this.changeWorld1(null);
                    this.displayGuiScreen(new GuiException(e));
                }
            }
        } catch (MinecraftError var20) {
        } catch (Throwable var21) {
            this.func_28002_e();
            var21.printStackTrace();
            this.onMinecraftCrash(new UnexpectedThrowable("Unexpected error", var21));
        } finally {
            this.shutdownMinecraftApplet();
        }

    }
}
