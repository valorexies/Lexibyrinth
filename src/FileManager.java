import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * <p>Class for managing file I/O between the main application and the CSV files.</p>
 * This class is responsible for reading and writing player statistics, high scores, and word lists to and from the corresponding CSV files.
 *
 * @author Leon Li
 * @version 1.5
 * @implNote Since file I/O is inefficient, this class and its methods are to be used only for managing persistent data storage and should not be used for cached data storage.
 * @see java.io
 */
@Statistic.ModifiesCSVFile
public abstract class FileManager {


    /**
     * Custom exception to indicate that a requested ID (username or word list ID) was not found in the corresponding CSV file.
     */
    public static class IDNotFoundException extends Exception {

        /**
         * Create a new IDNotFoundException with the specified error message.
         *
         * @param message The error message.
         */
        public IDNotFoundException(String message) {
            super(message);
        }
    }

    /// The file path for the accounts CSV file.
    public static final Path ACCOUNT_FILE = Paths.get("src", "FILES_ALL", "accounts.csv");
    public static final Path HIGH_SCORES_FILE = Paths.get("src", "FILES_ALL", "highscores.csv");
    public static final Path WORDS_FILE = Paths.get("src", "FILES_ALL", "WORDS_FILE.csv");

    /**
     * Private constructor for the FileManager class to prevent initialization.
     */
    private FileManager() {
        throw new UnsupportedOperationException("FileManager is a utility class and cannot be instantiated");
    }


    /**
     * Retrieves a word list based on a word list ID.
     *
     * @param wordListID The ID of the word list to retrieve.
     * @return An array of the words in the word list corresponding to the given ID.
     * @throws IDNotFoundException If the word list ID is not found in the word list CSV file.
     * @throws IOException         If an I/O error occurs while reading the word list CSV file.
     * @throws Exception           If any other exception occurs while retrieving the word list.
     */
    static String[] getWordList(String wordListID) throws Exception {

        try (BufferedReader reader = new BufferedReader(new FileReader(WORDS_FILE.toString()))) {

            String line;
            while ((line = reader.readLine()) != null) {

                String[] data = line.split(",");

                if (data[0].equals(wordListID)) {
                    return Arrays.copyOfRange(data, 1, data.length);
                }
            }
        }

        throw new IDNotFoundException("Word list ID not found: " + wordListID);
    }


    /**
     * Creates a new account with the statistics initialized to default values and writes the account information to the accounts CSV file.
     * If an entry with the same account ID already exists, it will be overwritten with its statistics reset.
     * Note that this method takes the information about the account as individual parameters, not a login object. PlayerLogin objects are only to be created from loading CSV data, not when writing new login data.
     *
     * @param username          The username for the new account.
     * @param password          The password for the new account.
     * @param securityQuestions The security question answers for the new account.
     * @param parental          Whether parental control is enabled for the new account.
     * @return True if the statistic already existed, false if a new one was created.
     * @throws IOException If an I/O error occurs while writing to the accounts CSV file.
     * @throws Exception   If any other exception occurs while retrieving the login information.
     */
    static boolean writeLogin(String username, String password, String[] securityQuestions, boolean parental) throws Exception {

        PlayerStatistics stats = new PlayerStatistics();

        stats.username = username;
        stats.password = password;
        stats.parentalControl = parental;

        for (int i = 0; i < securityQuestions.length; i++) {
            stats.securityQuestions[i] = securityQuestions[i].trim().toLowerCase(Locale.ROOT);
        }
        stats.meanWPM = 0.0f;
        stats.peakWPM = 0.0f;
        stats.accuracy = 0.0f;
        stats.errorCount = 0;
        stats.timeStat = 0.0f;
        stats.highScore = 0;
        stats.totalWordsTyped = 0;
        stats.levelsCompleted = 0;
        stats.levelProgression = 1;

        return writeStatistic(stats);

    }


