import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 * Centralized sprite / asset loader for Lexibyrinth.
 *
 * Load once at startup with:
 * Sprites.loadAll(screenWidth, screenHeight);
 *
 * Enemy animation files are expected in:
 * /Assets/sprites/enemy/<enemyType>/frame_001.png
 * ...
 * /Assets/sprites/enemy/<enemyType>/frame_016.png
 *
 * Also supports weird names like frame_0010.png if needed.
 *
 * @author Alan
 * @version 2.0
 */
public class Sprites {
    private static final int PLAYER_FRAME_COUNT = 16;

    private static Image[] PLAYER_IDLE_FRAMES;
    private static Image[] PLAYER_ATTACK_FRAMES;

    private static boolean loaded = false;
    private static int loadedScreenWidth = -1;
    private static int loadedScreenHeight = -1;

    private static final int ENEMY_FRAME_COUNT = 16;

    // ---------- general UI / screen assets ----------
    public static Image APP_WINDOW_ICON;
    public static ImageIcon TITLE_BAR_ICON;
    public static ImageIcon MAIN_LOGO_ICON;
    public static ImageIcon MENU_BUTTON_ICON;
    public static ImageIcon WIDE_BUTTON_ICON;
    public static ImageIcon BACK_BUTTON_ICON;
    public static ImageIcon OK_BUTTON_ICON;

    public static Image POPUP_BACKGROUND;
    public static ImageIcon MAIN_BACKGROUND_ICON;
    public static ImageIcon BACKGROUND_ICON;
    public static ImageIcon DOOR_SELECTION_ICON;
    public static ImageIcon DOOR_OPEN_ICON;
    public static ImageIcon CHEST_CLOSED_ICON;
    public static ImageIcon CHEST_OPEN_ICON;
    public static ImageIcon SHOP_BACKGROUND_ICON;
    public static ImageIcon TABLE_ICON;

    public static ImageIcon FREEZE_POWER_ICON;
    public static ImageIcon DOUBLE_POWER_ICON;
    public static ImageIcon HEAL_POWER_ICON;

    public static Image BATTLEFIELD_IMAGE;

    // ---------- enemy sprite animations ----------
    private static final Map<String, Image[]> ENEMY_SPRITES = new HashMap<>();

    // Public constructor so it behaves like a normal visible class,
    // though you do not need to instantiate it if you use static access.
    public Sprites() {
    }

    /**
     * Loads or reloads all assets for the given screen size.
     *
     * @param screenWidth current screen width
     * @param screenHeight current screen height
     */
    public static void loadAll(int screenWidth, int screenHeight) {
        if (loaded
                && loadedScreenWidth == screenWidth
                && loadedScreenHeight == screenHeight) {
            return;
        }

        loadedScreenWidth = screenWidth;
        loadedScreenHeight = screenHeight;

        loadUiAssets(screenWidth, screenHeight);
        loadEnemySprites();
        loadPlayerSprites();

        loaded = true;
    }

    /**
     * Returns the first frame of an enemy animation.
     * Kept for compatibility with older code.
     *
     * @param enemyType enemy type such as "behemoth", "ghost", "goblin", or "witch"
     * @return first frame, or null if not found
     */
    public static Image getEnemySprite(String enemyType) {
        return getEnemyFrame(enemyType, 0);
    }

    /**
     * Returns one frame from an enemy animation.
     *
     * @param enemyType enemy type
     * @param frameIndex animation frame index
     * @return image frame, or null if enemy type is invalid
     */
    public static Image getEnemyFrame(String enemyType, int frameIndex) {
        ensureLoaded();

        if (enemyType == null) {
            return null;
        }

        Image[] frames = ENEMY_SPRITES.get(enemyType.toLowerCase());
        if (frames == null || frames.length == 0) {
            return null;
        }

        int safeIndex = Math.floorMod(frameIndex, frames.length);
        return frames[safeIndex];
    }

    /**
     * Returns all frames for the given enemy type.
     *
     * @param enemyType enemy type
     * @return animation frame array, or null if not found
     */
    public static Image[] getEnemyAnimation(String enemyType) {
        ensureLoaded();

        if (enemyType == null) {
            return null;
        }

        return ENEMY_SPRITES.get(enemyType.toLowerCase());
    }

    /**
     * Returns a read-only view of all loaded enemy animations.
     *
     * @return unmodifiable map of enemy type -> image frame array
     */
    public static Map<String, Image[]> getEnemySprites() {
        ensureLoaded();
        return Collections.unmodifiableMap(ENEMY_SPRITES);
    }

