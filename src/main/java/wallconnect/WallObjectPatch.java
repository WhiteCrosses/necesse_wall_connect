package wallconnect;

import necesse.engine.GameLog;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.WallObject;
import net.bytebuddy.asm.Advice;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class WallObjectPatch {
    // Shared list of all wall object IDs
    public static List<Integer> wallObjIds = new ArrayList<>();

    @ModMethodPatch(target = WallObject.class, name = "registerWallObjects", arguments = {
            String.class,
            String.class,
            String.class,
            float.class,
            Color.class,
            ToolType.class,
            float.class,
            float.class,
            boolean.class,
            boolean.class
    })
    public static class RegisterWallObjectsPatch {
        @Advice.OnMethodExit
        public static void onExit(@Advice.Return int[] objIds) {
            // Add the newly registered wall object to the shared list
            wallObjIds.add(objIds[0]);
        }
    }

    @ModMethodPatch(target = WallObject.class, name = "onObjectRegistryClosed", arguments = {})
    public static class OnObjectRegistryClosedPatch {
        @Advice.OnMethodExit
        public static void onExit(@Advice.This WallObject target) {
            // Allow all wall objects in the shared list to connect
            target.connectedWalls.addAll(wallObjIds);
        }
    }

    @ModMethodPatch(target = WallObject.class, name = "isConnectedWall", arguments = {GameObject.class})
    public static class IsConnectedWallPatch {
        @Advice.OnMethodEnter(
                skipOn = Advice.OnNonDefaultValue.class
        )
        static boolean onEnter() {
            return true;
        }
        @Advice.OnMethodExit
        static void onExit(@Advice.This WallObject target,
                           @Advice.Argument(0) GameObject object,
                           @Advice.Return(readOnly = false) boolean returnVal){
            returnVal = object == target || target.connectedWalls.contains(object.getID());

            //returnVal = true;
        }
    }

}