    /**
     * Writes the given player statistics to the accounts CSV file. If an entry with the same account ID already exists, it will be overwritten with the new statistics.
     *
     * @param stats The PlayerStatistics object containing the statistics to be written to the accounts CSV file.
     * @return True if the statistic already existed, false if a new one was created.
     * @throws IOException If an I/O exception occurs while writing to the accounts CSV file.
     * @throws Exception   If any other exception occurs while writing to the accounts CSV file.
     */
    static boolean writeStatistic(PlayerStatistics stats) throws Exception {

        File file = new File(ACCOUNT_FILE.toString());
        if (!file.exists()) {
            throw new FileNotFoundException("Account file does not exist");
        }
        List<String> lines = new ArrayList<>();

        boolean found = false;

        String n = String.join(",",
                stats.username,
                stats.password,
                Boolean.toString(stats.parentalControl),
                Float.toString(stats.meanWPM),
                Float.toString(stats.peakWPM),
                Float.toString(stats.accuracy),
                Integer.toString(stats.errorCount),
                Float.toString(stats.timeStat),
                Integer.toString(stats.highScore),
                Integer.toString(stats.totalWordsTyped),
                Integer.toString(stats.levelsCompleted),
                Integer.toString(stats.levelProgression),
                Integer.toString(stats.points),
                Integer.toString(stats.totalPowerups[0]),
                Integer.toString(stats.totalPowerups[1]),
                Integer.toString(stats.totalPowerups[2])
        );


        StringBuilder q = new StringBuilder();

        for (int i = 0; i < PlayerLogin.SECURITY_QUESTIONS.length; i++) {
            q.append(stats.securityQuestions[i] == null ? "" : stats.securityQuestions[i]);
            if (i < PlayerLogin.SECURITY_QUESTIONS.length - 1) {
                q.append(",");
            }
        }

        String newLine = n + "," + q;


        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data = line.split(",");

                if (data[0].equals(stats.username)) {
                    lines.add(newLine);
                    found = true;
                } else {
                    lines.add(line);
                }
            }
        }


        if (!found) {
            lines.add(newLine);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {

            for (String l : lines) {
                writer.write(l);
                writer.newLine();
            }
        }
        return found;
    }


    /**
     * Retrieves the player statistics for a given account ID from the accounts CSV file.
     *
     * @param username The username of the player whose statistics are to be retrieved.
     * @return The PlayerStatistics object containing the retrieved statistics.
     * @throws IDNotFoundException If the username is not found in the accounts CSV file.
     * @throws IOException         If an I/O exception occurs while reading from the accounts CSV file.
     * @throws Exception           If any other exception occurs while reading from the accounts CSV file.
     */
    static PlayerStatistics getStatistic(String username) throws Exception {

        try (BufferedReader reader = new BufferedReader(new FileReader(ACCOUNT_FILE.toString()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data = line.split(",");

                if (data[0].equals(username)) {

                    PlayerStatistics stats = new PlayerStatistics();


                    stats.username = data[0];
                    stats.password = data[1];
                    stats.parentalControl = Boolean.parseBoolean(data[2]);
                    stats.meanWPM = Float.parseFloat(data[3]);
                    stats.peakWPM = Float.parseFloat(data[4]);
                    stats.accuracy = Float.parseFloat(data[5]);
                    stats.errorCount = Integer.parseInt(data[6]);
                    stats.timeStat = Float.parseFloat(data[7]);
                    stats.highScore = Integer.parseInt(data[8]);
                    stats.totalWordsTyped = Integer.parseInt(data[9]);
                    stats.levelsCompleted = Integer.parseInt(data[10]);
                    stats.levelProgression = Integer.parseInt(data[11]);
                    stats.points = Integer.parseInt(data[12]);
                    stats.totalPowerups[0] = Integer.parseInt(data[13]);
                    stats.totalPowerups[1] = Integer.parseInt(data[14]);
                    stats.totalPowerups[2] = Integer.parseInt(data[15]);

                    for (int i = 0; i < PlayerLogin.SECURITY_QUESTIONS.length; i++) {
                        stats.securityQuestions[i] = data.length < 16 + i + 1 ? "" : data[16 + i];
                    }
                    return stats;
                }
            }
        } catch (Exception e) {
            return null;
        }

        throw new IDNotFoundException("Username not found: " + username);
    }

    /**
     * Retrieves the current leaderboard from the high scores CSV file.
     *
     * @return An array of HighScore objects representing the current leaderboard, sorted in descending order by score.
     * @throws IOException If an I/O exception occurs while reading from the high scores CSV file.
     * @throws Exception   If any other exception occurs while reading from the high scores CSV file.
     */
    static HighScore[] getLeaderboard() throws Exception {

        HighScore[] leaderboard = new HighScore[10];

        try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORES_FILE.toString()))) {

            String line;
            int i = 0;

            while ((line = reader.readLine()) != null && i < leaderboard.length) {

                String[] data = line.split(",");

                int score = Integer.parseInt(data[0]);
                String username = data[1];

                leaderboard[i] = new HighScore(score, username);
                i++;
            }
        }

        return leaderboard;
    }

    /**
     * Updates the leaderboard with a new high score entry. This method overloads {@link #updateLeaderboard(int, String)}.
     *
     * @param newEntry The new high score entry to be added to the leaderboard.
     * @throws Exception if an exception occurs while modifying the high scores file.
     * @implNote If the new entry's score is lower than all the scored in the current leaderboard, nothing is changed.
     */
    static void updateLeaderboard(HighScore newEntry) throws Exception {
        updateLeaderboard(newEntry.highscore(), newEntry.username());
    }

    /**
     * Updates the leaderboard with a new high score entry.
     *
     * @param newScore The score of the new high score entry to be added to the leaderboard.
     * @param username The username of the new high score entry to be added to the leaderboard.
     * @throws Exception if an exception occurs while modifying the high scores file.
     * @implNote If the new entry's score is lower than all the scored in the current leaderboard, nothing is changed.
     */
    static void updateLeaderboard(int newScore, String username) throws Exception {

        Map<String, Integer> bestScores = new HashMap<>();

        // Read existing leaderboard
        try (BufferedReader reader = new BufferedReader(new FileReader(HIGH_SCORES_FILE.toString()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data = line.split(",");
                int score = Integer.parseInt(data[0]);
                String user = data[1];

                bestScores.put(user, Math.max(bestScores.getOrDefault(user, 0), score));
            }
        }

        // Insert/update the player's score
        bestScores.put(username, Math.max(bestScores.getOrDefault(username, 0), newScore));

        // Convert to HighScore objects
        List<HighScore> leaderboard = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : bestScores.entrySet()) {
            leaderboard.add(new HighScore(entry.getValue(), entry.getKey()));
        }

        // Sort descending by score
        leaderboard.sort((a, b) -> Integer.compare(b.highscore(), a.highscore()));

        // Write top 10 back to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORES_FILE.toString()))) {

            for (int i = 0; i < Math.min(10, leaderboard.size()); i++) {

                HighScore hs = leaderboard.get(i);

                writer.write(hs.highscore() + "," + hs.username());
                writer.newLine();
            }
        }
    }

    public static void clearLeaderboard() throws Exception {
        new FileWriter(HIGH_SCORES_FILE.toString(), false).close();
    }

    /**
     * Gets a login object from the accounts CSV data.
     *
     * @param username The username of the account to create a login object for.
     * @return A PlayerLogin object with the login information of the account with the given ID, or null if the ID is not found or if an exception occurs while reading the CSV file.
     * @see PlayerLogin
     */
    public static PlayerLogin getLogin(String username) {
        try {
            PlayerStatistics stats = getStatistic(username);

            if (stats == null) {

                return null;
            }

            if (stats.parentalControl) {
                return new Manage(stats.username, stats.password, stats.securityQuestions);
            }
            return new PlayerLogin(stats.username, stats.password, stats.securityQuestions);
        } catch (Exception e) {
            return null;
        }

    }

}
