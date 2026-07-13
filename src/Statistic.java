import java.util.*;

/**
 * <p>The Statistic class manages the current player's statistics, including WPM, accuracy, errors, and leaderboard rankings. It provides methods to update, calculate, save, and reset statistics for individual players and the overall leaderboard.
 * All data is stored statically and the current account for which the stats represent is swapped out dynamically.</p>
 * For efficiency purposes, {@code current} and {@code leader} should be up-to-date, and file I/O should be performed only occasionally to update the CSV files.
 * Use {@link ModifiesCSVFile} and {@link ModifiesLocalData} to determine if a method updates the local or persistent data (or both).
 * <br><br>Use: <ul>
 * <li>{@link Statistic#getCurrentStats()} and modify fields of the object to update the local statistic data and current user's highscore,</li>
 * <li>{@link Statistic#updateUserStatistics()} to update the statistics CSV file,</li>
 * <li>and {@link Statistic#updateHighScore()} to update the highscores CSV file.</li>
 * </ul>
 *
 * @author Leon Li
 * @version 1.0
 * @implNote The current implementation of this class is incomplete and may require additional methods and fields to fully support all desired functionality.
 * Also, the current implementation is not thread-safe.
 * @see FileManager
 */
public abstract class Statistic {

    /**
     * The annotated object modifies the CSV files.
     */
    public static @interface ModifiesCSVFile {
    }

    /**
     * The annotated object modifies local data.
     */
    public static @interface ModifiesLocalData {
    }

    private static PlayerStatistics current;

    /// The top 10 scores across all users, stored in descending order.
    private static final int[] leader = new int[10]; // top 10 scores


    /**
     * Private constructor to prevent instantiation of this utility class. All methods and fields are static, so there is no need to create an instance of {@code Statistic}.
     */
    private Statistic() {
        throw new UnsupportedOperationException("Statistic is a utility class and cannot be instantiated.");
    }


    /**
     * Load the user's statistics by reading from the CSV file into {@code current}. This method should be called whenever a user logs in or switches accounts to ensure that the displayed statistics are accurate for the current user.
     *
     * @param username The username of the account whose statistics should be loaded.
     * @return True if the statistics were successfully loaded, false otherwise.
     */
    public static boolean loadStatsFromCSV(String username) {
        try {
            current = FileManager.getStatistic(username);
            return true;
        } catch (Exception e) {
            return false;
        }

    }


    /**
     * Reset the leaderboard to its initial state.
     *
     * @return True if the CSV file was successfully cleared, false otherwise.
     */
    @ModifiesCSVFile
    @ModifiesLocalData
    public static boolean resetLeaderboard() {
        Arrays.fill(leader, 0);
        try {
            FileManager.clearLeaderboard();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates the statistics CSV file with the current user's current statistics.
     *
     * @return True if the CSV file was successfully updated, false otherwise.
     */
    @ModifiesCSVFile
    public static boolean updateUserStatistics() {
        if (current == null) {
            return false;
        }
        try {
            FileManager.writeStatistic(current);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reset the current user's statistics to their initial values.
     *
     * @return True if the CSV file was successfully updated, false otherwise.
     * @implNote This method is only intended to be used by administrators and should not be used in normal program flow.
     */
    @ModifiesCSVFile
    @ModifiesLocalData
    public static boolean resetStat() { // Do we even need this? Only parental accounts can reset stats, and those use Manage.resetPlayerStatistics().

        current.reset();

        try {
            FileManager.writeStatistic(current);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the current user's stats.
     *
     * @return The current user's stats.
     */
    public static PlayerStatistics getCurrentStats() {
        return current;
    }

    /**
     * Updates the high score in the CSV file.
     *
     * @return True if the CSV file was successfully updated, false otherwise.
     */
    @ModifiesCSVFile
    public static boolean updateHighScore() {
        if (current == null) return false;
        try {
            FileManager.updateLeaderboard(current.highScore, current.username);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}