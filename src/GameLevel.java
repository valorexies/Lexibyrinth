import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.*;

/**
 * The logic and data for a game level, which consists of a queue of enemies to spawn, active
 * enemies on the screen, and pending spawns. The player must defeat all enemies by typing their
 * defeat words before they can be defeated by timed attacks.
 *
 * The player can also use power-ups to temporarily freeze enemies, double points, or heal.
 *
 * @version 2.0
 * @author Kevin Qi
 * @author Alan Lu
 */
public final class GameLevel {
    private static final int MAX_ACTIVE_ENEMIES = 4;
    private static final int MAX_HEALTH = 100;

    private static final long RESPAWN_DELAY_MS = 2000L;
    private static final long FREEZE_DURATION_MS = 5000L;
    private static final long DOUBLE_POINTS_DURATION_MS = 5000L;

    private static final int[] SPAWN_POSITIONS = {0, 1, 2, 3};

    private final Queue<Enemy> enemyQueue = new ArrayDeque<>();
    private final List<ActiveEnemy> activeEnemies = new ArrayList<>();
    private final List<PendingSpawn> pendingSpawns = new ArrayList<>();

    private String input = "";
    private int health = MAX_HEALTH;
    private int currScore = 0;
    private int highScore = 0;
    private float currWPM = 0f;

    private final int levelNumber;
    private final long levelStartMs;

    private boolean levelFailed = false;
    private boolean levelCompleted = false;
    private boolean statsUpdated = false;

    private boolean freezeActive = false;
    private long freezeStartedMs = 0L;
    private long freezeUntilMs = 0L;

    private boolean doublePointsActive = false;
    private long doublePointsUntilMs = 0L;

    // Level stats accumulated during play, used for finalizing the player's overall statistics at the end of the level.
    private int correctKeystrokes = 0;  // Keystrokes that were a valid prefix or completed a word.
    private int totalKeystrokes = 0;    // Every keystroke the player made.
    private int errorCount = 0;         // Keystrokes that matched no enemy word prefix.
    private int wordsTypedThisLevel = 0;
    private int totalCharsTyped = 0;    // Total chars from successfully defeated words, used for WPM.
    private float peakWPMThisLevel = 0f;

    private int currPoints = 0;

    private static final long INITIAL_ATTACK_STAGGER_MS = 900L;
    private static final long RESPAWN_ATTACK_GRACE_MS = 1200L;
    private static final long GLOBAL_ATTACK_GAP_MS = 400L;

    private long lastAnyAttackMs = 0L;

    private static final long PLAYER_ATTACK_ANIMATION_MS = 450L;

    private long playerAttackStartedMs = -1L;
    //============images---------------------------
    Sprites sprites;

    // ----------------------Nested Helper Classes/Methods----------------------
    private long getEffectiveElapsedMs(ActiveEnemy active, long now) {
        if (active.freezePaused) {
            return active.frozenElapsedMs;
        }
        return Math.max(0L, now - active.lastAttackMs);
    }

    private double getEnemyAttackProgress(ActiveEnemy active, long now) {
        double interval = active.enemy.getAttackIntervalMs();
        double elapsed = getEffectiveElapsedMs(active, now);
        return Math.max(0.0, Math.min(1.0, elapsed / interval));
    }

    private void pauseActiveEnemiesForFreeze(long now) {
        for (ActiveEnemy active : activeEnemies) {
            if (!active.freezePaused) {
                active.frozenElapsedMs = Math.max(0L, now - active.lastAttackMs);
                active.freezePaused = true;
            }
        }
    }

    private void resumeActiveEnemiesAfterFreeze(long now) {
        for (ActiveEnemy active : activeEnemies) {
            if (active.freezePaused) {
                active.lastAttackMs = now - active.frozenElapsedMs;
                active.freezePaused = false;
            }
        }
    }

    private boolean isPlayerAttacking(long now) {
        return playerAttackStartedMs >= 0
                && now - playerAttackStartedMs < PLAYER_ATTACK_ANIMATION_MS;
    }

    private int getPlayerIdleAnimationFrame(long now) {
        return (int) ((now / 120) % 16);
    }

    private int getPlayerAttackAnimationFrame(long now) {
        if (!isPlayerAttacking(now)) {
            return 0;
        }

        double elapsed = now - playerAttackStartedMs;
        double progress = Math.max(0.0, Math.min(1.0, elapsed / (double) PLAYER_ATTACK_ANIMATION_MS));

        return Math.min(15, (int) Math.floor(progress * 16.0));
    }

