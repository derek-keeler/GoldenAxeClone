package infra;

import java.util.HashMap;
import java.util.Map;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 * ControllerSupport class.
 *
 * Polls gamepads and maps controller states into virtual key states consumed by
 * existing Input methods.
 */
public class ControllerSupport {

    private static final float DEADZONE_ENTER = 0.35f;
    private static final float DEADZONE_EXIT = 0.25f;

    private static final long RESCAN_INTERVAL_MS = 1500L;
    private long nextRescanTime;
    private boolean controllerPollingEnabled = true;

    private Controller player1Controller;
    private Controller player2Controller;

    private final Map<Integer, AxisState> axisStates = new HashMap<>();
    private final Map<Integer, AxisState> triggerStates = new HashMap<>();

    private static class AxisState {
        boolean negativeActive;
        boolean positiveActive;
    }

    public void update() {
        if (!controllerPollingEnabled) {
            return;
        }
        
        try {
            updateInternal();
        } catch (Throwable ex) {
            // Disable controller polling on backend/native errors to keep
            // keyboard gameplay fully functional.
            controllerPollingEnabled = false;
            Input.clearVirtualInputs();
        }
    }
    
    private void updateInternal() {
        long now = System.currentTimeMillis();
        if (now >= nextRescanTime || !isControllerActive(player1Controller)
                || !isControllerActive(player2Controller)) {

            assignControllers();
            nextRescanTime = now + RESCAN_INTERVAL_MS;
        }

        Input.clearVirtualInputs();

        if (player1Controller != null) {
            pollPlayerController(player1Controller, 1);
        }
        if (player2Controller != null) {
            pollPlayerController(player2Controller, 2);
        }
    }

    private boolean isControllerActive(Controller controller) {
        return controller != null
                && controller.getType() == Controller.Type.GAMEPAD
                && controller.poll();
    }

    private void assignControllers() {
        player1Controller = null;
        player2Controller = null;
        axisStates.clear();
        triggerStates.clear();
        Controller[] controllers = ControllerEnvironment
                .getDefaultEnvironment().getControllers();

        for (Controller controller : controllers) {
            if (controller.getType() != Controller.Type.GAMEPAD) {
                continue;
            }
            if (player1Controller == null) {
                player1Controller = controller;
            }
            else if (player2Controller == null) {
                player2Controller = controller;
                break;
            }
        }
    }

    private void pollPlayerController(Controller controller, int playerIndex) {
        if (!controller.poll()) {
            return;
        }

        int keyUp = playerIndex == 1
                ? Settings.KEY_PLAYER_1_UP : Settings.KEY_PLAYER_2_UP;
        int keyDown = playerIndex == 1
                ? Settings.KEY_PLAYER_1_DOWN : Settings.KEY_PLAYER_2_DOWN;
        int keyLeft = playerIndex == 1
                ? Settings.KEY_PLAYER_1_LEFT : Settings.KEY_PLAYER_2_LEFT;
        int keyRight = playerIndex == 1
                ? Settings.KEY_PLAYER_1_RIGHT : Settings.KEY_PLAYER_2_RIGHT;
        int keyAttack = playerIndex == 1
                ? Settings.KEY_PLAYER_1_ATTACK : Settings.KEY_PLAYER_2_ATTACK;
        int keyJump = playerIndex == 1
                ? Settings.KEY_PLAYER_1_JUMP : Settings.KEY_PLAYER_2_JUMP;
        int keyMagic = playerIndex == 1
                ? Settings.KEY_PLAYER_1_MAGIC : Settings.KEY_PLAYER_2_MAGIC;
        int keyStart = playerIndex == 1
                ? Settings.KEY_START_1 : Settings.KEY_START_2;

        for (Component component : controller.getComponents()) {
            Component.Identifier id = component.getIdentifier();
            float value = component.getPollData();

            // D-Pad
            if (id == Component.Identifier.Axis.POV) {
                boolean up = value == Component.POV.UP
                        || value == Component.POV.UP_LEFT
                        || value == Component.POV.UP_RIGHT;
                boolean down = value == Component.POV.DOWN
                        || value == Component.POV.DOWN_LEFT
                        || value == Component.POV.DOWN_RIGHT;
                boolean left = value == Component.POV.LEFT
                        || value == Component.POV.UP_LEFT
                        || value == Component.POV.DOWN_LEFT;
                boolean right = value == Component.POV.RIGHT
                        || value == Component.POV.UP_RIGHT
                        || value == Component.POV.DOWN_RIGHT;
                Input.setVirtualKeyState(keyUp, up);
                Input.setVirtualKeyState(keyDown, down);
                Input.setVirtualKeyState(keyLeft, left);
                Input.setVirtualKeyState(keyRight, right);
                continue;
            }

            // Left stick mapped to movement
            if (id == Component.Identifier.Axis.X) {
                updateAxisDirection(playerIndex * 10 + 1, value, keyLeft,
                        keyRight);
                continue;
            }
            if (id == Component.Identifier.Axis.Y) {
                updateAxisDirection(playerIndex * 10 + 2, value, keyUp,
                        keyDown);
                continue;
            }

            // Triggers map to magic (LT) and jump (RT)
            if (id == Component.Identifier.Axis.Z
                    || id == Component.Identifier.Axis.RZ) {

                updateTriggerDirection(playerIndex * 20 + id.getName().hashCode()
                        , value, keyMagic, keyJump);
                continue;
            }

            // Face/Start buttons
            if (id == Component.Identifier.Button._0) { // A
                Input.setVirtualKeyState(keyJump, value > 0.5f);
            }
            else if (id == Component.Identifier.Button._1) { // B
                Input.setVirtualKeyState(keyAttack, value > 0.5f);
            }
            else if (id == Component.Identifier.Button._2) { // X
                Input.setVirtualKeyState(keyMagic, value > 0.5f);
            }
            else if (id == Component.Identifier.Button._7) { // Start
                Input.setVirtualKeyState(keyStart, value > 0.5f);
            }
        }
    }

    private void updateAxisDirection(int id, float value, int negativeKey
            , int positiveKey) {

        AxisState state = axisStates.computeIfAbsent(id, k -> new AxisState());
        boolean shouldNegative = state.negativeActive
                ? value < -DEADZONE_EXIT : value < -DEADZONE_ENTER;
        boolean shouldPositive = state.positiveActive
                ? value > DEADZONE_EXIT : value > DEADZONE_ENTER;

        state.negativeActive = shouldNegative && !shouldPositive;
        state.positiveActive = shouldPositive && !shouldNegative;

        Input.setVirtualKeyState(negativeKey, state.negativeActive);
        Input.setVirtualKeyState(positiveKey, state.positiveActive);
    }

    private void updateTriggerDirection(int id, float value, int leftKey
            , int rightKey) {

        AxisState state = triggerStates.computeIfAbsent(id
                , k -> new AxisState());
        boolean shouldLeft = state.negativeActive
                ? value < -DEADZONE_EXIT : value < -DEADZONE_ENTER;
        boolean shouldRight = state.positiveActive
                ? value > DEADZONE_EXIT : value > DEADZONE_ENTER;

        state.negativeActive = shouldLeft && !shouldRight;
        state.positiveActive = shouldRight && !shouldLeft;

        Input.setVirtualKeyState(leftKey, state.negativeActive);
        Input.setVirtualKeyState(rightKey, state.positiveActive);
    }

}
