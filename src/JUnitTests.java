import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class for testing the code for other classes using JUnit5.
 *
 * @author Leon Li
 * @version 1.3
 */
class JUnitTests {

    /// Test statistics.
    PlayerStatistics testStats;

    /**
     * Sets up {@code testStats} for JUnit testing.
     */
    @BeforeEach
    void setUp() {
        Enemy.setUp();
        testStats = new PlayerStatistics();
        testStats.accuracy = 23.05f;
        testStats.errorCount = 15;
        testStats.highScore = -5;
        testStats.levelsCompleted = 2;
        testStats.username = "Very Creative Username";
        testStats.parentalControl = false;
        testStats.securityQuestions = new String[]{"a", "b", "c"};
    }

    /**
     * Cleans up resources after JUnit testing.
     */
    @AfterEach
    void tearDown() {
        // Close anything that needs closing
    }


    /**
     * Tests the setting of the defeat word for the Enemy class.
     */
    @Test
    void testEnemyDefeatWord() {
        Enemy enemy = new Enemy(3);
        System.out.println(enemy.getDefeatWord().length());
        assertTrue(enemy.getDefeatWord().length() == 9 || enemy.getDefeatWord().length() == 10 || enemy.getDefeatWord().length() == 11);
    }

    /**
     * Tests the writing and reading of the statistics in the FileManager class.
     */
    @Test
    void testStatisticsInFileManager() {
        try {

            FileManager.writeStatistic(testStats);
            PlayerStatistics result = FileManager.getStatistic("Very Creative Username");
            assertEquals(testStats.errorCount, result.errorCount);
        } catch (Exception e) {

            fail();
        }
    }

    /**
     * Tests the word list retrieving functionality in FileManager.
     */
    @Test
    void testWordsListInFileManager() {
        try {
            assertTrue(FileManager.getWordList("5 letter")[2].equals("quest") && FileManager.getWordList("6 letter")[3].equals("abroad") && FileManager.getWordList("10 letter")[3].equals("basketball"));
        } catch (Exception e) {
            System.out.println(e);
            fail();
        }
    }

    /**
     * Test finding statistics for an account that doesn't exist, it should throw an {@link FileManager.IDNotFoundException}.
     */
    @Test
    void testFindNonExistentStatistics() {
        assertThrows(FileManager.IDNotFoundException.class, () -> FileManager.getStatistic("usgdfbauigfd"));
    }

    /**
     * Test writing and reading a player login.
     */
    @Test
    void testPlayerLogin() {
        try {
            FileManager.writeLogin("creative username", "very-strong-password-123", new String[]{" Green   ", "3", "SFD"}, false);
            PlayerLogin playerLogin = FileManager.getLogin("creative username");

            boolean valid = playerLogin != null && playerLogin.checkValid("very-strong-password-123") && playerLogin.checkSecurityQuestion("3", 1);
            assertTrue(valid);
        } catch (Exception e) {
            System.out.println(e);
            fail();
        }
    }


    /**
     * Test resetting player password and player statistics using a parental account.
     */
    @Test
    void testManageScreen() {
        try {
            FileManager.writeLogin("name", "password", new String[]{"sgfdgfs", "sgfd", "sgfd"}, false);
            PlayerStatistics s = FileManager.getStatistic("name");
            s.highScore = 5000;
            FileManager.writeStatistic(s);
            FileManager.writeLogin("the teacher", "g", new String[]{"ret", "gdfs", "sgf"}, true);

            PlayerLogin login = FileManager.getLogin("the teacher");

            if (login.isParental()) {
                ((Manage) login).resetPlayerPassword("name", "x");
                ((Manage) login).resetPlayerStatistics("name");
            }

            PlayerStatistics retrievedStatistics = FileManager.getStatistic("name");
            assertTrue(retrievedStatistics.password.equals("x") && retrievedStatistics.highScore == 0);

        } catch (Exception e) {
            System.out.println(e);
            fail();
        }
    }

    /**
     * Test writing and reading some example highscores.
     */
    @Test
    void testHighScores() {
        try {
            FileManager.updateLeaderboard(new HighScore(9438, "very horrible typer"));
            FileManager.updateLeaderboard(new HighScore(5468432, "slightly less horrible typer"));
            FileManager.updateLeaderboard(new HighScore(33, "creative username"));
            FileManager.updateLeaderboard(new HighScore(55, "Very good Lexibyrinth Player"));

            int highscore = FileManager.getLeaderboard()[0].highscore();
            assertEquals(5468432, highscore);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test creation of a {@link GameLevel} object.
     */
    @Test
    void testGameLevel() {
        GameLevel gameLevel = new GameLevel(2);

        assertEquals(2, gameLevel.getLevelNumber());
    }
}