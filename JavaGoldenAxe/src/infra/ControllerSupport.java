package infra;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
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

    private static final float DEADZONE_ENTER = 0.45f;
    private static final float DEADZONE_EXIT = 0.35f;
    private static final long DIRECTION_RELEASE_DELAY_MS = 90L;
    private static final long DIRECTION_REPRESS_DELAY_MS = 140L;

    private static final long RESCAN_INTERVAL_MS = 1500L;
    private long nextRescanTime;
    private boolean controllerPollingEnabled = true;

    private Controller player1Controller;
    private Controller player2Controller;

    private final Map<Integer, AxisState> axisStates = new HashMap<>();
    private final Map<Integer, AxisState> triggerStates = new HashMap<>();
    private final Set<Integer> activeDirectionalKeys = new HashSet<>();
    private final Map<Integer, Long> lastDirectionalPressTime = new HashMap<>();
    private final Map<Integer, Long> directionalNeutralSinceTime = new HashMap<>();

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
        
        boolean up = false;
        boolean down = false;
        boolean left = false;
        boolean right = false;
        boolean attack = false;
        boolean jump = false;
        boolean magic = false;
        boolean start = false;

        for (Component component : controller.getComponents()) {
            Component.Identifier id = component.getIdentifier();
            float value = component.getPollData();

            // D-Pad
            if (id == Component.Identifier.Axis.POV) {
                boolean povUp = value == Component.POV.UP
                        || value == Component.POV.UP_LEFT
                        || value == Component.POV.UP_RIGHT;
                boolean povDown = value == Component.POV.DOWN
                        || value == Component.POV.DOWN_LEFT
                        || value == Component.POV.DOWN_RIGHT;
                boolean povLeft = value == Component.POV.LEFT
                        || value == Component.POV.UP_LEFT
                        || value == Component.POV.DOWN_LEFT;
                boolean povRight = value == Component.POV.RIGHT
                        || value == Component.POV.UP_RIGHT
                        || value == Component.POV.DOWN_RIGHT;
                if (povUp) {
                    up = true;
                }
                if (povDown) {
                    down = true;
                }
                if (povLeft) {
                    left = true;
                }
                if (povRight) {
                    right = true;
                }
                continue;
            }

            // Left stick mapped to movement
            if (id == Component.Identifier.Axis.X) {
                AxisState state = updateAxisState(axisStates
                        , playerIndex * 10 + 1, value);
                left = left || state.negativeActive;
                right = right || state.positiveActive;
                continue;
            }
            if (id == Component.Identifier.Axis.Y) {
                AxisState state = updateAxisState(axisStates
                        , playerIndex * 10 + 2, value);
                up = up || state.negativeActive;
                down = down || state.positiveActive;
                continue;
            }

            // Triggers map to magic (LT) and jump (RT)
            if (id == Component.Identifier.Axis.Z
                    || id == Component.Identifier.Axis.RZ) {

                AxisState state = updateAxisState(triggerStates
                        , playerIndex * 20 + id.getName().hashCode(), value);
                magic = magic || state.negativeActive;
                jump = jump || state.positiveActive;
                continue;
            }

            // Face/Start buttons
            if (id == Component.Identifier.Button._0) { // A
                jump = jump || value > 0.5f;
            }
            else if (id == Component.Identifier.Button._1) { // B
                attack = attack || value > 0.5f;
            }
            else if (id == Component.Identifier.Button._2) { // X
                magic = magic || value > 0.5f;
            }
            else if (id == Component.Identifier.Button._7) { // Start
                start = start || value > 0.5f;
            }
        }
        
        applyDirectionalKey(keyUp, up);
        applyDirectionalKey(keyDown, down);
        applyDirectionalKey(keyLeft, left);
        applyDirectionalKey(keyRight, right);
        
        Input.setVirtualKeyState(keyAttack, attack);
        Input.setVirtualKeyState(keyJump, jump);
        Input.setVirtualKeyState(keyMagic, magic);
        Input.setVirtualKeyState(keyStart, start);
    }

    private AxisState updateAxisState(Map<Integer, AxisState> axisMap
            , int id, float value) {
        AxisState state = axisMap.computeIfAbsent(id, k -> new AxisState());
        boolean shouldNegative = state.negativeActive
                ? value < -DEADZONE_EXIT : value < -DEADZONE_ENTER;
        boolean shouldPositive = state.positiveActive
                ? value > DEADZONE_EXIT : value > DEADZONE_ENTER;

        state.negativeActive = shouldNegative && !shouldPositive;
        state.positiveActive = shouldPositive && !shouldNegative;
        return state;
    }

    private void applyDirectionalKey(int keyCode, boolean desired) {
        long now = System.currentTimeMillis();
        if (desired) {
            directionalNeutralSinceTime.remove(keyCode);
            if (!activeDirectionalKeys.contains(keyCode)) {
                long lastPress = lastDirectionalPressTime
                        .getOrDefault(keyCode, 0L);
                if (now - lastPress >= DIRECTION_REPRESS_DELAY_MS) {
                    activeDirectionalKeys.add(keyCode);
                    lastDirectionalPressTime.put(keyCode, now);
                    Input.setVirtualKeyState(keyCode, true);
                }
            }
            else {
                Input.setVirtualKeyState(keyCode, true);
            }
            return;
        }

        if (!activeDirectionalKeys.contains(keyCode)) {
            return;
        }
        
        long neutralSince = directionalNeutralSinceTime.computeIfAbsent(keyCode
                , k -> now);
        if (now - neutralSince >= DIRECTION_RELEASE_DELAY_MS) {
            activeDirectionalKeys.remove(keyCode);
            directionalNeutralSinceTime.remove(keyCode);
            Input.setVirtualKeyState(keyCode, false);
        }
        else {
            Input.setVirtualKeyState(keyCode, true);
        }
    }

}