    private int getAttackAnimationFrame(ActiveEnemy active, long now) {
        if (freezeActive) {
            return 0;
        }

        double interval = active.enemy.getAttackIntervalMs();
        double elapsed = getEffectiveElapsedMs(active, now);
        double progress = Math.max(0.0, Math.min(1.0, elapsed / interval));

        // Stay idle for most of the timer.
        if (progress < 0.75) {
            return 0;
        }

        // Play the 16-frame animation only in the last 25% before attacking.
        double attackPhase = (progress - 0.75) / 0.25;
        int frame = (int) Math.round(attackPhase * 15.0);

        return Math.max(0, Math.min(15, frame));
    }

    private static final class ActiveEnemy {
        private final Enemy enemy;
        private final int slotIndex;
        private long lastAttackMs;

        private boolean freezePaused;
        private long frozenElapsedMs;

        private ActiveEnemy(Enemy enemy, int slotIndex, long now) {
            this.enemy = enemy;
            this.slotIndex = slotIndex;
            this.lastAttackMs = now;
            this.freezePaused = false;
            this.frozenElapsedMs = 0L;
        }
    }

    private static final class PendingSpawn {
        private final int slotIndex;
        private final long spawnAtMs;

        private PendingSpawn(int slotIndex, long spawnAtMs) {
            this.slotIndex = slotIndex;
            this.spawnAtMs = spawnAtMs;
        }
    }

