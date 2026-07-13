import java.awt.*;
import java.awt.event.KeyEvent;
import java.security.Key;

/**
 * <p>Class for managing the keyboard inputs during the gameplay.</p>
 * This is a utility class that handles keyboard input during gameplay of levels.
 *
 * @author Leon Li
 * @version 1.0
 */
public abstract class KeyboardManager {

    /**
     * Record for holding a keyboard shortcut.
     *
     * @param commandKey The command key that needs to be held down.
     * @param key        The key that needs to be pressed.
     * @param action     The Runnable to be run when the user invokes the keyboard shortcut.
     */
    public record KeyboardShortcut(CommandKey commandKey, char key, Runnable action) {

        /**
         * Enum to represent the possible command keys.
         */
        public static enum CommandKey {
            CTRL, SHIFT, ALT;

            /**
             * Determines if the command key is held.
             *
             * @param keyEvent The key event.
             * @return True if the key is held, false otherwise.
             */
            private boolean commandKeyHeld(KeyEvent keyEvent) {
                switch (this) {
                    case CTRL -> {
                        return keyEvent.isControlDown();
                    }
                    case SHIFT -> {
                        return keyEvent.isShiftDown();
                    }
                    case ALT -> {
                        return keyEvent.isAltDown();
                    }
                    default -> {
                        return false;
                    }
                }
            }
        }


    }

    /// Array of all the available keyboard shortcuts.
    private static final KeyboardShortcut[] KEYBOARD_SHORTCUTS = { // Add/modify this as needed.
            new KeyboardShortcut(KeyboardShortcut.CommandKey.CTRL, 'b', () -> KeyboardManager.maybeUsePowerup(PowerUp.FREEZE)), new KeyboardShortcut(KeyboardShortcut.CommandKey.CTRL, 'n', () -> KeyboardManager.maybeUsePowerup(PowerUp.SCORE)), new KeyboardShortcut(KeyboardShortcut.CommandKey.CTRL, 'm', () -> KeyboardManager.maybeUsePowerup(PowerUp.HEALTH))


    };


    /// The Frame object that is currently being used by the program. Initialized in {@link KeyboardManager#setup(Frame)}.
    private static Frame frame;

    /**
     * Sets up the keyboard manager functionality.
     *
     * @param f The Frame object that is currently being used by the program.
     * @apiNote This must be invoked for keyboard functionality in this class to work.
     * @implNote This method assumes that all words are comprised of capital or lowercase letters.
     */
    public static void setup(Frame f) {
        frame = f;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getKeyCode() == 32) {
                return true;
            } // Consume event so that the spacebar doesn't close the program.

            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                frame.submitFormShortcut();
                return false;
            }

            if (!frame.isInGameplay()) { // Implement these in Frame.java. please.
                return false;
            }

            if (e.getID() == KeyEvent.KEY_TYPED) {

                char c = e.getKeyChar();
                if (Character.isLetter(c)) {
                    GameLevel l = frame.getGameLevel();
                    if (l != null) {
                        l.processInput(c);
                    }
                }


            }


            if (e.getID() == KeyEvent.KEY_PRESSED) {
                int c = e.getKeyCode();
                for (KeyboardShortcut shortcut : KEYBOARD_SHORTCUTS) {
                    if ((Character.toUpperCase(c) == shortcut.key || Character.toLowerCase(c) == shortcut.key) && shortcut.commandKey.commandKeyHeld(e)) {
                        shortcut.action.run();
                    }
                }

            }
            return false;
        });
    }

    /**
     * Possibly uses a powerup in the game, program is in gameplay screen.
     *
     * @param powerUp The powerup to be used.
     */
    private static void maybeUsePowerup(PowerUp powerUp) {
        if (!frame.isInGameplay()) return;
        GameLevel g = frame.getGameLevel();
        if (g == null) return;
        g.powerupAction(powerUp);
    }
}