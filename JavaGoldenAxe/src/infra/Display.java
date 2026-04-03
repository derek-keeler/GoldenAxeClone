package infra;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

/**
 * Display class.
 * 
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class Display extends JFrame {
    
    private final GameCanvas gameCanvas;
    private final GraphicsEnvironment ge;
    private final GraphicsDevice gd;
    private DisplayMode fullscreenDisplayMode;
    
    public Display(GameCanvas gameCanvas) {
        gameCanvas.setBackground(Color.BLACK);
        this.gameCanvas = gameCanvas;
        ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gd = ge.getDefaultScreenDevice();
        fullscreenDisplayMode = gd.getDisplayMode();
    }

    public DisplayMode getFullscreenDisplayMode() {
        return fullscreenDisplayMode;
    }

    public void setFullscreenDisplayMode(DisplayMode fullscreenDisplayMode) {
        this.fullscreenDisplayMode = fullscreenDisplayMode;
    }
    
    public void start() {
        getContentPane().add(gameCanvas);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        gameCanvas.start();
        gameCanvas.requestFocus();
        Input.addListener(new KeyHandler());
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                Input.clearVirtualInputs();
            }
        });
    }
    
    public void setWindowedMode() {
        dispose();
        setUndecorated(false);
        gd.setFullScreenWindow(null);
        setVisible(true);
        gameCanvas.updateAspectRatioDimension();
        gameCanvas.requestFocus();
        showMouseCursor();
    }
    
    public void setFullscreenMode() {
        dispose();
        setUndecorated(true);
        setVisible(true);
        //System.out.println("fullscreen supported: " 
        //                          + gd.isFullScreenSupported());
        gd.setFullScreenWindow(this);
        gd.setDisplayMode(fullscreenDisplayMode);
        gameCanvas.updateAspectRatioDimension();
        gameCanvas.requestFocus();
        hideMouseCursor();
    }

//    private void listSupportedDisplayModes() {
//        System.out.println("display change supported: " 
//                                        + gd.isDisplayChangeSupported());
//
//        int i = 0;
//        for (DisplayMode dm : gd.getDisplayModes()) {
//            System.out.println(i++ + "Diplay mode: " 
//                    + dm.getWidth() + ", " + dm.getHeight());
//        }
//        //gd.setDisplayMode(gd.getDisplayModes()[9]);
//    }
    
    private class KeyHandler extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            // keep aspect ratio
            if (e.getKeyCode() == Settings.KEY_KEEP_ASPECT_RATIO) {
                Settings.keepAspectRatio = !Settings.keepAspectRatio;
            }
            // full screen
            if (e.getKeyCode() == Settings.KEY_FULLSCREEN) {
                if (gd.getFullScreenWindow() == null) {
                    setFullscreenMode();
                }
                else {
                    setWindowedMode();
                }
            }
        }
        
    }
    
    public void hideMouseCursor() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        setCursor(toolkit.createCustomCursor(
                    new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB)
                    , new Point(0, 0), "null"));        
    }

    public void showMouseCursor() {
        setCursor(Cursor.getDefaultCursor());
    }
    
}
