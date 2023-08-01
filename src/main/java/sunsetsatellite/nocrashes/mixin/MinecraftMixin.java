package sunsetsatellite.nocrashes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.EntityPlayerSP;
import net.minecraft.client.gui.GuiAchievement;
import net.minecraft.client.gui.GuiConflictWarning;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.input.controller.ControllerInput;
import net.minecraft.client.option.GameSettings;
import net.minecraft.client.player.controller.PlayerController;
import net.minecraft.client.render.*;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.core.MinecraftError;
import net.minecraft.core.MinecraftException;
import net.minecraft.core.Timer;
import net.minecraft.core.UnexpectedThrowable;
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

@Mixin(
        value = Minecraft.class,
        remap = false
)
public abstract class MinecraftMixin {
    @Shadow public PlayerController playerController;
    @Shadow private Timer timer;
    @Shadow public World theWorld;
    @Shadow public EntityPlayerSP thePlayer;
    @Shadow public volatile boolean isGamePaused;
    @Shadow public GuiScreen currentScreen;
    @Shadow public WorldRenderer worldRenderer;
    @Shadow private int ticksRan;
    @Shadow public GuiAchievement guiAchievement;
    @Shadow public boolean skipRenderWorld;
    @Shadow public GameSettings gameSettings;
    @Shadow public SoundManager sndManager;
    @Shadow public volatile boolean running;
    @Shadow public String debug;
    @Shadow public String debugFPS;
    @Shadow long prevFrameTime;
    @Shadow public Renderer render;
    @Shadow public ControllerInput controllerInput;
    @Shadow @Final public MouseInput mouseInput;
    @Shadow private long lastFocusTime;

    @Shadow public abstract void onMinecraftCrash(UnexpectedThrowable unexpectedthrowable);

    @Shadow public abstract void startGame() throws LWJGLException;

    @Shadow public abstract void displayGuiScreen(GuiScreen guiscreen);

    @Shadow public abstract void shutdownMinecraftApplet();

    @Shadow public abstract void freeUpMemory();

    @Shadow protected abstract void screenshotListener();

    @Shadow protected abstract void drawFrameTimeGraph(long frameTime);

    @Shadow public abstract void shutdown();

    @Shadow public abstract void displayInGameMenu();

    @Shadow public abstract void resize();

    @Shadow public abstract void runTick();

    @Shadow public abstract boolean isMultiplayerWorld();

    @Shadow public abstract void changeWorld1(World world);

    /**
     * @author MartinSVK12
     * @reason Crash handler
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
                    AABB.initializePool();
                    Vec3d.initializePool();
                    if (Display.isCloseRequested()) {
                        this.shutdown();
                    }

                    if (this.isGamePaused && this.theWorld != null) {
                        float temp = this.timer.alpha;
                        this.timer.advanceTime();
                        this.timer.alpha = temp;
                    } else {
                        this.timer.advanceTime();
                    }

                    long nanoTime = System.nanoTime();
                    this.mouseInput.update();
                    if (this.controllerInput != null && this.currentScreen != null) {
                        this.controllerInput.moveCursor();
                    }

                    for (int tick = 0; tick < this.timer.frames; ++tick) {
                        ++this.ticksRan;

                        try {
                            this.runTick();
                        } catch (MinecraftException var16) {
                            this.theWorld = null;
                            this.changeWorld1(null);
                            this.displayGuiScreen(new GuiConflictWarning());
                        }
                    }

                    long frameTime = System.nanoTime() - nanoTime;
                    OpenGLHelper.checkError("pre render");
                    RenderBlocks.fancyGrass = this.gameSettings.fancyGraphics.value == 1;
                    this.sndManager.updateListenerPosition(this.thePlayer, this.timer.alpha);
                    GL11.glEnable(3553);
                    if (this.theWorld != null) {
                        this.theWorld.updatingLighting();
                    }

                    Display.update();
                    if (this.thePlayer != null && this.thePlayer.isInWall()) {
                        this.gameSettings.thirdPersonView.value = 0;
                    }

                    this.render.beginRenderGame();
                    GL11.glEnable(3008);
                    if (!this.skipRenderWorld) {
                        if (this.playerController != null) {
                            this.playerController.setPartialTime(this.timer.alpha);
                        }

                        this.worldRenderer.updateCameraAndRender(this.timer.alpha);
                    }

                    if (this.gameSettings.pauseOnLostFocus.value) {
                        if (!Display.isActive()) {
                            if (System.currentTimeMillis() > this.lastFocusTime + 250L) {
                                this.displayInGameMenu();
                            }
                        } else {
                            this.lastFocusTime = System.currentTimeMillis();
                        }
                    }

                    this.sndManager.setMuted(this.gameSettings.muteOnLostFocus.value && !Display.isActive());
                    if (this.gameSettings.showFrameTimes.value) {
                        this.drawFrameTimeGraph(frameTime);
                    } else {
                        this.prevFrameTime = System.nanoTime();
                    }

                    this.guiAchievement.updateAchievementWindow();
                    this.render.endRenderGame();
                    Thread.yield();
                    this.screenshotListener();
                    if (Display.wasResized()) {
                        this.resize();
                    }

                    OpenGLHelper.checkError("post render");
                    ++fpsCounter;

                    for (this.isGamePaused = !this.isMultiplayerWorld() && this.currentScreen != null && this.currentScreen.doesGuiPauseGame();
                         System.currentTimeMillis() >= currentTime + 1000L;
                         fpsCounter = 0
                    ) {
                        this.debug = fpsCounter + " fps, " + ChunkRenderer.chunksUpdated + " chunk updates";
                        this.debugFPS = fpsCounter + " fps";
                        ChunkRenderer.chunksUpdated = 0;
                        currentTime += 1000L;
                    }
                } catch (MinecraftException var18) {
                    this.theWorld = null;
                    this.changeWorld1(null);
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