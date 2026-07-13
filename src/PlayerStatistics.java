/**
 * <p>Represents the statistics for a player.</p>
 *
 * @author Kevin Qi
 * @author Leon Li
 * @version 1.1
 */
@Statistic.ModifiesLocalData
public class PlayerStatistics {

    /// The username.
    public String username;

    /// The password.
    public String password;

    ///  The security questions for this account.
    /// These should be all-lowercase and have leading and trailing whitespace removed, and so should Strings that are compared to these.
    public String[] securityQuestions = new String[PlayerLogin.SECURITY_QUESTIONS.length];

    /// Whether parental control is enabled for this account.
    public boolean parentalControl;

    /// The mean WPM.
    public float meanWPM;

    /// The peak WPM.
    public float peakWPM;

    /// The accuracy.
    public float accuracy;

    /// The error count.
    public int errorCount;

    /// The total time spent typing (in seconds).
    public float timeStat;

    /// The high score.
    public int highScore;

    /// The total number of words typed.
    public int totalWordsTyped;

    /// The number of levels completed.
    public int levelsCompleted;

    /// The level progression.
    public int levelProgression;

    ///  The point balance for the account.
    public int points;

    ///  The number of all the types of powerups this user has.
    public int[] totalPowerups = new int[]{0, 0, 0};

    /**
     * Buys a powerup.
     *
     * @param powerUp The powerup type.
     * @return True if the purchase was successful, false if the user has insufficient point balance.
     */
    public boolean buyPowerup(PowerUp powerUp) {
        if (points < powerUp.cost) {
            return false;
        }

        points -= powerUp.cost;
        totalPowerups[powerUp.ordinal()] += 1;
        return true;

    }


    /**
     * Resets the statistic.
     */
    @Statistic.ModifiesLocalData
    public void reset() {
        meanWPM = 0;
        peakWPM = 0;
        accuracy = 0;
        errorCount = 0;
        timeStat = 0;
        highScore = 0;
        totalWordsTyped = 0;
        levelsCompleted = 0;
        levelProgression = 1;
        points = 0;
        totalPowerups = new int[]{0, 0, 0};
    }
}