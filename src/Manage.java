/**
 * This class represents a parental control account manage dashboard.
 * This class extends {@link PlayerLogin}.
 *
 * @author Leon Li
 * @version 1.1
 * @see PlayerLogin
 */
public class Manage extends PlayerLogin {


    /**
     * Creates a new Manage object.
     * This constructor is intended to be used only by {@link FileManager#getLogin(String)}.
     *
     * @param username The username of the Manage object to be created.
     * @param password The password of the Manage object to be created.
     * @throws IllegalArgumentException if the length of the security question answers array is not the same as the length of the security questions array.
     */
    public Manage(String username, String password, String[] securityQuestionAnswers) throws IllegalArgumentException {
        super(username, password, securityQuestionAnswers);
    }


    /**
     * Changes the password of an account.
     *
     * @param username    The ID of the account whose password is to be changed.
     * @param newPassword The new password.
     * @return True if the password was successfully reset, false otherwise.
     */
    public boolean resetPlayerPassword(String username, String newPassword) {
        try {
            PlayerStatistics stats = FileManager.getStatistic(username);
            stats.password = newPassword;
            FileManager.writeStatistic(stats);
            return true;
        } catch (Exception e) {
            System.out.println("Error resetting password: " + e);
            return false;
        }
    }

    /**
     * Resets the statistics of an account to the default statistics.
     *
     * @param accountID The ID of the account whose statistics are to be reset.
     * @return True if the statistics were successfully reset, false otherwise.
     */
    public boolean resetPlayerStatistics(String accountID) {
        try {
            PlayerStatistics stats = FileManager.getStatistic(accountID);
            if (stats == null) {
                return false;
            }
            FileManager.writeLogin(stats.username, stats.password, stats.securityQuestions, stats.parentalControl);
            return true;
        } catch (Exception e) {
            System.out.println("Error resetting statistics: " + e);
            return false;
        }
    }
}
