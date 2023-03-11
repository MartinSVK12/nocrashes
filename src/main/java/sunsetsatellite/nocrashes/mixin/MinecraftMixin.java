package sunsetsatellite.nocrashes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MinecraftApplet;
import net.minecraft.client.render.camera.ICamera;
import net.minecraft.core.*;
import net.minecraft.core.achievement.stat.StatFileWriter;
import net.minecraft.core.entity.EntityLiving;
import net.minecraft.core.entity.player.EntityPlayer;
import net.minecraft.core.entity.player.EntityPlayerSP;
import net.minecraft.core.enums.EnumOS2;
import net.minecraft.core.gui.*;
import net.minecraft.core.input.InputType;
import net.minecraft.core.input.MouseInput;
import net.minecraft.core.input.controller.ControllerInput;
import net.minecraft.core.net.command.ClientCommandHandler;
import net.minecraft.core.net.handler.NetClientHandler;
import net.minecraft.core.net.thread.ThreadDownloadResources;
import net.minecraft.core.option.GameSettings;
import net.minecraft.core.player.Session;
import net.minecraft.core.player.controller.PlayerController;
import net.minecraft.core.render.*;
import net.minecraft.core.sound.SoundManager;
import net.minecraft.core.util.helper.Time;
import net.minecraft.core.util.phys.AABB;
import net.minecraft.core.util.phys.Vec3d;
import net.minecraft.core.world.World;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sunsetsatellite.nocrashes.GuiException;

import java.awt.*;

@Mixin(
        value = Minecraft.class,
        remap = false
)
public abstract class MinecraftMixin {

    @Shadow public volatile boolean running;

    @Shadow public abstract void startGame() throws LWJGLException;

    @Shadow public abstract void onMinecraftCrash(UnexpectedThrowable unexpectedthrowable);

    @Shadow protected MinecraftApplet mcApplet;

    @Shadow public Canvas mcCanvas;

    @Shadow public abstract void shutdown();

    @Shadow public volatile boolean isGamePaused;

    @Shadow public World theWorld;

    @Shadow private Timer timer;

    @Shadow @Final public MouseInput mouseInput;

    @Shadow public ControllerInput controllerInput;

    @Shadow private int ticksRan;

    @Shadow public abstract void runTick();

    @Shadow public abstract void changeWorld1(World world);

    @Shadow public abstract void displayGuiScreen(GuiScreen guiscreen);

    @Shadow public PlayerController playerController;
    @Shadow private boolean fullscreen;
    @Shadow public EntityPlayerSP thePlayer;
    @Shadow public GuiScreen currentScreen;
    @Shadow public WorldRenderer worldRenderer;
    @Shadow public GuiAchievement guiAchievement;
    @Shadow public boolean skipRenderWorld;
    @Shadow public GameSettings gameSettings;
    @Shadow public SoundManager sndManager;
    @Shadow public String debug;
    @Shadow public String debugFPS;
    @Shadow long prevFrameTime;
    @Shadow @Final public GameResolution resolution;
    @Shadow public IRenderer render;

    @Shadow protected abstract void checkGLError(String stageName);

    @Shadow public abstract void shutdownMinecraftApplet();

    @Shadow public abstract void freeUpMemory();

    @Shadow protected abstract void screenshotListener();

    @Shadow protected abstract void drawFrameTimeGraph(long frameTime);

    @Shadow public abstract boolean shadersActive();

    @Shadow public abstract void displayInGameMenu();

    @Shadow protected abstract void resize();

    @Shadow public abstract boolean isMultiplayerWorld();


    /**
     * @author MartinSVK12
     * @reason Better crash handler.
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
            long currentTime = System.currentTimeMillis();
            int fpsCounter = 0;

            while(this.running) {
                try {
                    Time.tick();
                    //noinspection removal
                    if (this.mcApplet != null && !this.mcApplet.isActive()) {
                        break;
                    }

                    AABB.initializePool();
                    Vec3d.initializePool();
                    if (this.mcCanvas == null && Display.isCloseRequested()) {
                        this.shutdown();
                    }

                    if (this.isGamePaused && this.theWorld != null) {
                        float temp = this.timer.renderPartialTicks;
                        this.timer.updateTimer();
                        this.timer.renderPartialTicks = temp;
                    } else {
                        this.timer.updateTimer();
                    }

                    long nanoTime = System.nanoTime();
                    this.mouseInput.update();
                    if (this.controllerInput != null && this.currentScreen != null) {
                        this.controllerInput.moveCursor();
                    }

                    for(int tick = 0; tick < this.timer.elapsedTicks; ++tick) {
                        ++this.ticksRan;

                        try {
                            this.runTick();
                        } catch (MinecraftException var16) {
                            this.theWorld = null;
                            this.changeWorld1((World)null);
                            this.displayGuiScreen(new GuiConflictWarning());
                        }
                    }

                    long frameTime = System.nanoTime() - nanoTime;
                    this.checkGLError("Pre render");
                    RenderBlocks.fancyGrass = (Integer)this.gameSettings.fancyGraphics.value == 1;
                    this.sndManager.updateListenerPosition(this.thePlayer, this.timer.renderPartialTicks);
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

                        this.worldRenderer.updateCameraAndRender(this.timer.renderPartialTicks);
                    }

                    if ((Boolean)this.gameSettings.pauseOnLostFocus.value && !Display.isActive() && this.currentScreen == null) {
                        this.displayInGameMenu();
                    }

                    this.sndManager.setMuted((Boolean)this.gameSettings.muteOnLostFocus.value && !Display.isActive());
                    if ((Boolean)this.gameSettings.showFrameTimes.value) {
                        this.drawFrameTimeGraph(frameTime);
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
                    ++fpsCounter;

                    for(this.isGamePaused = !this.isMultiplayerWorld() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame(); System.currentTimeMillis() >= currentTime + 1000L; fpsCounter = 0) {
                        this.debug = fpsCounter + " fps, " + ChunkRenderer.chunksUpdated + " chunk updates";
                        this.debugFPS = fpsCounter + " fps";
                        ChunkRenderer.chunksUpdated = 0;
                        currentTime += 1000L;
                    }
                } catch (MinecraftException var18) {
                    this.theWorld = null;
                    this.changeWorld1((World)null);
                    this.displayGuiScreen(new GuiConflictWarning());
                } catch (OutOfMemoryError var19) {
                    this.freeUpMemory();
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
            this.freeUpMemory();
            var21.printStackTrace();
            this.onMinecraftCrash(new UnexpectedThrowable("Unexpected error", var21));
        } finally {
            this.shutdownMinecraftApplet();
        }

    }
}

/*

 */