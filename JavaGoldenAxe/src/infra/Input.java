package infra;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Input class.
 * 
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class Input implements KeyListener {
    
    private static final Set<Integer> KEYS_PRESSED = new HashSet<>();
    private static final Set<Integer> KEYS_PRESSED_CONSUMED = new HashSet<>();
    private static final Set<Integer> VIRTUAL_KEYS_PRESSED = new HashSet<>();
    private static final List<KeyListener> LISTENERS = new ArrayList<>();
    
    private static final long DOUBLE_PRESSED_TIME = 250;
    private static final Map<Integer, long[]> KEYS_DOUBLE_PRESSED 
                                                            = new HashMap<>();
    private static final ControllerSupport CONTROLLER_SUPPORT
            = new ControllerSupport();

    public static void addListener(KeyListener listener) {
        Input.LISTENERS.add(listener);
    }

    public static synchronized boolean isKeyPressed(int keyCode) {
        return KEYS_PRESSED.contains(keyCode);
    }

    public static synchronized boolean isKeyDoublePressed(int keyCode) {
        boolean result = false;
        long[] lastTimes = KEYS_DOUBLE_PRESSED.get(keyCode);
        if (lastTimes != null) {
            if (lastTimes[0] > 0 && lastTimes[1] > 0) {
                result = Util.getTime() - lastTimes[1] <= DOUBLE_PRESSED_TIME
                            && KEYS_PRESSED.contains(keyCode);

                if (result) {
                    lastTimes[0] = -1;
                    lastTimes[1] = -1;
                }
            }
            else {
                result = false;
            }
        }
        return result;
    }

    public static synchronized boolean isKeyJustPressed(int keyCode) {
        if (!KEYS_PRESSED_CONSUMED.contains(keyCode) 
                && KEYS_PRESSED.contains(keyCode)) {
            
            KEYS_PRESSED_CONSUMED.add(keyCode);
            return true;
        }
        return false;
    }

    public static synchronized void pollControllers() {
        CONTROLLER_SUPPORT.update();
    }

    public static synchronized void setVirtualKeyState(int keyCode
            , boolean pressed) {

        if (pressed) {
            if (!VIRTUAL_KEYS_PRESSED.contains(keyCode)) {
                VIRTUAL_KEYS_PRESSED.add(keyCode);
                pressKeyInternal(keyCode);
            }
        }
        else if (VIRTUAL_KEYS_PRESSED.contains(keyCode)) {
            VIRTUAL_KEYS_PRESSED.remove(keyCode);
            releaseKeyInternal(keyCode);
        }
    }

    public static synchronized void clearVirtualInputs() {
        // copy keys to avoid concurrent modification while releasing
        List<Integer> activeKeys = new ArrayList<>(VIRTUAL_KEYS_PRESSED);
        activeKeys.forEach(keyCode -> setVirtualKeyState(keyCode, false));
    }

    private static void pressKeyInternal(int keyCode) {
        if (!KEYS_PRESSED.contains(keyCode)) {
            long[] lastTimes = KEYS_DOUBLE_PRESSED.get(keyCode);
            if (lastTimes == null) {
                lastTimes = new long[2];
                KEYS_DOUBLE_PRESSED.put(keyCode, lastTimes);
            }
            if (lastTimes[0] < 0
                    || Util.getTime() - lastTimes[0] > DOUBLE_PRESSED_TIME) {

                lastTimes[0] = Util.getTime();
                lastTimes[1] = -1;
            }
        }
        KEYS_PRESSED.add(keyCode);
    }

    private static void releaseKeyInternal(int keyCode) {
        KEYS_PRESSED.remove(keyCode);
        long[] lastTimes = KEYS_DOUBLE_PRESSED.get(keyCode);
        if (lastTimes != null) {
            if (Util.getTime() - lastTimes[0] <= DOUBLE_PRESSED_TIME) {
                lastTimes[1] = Util.getTime();
            }
            else {
                lastTimes[0] = -1;
                lastTimes[1] = -1;
            }
        }
        KEYS_PRESSED_CONSUMED.remove(keyCode);
    }
    
    @Override
    public synchronized void keyTyped(KeyEvent e) {
        if (LISTENERS != null) {
            LISTENERS.forEach(action -> action.keyTyped(e));
        }
    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        pressKeyInternal(e.getKeyCode());
        if (LISTENERS != null) {
            LISTENERS.forEach(action -> action.keyPressed(e));
        }
    }
    
    @Override
    public synchronized void keyReleased(KeyEvent e) {
        releaseKeyInternal(e.getKeyCode());
        if (LISTENERS != null) {
            LISTENERS.forEach(action -> action.keyReleased(e));
        }
    }
    
}