    /**
     * Draws a single enemy's word centered above its position.
     *
     * <p>
     * The portion of the word that matches the current input buffer is drawn in green,
     * and the remaining untyped characters are drawn in white. If the word does not
     * start with the current input, the entire word is drawn in white (unmatched).
     * A black drop-shadow is drawn behind the text for readability.
     * </p>
     *
     * @param g         The graphics context to draw with.
     * @param word      The word for this enemy.
     * @param centerX   The horizontal center point (typically the center of the enemy sprite).
     * @param baselineY The y-coordinate of the text baseline.
     */
    private void drawEnemyWord(Graphics g, String word, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();

        String typedPart = "";
        String remainingPart = word;

        String lowerWord = word.toLowerCase();
        String lowerInput = input.toLowerCase();

        if (!lowerInput.isEmpty() && lowerWord.startsWith(lowerInput)) {
            typedPart = word.substring(0, input.length());
            remainingPart = word.substring(input.length());
        }

        int totalWidth = fm.stringWidth(word);
        int startX = centerX - totalWidth / 2;

        // Thick outline / shadow so the word stands out more.
        g.setColor(Color.BLACK);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    g.drawString(word, startX + dx, baselineY + dy);
                }
            }
        }

        // Typed part in brighter green.
        g.setColor(new Color(80, 255, 80));
        g.drawString(typedPart, startX, baselineY);

        // Remaining part in white.
        int typedWidth = fm.stringWidth(typedPart);
        g.setColor(Color.WHITE);
        g.drawString(remainingPart, startX + typedWidth, baselineY);
    }

    // ----------------------Constructor and Initialization----------------------

    public GameLevel(int level, Sprites sprite) {
        this.sprites = sprite;
        this.levelNumber = level;
        this.levelStartMs = System.currentTimeMillis();

        // Load the player's persisted high score.
        PlayerStatistics stats = Statistic.getCurrentStats();
        if (stats != null) {
            this.highScore = stats.highScore;
        }

        generateEnemies(level);
        spawnInitialEnemies();
    }

    //---------------------Level Setup Methods---------------------

    private void generateEnemies(int level) {
        // Each level has 10 more enemies than the previous, starting with 10 at level 1.
        int enemyCount = 10 + (level * 10);
        // Cap difficulty between 1 and 3.
        int difficulty = Math.max(1, Math.min(3, level));

        for (int i = 0; i < enemyCount; i++) {
            enemyQueue.add(new Enemy(difficulty));
        }
    }

    /**
     * Schedules the initial enemies to appear one by one via {@link PendingSpawn} entries,
     * each staggered by {@code INITIAL_ATTACK_STAGGER_MS}. This means the first enemy
     * appears immediately and subsequent enemies arrive at regular intervals rather than
     * all spawning on the same frame.
     */
    private void spawnInitialEnemies() {
        long now = System.currentTimeMillis();
        int slotsToFill = Math.min(MAX_ACTIVE_ENEMIES, enemyQueue.size());

        for (int slot = 0; slot < slotsToFill; slot++) {
            long spawnAt = now + (slot * INITIAL_ATTACK_STAGGER_MS);
            pendingSpawns.add(new PendingSpawn(slot, spawnAt));
        }
    }

    //--------------------Loop Update Methods---------------------

    /**
     * Draws all active enemies and their defeat words positioned above each enemy sprite.
     * Also draws the player health bar and any active power-up indicators.
     *
     * <p>
     * Each enemy's word is drawn directly above its own sprite. Characters that match the
     * current input buffer are highlighted in green for every enemy whose word starts with
     * that prefix, giving the player visual feedback on all valid targets simultaneously.
     * </p>
     */
    public void draw(Graphics g, int screenwd, int screenht) {
        int[] xPositions = {
            (3 * screenwd / 4) - screenwd / 8,
            (2 * screenwd / 3) - screenwd / 8,
            (5 * screenwd / 6) - screenwd / 8,
            (3 * screenwd / 4) - screenwd / 8
        };

        int[] yPositions = {
            screenht / 3,
            5 * screenht / 12,
            5 * screenht / 12,
            screenht / 2
        };

        Font wordFont = new Font("Arial", Font.BOLD, Math.max(20, screenwd / 50));
        g.setFont(wordFont);
        FontMetrics fm = g.getFontMetrics();

        // Animation frame: changes every 100 ms, loops through 16 frames.
        long now = System.currentTimeMillis();
        // -----------------------------------------------------------------
        // PASS 0: draw player
        // -----------------------------------------------------------------
        int playerX = (int) Math.round(170.0 * screenwd / 1536.0);
        int playerY = (int) Math.round(500.0 * screenht / 1024.0);

        Image playerFrame;
        if (isPlayerAttacking(now)) {
            playerFrame = Sprites.getPlayerAttackFrame(getPlayerAttackAnimationFrame(now));
        } else {
            playerFrame = Sprites.getPlayerIdleFrame(getPlayerIdleAnimationFrame(now));
        }

        if (playerFrame != null) {
            g.drawImage(playerFrame, playerX, playerY, null);
        }

        // -----------------------------------------------------------------
        // PASS 1: draw enemy sprites in fixed slot order: 0, 1, 2, 3
        // -----------------------------------------------------------------
        for (int slot = 0; slot < xPositions.length; slot++) {
            for (ActiveEnemy active : activeEnemies) {
                if (active.slotIndex == slot) {
                    int x = xPositions[slot];
                    int y = yPositions[slot];

                    int frame = getAttackAnimationFrame(active, now);
                    Image enemyFrame = Sprites.getEnemyFrame(active.enemy.getType(), frame);
                    if (enemyFrame != null) {
                        g.drawImage(enemyFrame, x-100, y, null);
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // PASS 2: draw progress bars in fixed slot order
        // -----------------------------------------------------------------
        for (int slot = 0; slot < xPositions.length; slot++) {
            for (ActiveEnemy active : activeEnemies) {
                if (active.slotIndex == slot) {
                    int x = xPositions[slot];
                    int y = yPositions[slot];

                    if (!freezeActive) {
                        g.setColor(Color.ORANGE);
                    } else {
                        g.setColor(Color.decode("#66E9FF"));
                    }

                    double progress = getEnemyAttackProgress(active, now);
                    g.fillRect(x - 25, y + 256, (int) (progress * 100), 5);
                    g.drawRect(x - 25, y + 256, 100, 5);
                }
            }
        }

        // -----------------------------------------------------------------
        // HUD / UI
        // -----------------------------------------------------------------

        // power up amounts
        g.setColor(Color.decode("#66E9FF"));
        g.drawString(
            String.valueOf(Statistic.getCurrentStats().totalPowerups[PowerUp.FREEZE.ordinal()]) + ": ctrl + B",
            175, 150
        );

        g.setColor(Color.decode("#9566FF"));
        g.drawString(
            String.valueOf(Statistic.getCurrentStats().totalPowerups[PowerUp.SCORE.ordinal()]) + ": ctrl + N",
            175, 275
        );

        g.setColor(Color.decode("#80BA4A"));
        g.drawString(
            String.valueOf(Statistic.getCurrentStats().totalPowerups[PowerUp.HEALTH.ordinal()]) + ": ctrl + M",
            175, 400
        );

        // score and wpm
        g.setColor(Color.white);
        g.drawString("Score: " + String.valueOf(currScore), screenwd / 2 - 100, 100);

        if (doublePointsActive) {
            g.setColor(Color.decode("#9566FF"));
        } else {
            g.setColor(Color.white);
        }

        g.drawString("Current WPM: " + String.format("%.2f", currWPM), screenwd / 2 - 100, 50);

        // Draw player health bar.
        if (health > 0) {
            int barX = (int) Math.round(164.0 * screenwd / 1500.0);
            int barY = (int) Math.round(943.0 * screenht / 1024.0);
            int barW = (int) Math.round(285.0 * screenwd / 1536.0);
            int barH = (int) Math.round(35.0 * screenht / 1024.0);

            int currentBarW = (int) Math.round(barW * (health / 100.0));

            g.setColor(Color.DARK_GRAY);
            g.fillRect(barX, barY, barW, barH);

            g.setColor(Color.RED);
            g.fillRect(barX, barY, currentBarW, barH);
        }

        // -----------------------------------------------------------------
        // PASS 3: draw enemy words LAST so they stay on top
        // -----------------------------------------------------------------
        g.setFont(wordFont);
        for (int slot = 0; slot < xPositions.length; slot++) {
            for (ActiveEnemy active : activeEnemies) {
                if (active.slotIndex == slot) {
                    int x = xPositions[slot];
                    int y = yPositions[slot];

                    int wordBaselineY = y - fm.getDescent() - 12;
                    int enemyCenterX = x + 25; // adjust if sprite width changes
                    drawEnemyWord(g, active.enemy.getDefeatWord(), enemyCenterX, wordBaselineY);
                }
            }
        }
    }

    /**
     * Updates the game state: processes enemy attacks, spawns pending enemies, and
     * handles power-up expirations. Should be called on every game tick.
     */
    public void update() {
        if (levelFailed || levelCompleted) {
            return;
        }

        long now = System.currentTimeMillis();

        updatePowerUps(now);
        processPendingSpawns(now);
        processEnemyAttacks(now);
        recalculateWPM();

        if (health <= 0) {
            failLevel();
            return;
        }

        if (isLevelComplete()) {
            levelCompleted = true;
            finalizeStats();
        }
    }

    private void updatePowerUps(long now) {
        if (freezeActive && now >= freezeUntilMs) {
            freezeActive = false;
            resumeActiveEnemiesAfterFreeze(now);
        }

        if (doublePointsActive && now >= doublePointsUntilMs) {
            doublePointsActive = false;
        }
    }

    private void processPendingSpawns(long now) {
        Iterator<PendingSpawn> iterator = pendingSpawns.iterator();

        while (iterator.hasNext()) {
            PendingSpawn pending = iterator.next();

            if (now >= pending.spawnAtMs && !enemyQueue.isEmpty() && !isSlotOccupied(pending.slotIndex)) {
                Enemy enemy = enemyQueue.poll();
                enemy.setPosition(SPAWN_POSITIONS[pending.slotIndex]);

                ActiveEnemy spawned = new ActiveEnemy(enemy, pending.slotIndex, now + RESPAWN_ATTACK_GRACE_MS);

                if (freezeActive) {
                    spawned.freezePaused = true;
                    spawned.frozenElapsedMs = 0L;
                }

                activeEnemies.add(spawned);
                iterator.remove();
            }
        }
    }

    private void processEnemyAttacks(long now) {
        if (freezeActive) {
            return;
        }

        if (now - lastAnyAttackMs < GLOBAL_ATTACK_GAP_MS) {
            return;
        }

        ActiveEnemy nextAttacker = null;
        double mostOverdue = Double.NEGATIVE_INFINITY;

        for (ActiveEnemy active : activeEnemies) {
            double overdue = (now - active.lastAttackMs) - active.enemy.getAttackIntervalMs();

            if (overdue >= 0 && overdue > mostOverdue) {
                mostOverdue = overdue;
                nextAttacker = active;
            }
        }

        if (nextAttacker != null) {
            takeDMG(nextAttacker.enemy.getDamage());
            nextAttacker.lastAttackMs = now;
            lastAnyAttackMs = now;
        }
    }

    // --------------------Input Handling Method---------------------

    /**
     * Processes a single character of player input against all active enemies.
     *
     * <p>
     * The input buffer is matched against every active enemy's word simultaneously.
     * This allows the player to freely choose which enemy to defeat by typing its word:
     * </p>
     *
     * <ul>
     *   <li>If the accumulated input exactly matches an enemy's defeat word, that enemy is
     *       removed and the input buffer resets.</li>
     *   <li>If the accumulated input is a valid prefix of one or more defeat words, the
     *       keystroke is accepted and the highlighted prefix grows on every matching word,
     *       narrowing the set of viable targets as the player continues typing.</li>
     *   <li>If the accumulated input matches no defeat word prefix at all, the keystroke is
     *       counted as an error and the buffer resets so the player can start a new word.</li>
     * </ul>
     *
     * @param c The character typed by the player.
     */
    public void processInput(char c) {
        if (levelFailed || levelCompleted) {
            return;
        }

        totalKeystrokes++;

        if (activeEnemies.isEmpty()) {
            return;
        }

        String tentative = input + c;

        // --- Check for an exact word completion first ---
        ActiveEnemy exactMatch = null;
        for (ActiveEnemy active : activeEnemies) {
            if (active.enemy.getDefeatWord().equalsIgnoreCase(tentative)) {
                exactMatch = active;
                break;
            }
        }

        if (exactMatch != null) {
            correctKeystrokes++;
            wordsTypedThisLevel++;
            totalCharsTyped += tentative.length();

            int points = pointsFor(exactMatch.enemy.getDifficulty());
            if (doublePointsActive) {
                points *= 2;
            }
            currScore += points;

            if (currScore > highScore) {
                highScore = currScore;
            }

            long now = System.currentTimeMillis();
            playerAttackStartedMs = now;

            for (Iterator<ActiveEnemy> it = activeEnemies.iterator(); it.hasNext();) {
                ActiveEnemy active = it.next();

                if (active == exactMatch) {
                    it.remove();

                    if (!enemyQueue.isEmpty()) {
                        pendingSpawns.add(new PendingSpawn(active.slotIndex, now + RESPAWN_DELAY_MS));
                    }
                    break;
                }
            }

            input = "";
            recalculateWPM();
            return;
        }

        // Check whether the tentative input is a valid prefix of any active word.
        boolean anyPrefixMatch = false;
        for (ActiveEnemy active : activeEnemies) {
            if (active.enemy.getDefeatWord().toLowerCase().startsWith(tentative.toLowerCase())) {
                anyPrefixMatch = true;
                break;
            }
        }

        if (anyPrefixMatch) {
            // Accept the character: the typed prefix now narrows the set of targetable enemies.
            correctKeystrokes++;
            input = tentative;
        } else {
            // No enemy word starts with this prefix — count as an error and reset.
            errorCount++;
            input = "";
        }
    }

    // ---------------------WPM Calculation Method---------------------

    /**
     * Recalculates current WPM from the total characters that have been typed.
     * Standard WPM formula: (total chars / 5) / time in minutes.
     * Also updates the peak WPM seen so far this level.
     */
    private void recalculateWPM() {
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - levelStartMs);
        double minutes = elapsedMs / 60000.0;

        currWPM = (float) ((totalCharsTyped / 5.0) / minutes);

        if (currWPM > peakWPMThisLevel) {
            peakWPMThisLevel = currWPM;
        }
    }

    // --------------------Stats Finalization Method---------------------

    /**
     * Merges this level's results into the player's persistent {@link PlayerStatistics}.
     * Runs and on the level over screen.
     */
    public void finalizeStats() {
        if(statsUpdated){
            return;
        }
        PlayerStatistics stats = Statistic.getCurrentStats();
        if (stats == null) {
            return;
        }

        // Calculate WPM for this level.
        long levelDurationMs = Math.max(1L, System.currentTimeMillis() - levelStartMs);
        double minutes = levelDurationMs / 60000.0;
        float levelWPM = (float) (wordsTypedThisLevel / minutes);

        // Peak WPM (Player's highest WPM achieved across levels).
        if (levelWPM > stats.peakWPM) {
            stats.peakWPM = levelWPM;
        }

        // Running average WPM from levels completed.
        stats.meanWPM = ((stats.meanWPM * stats.levelsCompleted) + levelWPM)
                           / (stats.levelsCompleted + 1);

        // Accuracy for this level.
        float levelAccuracy;
        if (totalKeystrokes > 0) {
            levelAccuracy = (float) (correctKeystrokes * 100f / totalKeystrokes);
        } else {
            levelAccuracy = 100f; // Start with 100% accuracy if the player made no keystrokes.
        }

        // Average accuracy. Like WPM, this is a running average across levels completed.
        stats.accuracy = ((stats.accuracy * stats.levelsCompleted) + levelAccuracy)
                         / (stats.levelsCompleted + 1);

        // Cumulative error count.
        stats.errorCount += errorCount;

        // Total words typed.
        stats.totalWordsTyped += wordsTypedThisLevel;

        // High score. If the player beat their previous high score this level, then update.
        if (currScore > stats.highScore) {
            stats.highScore = currScore;
        }

        // Level progression.
        // The player has now completed levelNumber, so their progression is at least levelNumber + 1,
        // but never beyond 3, since we only have 3 levels.
        if(!levelFailed){
            int nextLevel = Math.min(3, levelNumber + 1);
            if (nextLevel > stats.levelProgression) {
                stats.levelProgression = nextLevel;
            }
            // Increment the completed-levels counter last for running averages.
            stats.levelsCompleted++;
        }

        statsUpdated = true;

        Statistic.updateUserStatistics();
        Statistic.updateHighScore();
    }

    // ----------------------Power-ups Methods----------------------

    public void powerupAction(PowerUp choice) {
        long now = System.currentTimeMillis();

        switch (choice) {
            case FREEZE -> useFreeze(now);
            case SCORE  -> useDoublePoints(now);
            case HEALTH -> useHeal();
        }
    }

    private void useFreeze(long now) {
        if (Statistic.getCurrentStats().totalPowerups[PowerUp.FREEZE.ordinal()] < 1) {
            return;
        }

        Statistic.getCurrentStats().totalPowerups[PowerUp.FREEZE.ordinal()]--;

        if (!freezeActive) {
            freezeActive = true;
            freezeStartedMs = now;
            freezeUntilMs = now + FREEZE_DURATION_MS;

            pauseActiveEnemiesForFreeze(now);
        } else {
            freezeUntilMs += FREEZE_DURATION_MS;
        }
    }

    private void useDoublePoints(long now) {
        if (Statistic.getCurrentStats().totalPowerups[PowerUp.SCORE.ordinal()] < 1) return;
        Statistic.getCurrentStats().totalPowerups[PowerUp.SCORE.ordinal()]--;
        if (!doublePointsActive) {
            doublePointsActive = true;
            doublePointsUntilMs = now + DOUBLE_POINTS_DURATION_MS;
        } else {
            doublePointsUntilMs += DOUBLE_POINTS_DURATION_MS;
        }
    }

    private void useHeal() {
        if (Statistic.getCurrentStats().totalPowerups[PowerUp.HEALTH.ordinal()] < 1) return;
        Statistic.getCurrentStats().totalPowerups[PowerUp.HEALTH.ordinal()]--;
        if (health < MAX_HEALTH) {
            health=Math.min(health+40, 100); //capped 100
        }
    }

    public int getPoints() {
        return currPoints;
    }

    public void setPoints(int newPointValue) {
        currPoints = newPointValue;
    }

    // ----------------------Other Helper Methods----------------------

    public void takeDMG(int dmg) {
        health -= dmg;
        if (health <= 0) {
            failLevel();
        }
    }

    public void failLevel() {
        levelFailed = true;
        finalizeStats();
        // stats are written on both successful and unsuccessful completion.
    }

    public boolean isLevelComplete() {
        return !levelFailed
                && enemyQueue.isEmpty()
                && activeEnemies.isEmpty();
    }

    public void exitLevel() {
        activeEnemies.clear();
        pendingSpawns.clear();
        enemyQueue.clear();
    }

    private boolean isSlotOccupied(int slotIndex) {
        for (ActiveEnemy active : activeEnemies) {
            if (active.slotIndex == slotIndex) {
                return true;
            }
        }
        return false;
    }

    private int pointsFor(int difficulty) {
        return switch (difficulty) {
            case 1  -> 100;
            case 2  -> 120;
            case 3  -> 140;
            default -> 100;
        };
    }

    // ----------------------Getters Methods----------------------

    public int getHealth() {
        return health;
    }

    public int getScore() {
        return currScore;
    }

    public int getHighScore() {
        return highScore;
    }

    public float getWPM() {
        return currWPM;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isLevelFailed() {
        return levelFailed;
    }

    public boolean isLevelCompleted() {
        return levelCompleted;
    }

    /**
     * Returns the current typed input buffer which helps highlight matched prefixes in the UI.
     */
    public String getCurrentInput() {
        return input;
    }

    public List<Enemy> getActiveEnemies() {
        List<Enemy> result = new ArrayList<>();

        for (ActiveEnemy active : activeEnemies) {
            result.add(active.enemy);
        }

        return Collections.unmodifiableList(result);
    }

    public int getEnemyPosition(Enemy enemy) {
        for (ActiveEnemy active : activeEnemies) {
            if (active.enemy == enemy) {
                return active.slotIndex;
            }
        }

        return -1;
    }
}
