import java.util.Random;

/**
 * The logic and data for an enemy the player must defeat by typing words. Otherwise, enemy timed attacks will defeat the player.
 *
 * @author Eric Zhang
 * @author Kevin Qi
 */
public class Enemy {
    private int position;
    private double interval; //time between attacks
    private double prevTime;
    private String defeatWord;
    private Random random;
    private int difficulty; //word difficulty, time amount per word: based on difficulty of the level (1-3)
    private static boolean freezeActive;
    private static String[][] words = new String[8][];
    private String type;
    private int damage;
    private boolean wasFrozen;
    private double freezeStartTime;

    /**
     * Creates a new Enemy.
     *
     * @param difficulty The difficulty.
     */
    public Enemy(int difficulty) {
        random = new Random();
        this.difficulty = difficulty;

        int randType = random.nextInt(0, 4);
        switch (randType) {
            case 0:
                type = "behemoth";
                break;
            case 1:
                type = "ghost";
                break;
            case 2:
                type = "goblin";
                break;
            case 3:
                type = "witch";
                break;
            default:
                throw new AssertionError();
        }

        interval = 3000 * random.nextDouble(1, 3); // random time amount (ms) between 3-6 seconds)
        damage = (int) (Math.log(interval) * difficulty); //more damage for atks that take longer and, min 8, max 27
        prevTime = System.currentTimeMillis(); //initialize previous time, accounting for frozen time

        //get a defeatWord randomly (the .setUp() static method must be called beforehand)
        assignWord();
    }

    public String getType(){
        return type;
    }

    /**
     * Initializer for the arrays of words needed to defeat enemies.
     * Call static the static method on Enemy before needing to create any enemy.
     */
    public static void setUp() {
        try {
            for (int i = 4; i <= 11; i++) {
                String[] wordArray = FileManager.getWordList(i + " letter");

                words[i - 4] = wordArray;
            }
        } catch (Exception e) {
            System.out.println(e + "\nFatal: selected WORD_FILE.csv is not downloaded or does not exist." +
                    "\nPlease check that all files were downloaded.");
        }
    }

    /**
     * Assigns a random word as the defeat word based on the desired word length (4-11 letters).
     */
    private void assignWord() {
        int wordLength = 4;
        int r = random.nextInt(0, 3);
        switch (difficulty) {
            case 1 -> wordLength = 4 + r;
            case 2 -> wordLength = 7 + r;
            case 3 -> wordLength = 9 + r;
            default -> wordLength = 5;
        }
        int selection = random.nextInt(words[wordLength - 4].length); //random index from word list of wanted wordLength
        defeatWord = words[wordLength - 4][selection];
    }

    /**
     * Update the time and get how much time is left
     *
     * @return a fraction of how far along the interval the enemy is: time gone/time interval. <b>returning a negative means it's time to attack (only get the this once per atk)</b>
     */
    public double getInterval() {
        double currentTime = System.currentTimeMillis();

        if (freezeActive) {
            if (!wasFrozen) { //was not already frozen
                freezeStartTime = currentTime;
                wasFrozen = true;
            }
            // lock progress exactly where it froze
            return (freezeStartTime - prevTime) / interval;
        }

        if (wasFrozen) {
            double freezeDuration = currentTime - freezeStartTime;
            prevTime += freezeDuration;   // shift timeline forward
            wasFrozen = false;

            currentTime = System.currentTimeMillis();
        }

        double passed = currentTime - prevTime;

        if (passed > interval) { //time passed more than the time of the interval: now time to attack
            prevTime += interval;
            return 1;
        }

        return passed / interval;
    }

    //for spawning new enemies during freeze

    /**
     * Resets the attack timer to 0 and makes them not frozen.
     *
     * @param now the current time
     */
    public void resetAttackTimer(long now) {
        prevTime = now;
        wasFrozen = false;
        freezeStartTime = 0;
    }

    /**
     * Sets the position of this enemy, 0 is first position.
     *
     * @param position the numeric position to set for this enemy.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Gets the time interval for this enemy's attack in milliseconds.
     *
     * @return the enemy attack interval in milliseconds.
     */
    public double getAttackIntervalMs() {
        return interval;
    }

    /**
     * Tells enemies whether or not the freeze power up is on, which stops the enemies from continuing their attack timer.
     * If wasn't frozen, mark the time of freezing to subtract later.
     *
     * @param state of the freeze power on/off -> true/false
     */
    public static void freezeUse(boolean state) {
        freezeActive = state;
    }

    /**
     * Gets the position of this enemy, 0 is first position.
     *
     * @return the numeric position of this enemy.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Gets the difficulty of this enemy (1-3).
     *
     * @return The difficulty of this enemy.
     */
    public int getDifficulty() {
        return difficulty;
    }

    /**
     * Gets the word needed to be typed to defeat this enemy.
     *
     * @return The defeat word of this enemy.
     */
    public String getDefeatWord() {
        return defeatWord;
    }

    /**
     * Gets the damage of the enemy.
     *
     * @return The damage of the enemy.
     */
    public int getDamage() {
        return damage;
    }
}