    private static void loadUiAssets(int screenWidth, int screenHeight) {
        APP_WINDOW_ICON = loadImage("/Assets/Icon.png");
        TITLE_BAR_ICON = loadScaledIcon("/Assets/Icon.png", 24, 24);
        MAIN_LOGO_ICON = loadScaledIcon("/Assets/Icon.png", 650, 500);

        MENU_BUTTON_ICON = loadScaledIcon("/Assets/menubutton.png", 350, 120);
        WIDE_BUTTON_ICON = loadScaledIcon("/Assets/menubutton.png", 800, 60);
        BACK_BUTTON_ICON = loadScaledIcon("/Assets/backbutton.png", 160, 120);
        OK_BUTTON_ICON = loadScaledIcon("/Assets/okbutton.png", 130, 50);

        POPUP_BACKGROUND = loadImage("/Assets/optionpane.png");

        MAIN_BACKGROUND_ICON = loadScaledIcon("/Assets/mainbackground.png", screenWidth, screenHeight);
        BACKGROUND_ICON = loadScaledIcon("/Assets/background.png", screenWidth, screenHeight);
        DOOR_SELECTION_ICON = loadScaledIcon("/Assets/doorSelection.png", screenWidth, screenHeight);

        DOOR_OPEN_ICON = loadScaledIcon("/Assets/openDoor.png", 250, 420);
        CHEST_CLOSED_ICON = loadScaledIcon("/Assets/chestClosed.png", 300, 200);
        CHEST_OPEN_ICON = loadScaledIcon("/Assets/chestOpen.png", 300, 200);

        SHOP_BACKGROUND_ICON = loadScaledIcon("/Assets/shopBackground.png", screenWidth, screenHeight);
        TABLE_ICON = loadScaledIcon("/Assets/table.png", 250, 300);

        FREEZE_POWER_ICON = loadScaledIcon("/Assets/freezePower.png", 100, 100);
        DOUBLE_POWER_ICON = loadScaledIcon("/Assets/doublePower.png", 100, 100);
        HEAL_POWER_ICON = loadScaledIcon("/Assets/healPower.png", 100, 100);

        BATTLEFIELD_IMAGE = loadImage("/Assets/battlefield.png");
    }

    private static void loadEnemySprites() {
        ENEMY_SPRITES.clear();

        registerEnemyAnimation("behemoth");
        registerEnemyAnimation("ghost");
        registerEnemyAnimation("goblin");
        registerEnemyAnimation("witch");
    }

    private static void registerEnemyAnimation(String type) {
        Image[] frames = new Image[ENEMY_FRAME_COUNT];

        for (int i = 1; i <= ENEMY_FRAME_COUNT; i++) {
            frames[i - 1] = loadEnemyFrame(type, i);
        }

        ENEMY_SPRITES.put(type.toLowerCase(), frames);
    }

    private static Image loadEnemyFrame(String type, int frameNumber) {
        String normalPath = "/Assets/sprites/enemy/" + type + "/frame_" + String.format("%03d", frameNumber) + ".png";

        // Handles names like frame_0010.png, frame_0011.png
        String alternatePath = "/Assets/sprites/enemy/" + type + "/frame_00" + frameNumber + ".png";

        Image img = loadImage(normalPath);
        if (img != null) {
            return img;
        }

        if (frameNumber >= 10) {
            img = loadImage(alternatePath);
            if (img != null) {
                return img;
            }
        }

        System.err.println("Warning: missing enemy frame for " + type + " frame " + frameNumber);
        return null;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            throw new IllegalStateException(
                    "Sprites not loaded yet. Call Sprites.loadAll(screenWidth, screenHeight) first."
            );
        }
    }

    private static Image loadImage(String path) {
        try {
            URL url = Sprites.class.getResource(path);
            if (url != null) {
                return new ImageIcon(url).getImage();
            }

            // Fallback: load directly from project folder if resource path is not on classpath
            String filePath = path.startsWith("/") ? path.substring(1) : path;
            File file = new File(filePath);
            if (file.exists()) {
                return new ImageIcon(file.getAbsolutePath()).getImage();
            }

            System.err.println("Warning: missing asset " + path);
            return null;
        } catch (Exception e) {
            System.err.println("Warning: failed to load asset " + path);
            e.printStackTrace();
            return null;
        }
    }

    private static ImageIcon loadScaledIcon(String path, int width, int height) {
        Image img = loadImage(path);
        if (img == null) {
            return null;
        }

        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static Image getPlayerIdleFrame(int frameIndex) {
        ensureLoaded();

        if (PLAYER_IDLE_FRAMES == null || PLAYER_IDLE_FRAMES.length == 0) {
            return null;
        }

        int safeIndex = Math.floorMod(frameIndex, PLAYER_IDLE_FRAMES.length);
        return PLAYER_IDLE_FRAMES[safeIndex];
    }

    public static Image getPlayerAttackFrame(int frameIndex) {
        ensureLoaded();

        if (PLAYER_ATTACK_FRAMES == null || PLAYER_ATTACK_FRAMES.length == 0) {
            return null;
        }

        int safeIndex = Math.floorMod(frameIndex, PLAYER_ATTACK_FRAMES.length);
        return PLAYER_ATTACK_FRAMES[safeIndex];
    }

    private static void loadPlayerSprites() {
        PLAYER_IDLE_FRAMES = loadAnimationSet("/Assets/sprites/player/idle", PLAYER_FRAME_COUNT);
        PLAYER_ATTACK_FRAMES = loadAnimationSet("/Assets/sprites/player/attack", PLAYER_FRAME_COUNT);
    }

    private static Image[] loadAnimationSet(String folderPath, int frameCount) {
        Image[] frames = new Image[frameCount];

        for (int i = 1; i <= frameCount; i++) {
            frames[i - 1] = loadAnimationFrame(folderPath, i);
        }

        return frames;
    }

    private static Image loadAnimationFrame(String folderPath, int frameNumber) {
        String normalPath = folderPath + "/frame_" + String.format("%03d", frameNumber) + ".png";
        String alternatePath = folderPath + "/frame_00" + frameNumber + ".png";

        Image img = loadImage(normalPath);
        if (img != null) {
            return img;
        }

        if (frameNumber >= 10) {
            img = loadImage(alternatePath);
            if (img != null) {
                return img;
            }
        }

        System.err.println("Warning: missing animation frame " + folderPath + " frame " + frameNumber);
        return null;
    }
}
