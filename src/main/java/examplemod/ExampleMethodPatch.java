package examplemod;

import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.WallObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import net.bytebuddy.asm.Advice;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static necesse.level.gameObject.WallObject.getAdvancedLight;

/**
 * The game uses Byte Buddy to allow mods to make patches inside the source code.
 * The class you assign as a mod patch acts as the advice class in Byte Buddy.
 * Use @ModMethodPatch to define which class and method to apply the patch to.
 * Define an optional priority if you want some patch to happen before another (higher priority means happens first)
 *
 * You can read the official documentation here:
 * <a href="https://javadoc.io/doc/net.bytebuddy/byte-buddy/1.12.8/net/bytebuddy/asm/Advice.html">...</a>
 *
 * I have written a brief overview of some of the most important annotations:
 *
 * "@Advice.OnMethodEnter" - The annotated method happens when we enter the method, before the methods
 *      content is run.
 *      Combine this with onSkip = Advice.OnNonDefaultValue.class and return true to skip the original
 *      method's execution. Make sure the annotated method returns something for this to work. See the example below.
 *      If you do not plan to ever skip the original method, the annotated OnMethodEnter method
 *      does not have to return anything (it can be a void method).
 *
 * "@Advice.OnMethodExit" - The annotate method happens when we exit the method, after the method has
 *      returned what it should.
 *      This returned object can be overridden using the "@Advice.Return(readOnly = false)" for a parameter
 *
 * "@Advice.This" - The annotated parameter is mapped to the object running, similar to "this" in java.
 *
 * "@Advice.FieldValue" - The annotated parameter is mapped to a field in the scope of the method.
 *      This could for example be a private field, or something similar.
 *      Setting "readOnly = false" on this indicates being able to write a value to the field.
 *
 * "@Advice.AllArguments" - The annotated parameter is assigned all arguments passed into the target method.
 *      This annotated parameter must be an array type
 *
 * "@Advice.Argument(n)" - The annotated parameter is mapped to the n argument passed into the target method.
 */

@ModMethodPatch(target = WallObject.class, name = "addWallDrawOptions", arguments = {SharedTextureDrawOptions.class, Level.class, int.class, int.class, GameLight.class, TickManager.class, GameCamera.class, PlayerMob.class})
public class ExampleMethodPatch {
    @Advice.OnMethodEnter(
            skipOn = Advice.OnNonDefaultValue.class
    )
    static boolean onEnter() {
        return true;
    }
    @Advice.OnMethodExit
    static void onExit(@Advice.This WallObject target,
                                  @Advice.Argument(0) SharedTextureDrawOptions options,
                                  @Advice.Argument(1) Level level,
                                  @Advice.Argument(2) int tileX,
                                  @Advice.Argument(3) int tileY,
                                  @Advice.Argument(4) GameLight lightOverride,
                                  @Advice.Argument(5) TickManager tickManager,
                                  @Advice.Argument(6) GameCamera camera,
                                  @Advice.Argument(7) PlayerMob perspective) {
        GameLog.out.println("Server Option: :");
        Performance.record(tickManager, "wallSetup", () -> {
            System.out.println("ExampleMethodPatch: onEnter");
            int drawX = camera.getTileDrawX(tileX);
            int drawY = camera.getTileDrawY(tileY);
            GameObject[] adj = level.getAdjacentObjects(tileX, tileY);
            boolean allIsSameWall = true;
            boolean[] sameWall = new boolean[adj.length];
            boolean forceDrawTop = false;
            boolean forceRemoveBot = false;

            for (int i = 0; i < adj.length; ++i) {
                GameObject adjObject = adj[i];
                // maybe should patch isConnectedWall ?
                boolean connectedWall = true; // boolean connectedWall = this.isConnectedWall(adjObject);
                sameWall[i] = connectedWall;
                // allIsSameWall = allIsSameWall && connectedWall;
                if (connectedWall) {
                    if (i == 1) {
                        if (adjObject instanceof WallObject && ((WallObject) adjObject).isWallDrawingTop()) {
                            forceDrawTop = true;
                        }
                    } else if (i == 6 && adjObject instanceof WallObject && ((WallObject) adjObject).isWallDrawingTop()) {
                        forceRemoveBot = true;
                    }
                }
            }

            float alpha = 1.0F;
            if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
                Rectangle alphaRec = new Rectangle(tileX * 32 - 16, tileY * 32 - 32, 64, 48);
                if (perspective.getCollision().intersects(alphaRec)) {
                    alpha = 0.5F;
                } else if (alphaRec.contains(camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY)) {
                    alpha = 0.5F;
                }
            }

            GameLight[] lights;
            if (lightOverride == null) {
                Point[] var10003 = Level.adjacentGettersWithCenter;
                Objects.requireNonNull(level);
                lights = (GameLight[]) level.getRelative(tileX, tileY, var10003, level::getLightLevelWall, GameLight[]::new);
            } else {
                lights = new GameLight[9];
                Arrays.fill(lights, lightOverride);
            }

            // Trying to allow for calling overloaded methods
            target.addWallDrawOptions((SharedTextureDrawOptions)options, (GameTextureSection)target.wallTexture, (int)drawX, (int)drawY, (GameLight[])lights, (float)alpha, (boolean[])sameWall, (boolean)allIsSameWall, (boolean)forceRemoveBot, (boolean)forceDrawTop);
            // this.addWallDrawOptions(options, this.wallTexture, drawX, drawY, lights, alpha, sameWall, allIsSameWall, forceDrawTop, forceRemoveBot);
        });
    }
}