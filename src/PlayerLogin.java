
/**
 * <p>Class representing a login attempt from a user.</p>
 * This class is used by the login screen to represent login attempts.
 *
 * @author Leon Li
 * @version 1.0
 */
public class PlayerLogin {


    ///  The security questions to be used by the Frame when logging in.
    public static final String[] SECURITY_QUESTIONS = {
            "What's your favorite color?",
            "What is your mother's maiden name?",
            "What was the name of your first pet?"
    };

    /// The username.
    public final String username;

    ///  The password.
    public final String password;

    ///  The security question answers.
    private final String[] securityQuestionAnswers = new String[SECURITY_QUESTIONS.length];


    /**
     * Creates a new PlayerLogin object.
     * This constructor is intended to be used only by {@link FileManager#getLogin(String)}.
     *
     * @param username The username of the PlayerLogin object to be created.
     * @param password The password of the PlayerLogin object to be created.
     * @throws IllegalArgumentException if the length of the security question answers array is not the same as the length of the security questions array.
     */
    public PlayerLogin(String username, String password, String[] securityQuestionAnswers) throws IllegalArgumentException {
        if (securityQuestionAnswers.length != SECURITY_QUESTIONS.length) {
            throw new IllegalArgumentException("Incorrect length of security question answers");
        }
        this.username = username;
        this.password = password;

        for (int i = 0; i < SECURITY_QUESTIONS.length; i++) {
            this.securityQuestionAnswers[i] = securityQuestionAnswers[i].trim().toLowerCase();
        }


    }

    /**
     * Checks if a login is valid.
     *
     * @param enteredPassword The password entered by the user.
     * @return True if the login is valid, false if the login is invalid or if an exception occurs while checking validity.
     */
    public boolean checkValid(String enteredPassword) {
        return password.equals(enteredPassword);
    }


    /**
     * Checks if given security question answer is valid.
     *
     * @param answer The answer to be checked.
     * @param index  The index of the security question.
     * @return True if the answer is valid, false otherwise.
     */
    public boolean checkSecurityQuestion(String answer, int index) {
        System.out.println(answer + ", " + index);
        if (answer == null) return false;
        return answer.trim().equalsIgnoreCase(securityQuestionAnswers[index].trim());
    }

    /**
     * Determines if the PlayerLogin object is parental.
     *
     * @return True if the PlayerLogin object is parental, false otherwise.
     */
    public boolean isParental() {
        return this instanceof Manage;
    }


    /**
     * Checks if credentials are valid.
     *
     * @param username The username to be checked.
     * @param password The password to be checked.
     * @return True if the credentials are valid, false otherwise.
     */
    public static boolean credentialsValid(String username, String password) {
        PlayerLogin login = FileManager.getLogin(username);
        if (login == null) {
            return false;
        }
        return login.password.equals(password);
    }


}

