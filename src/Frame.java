import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;    
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * <p>The GUI for Lexibyrinth.</p>
 * @version 20.0
 * @author Alan Lu
 * @author Alexia Lee
 * @author Eric Zhang
*/

public class Frame extends JPanel{

    // ---------- constants ----------
    /** title bar colour */
    private static final Color PURPLE = new Color(123, 50, 255);
    /** default background colour */
    private static final Color BG_DARK = Color.decode("#333333");
    /** login screen background colour */
    private static final Color BG_LOGIN = Color.decode("#333333");
    /** default text colour */
    private static final Color TEXT_LIGHT = Color.decode("#D9D9D9");
    /** background colour for input fields */
    private static final Color INPUT_BG = Color.decode("#B2B2B2");

    /** button width */
    private static final int BUTTON_W = 350;
    /** button height */
    private static final int BUTTON_H = 60;
    /** standard form width */
    private static final int FORM_W = 400;
    /** large form width */
    private static final int LARGE_FORM_W = 500;
    /** wide menu button width */
    private static final int WIDE_BUTTON_W = 800;
    /** wide menu button height */
    private static final int WIDE_BUTTON_H = 60;
    /** reference width for level background */
    private static final int LVLBG_REF_W = 1536;
    /** reference height for level background */
    private static final int LVLBG_REF_H = 1024;


    // ---------- fields ----------
    /** main frame */
    private JFrame frame;
    /** content panel */
    private JPanel content;
    /** custom font */
    private static Font customFont;
    /** screen width */
    private int screenWidth;
    /** screen height */
    private int screenHeight;

    private Timer timer;

    private Screen currentScreen = null;
    /** screen to display after login */
    private Screen afterLoginScreen = null;
    /** user for password reset */
    private String passwordResetUser = null;
    /** calls the manage class */
    private Manage manage;
    private int level;

    // ---------- cached assets ----------
    /** window icon image used for the application's frame icon. */
    private Image appWindowIcon;
    /** small icon shown in the custom title bar. */
    private ImageIcon titleBarIcon;
    /** main logo shown on the main menu. */
    private ImageIcon mainLogoIcon;
    /** image icon used for standard menu buttons. */
    private ImageIcon menuButtonIcon;
    /** image icon used for wider buttons. */
    private ImageIcon wideButtonIcon;
    /** back button icon. */
    private ImageIcon backButtonIcon;
    /** ok button image */
    private ImageIcon okButtonIcon;
    /** popup background image */
    private Image popupBackground;
    /** main menu background image */
    private ImageIcon mainBackgroundIcon;
    /** background image */
    private ImageIcon backgroundIcon;
    /** 3 door selection image in level selection */
    private ImageIcon doorSelectionIcon;
    /** door open image */
    private ImageIcon doorOpenIcon;
    /** closed chest image */
    private ImageIcon chestClosedIcon;
    /** open chest image */
    private ImageIcon chestOpenIcon;
    /** background image for the shop screen */
    private ImageIcon shopBackgroundIcon;
    /** table for power up in shop */
    private ImageIcon tableIcon;
    /** freeze power up image */
    private ImageIcon freezePowerIcon;
    /** double power-up image */
    private ImageIcon doublePowerIcon;
    /** heal power image */
    private ImageIcon healPowerIcon;
    private ImageIcon defeatBackgroundIcon;
    private ImageIcon victoryBackgroundIcon;
    private Image battleFieldImg1;
    private Image battleFieldImg2;
    private Image battleFieldImg3;
    private Image battleFieldImg;

    // ---------- objects ----------
    /** current login information for the logged in player. */
    private PlayerLogin login;
    /** current game level */
    private GameLevel gamelevel ;
    /** sprites loading at the start of application run */
    private Sprites sprites;

    /**
     * gets the current game level
     * @return the current {@link GameLevel}
     */
    public GameLevel getGameLevel() {
        return gamelevel;
    }
    /**
     * determines if the user is currently outside gameplay screen
     * @return {@code true} if the user is currently in a screen other than the gameplay screen, {@code false} otherwise
     */
    public boolean isInGameplay(){
        return currentScreen != Screen.NEW_GAME; 
    }

    // ---------- entry point ----------
    /**
     * the main method that launches Lexibyrinth
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Frame().start());
    }

    // ---------- init ----------
    /**
     * initializes the frame, loads resources, sets up scheduled statistical updates, and renders the main menu
     */
    private void start() {
        ScheduledExecutorService scheduledExecutorService =  Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

        int x = 0;

            @Override
            public void run() {
                PlayerStatistics current = Statistic.getCurrentStats();
                if (current != null) {
                    current.timeStat = current.timeStat + 5;
                    x++;
                    if (x >= 5) {
                        x = 0;
                        Statistic.updateUserStatistics();
                    }
                }
            }

        },5000,5000, TimeUnit.MILLISECONDS);

        KeyboardManager.setup(this);
        Enemy.setUp();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screen.width;
        screenHeight = screen.height;

        loadFont();
        loadAssets();

        frame = new JFrame("Lexibyrinth");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());

        setAppIcon();

        //sprites
        sprites = new Sprites();
        Sprites.loadAll(screenWidth, screenHeight);

        content = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentScreen == Screen.GAMEPLAY) {
                    g.drawImage(battleFieldImg, 0, 0, getWidth(), getHeight(), this);
                    gamelevel.draw(g, getWidth(), getHeight());








                }
            }
        };
        content.setBackground(BG_DARK);
        content.setDoubleBuffered(true);

        frame.add(createTitleBar(), BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);

        render(Screen.MAIN_MENU);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    // ---------- assets ----------
    /** 
     * loads the custom font, which is used throughout the game
     * if custom font fails to load, falls back to a default serif font
     */
    private void loadFont() {
        try {
            InputStream fontStream = getClass().getResourceAsStream("/Quintessential/Quintessential-Regular.ttf");
            if (fontStream != null) {
                customFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(16f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(customFont);
            } else {
                customFont = new Font("Serif", Font.PLAIN, 16);
            }
        } catch (Exception e) {
            e.printStackTrace();
            customFont = new Font("Serif", Font.PLAIN, 16);
        }
    }
    /**
     * loads assets used in the game and stores them in fields for later use
     */
    private void loadAssets() {
        appWindowIcon = loadImage("/Assets/Icon.png");
        titleBarIcon = loadScaledIcon("/Assets/Icon.png", 24, 24);
        mainLogoIcon = loadScaledIcon("/Assets/Icon.png", 650, 500);
        menuButtonIcon = loadScaledIcon("/Assets/menubutton.png", BUTTON_W, 120);
        wideButtonIcon = loadScaledIcon("/Assets/menubutton.png", WIDE_BUTTON_W, WIDE_BUTTON_H);
        backButtonIcon = loadScaledIcon("/Assets/backbutton.png", 160, 120);
        okButtonIcon = loadScaledIcon("/Assets/okbutton.png", 130, 50);
        popupBackground = loadImage("/Assets/optionpane.png");
        mainBackgroundIcon = loadScaledIcon("/Assets/mainbackground.png", screenWidth, screenHeight);
        backgroundIcon = loadScaledIcon("/Assets/background.png", screenWidth, screenHeight);
        doorSelectionIcon = loadScaledIcon("/Assets/doorSelection.png", screenWidth, screenHeight);
        doorOpenIcon = loadScaledIcon("/Assets/openDoor.png", 250, 420);
        chestClosedIcon = loadScaledIcon("/Assets/chestClosed.png", 300, 200);
        chestOpenIcon = loadScaledIcon("/Assets/chestOpen.png", 300, 200);
        shopBackgroundIcon = loadScaledIcon("/Assets/shopBackground.png", screenWidth, screenHeight);
        tableIcon = loadScaledIcon("/Assets/table.png", 250, 300);
        freezePowerIcon = loadScaledIcon("/Assets/freezePower.png", 100,100);
        doublePowerIcon = loadScaledIcon("/Assets/doublePower.png", 100,100);
        healPowerIcon = loadScaledIcon("/Assets/healPower.png", 100,100);
        defeatBackgroundIcon = loadScaledIcon("/Assets/defeatBackground.png", screenWidth, screenHeight);
        victoryBackgroundIcon = loadScaledIcon("/Assets/victoryBackground.png", screenWidth, screenHeight);
        battleFieldImg1 = loadImage("/Assets/battlefield.png");
        battleFieldImg2 = loadImage("/Assets/battlefield2.png");
        battleFieldImg3 = loadImage("/Assets/battlefield3.png");
    }

    /**
     * loads an image from the given resource path
     * 
     * @param path the resource path of the image to be loaded
     * @return the loaded {@link Image}, or {@code null} if the image fails to load
     */
    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        return (url == null) ? null : new ImageIcon(url).getImage();
    }

    /**
     * loads an image from the given resource path and scales it to the specified width and height
     * @param path the resource path of the image to be loaded
     * @param width the target width
     * @param height the target height
     * @return the scaled image {@link ImageIcon} or {@code null} if the image fails to load
     */
    private ImageIcon loadScaledIcon(String path, int width, int height) {
        URL url = getClass().getResource(path);
        if (url == null) return null;

        Image scaled = new ImageIcon(url)
                .getImage()
                .getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * sets the application window icon using the loaded appWindowIcon image, if it was successfully loaded
     */
    private void setAppIcon() {
        if (appWindowIcon != null) {
            frame.setIconImage(appWindowIcon);
        }
    }

    // ---------- navigation ----------
    /**
     * navigates to the specified screen, updating the current screen and rendering the new screen's components
     * @param nextScreen the screen to navigate to
     */
    private void navigateTo(Screen nextScreen) {
        if (currentScreen != null && currentScreen != nextScreen) {

        }
        render(nextScreen);
    }

    /**
     *  navigates back to the previous screen, if there is one, and renders the previous screen's components
     */
    private void goBack() {
        Screen s = currentScreen.previous;
        if(currentScreen==Screen.GAMEPLAY){
            stopGameLoop(); //kill game
        }
        if (s != null) {render(s);}
    }

    /**
     * clears the current content and renders the components for the specicified screen and adds the back button
     * 
     * @param screen the {@link Screen} to render
     */
    private void render(Screen screen) {
        currentScreen = screen;
        content.removeAll();
        content.setBackground(backgroundFor(screen));

        if (screen != Screen.MAIN_MENU && screen != Screen.PLAYER && screen != Screen.CREATE_ADMIN && screen != Screen.CREATE_STUDENT) {
            addBackButton();
        }

        switch (screen) {
            case MAIN_MENU:
                buildMainMenu();
                break;
            case LOGIN:
                buildLoginScreen();
                break;
            case PLAYER:
                buildPlayerScreen();
                break;
            case CREATE_ADMIN:
                buildCreateAdminScreen();
                break;
            case FORGOT_PASSWORD:
                buildForgotPasswordScreen();
                break;
            case RESETFORGOT_PASSWORD:
                buildResetForgotPasswordScreen();
                break;
            case HIGH_SCORES:
                buildHighScoresScreen();
                break;
            case PARENTAL_CONTROLS:
                buildParentalControlsScreen();
                break;
            case ACCOUNT_MANAGEMENT:
                buildAccountManagementScreen();
                break;
            case CREATE_STUDENT:
                buildCreateStudentAccountScreen();
                break;
            case CHOOSE_STUDENT_PASSWORD_RESET:
                chooseStudentPasswordResetScreen();
                break;
            case RESET_PASSWORD:
                buildResetPasswordScreen();
                break;
            case RESET_STATISTICS:
                buildResetStatisticsScreen();
                break;
            case RESET_HIGH_SCORES:
                buildResetHighScoreScreen();
                break;
            case TUTORIAL:
                buildTutorialScreen();
                break;
            case PLAYER_STATISTICS:
                buildPlayerStatisticsScreen();
                break;
            case SELECT_LEVEL:
                buildSelectLevelScreen();
                break;
            case NEW_GAME:
                buildNewGameScreen();
                break;
            case SHOP:
                buildShopScreen();
                break;
            case TYPING_STATISTICS:
                buildTypingStatisticsPage();
                break;
            case GAMEPLAY:
                buildGameplayScreen(level);
                break;
        }
        refreshContent();
    }

    /**
     * returns the appropriate background colour
     * @param screen the screen for which to return a background colour
     * @return the background {@link Color} for the specified screen
     */
    private Color backgroundFor(Screen screen) {
        switch (screen) {
            case LOGIN:
                return BG_LOGIN;
            default:
                return BG_DARK;
        }
    }

    public void startGameLoop(GameLevel gamelvl) {
        timer = new Timer(16, e -> update(gamelvl));
        timer.start();
    }

    public void stopGameLoop() {
        if (timer != null) {
            timer.stop();
        }
    }

    public void update(GameLevel gamelvl) {
        gamelvl.update();
        content.repaint();
        if(gamelvl.isLevelCompleted()){
            Statistic.getCurrentStats().points+=gamelvl.getScore();
            showGameLevelCompleted(true);
        }
        else if(gamelvl.isLevelFailed()){
            showGameLevelCompleted(false);
        }
    }

    // ---------- title bar ----------
    /**
     * creates a custom title bar with the game title and a close button
     * @return the title bar panel
     */
    private JPanel createTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(PURPLE);
        titleBar.setPreferredSize(new Dimension(0, 30));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        left.setOpaque(false);

        if (titleBarIcon != null) {
            left.add(new JLabel(titleBarIcon));
        }

        JLabel titleLabel = new JLabel("Lexibyrinth");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Papyrus", Font.BOLD, 16));
        left.add(titleLabel);

        JButton close = new JButton("✕");
        close.setForeground(Color.WHITE);
        close.setBackground(PURPLE);
        close.setOpaque(true);
        close.setContentAreaFilled(true);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.addActionListener(e -> System.exit(0));
        close.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { close.setBackground(Color.RED); }
            @Override public void mouseExited(MouseEvent e) { close.setBackground(PURPLE); }
        });

        titleBar.add(left, BorderLayout.WEST);
        titleBar.add(close, BorderLayout.EAST);
        return titleBar;
    }

    // ---------- screens ----------
    /**
     * builds the main menu screen and leads to the appropriate screens based on user selection
     */
    private void buildMainMenu() {

        int cx = centerX(BUTTON_W);

        addMenuButton("Login", cx, 400, () -> navigateTo(Screen.LOGIN));
        addMenuButton("View High Scores", cx, 460, () -> navigateTo(Screen.HIGH_SCORES));
        addMenuButton("Tutorial", cx, 520, () -> navigateTo(Screen.TUTORIAL));
        addMenuButton("Parental Controls", cx, 580, () -> {
            navigateTo(Screen.LOGIN);
            afterLoginScreen = Screen.PARENTAL_CONTROLS;
        }); 

        addMenuButton("Exit", cx, 640, this::showExitDialog);
        
        JLabel credits = new JLabel("<html>Game developed by Team 78: <br> Alexia Lee, Alan Lu, Eric <br> Zhang, Leon Li, and Kevin Qi</html>");
        credits.setBounds(25, 720, 300, 100);
        credits.setForeground(TEXT_LIGHT);
        credits.setFont(customFont.deriveFont(15f));
        content.add(credits);

        JLabel course = new JLabel("<html>Created for CS2212 <br> at Western University <br> Winter 2026</html>");
        course.setBounds(screenWidth - 175, 720, 300, 100);
        course.setForeground(TEXT_LIGHT);
        course.setFont(customFont.deriveFont(15f));
        content.add(course);

        if (mainLogoIcon != null) {
            JLabel logoLabel = new JLabel(mainLogoIcon);
            logoLabel.setBounds(centerX(650), 60, 650, 500);
            content.add(logoLabel);
        }

        if (mainBackgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(mainBackgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    private ActionListener listener ;
    
    /**
     * builds the login screen and handles user login logic, including validation and navigation to the appropriate screen after login
     */
    private void buildLoginScreen() {
        int center = centerX(BUTTON_W);

        JLabel usernameLabel = createLabel("Username", center, 240, 200, 30, 25f);
        JTextField usernameInput = createTextField(center, 280, BUTTON_W, BUTTON_H, 25f);

        JLabel passwordLabel = createLabel("Password", center, 340, 200, 30, 25f);
        JPasswordField passwordInput = createPasswordField(center, 380, BUTTON_W, BUTTON_H, 25f);

        listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameInput.getText().trim();
                String enteredPassword = String.valueOf(passwordInput.getPassword()).trim();

                PlayerLogin candidate = FileManager.getLogin(username);

                if (candidate != null && enteredPassword.equals(candidate.password)) {
                    login = candidate;

                    if (login.isParental()) {
                        manage = (Manage) login;
                    } else {
                        manage = null;
                    }
                    Statistic.loadStatsFromCSV(username);

                    navigateTo(login.isParental() ? Screen.PARENTAL_CONTROLS : Screen.PLAYER);
                } else {
                    showCustomPopup("Login failed.");
                }
            }
        };

        JButton loginButton = createActionButton("Login", center, 450, BUTTON_W, BUTTON_H, listener);

        JLabel createAdmin = createLinkLabel(
                "Create admin account", center, 570, 300, 30, 20f,
                () -> navigateTo(Screen.CREATE_ADMIN)
        );
        JLabel forgotPassword = createLinkLabel(
                "Forgot password", center, 610, 300, 30, 20f,
                () -> navigateTo(Screen.FORGOT_PASSWORD)
        );

        content.add(usernameLabel);
        content.add(usernameInput);
        content.add(passwordLabel);
        content.add(passwordInput);
        content.add(loginButton);
        content.add(createAdmin);
        content.add(forgotPassword);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }
    /**
     * builds the player main menu screen 
     */
    private void buildPlayerScreen() {
        int cx = centerX(BUTTON_W);

        addMenuButton("New Game", cx, 400, () -> navigateTo(Screen.NEW_GAME));
        addMenuButton("Select Level", cx, 460, () -> navigateTo(Screen.SELECT_LEVEL));
        addMenuButton("Typing Statistics", cx, 520, () -> navigateTo(Screen.TYPING_STATISTICS));
        addMenuButton("Logout", cx, 580, () -> navigateTo(Screen.MAIN_MENU));
        addMenuButton("Exit", cx, 640, this::showExitDialog);
        if (mainLogoIcon != null) {
            JLabel logoLabel = new JLabel(mainLogoIcon);
            logoLabel.setBounds(centerX(650), 60, 650, 500);
            content.add(logoLabel);
        }

        if (mainBackgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(mainBackgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the admin creation screen
     */

    private ActionListener registerAdminListener;

    private void buildCreateAdminScreen() {
        int center = centerX(BUTTON_W);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(0, 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }

        JPanel formPanel = new JPanel(null);
        formPanel.setPreferredSize(new Dimension(screenWidth, 1250));
        formPanel.setOpaque(false);

        formPanel.add(createCenteredTitle("Create admin account", 50, 400, 30f));
        
        formPanel.add(createLabel("Username", center, 130, 200, 30, 25f));
        JTextField usernameInput = createTextField(center, 170, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(usernameInput);

        formPanel.add(createLabel("Password", center, 240, 200, 30, 25f));
        JPasswordField passwordInput = createPasswordField(center, 280, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(passwordInput);

        formPanel.add(createLabel("Re-input password", center, 350, 250, 30, 25f));
        JPasswordField passwordReinput = createPasswordField(center, 390, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(passwordReinput);

        formPanel.add(createLabel("Choose a security question 1:", center, 460, 300, 30, 25f));
        JTextField questionSelect = new JTextField("What's your favourite colour?");
        questionSelect.setBounds(center, 500, BUTTON_W, 50);
        questionSelect.setFont(customFont.deriveFont(25f));
        questionSelect.setEditable(false);
        formPanel.add(questionSelect);

        formPanel.add(createLabel("Security Question Answer 1", center, 570, 300, 30, 25f));
        JTextField securityAnswer = createTextField(center, 610, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(securityAnswer);

        formPanel.add(createLabel("Choose a security question 2:", center, 680, 300, 30, 25f));
        JTextField questionSelect2 = new JTextField("What is your mother's maiden name?");
        questionSelect2.setBounds(center, 720, BUTTON_W, 50);
        questionSelect2.setFont(customFont.deriveFont(25f));
        questionSelect2.setEditable(false);
        formPanel.add(questionSelect2);

        formPanel.add(createLabel("Security Question Answer 2", center, 790, 300, 30, 25f));
        JTextField securityAnswer2 = createTextField(center, 830, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(securityAnswer2);

        formPanel.add(createLabel("Choose a security question 3:", center, 900, 300, 30, 25f));
        JTextField questionSelect3 = new JTextField("What was the name of your first pet?");
        questionSelect3.setBounds(center, 940, BUTTON_W, 50);
        questionSelect3.setFont(customFont.deriveFont(25f));
        questionSelect3.setEditable(false);
        formPanel.add(questionSelect3);

        formPanel.add(createLabel("Security Question Answer 3", center, 1010, 300, 30, 25f));
        JTextField securityAnswer3 = createTextField(center, 1050, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(securityAnswer3);

        
        registerAdminListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String username = usernameInput.getText().trim();
                    String password = String.valueOf(passwordInput.getPassword()).trim();
                    String rePassword = String.valueOf(passwordReinput.getPassword()).trim();
                    String answer1 = securityAnswer.getText().trim();
                    String answer2 = securityAnswer2.getText().trim();
                    String answer3 = securityAnswer3.getText().trim();

                    if (username.isEmpty() || password.isEmpty() || rePassword.isEmpty()
                            || answer1.isEmpty() || answer2.isEmpty() || answer3.isEmpty()) {
                        showCustomPopup("Please fill in all fields.");
                        return;
                    }

                    if (!password.equals(rePassword)) {
                        showCustomPopup("Passwords do not match.");
                        return;
                    }

                    String[] securityQuestions = {answer1, answer2, answer3};

                    boolean success = !FileManager.writeLogin(
                        username,
                        password,
                        securityQuestions,
                        true
                    );

                    if (!success) {
                        showCustomPopup("Account created successfully.");
                        navigateTo(Screen.MAIN_MENU);
                    } else {
                        showCustomPopup("Failed to create account.");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showCustomPopup("Error creating account.");
                }
            
            }
        };
        JButton createAccount = createActionButton(
            "Create account", center, 1130, BUTTON_W, BUTTON_H, registerAdminListener);
        
        formPanel.add(createAccount);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBounds(0, 0, screenWidth, screenHeight - 30);
        scrollPane.setBorder(null);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        content.add(scrollPane);


        content.setComponentZOrder(scrollPane, 0);    
        content.setComponentZOrder(content.getComponent(1), 1); 
        JButton backButton = new JButton();

        if (backButtonIcon != null) {
            backButton.setIcon(backButtonIcon);
            backButton.setContentAreaFilled(false);
            backButton.setOpaque(false);
            backButton.setBounds(10, 10, 120, 60);
        } else {
            backButton.setText("← Back");
            backButton.setFont(customFont.deriveFont(Font.PLAIN, 16f));
            backButton.setForeground(Color.WHITE);
            backButton.setBackground(PURPLE);
            backButton.setOpaque(true);
            backButton.setContentAreaFilled(true);
            backButton.setBounds(10, 10, 110, 36);
        }
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> goBack());
        formPanel.add(backButton);
    }

    /**
     * resets admin account
     * builds the forgot password screen and handles user input for password recovery, 
     * including validation of security question answers and navigation to the password reset screen if validation is successful
     */

    private ActionListener forgotPasswordListener;

    private void buildForgotPasswordScreen() {
        int center = centerX(LARGE_FORM_W);

        JLabel instruction = new JLabel("<html>Choose a security question to answer <br> for password recovery:</html>");
        instruction.setBounds(center, 140, 500, 100);
        instruction.setForeground(Color.WHITE);
        instruction.setFont(customFont.deriveFont(Font.PLAIN, 30f));
        content.add(instruction);

        content.add(createLabel("Username:", center, 260, 200, 30, 25f));
        JTextField accountIDInput = createTextField(center, 300, LARGE_FORM_W, 50, 25f);
        content.add(accountIDInput);

        content.add(createLabel("Pick one security question to answer:", center, 380, 500, 30, 25f));
        JComboBox<String> questionSelect = createComboBox(PlayerLogin.SECURITY_QUESTIONS, center, 420, LARGE_FORM_W, 50, 20f);
        content.add(questionSelect);

        content.add(createLabel("Answer:", center, 480, 200, 30, 25f));
        JTextField answerInput = createTextField(center, 520, LARGE_FORM_W, 50, 20f);
        content.add(answerInput);

        forgotPasswordListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String username = accountIDInput.getText().trim();
                    String selectedQuestion = (String) questionSelect.getSelectedItem();
                    String enteredAnswer = answerInput.getText().trim().toLowerCase();

                    if (username.isEmpty() || enteredAnswer.isEmpty()) {
                        showCustomPopup("Please fill in all fields.");
                        return;
                    }
                    PlayerLogin account = FileManager.getLogin(username);

                    if (account == null) {
                        showCustomPopup("Username not found.");
                        return;
                    }

                    boolean correct = false;

                    if (account.checkSecurityQuestion(enteredAnswer, questionSelect.getSelectedIndex())) {
                        correct = true;
                    }

                    if (!correct) {
                        showCustomPopup("Security question or answer is incorrect.");
                        return;
                    } else {
                        navigateTo(Screen.RESETFORGOT_PASSWORD);
                    }

                    passwordResetUser = username;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showCustomPopup("Error verifying account.");
                }
            }
        };

        JButton submitButton = createActionButton(
        "Submit", centerX(BUTTON_W), 620, BUTTON_W, BUTTON_H,forgotPasswordListener);
            
        content.add(submitButton);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the password reset screen for users who have successfully verified their identity through the 
     * forgot password process
     */
    private ActionListener resetForgotPasswordListener;

    private void buildResetForgotPasswordScreen() {
        int center = centerX(LARGE_FORM_W);

        JLabel newPassLabel = new JLabel("Create a new password");
        newPassLabel.setBounds(center, 140, 500, 100);
        newPassLabel.setForeground(Color.WHITE);
        newPassLabel.setFont(customFont.deriveFont(Font.PLAIN, 30f));
        content.add(newPassLabel);

        content.add(createLabel("Enter your new password:", center, 260, 500, 30, 25f));
        JPasswordField newPass = createPasswordField(center, 300, LARGE_FORM_W, 50, 25f);
        content.add(newPass);

        content.add(createLabel("Re-enter your new password:", center,  380, 500, 30, 25f));
        JPasswordField confirmNewPass = createPasswordField(center, 420, LARGE_FORM_W, 50, 20f);
        content.add(confirmNewPass);

        resetForgotPasswordListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String pass1 = String.valueOf(newPass.getPassword()).trim();
                String pass2 = String.valueOf(confirmNewPass.getPassword()).trim();

                if (passwordResetUser == null) {
                    showCustomPopup("No user selected.");
                    return;
                }
                if (pass1.isEmpty() || pass2.isEmpty()) {
                    showCustomPopup("Fill all fields.");
                    return;
                }
                if (!pass1.equals(pass2)) {
                    showCustomPopup("Passwords do not match.");
                    return;
                }

                try {
                    boolean success = new Manage(" ", "", new String[] {"", "",""}).resetPlayerPassword(passwordResetUser, pass1);

                    if (success) {
                        showCustomPopup("Password reset successful!");
                        passwordResetUser = null;
                        navigateTo(Screen.LOGIN);
                    } else {
                        showCustomPopup("Reset failed.");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showCustomPopup("Error resetting password.");
                }
            }
        };

        JButton submit = createActionButton(
        "Submit", centerX(BUTTON_W), 620, BUTTON_W, BUTTON_H, resetForgotPasswordListener);

        content.add(submit);
    
        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
        refreshContent();
    }

    /**
     * builds the high scores screen and displays the leaderboard data in a table format
     */
    private void buildHighScoresScreen() {
        JLabel title = createCenteredTitle("High Scores", 100, 400, 40f);
        content.add(title);

        HighScore[] leaderboard;
        try {
            leaderboard = FileManager.getLeaderboard();
        } catch (Exception e) {
            e.printStackTrace();
            leaderboard = new HighScore[0];
        }

        Object[][] tableData = new Object[leaderboard.length][3];
        for (int i = 0; i < leaderboard.length; i++) {
            HighScore hs = leaderboard[i];
            if (hs != null) {
                tableData[i][0] = i + 1;
                tableData[i][1] = hs.highscore();
                tableData[i][2] = hs.username();
            }
        }

        JTable table = new JTable(tableData, new String[]{"Rank", "Score", "Username"});
        table.setEnabled(false);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(centerX(400), 200, 400, 400);
        content.add(scrollPane);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the parental controls screen, which is only accessible to users with a parent account
     * gives options on player screen, player statistics, account management, and resetting the high score table
     */
    private void buildParentalControlsScreen() {

        if (!login.isParental()) {
            showCustomPopup("Access denied.");
            return;
        }

        int center = centerX(BUTTON_W);
        addMenuButton("Player Screen", center, 200,() -> navigateTo(Screen.PLAYER));
        addMenuButton("Player Statistics", center, 300, () -> navigateTo(Screen.PLAYER_STATISTICS));
        addMenuButton("Account Management", center, 400, () -> navigateTo(Screen.ACCOUNT_MANAGEMENT));
        addMenuButton("Reset High Score Table", center, 500, () -> navigateTo(Screen.RESET_HIGH_SCORES));

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the account management screen, which is only accessible to users with a parent account
     * can create new student accounts, reset student passwords, and reset student statistics
     */
    private void buildAccountManagementScreen() {
        int center = centerX(BUTTON_W);

        JLabel title = createCenteredTitle("Account Management", 100, 450, 50f);
        content.add(title);

        JButton newStudentButton = createActionButton(
                "Create new student account", center, 250, BUTTON_W, BUTTON_H,
                e -> navigateTo(Screen.CREATE_STUDENT)
        );
        newStudentButton.setFont(customFont.deriveFont(Font.PLAIN, 25f));
        content.add(newStudentButton);

        JButton resetPasswordButton = createActionButton(
                "Reset password for student", center, 350, BUTTON_W, BUTTON_H,
                e -> navigateTo(Screen.CHOOSE_STUDENT_PASSWORD_RESET)
        );
        resetPasswordButton.setFont(customFont.deriveFont(Font.PLAIN, 25f));
        content.add(resetPasswordButton);

        JButton resetStatisticsButton = createActionButton(
                "Reset typing statistics", center, 450, BUTTON_W, BUTTON_H,
                e -> navigateTo(Screen.RESET_STATISTICS)
        );
        resetStatisticsButton.setFont(customFont.deriveFont(Font.PLAIN, 25f));
        content.add(resetStatisticsButton);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }
    /**
     * builds the student account creation screen, which is accessible from the account management screen in parental controls
     */
    private ActionListener registerStudentListener;
    private void buildCreateStudentAccountScreen() {
        int center = centerX(BUTTON_W);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(0, 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }

        JPanel formPanel = new JPanel(null);
        formPanel.setPreferredSize(new Dimension(screenWidth, 1250));
        formPanel.setOpaque(false);

        formPanel.add(createCenteredTitle("Create student account", 50, 400, 30f));
        
        formPanel.add(createLabel("Username", center, 130, 200, 30, 25f));
        JTextField usernameInput = createTextField(center, 170, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(usernameInput);

        formPanel.add(createLabel("Password", center, 240, 200, 30, 25f));
        JPasswordField passwordInput = createPasswordField(center, 280, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(passwordInput);

        formPanel.add(createLabel("Re-input password", center, 350, 250, 30, 25f));
        JPasswordField passwordReinput = createPasswordField(center, 390, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(passwordReinput);

        formPanel.add(createLabel("Choose a security question 1:", center, 460, 300, 30, 25f));
        JTextField questionSelect = new JTextField("What's your favourite colour?");
        questionSelect.setBounds(center, 500, BUTTON_W, 50);
        questionSelect.setFont(customFont.deriveFont(25f));
        questionSelect.setEditable(false);
        formPanel.add(questionSelect);

        formPanel.add(createLabel("Security Question Answer 1", center, 570, 300, 30, 25f));
        JTextField securityAnswer = createTextField(center, 610, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(securityAnswer);

        formPanel.add(createLabel("Choose a security question 2:", center, 680, 300, 30, 25f));
        JTextField questionSelect2 = new JTextField("What is your mother's maiden name?");
        questionSelect2.setBounds(center, 720, BUTTON_W, 50);
        questionSelect2.setFont(customFont.deriveFont(25f));
        questionSelect2.setEditable(false);
        formPanel.add(questionSelect2);

        formPanel.add(createLabel("Security Question Answer 2", center, 790, 300, 30, 25f));
        JTextField securityAnswer2 = createTextField(center, 830, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(securityAnswer2);

        formPanel.add(createLabel("Choose a security question 3:", center, 900, 300, 30, 25f));
        JTextField questionSelect3 = new JTextField("What was the name of your first pet?");
        questionSelect3.setBounds(center, 940, BUTTON_W, 50);
        questionSelect3.setFont(customFont.deriveFont(25f));
        questionSelect3.setEditable(false);
        formPanel.add(questionSelect3);

        formPanel.add(createLabel("Security Question Answer 3", center, 1010, 300, 30, 25f));
        JTextField securityAnswer3 = createTextField(center, 1050, BUTTON_W, BUTTON_H, 25f);
        formPanel.add(securityAnswer3);

        registerStudentListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String username = usernameInput.getText().trim();
                    String password = String.valueOf(passwordInput.getPassword()).trim();
                    String rePassword = String.valueOf(passwordReinput.getPassword()).trim();
                    String answer1 = securityAnswer.getText().trim();
                    String answer2 = securityAnswer2.getText().trim();
                    String answer3 = securityAnswer3.getText().trim();

                    if (username.isEmpty() || password.isEmpty() || rePassword.isEmpty()
                            || answer1.isEmpty() || answer2.isEmpty() || answer3.isEmpty()) {
                        showCustomPopup("Please fill in all fields.");
                        return;
                    }

                    if (!password.equals(rePassword)) {
                        showCustomPopup("Passwords do not match.");
                        return;
                    }

                    String[] securityQuestions = { answer1, answer2, answer3 };

                    boolean success = FileManager.writeLogin(
                        username,
                        password,
                        securityQuestions,
                        false
                    );

                    if (!success) {
                        showCustomPopup("Account created successfully.");
                        navigateTo(Screen.MAIN_MENU);
                    } else {
                        showCustomPopup("Failed to create account.");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showCustomPopup("Error creating account.");
                }
            }
        };

        JButton createAccount = createActionButton(
            "Create account", center, 1130, BUTTON_W, BUTTON_H,registerStudentListener);
        
        formPanel.add(createAccount);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBounds(0, 0, screenWidth, screenHeight - 30);
        scrollPane.setBorder(null);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        content.add(scrollPane);

        content.setComponentZOrder(scrollPane, 0);  
        content.setComponentZOrder(content.getComponent(1), 1); 
        
        JButton backButton = new JButton();

        if (backButtonIcon != null) {
            backButton.setIcon(backButtonIcon);
            backButton.setContentAreaFilled(false);
            backButton.setOpaque(false);
            backButton.setBounds(10, 10, 120, 60);
        } else {
            backButton.setText("← Back");
            backButton.setFont(customFont.deriveFont(Font.PLAIN, 16f));
            backButton.setForeground(Color.WHITE);
            backButton.setBackground(PURPLE);
            backButton.setOpaque(true);
            backButton.setContentAreaFilled(true);
            backButton.setBounds(10, 10, 110, 36);
        }
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> goBack());
        formPanel.add(backButton);    
    }

    /**
     * allows parents to select a student account and navigate to the password reset screen for that account
     */

    private void chooseStudentPasswordResetScreen() {
        int center = centerX(BUTTON_W);

        content.add(createLabel("Choose a student account to reset password:", center, 150, 500, 50, 25f));
        JComboBox<String> studentSelect = createComboBox(loadAccountUsernames(), center, 220, LARGE_FORM_W, 50, 20f);
        content.add(studentSelect);

        JButton confirmButton = createActionButton(
                "Confirm", centerX(BUTTON_W), 300, BUTTON_W, BUTTON_H,
                e -> {
                    String selectedUser = (String) studentSelect.getSelectedItem();

                    if (selectedUser == null || selectedUser.equals("No accounts found")) {
                        showCustomPopup("Please select a valid student.");
                        return;
                    }

                    passwordResetUser = selectedUser;
                    navigateTo(Screen.RESET_PASSWORD);
                }
        );
        content.add(confirmButton);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the reset password screen for student accounts in parental controls
     */

    private ActionListener resetPasswordListener;

    private void buildResetPasswordScreen() {
        int center = centerX(LARGE_FORM_W);

        JLabel instruction = new JLabel("<html>Choose a security question to answer <br> for password recovery:</html>");
        instruction.setBounds(center, 140, 500, 100);
        instruction.setForeground(Color.WHITE);
        instruction.setFont(customFont.deriveFont(Font.PLAIN, 30f));
        content.add(instruction);
        
        content.add(createLabel("Pick one security question to answer:", center, 270, 500, 30, 25f));
        JComboBox<String> questionSelect = createComboBox(PlayerLogin.SECURITY_QUESTIONS, center, 310, LARGE_FORM_W, 50, 20f);
        content.add(questionSelect);

        content.add(createLabel("Answer:", center, 380, 200, 30, 25f));
        JTextField answerInput = createTextField(center, 420, LARGE_FORM_W, 50, 20f);
        content.add(answerInput);

        resetPasswordListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String username = passwordResetUser.trim();
                    String selectedQuestion = (String) questionSelect.getSelectedItem();
                    String enteredAnswer = answerInput.getText().trim().toLowerCase();

                    if (username.isEmpty() || enteredAnswer.isEmpty()) {
                        showCustomPopup("Please fill in all fields.");
                        return;
                    }
                    PlayerLogin account = FileManager.getLogin(passwordResetUser);

                    if (account == null) {
                        showCustomPopup("Username not found.");
                        return;
                    }

                    boolean correct = false;

                    if (account.checkSecurityQuestion(enteredAnswer, questionSelect.getSelectedIndex())) {
                        correct = true;
                    }

                    if (!correct) {
                        showCustomPopup("Security question or answer is incorrect.");
                        return;
                    } else {
                        navigateTo(Screen.RESETFORGOT_PASSWORD);
                    }

                    passwordResetUser = username;

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showCustomPopup("Error resetting password.");
                }
            }
        };

        JButton submitButton = createActionButton(
            "Submit", centerX(BUTTON_W), 590, BUTTON_W, BUTTON_H, resetForgotPasswordListener);       
        
        content.add(submitButton);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the reset statistics screen for parental accounts
     */
    private void buildResetStatisticsScreen() {
        int center = centerX(LARGE_FORM_W);

        content.add(createLabel("Choose player account to reset:", center, 150, 500, 50, 25f));
        JComboBox<String> playerSelect = createComboBox(loadAccountUsernames(), center, 220, LARGE_FORM_W, 50, 20f);
        content.add(playerSelect);

        content.add(createLabel("Confirm resetting typing statistics for selected player", center, 300, 800, 50, 25f));
        JButton confirmButton = createActionButton(
                "Confirm", centerX(BUTTON_W), 370, BUTTON_W, BUTTON_H,
                    e -> {String selectedUser = (String) playerSelect.getSelectedItem();

                if (selectedUser == null || selectedUser.equals("No accounts found")) {
                    showCustomPopup("Please select a valid player.");
                    return;
                }

                boolean success = manage.resetPlayerStatistics(selectedUser);

                if (success) {
                    showCustomPopup("Typing statistics reset successfully.");
                } else {
                    showCustomPopup("Failed to reset typing statistics.");
                }
            }
    
        );
        content.add(confirmButton);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the reset high score table screen for parental accounts, allowing them to clear the leaderboard data
     */
    private void buildResetHighScoreScreen() {
        JLabel resetLabel = createCenteredTitle("Reset High Score Table", 200, 400, 30f);
        content.add(resetLabel);

        JButton confirmButton = createActionButton("Confirm Reset", centerX(BUTTON_W), 300, BUTTON_W, BUTTON_H, e -> {
            try {
                Statistic.resetLeaderboard();
                showCustomPopup("Leaderboard reset!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showCustomPopup("Unable to reset leaderboard.");
            }
        });
        content.add(confirmButton);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the tutorial screen, which provides instructions on how to play the game
     */
    private void buildTutorialScreen() {

        String freezePath = getClass().getResource("/Assets/freezePower.png").toString();
        String doublePath = getClass().getResource("/Assets/doublePower.png").toString();
        String healPath = getClass().getResource("/Assets/healPower.png").toString();
        
        
        JPanel formPanel = new JPanel(null);
        formPanel.setPreferredSize(new Dimension(screenWidth, 2200));
        formPanel.setOpaque(false);
        
        JLabel tutorialLabel = createCenteredTitle("How to Play", 100, 300, 40f);
        formPanel.add(tutorialLabel);

        ImageIcon tutorialBoxIcon = loadScaledIcon("/Assets/tutorialbackground.png", 1700, 2400);
        JLabel tutorialBox = new JLabel(tutorialBoxIcon);
        tutorialBox.setBounds(centerX(1500), 200, 1500, 1900);
        formPanel.add(tutorialBox);

        JLabel instructions = new JLabel(
            "<html>"+
            
            "Welcome to Lexibyrinth, a dungeon-themed typing game, where you can sharpen your typing skills while defeating enemies and exploring a dungeon! <br><br>"+
            "While you explore, you will encounter lots of enemies that try to damage you. You must use your sublime typing skills to defeat them! <br><br> "+
            "The gameplay consists of 3 levels; to play a level, you must complete the previous level. Once you complete a level, you have it unlocked, "+
            "and you can replay it to try and get a better highscore.  <br><br> "+
            
            "In each level, there will be evil behemoths, ghosts, goblins, and witches that will try to kill you – when their yellow bar fills up, " +
            "they will attack you and you will lose a bit of life. If you run out of lives, you lose the level. <br><br>" +
            
            "To defeat an enemy, simply type the word that appears on top of it. But be careful – one mistake and you will have to retype the word from the beginning! " +
            "4 enemies can be on screen at a time. Once you defeat one, another one will spawn in its place. <br> <br>Each level has a specific number of enemies – once you’ve defeated them all, " +
            "you win the level. Defeating an enemy awards you points, and enemies with longer words award you more points. Compete with your friends to try and get the best highscore. <br><br>" +
            "But be smart – you can also spend your points in the shop, where you can buy powerups to help you beat more levels or boost your score. <br><br>"+

            "There are 3 powerups you can buy: <br><br>" +

            "<img src='" + freezePath + "' width='60' height='60'> " +
            "&nbsp;&nbsp; FREEZE (1200 pts) [CTRL + B] - Freezes enemies<br><br>" +

            "<img src='" + doublePath + "' width='60' height='60'> " +
            "&nbsp;&nbsp; SCORE (2000 pts) [CTRL + N] - Doubles score<br><br>" +

            "<img src='" + healPath + "' width='60' height='60'> " +
            "&nbsp;&nbsp; HEALTH (1500 pts) [CTRL + M] - Heals you<br><br>" +

            "You can view your current WPM at the top of the screen. For a more comprehensive view of your typing performance, you can go to the Statistics screen. <br><br>"+ 
            "O Hail our brave knight, use your superior typing skills to slay some enemies and conquer the Lexibyrinth! Have fun! " +
            "</html>" 
            
            
           );

        
        
        instructions.setBounds(centerX(1000), 300, 1000, 1500);
        instructions.setForeground(Color.BLACK);
        instructions.setFont(customFont.deriveFont(Font.PLAIN, 23f));
        
        formPanel.add(instructions);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBounds(0, 0, screenWidth, screenHeight - 30);
        scrollPane.setBorder(null);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);

        content.add(scrollPane);

        
        formPanel.setComponentZOrder(instructions, 0);
        formPanel.setComponentZOrder(tutorialBox, 1);

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
            content.setComponentZOrder(backgroundLabel, content.getComponentCount() - 1);
        }
    }

    /**
     * builds the player statistics screen, which displays typing statistics for the selected player account
     */
    private void buildPlayerStatisticsScreen() {
        JLabel title = createCenteredTitle("Choose player account to view statistics", 100, 700, 40f);
        content.add(title);

        JComboBox<String> playerSelect = createComboBox(loadAccountUsernames(), centerX(LARGE_FORM_W), 170, LARGE_FORM_W, 50, 20f);
        content.add(playerSelect);

        JPanel statsHolder = new JPanel(null);
        statsHolder.setOpaque(false);
        statsHolder.setBounds(0, 150, screenWidth, screenHeight - 220);
        content.add(statsHolder);

        String selectedUser = (String) playerSelect.getSelectedItem();
        if (selectedUser != null && !selectedUser.equals("No accounts found")) {
            JPanel statsPanel = createStatisticsPanel(selectedUser);
            if (statsPanel != null) {
                statsHolder.add(statsPanel);
            }
        }

        playerSelect.addActionListener(e -> {
            statsHolder.removeAll();

            String username = (String) playerSelect.getSelectedItem();
            if (username != null && !username.equals("No accounts found")) {
                JPanel statsPanel = createStatisticsPanel(username);
                if (statsPanel != null) {
                    statsHolder.add(statsPanel);
                } 
            }

            statsHolder.revalidate();
            statsHolder.repaint();
        });

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds a new game screen to create a new game session
     */
    private void buildNewGameScreen() {
        int cx = centerX(BUTTON_W);
        JLabel confirmationTitle1 = createCenteredTitle("Are you sure?", 100, screenWidth, 40f);
        confirmationTitle1.setForeground(Color.RED);
        JLabel confirmationTitle2 = createCenteredTitle("This will clear all level progress, shop points,", 160, screenWidth, 40f);
        JLabel confirmationTitle3 = createCenteredTitle("and power ups, but keeps statistics.", 220, screenWidth, 40f);
        JLabel confirmationTitle4 = createCenteredTitle("This cannot be undone.", 280, screenWidth, 40f);
        JButton confirmButton = createActionButton("Confirm", cx, 380, BUTTON_W, BUTTON_H, e->{
            Statistic.getCurrentStats().levelProgression=1; //reset to new acc, but keep other stats
            Statistic.getCurrentStats().points=0;
            for(int i=0;i<3;i++){
                Statistic.getCurrentStats().totalPowerups[i]=0;
            }
            Statistic.updateUserStatistics();
            navigateTo(Screen.SELECT_LEVEL);
        });

        JLabel backgroundLabel=new JLabel();
        if (backgroundIcon != null) {
            backgroundLabel.setIcon(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }

        content.add(confirmationTitle1);
        content.add(confirmationTitle2);
        content.add(confirmationTitle3);
        content.add(confirmationTitle4);
        content.add(confirmButton);
        content.add(backgroundLabel);
    }

    /**
     * Builds a level selection screen where players can choose which level to play using the doors, as well as access the shop after clicking on the chest
     * The player will return to the level selection when they are able to go to the next level
     */
    private void buildSelectLevelScreen() {
        content.setLayout(null);

        JLabel doorselectbg = new JLabel(doorSelectionIcon);
        doorselectbg.setBounds(0, 0, screenWidth, screenHeight);
        doorselectbg.setLayout(null);
        content.add(doorselectbg);

        JLabel title = createCenteredTitle("Choose Your Path", 100, 500, 40f);
        title.setForeground(Color.YELLOW);
        doorselectbg.add(title);

        //x and y are consts based on background image
        Rectangle door1 = scaleRect(197, 210, doorOpenIcon.getIconWidth(), doorOpenIcon.getIconHeight());
        Rectangle door2 = scaleRect(640, 210, doorOpenIcon.getIconWidth(), doorOpenIcon.getIconHeight());
        Rectangle door3 = scaleRect(1085, 210, doorOpenIcon.getIconWidth(), doorOpenIcon.getIconHeight());

        JLabel doorText1 = new JLabel("Level 1");
        JLabel doorText2 = new JLabel("Level 2");
        JLabel doorText3 = new JLabel("Level 3");

        Font selectLevelFont = customFont.deriveFont(Font.PLAIN, 30f);
        
        doorText1.setFont(selectLevelFont);
        doorText2.setFont(selectLevelFont);
        doorText3.setFont(selectLevelFont);

        int widthText1 = doorText1.getFontMetrics(selectLevelFont).stringWidth("Level 1")/2;
        int widthText2 = doorText2.getFontMetrics(selectLevelFont).stringWidth("Level 2")/2;
        int widthText3 = doorText3.getFontMetrics(selectLevelFont).stringWidth("Level 3")/2;
        int heightTextAll = doorText1.getFontMetrics(selectLevelFont).getHeight()/2;

        int posX1 = door1.x + door1.width/2 - widthText1;
        int posX2 = door2.x + door1.width/2 - widthText2;
        int posX3 = door3.x + door1.width/2 - widthText3;
        int posY1 = door1.y + door1.height/2 - heightTextAll;
        int posY2 = door2.y + door1.height/2 - heightTextAll;
        int posY3 = door3.y + door1.height/2 - heightTextAll;
        //text will be centred inside each door

        doorText1.setBounds(posX1, posY1, door1.width, 50);
        doorText2.setBounds(posX2, posY2, door2.width, 50);
        doorText3.setBounds(posX3, posY3, door3.width, 50);

        doorText1.setForeground(Color.YELLOW);
        doorText2.setForeground(Color.YELLOW);
        doorText3.setForeground(Color.YELLOW);

        doorText1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        doorText2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        doorText3.setCursor(new Cursor(Cursor.HAND_CURSOR));

        doorselectbg.add(doorText1);
        doorselectbg.add(doorText2);
        doorselectbg.add(doorText3);

        int levelProgression;
        try{
            levelProgression = Statistic.getCurrentStats().levelProgression;
            //door clicked actions (doors open on hover)
            addDoor(doorselectbg, door1,  () -> {
                if(levelProgression>=1){
                    level = 1;
                    navigateTo(Screen.GAMEPLAY);
                }
                else{
                    showCustomPopup("<html>Must complete all<br>previous levels first.</html>");
                }
            }, levelProgression>=1);

            addDoor(doorselectbg, door2, () -> {
                if(levelProgression>=2){
                    level = 2;
                    navigateTo(Screen.GAMEPLAY);
                }
                else{
                    showCustomPopup("<html>Must complete all<br>previous levels first.</html>");
                }
            }, levelProgression>=2);

            addDoor(doorselectbg, door3, () -> {
                if(levelProgression==3){
                    level = 3;
                    navigateTo(Screen.GAMEPLAY);
                }
                else{
                    showCustomPopup("<html>Must complete all<br>previous levels first.</html>");
                }
            }, levelProgression==3);
        }
        catch(Exception e){
            showCustomPopup("<html>Statistic Error:<br>Close and log in again.</html>");
        }

        //chest icon to go to shop (chest opens on hover)
        JLabel chestImage = new JLabel(chestClosedIcon);
        JLabel hitbox = new JLabel();
        JLabel shopText = new JLabel("Shop");
        shopText.setFont(selectLevelFont);

        int chestWidth = 300;
        int chestHeight = 200;
        // center horizontally
        int shopX = (screenWidth - chestWidth) / 2;
        // position near bottom, scaled
        int shopY = screenHeight - chestHeight - screenHeight/10;

        int widthShopText = shopText.getFontMetrics(selectLevelFont).stringWidth("Shop");
        shopText.setBounds(shopX +chestWidth/2 - widthShopText/2, shopY + 25, widthShopText+1, 50);
        shopText.setForeground(Color.magenta);
        shopText.setCursor(new Cursor(Cursor.HAND_CURSOR));

        chestImage.setBounds(shopX, shopY, chestWidth, chestHeight);
        chestImage.setCursor(new Cursor(Cursor.HAND_CURSOR));

        hitbox.setBounds(shopX, shopY, chestWidth, chestHeight);
        hitbox.setOpaque(false);
        hitbox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                chestImage.setIcon(chestOpenIcon);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                chestImage.setIcon(chestClosedIcon);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                navigateTo(Screen.SHOP);
            }
        });

        doorselectbg.add(shopText);
        doorselectbg.add(chestImage);
        doorselectbg.add(hitbox);
    }

    /**
     * builds the typing statistics page for the logged-in player
     */
    private void buildTypingStatisticsPage() {
        content.setLayout(null);
        content.setBackground(BG_DARK);

        if (login == null) {
            showCustomPopup("No player is logged in.");
            return;
        }

        JPanel statsPanel = createStatisticsPanel(login.username);
        if (statsPanel != null) {
            statsPanel.setLocation(centerX(statsPanel.getWidth()), 110);
            content.add(statsPanel);
        }

        if (backgroundIcon != null) {
            JLabel backgroundLabel = new JLabel(backgroundIcon);
            backgroundLabel.setBounds(centerX(screenWidth), 0, screenWidth, screenHeight);
            content.add(backgroundLabel);
        }
    }

    /**
     * builds the shop screen for in between levels, where players can spend points derived from score to purchase power-ups.
     */
    private void buildShopScreen(){
        int shopPoints = Statistic.getCurrentStats().points; //get correct points
        //Points persist, score is per-level only.

        content.setLayout(null);
        JLabel shopBackgroundImage = new JLabel(shopBackgroundIcon);
        shopBackgroundImage.setBounds(0, 0, screenWidth, screenHeight);

        JLabel title = createCenteredTitle("Power Up Shop", 150, 500, 40f);
        title.setForeground(Color.magenta);
        content.add(title);

        Font shopFont = customFont.deriveFont(Font.PLAIN, 27);

        //shop font small is size 27
        //create part that tells user how many points they are able to spend
        JLabel pointsLabel = createCenteredTitle("Points: " + shopPoints, 75, 200, 27);
        pointsLabel.setForeground(Color.magenta);

        content.add(pointsLabel);

        JLabel tableImage1 = new JLabel(tableIcon);
        JLabel tableImage2 = new JLabel(tableIcon);
        JLabel tableImage3 = new JLabel(tableIcon);

        int tableWidth = 250;
        int tableHeight = 300;
        int marginX = 150;

        //"center" within "thirds" of the screen, accounting for margin x
        int div = (screenWidth - 2*marginX)/6;
        int tableX1 = div - tableWidth/2 + marginX;
        int tableX2 = div*3 - tableWidth/2 +marginX;
        int tableX3 = div*5 - tableWidth/2 + marginX;
        int tableY = screenHeight/2 - tableHeight/2 + 50;

        tableImage1.setBounds(tableX1, tableY, tableWidth, tableHeight);
        tableImage2.setBounds(tableX2, tableY, tableWidth, tableHeight);
        tableImage3.setBounds(tableX3, tableY, tableWidth, tableHeight);

        JLabel freezePowerImage = new JLabel(freezePowerIcon);
        JLabel doublePowerImage = new JLabel(doublePowerIcon);
        JLabel healPowerImage = new JLabel(healPowerIcon);

        int powerSideLength = 100;

        //power up x is table x + half table width - half power up width
        int freezeX = tableX1 + tableWidth/2 - powerSideLength/2;
        int doubleX = tableX2 + tableWidth/2 - powerSideLength/2;
        int healX = tableX3 + tableWidth/2 - powerSideLength/2;
        int powerY = tableY - powerSideLength/2;

        freezePowerImage.setBounds(freezeX, powerY, powerSideLength, powerSideLength);
        doublePowerImage.setBounds(doubleX, powerY, powerSideLength, powerSideLength);
        healPowerImage.setBounds(healX, powerY, powerSideLength, powerSideLength);

        //create title above each power up
        JLabel freezePowerTitle = new JLabel("Freeze Attacks");
        JLabel doublePowerTitle = new JLabel("Point Doubler");
        JLabel healPowerTitle = new JLabel("Heal");
        freezePowerTitle.setFont(shopFont);
        doublePowerTitle.setFont(shopFont);
        healPowerTitle.setFont(shopFont);
        freezePowerTitle.setForeground(Color.decode("#66E9FF"));
        doublePowerTitle.setForeground(Color.decode("#9566FF"));
        healPowerTitle.setForeground(Color.decode("#80BA4A"));

        //must replace both the text here and in the created JLabel for correct text
        int widthFreezeTitle = freezePowerTitle.getFontMetrics(shopFont).stringWidth("Freeze Attacks");
        int widthDoubleTitle = doublePowerTitle.getFontMetrics(shopFont).stringWidth("Point Doubler");
        int widthHealTitle = healPowerTitle.getFontMetrics(shopFont).stringWidth("Heal");

        freezePowerTitle.setBounds(freezeX - widthFreezeTitle/2 + powerSideLength/2, powerY - 50, widthFreezeTitle+5, 40);
        doublePowerTitle.setBounds(doubleX - widthDoubleTitle/2 + powerSideLength/2, powerY - 50, widthDoubleTitle+5, 40);
        healPowerTitle.setBounds(healX - widthHealTitle/2 + powerSideLength/2, powerY - 50, widthHealTitle+5, 40);

        //power up descriptions
        int descY = tableY + tableHeight;
        JLabel freezeDescription = createLabel("<html>Cost: "+PowerUp.FREEZE.cost+"<br>Freezes enemy attacks<br>for 5 seconds.<br>Your amount: "
        + Statistic.getCurrentStats().totalPowerups[PowerUp.FREEZE.ordinal()] +"</html>"
        , tableX1, descY, 400, 200, 27);
        JLabel doubleDescription = createLabel("<html>Cost: "+PowerUp.SCORE.cost+"<br>Doubles your score<br>for 5 seconds.<br>Your amount: " 
        + Statistic.getCurrentStats().totalPowerups[PowerUp.SCORE.ordinal()] +"</html>"
        , tableX2, descY, 400, 200, 27);
        JLabel healDescription = createLabel("<html>Cost: "+PowerUp.HEALTH.cost+"<br>Heals your health by 40.<br><br>Your amount: " 
        + Statistic.getCurrentStats().totalPowerups[PowerUp.HEALTH.ordinal()] +"</html>"
        , tableX3, descY, 400, 200, 27);
        freezeDescription.setForeground(Color.decode("#66E9FF"));
        doubleDescription.setForeground(Color.decode("#9566FF"));
        healDescription.setForeground(Color.decode("#80BA4A"));
        
        int buyButtonY = powerY + tableHeight/2;
        // buy button x is tableX - extra protruding amount of buy button /2
        JButton freezeBuyButton = createActionButton("Buy Freeze", tableX1 - (BUTTON_W - tableWidth)/2, buyButtonY, BUTTON_W, BUTTON_H, e -> {
            if(Statistic.getCurrentStats().points>=PowerUp.FREEZE.cost){
                Statistic.getCurrentStats().points -= PowerUp.FREEZE.cost;
                Statistic.getCurrentStats().totalPowerups[PowerUp.FREEZE.ordinal()] += 1;
                Statistic.updateUserStatistics();
                freezeDescription.setText("<html>Cost: "+PowerUp.FREEZE.cost+"<br>Freezes enemy attacks<br>for 5 seconds.<br>Your amount: "
        + Statistic.getCurrentStats().totalPowerups[PowerUp.FREEZE.ordinal()] +"</html>");
                pointsLabel.setText("Points: " + Statistic.getCurrentStats().points); //update after buying something
            }
            else{
                showCustomPopup("<html>Your points are<br>insufficient to buy this.</html>");
            }
        });
        JButton doubleBuyButton = createActionButton("Buy Doubler", tableX2 - (BUTTON_W - tableWidth)/2,buyButtonY, BUTTON_W, BUTTON_H, e ->{
            if(Statistic.getCurrentStats().points>=PowerUp.SCORE.cost){
                Statistic.getCurrentStats().points -=  PowerUp.SCORE.cost;
                Statistic.getCurrentStats().totalPowerups[PowerUp.SCORE.ordinal()] += 1;
                Statistic.updateUserStatistics();
                doubleDescription.setText("<html>Cost: "+PowerUp.SCORE.cost+"<br>Doubles your score<br>for 5 seconds.<br>Your amount: " 
        + Statistic.getCurrentStats().totalPowerups[PowerUp.SCORE.ordinal()] +"</html>");
                pointsLabel.setText("Points: " + Statistic.getCurrentStats().points); //update after buying something
            }
            else{
                showCustomPopup("<html>Your points are<br>insufficient to buy this.</html>");
            }
        });
        JButton healBuyButton = createActionButton("Buy Heal", tableX3 - (BUTTON_W - tableWidth)/2, buyButtonY, BUTTON_W, BUTTON_H, e ->{
            if(Statistic.getCurrentStats().points>=PowerUp.HEALTH.cost){
                Statistic.getCurrentStats().points -=  PowerUp.HEALTH.cost;
                Statistic.getCurrentStats().totalPowerups[PowerUp.HEALTH.ordinal()] += 1;
                Statistic.updateUserStatistics();
                healDescription.setText("<html>Cost: "+PowerUp.HEALTH.cost+"<br>Heals your health by 40.<br><br>Your amount: " 
        + Statistic.getCurrentStats().totalPowerups[PowerUp.HEALTH.ordinal()] +"</html>");
                pointsLabel.setText("Points: " + Statistic.getCurrentStats().points); //update after buying something
            }
            else{
                showCustomPopup("<html>Your points are<br>insufficient to buy this.</html>");
            }
        });

        content.add(freezeDescription);
        content.add(doubleDescription);
        content.add(healDescription);

        content.add(freezeBuyButton);
        content.add(doubleBuyButton);
        content.add(healBuyButton);

        content.add(freezePowerTitle);
        content.add(doublePowerTitle);
        content.add(healPowerTitle);

        content.add(freezePowerImage);
        content.add(doublePowerImage);
        content.add(healPowerImage);

        content.add(tableImage1);
        content.add(tableImage2);
        content.add(tableImage3);

        content.add(shopBackgroundImage);
    }

    // ---------- reusable UI helpers ----------
    /**
     * helper method to create the statistics panel for a given player username, which is used in 
     * both the player statistics screen and the reset statistics screen
     * 
     * @param username the username of the user who's statistics are being displayed
     * @return a panel containing the user's statistics, or {@code null} if there was an error loading the statistics
     */
    private JPanel createStatisticsPanel(String username) {
        PlayerStatistics stats;

        try {
            stats = FileManager.getStatistic(username);
        } catch (Exception e) {
            e.printStackTrace();
            showCustomPopup("Error loading statistics.");
            return null;
        }

        int panelW = (int)(screenWidth * 0.78);
        int panelH = (int)(screenHeight * 0.68);
        int panelX = centerX(panelW);
        int panelY = (screenHeight - panelH) / 2 - 50;

        JPanel statsPanel = new JPanel(null);
        statsPanel.setBounds(panelX, panelY, panelW, panelH);
        statsPanel.setBackground(Color.decode("#EDEDED"));

        int topMargin = 70;
        int leftColX = (int)(panelW * 0.08);
        int midColX = (int)(panelW * 0.40);
        int rightColX = (int)(panelW * 0.72);

        int row1Y = topMargin;
        int row2Y = topMargin + 180;
        int row3Y = topMargin + 360;

        /** username */
        JLabel usernameLabel = new JLabel(username, SwingConstants.CENTER);
        usernameLabel.setBounds(leftColX, row1Y + 20, 350, 70);
        usernameLabel.setFont(customFont.deriveFont(Font.PLAIN, 50f));
        usernameLabel.setForeground(Color.BLACK);
        statsPanel.add(usernameLabel);

        /** top row*/
        statsPanel.add(createStatBox("Average WPM", String.format("%.0f",stats.meanWPM), midColX, row1Y));
        statsPanel.add(createStatBox("Peak WPM", String.format("%.0f",stats.peakWPM), rightColX, row1Y));

        /** middle row */
        statsPanel.add(createStatBox("Error Count", String.valueOf(stats.errorCount), leftColX, row2Y));
        statsPanel.add(createStatBox("Total Time Played", String.format("%.0f", stats.timeStat) + "s", midColX, row2Y));
        statsPanel.add(createStatBox("Level Progression", String.valueOf(stats.levelsCompleted), rightColX, row2Y));

        /** bottom row */
        statsPanel.add(createStatBox("High Score", String.valueOf(stats.highScore), leftColX, row3Y));
        statsPanel.add(createStatBox("Words Typed", String.valueOf(stats.totalWordsTyped), midColX, row3Y));
        statsPanel.add(createStatBox("Accuracy %", String.format("%.1f",stats.accuracy) + "%", rightColX, row3Y));
        
        return statsPanel;
    }

    /**
     * helper method to create a box containing a single statistic, which is used in the statistics panel
     * @param title the label for the statistic being displayed
     * @param value the displayed value of the statistic
     * @param x the x-coordinate of the top-left corner of the box
     * @param y the y-coordinate of the top-left corner of the box
     * @return a panel containing the statistic
     */
    private JPanel createStatBox(String title, String value, int x, int y) {
        JPanel boxPanel = new JPanel(null);
        boxPanel.setOpaque(false);
        boxPanel.setBounds(x, y, 230, 110);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setBounds(0, 0, 230, 35);
        titleLabel.setFont(customFont.deriveFont(Font.PLAIN, 24f));
        titleLabel.setForeground(Color.BLACK);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setBounds(45, 45, 140, 55);
        valueLabel.setOpaque(true);
        valueLabel.setBackground(Color.decode("#D3D3D3"));
        valueLabel.setForeground(Color.BLACK);
        valueLabel.setFont(customFont.deriveFont(Font.PLAIN, 24f));

        boxPanel.add(titleLabel);
        boxPanel.add(valueLabel);

        return boxPanel;
    }

    private void buildGameplayScreen(int level) {
        stopGameLoop();
        this.gamelevel = new GameLevel(level,sprites);
        switch(level){
            case 1:
                battleFieldImg=battleFieldImg1;
                break;
            case 2:
                battleFieldImg=battleFieldImg2;
                break;
            case 3: 
                battleFieldImg=battleFieldImg3;
                break;
            default:
                battleFieldImg=battleFieldImg1;
        }
        startGameLoop(this.gamelevel);
        content.repaint();
        //static power up images
        JLabel freezePowerImage = new JLabel(freezePowerIcon);
        JLabel doublePowerImage = new JLabel(doublePowerIcon);
        JLabel healPowerImage = new JLabel(healPowerIcon);
        freezePowerImage.setBounds(50, 100, 100, 100);
        doublePowerImage.setBounds(50, 225, 100, 100);
        healPowerImage.setBounds(50, 350, 100, 100);
        content.add(freezePowerImage);
        content.add(doublePowerImage);
        content.add(healPowerImage);
    }

    private void showGameLevelCompleted(boolean isComplete){
        stopGameLoop();
        content.removeAll();
        content.revalidate();
        content.repaint();
        if(gamelevel==null){
            navigateTo(Screen.PLAYER);
        }
        int cx = centerX(BUTTON_W);
        Color GRAY = new Color(128,128,128,200); //slightly clear gray

        JLabel scoreLabel = createLabel("Score obtained: "+ gamelevel.getScore(), cx, 200, BUTTON_W,50, 30f);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setForeground(Color.magenta);
        scoreLabel.setBackground(GRAY);
        scoreLabel.setOpaque(true);
        JButton restartLevelButton = createActionButton("Restart Level", cx,  360, BUTTON_W, BUTTON_H, e ->{
            navigateTo(Screen.GAMEPLAY);
        });
        JButton returnButton = createActionButton("Quit Level", cx, 420, BUTTON_W, BUTTON_H, e ->{
            navigateTo(Screen.PLAYER);
        });
        content.add(scoreLabel);
        content.add(restartLevelButton);
        content.add(returnButton);

        JLabel backGroundImage = new JLabel();
        if(isComplete){
            backGroundImage.setIcon(victoryBackgroundIcon);
            backGroundImage.setBounds(0,0,screenWidth,screenHeight);
            JLabel title = createCenteredTitle("Level Completed!", 100, 400, 50f);
            title.setForeground(Color.green);
            title.setBackground(GRAY);
            title.setOpaque(true);
            JButton nextLevelButton = createActionButton("Next Level", cx, 300, BUTTON_W, BUTTON_H, e -> {
                navigateTo(Screen.SELECT_LEVEL);
            });
            content.add(nextLevelButton);
            content.add(title);
        }
        else if(!isComplete){
            backGroundImage.setIcon(defeatBackgroundIcon);
            backGroundImage.setBounds(0,0, screenWidth, screenHeight);
            
            JLabel title = createCenteredTitle("Level failed: ran out of health", 100, 1000, 50f);
            title.setForeground(Color.red);
            title.setBackground(GRAY);
            title.setOpaque(true);
            content.add(title);
        }
        else{
            JLabel title = createCenteredTitle("Level Error", 100, 400, 50f);
            content.add(title);
        }
        content.add(backGroundImage);
        refreshContent();
    }

    // ---------- back button ----------
    /**
     * helper method to add a back button to the current screen, which navigates back to the previous screen when clicked
     */
    private void addBackButton() {
        JButton backButton = new JButton();

        if (backButtonIcon != null) {
            backButton.setIcon(backButtonIcon);
            backButton.setContentAreaFilled(false);
            backButton.setOpaque(false);
            backButton.setBounds(10, 10, 120, 60);
        } else {
            backButton.setText("← Back");
            backButton.setFont(customFont.deriveFont(Font.PLAIN, 16f));
            backButton.setForeground(Color.WHITE);
            backButton.setBackground(PURPLE);
            backButton.setOpaque(true);
            backButton.setContentAreaFilled(true);
            backButton.setBounds(10, 10, 110, 36);
        }
        //logic
        if (currentScreen == Screen.GAMEPLAY){
            stopGameLoop();
        }

        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> goBack());
        content.add(backButton);
    }

    /**
     * helped method that scales the rectangle based on the reference size of the level background
     * 
     * @param x original x-coordinate
     * @param y original y-coordinate
     * @param w origin width
     * @param h original height
     * @return the scaled {@link Rectangle} based on the current screen size and the reference size of the level background
     */
    private Rectangle scaleRect(int x, int y, int w, int h) {
        return new Rectangle(
            (int) Math.round(x * (double) screenWidth / LVLBG_REF_W),
            (int) Math.round(y * (double) screenHeight / LVLBG_REF_H),
            (int) Math.round(w * (double) screenWidth / LVLBG_REF_W),
            (int) Math.round(h * (double) screenHeight / LVLBG_REF_H)
        );
    }

    /**
     * helper method to add an interactive door to the given parent label, which navigates to a new screen when clicked 
     * the door will display a hover effect when the mouse is over it
     * 
     * @param parent the parent component to which the door will be added
     * @param r the bounds of the clickable door
     * @param action the action to run when the door is called
     */
    private void addDoor(JLabel parent, Rectangle r, Runnable action, boolean available) {
        JPanel doorArea = new JPanel(null);
        doorArea.setBounds(r);
        doorArea.setOpaque(false);

        JLabel hoverOverlay = new JLabel(doorOpenIcon);
        hoverOverlay.setBounds(0, 0, r.width, r.height);
        hoverOverlay.setVisible(false);
        doorArea.add(hoverOverlay);
        doorArea.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel hitbox = new JLabel();
        hitbox.setBounds(0, 0, r.width, r.height);
        hitbox.setOpaque(false);

        hitbox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if(available){
                    hoverOverlay.setVisible(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverOverlay.setVisible(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });

        doorArea.add(hitbox);
        parent.add(doorArea);
    }

    /**
     * 
     * 
     */

    public void submitFormShortcut(){
        if (currentScreen == Screen.LOGIN){
            listener.actionPerformed(null);
        }
        else if (currentScreen == Screen.CREATE_ADMIN){
            registerAdminListener.actionPerformed(null);
        }
        else if (currentScreen == Screen.CREATE_STUDENT){
            registerStudentListener.actionPerformed(null);
        }
        else if (currentScreen == Screen.RESET_PASSWORD){
            resetPasswordListener.actionPerformed(null);
        }
        else if (currentScreen == Screen.RESETFORGOT_PASSWORD){
            resetForgotPasswordListener.actionPerformed(null);
        }
        else if (currentScreen == Screen.FORGOT_PASSWORD){
            forgotPasswordListener.actionPerformed(null);
        }
    }

    /**
     * helper method to add a menu button to the current screen, which navigates to a new screen when clicked
     * @param text the text to display on the button
     * @param x the x-coordinate of the top-left corner of the button
     * @param y the y-coordinate of the top-left corner of the button
     * @param action the action to run when the button is clicked
     */
    private void addMenuButton(String text, int x, int y, Runnable action) {
        JButton button = createActionButton(text, x, y, BUTTON_W, BUTTON_H, e -> action.run());
        content.add(button);
    }

    /**
     * helper method to create a button with the given properties, which can be used for both menu buttons and wide buttons
     * button background is set to our image
     * 
     * @param text the text to display on the button
     * @param x x-coordinate
     * @param y y-coordinate
     * @param width the width of the button
     * @param height the height of the button
     * @param action the action to run when the button is clicked
     * @return a {@link JButton} with the given properties and action listener
     */
    private JButton createActionButton(String text, int x, int y, int width, int height, ActionListener action) {
        JButton button = new JButton(text);
        ImageIcon icon = null;

        if (width == BUTTON_W && height == BUTTON_H) {
            icon = menuButtonIcon;
        } else if (width == WIDE_BUTTON_W && height == WIDE_BUTTON_H) {
            icon = wideButtonIcon;
        }

        if (icon != null) {
            button.setIcon(icon);
        }

        button.setForeground(Color.decode("#F0EDE4"));
        button.setFont(customFont.deriveFont(Font.BOLD, 26f));
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setMargin(new Insets(0, 0, 12, 0));

        button.setBounds(x, y, width, height);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(action);
        return button;
    }

    /**
     * helper method to create a styled label
     * 
     * @param text the text to display on the label
     * @param x x-coordinate
     * @param y y-coordinate 
     * @param w width
     * @param h height
     * @param size font size
     * @return a {@link JLabel} with the given properties
     */
    private JLabel createLabel(String text, int x, int y, int w, int h, float size) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, w, h);
        label.setFont(customFont.deriveFont(Font.PLAIN, size));
        label.setForeground(TEXT_LIGHT);
        return label;
    }

    /**
     * helper method to create a styled label with centered text, used for titles on various screens
     * @param text the text to display on the label
     * @param y y-coordinate
     * @param width the width
     * @param size the font size
     * @return a {@link JLabel} with the given properties
     */
    private JLabel createCenteredTitle(String text, int y, int width, float size) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setBounds(centerX(width), y, width, 60);
        label.setForeground(TEXT_LIGHT);
        label.setFont(customFont.deriveFont(Font.PLAIN, size));
        return label;
    }

    /**
     * helper method to create a styled text field, used for various forms in the application
     * @param x x-coordinate
     * @param y y-coordinate
     * @param w width
     * @param h height
     * @param size font size
     * @return a {@link JTextField} with the given properties
     */
    private JTextField createTextField(int x, int y, int w, int h, float size) {
        JTextField field = new JTextField();
        field.setBounds(x, y, w, h);
        field.setFont(customFont.deriveFont(Font.PLAIN, size));
        field.setBackground(INPUT_BG);
        field.setForeground(Color.BLACK);
        return field;
    }

    /**
     * helper method to create a styled password field, used for various forms in the application
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @param w width
     * @param h height
     * @param size font size
     * @return a {@link JPasswordField} with the given properties
     */
    private JPasswordField createPasswordField(int x, int y, int w, int h, float size) {
        JPasswordField field = new JPasswordField();
        field.setBounds(x, y, w, h);
        field.setFont(customFont.deriveFont(Font.PLAIN, size));
        field.setBackground(INPUT_BG);
        field.setForeground(Color.BLACK);
        return field;
    }

    /**
     * helper method to create a styled combo box with the given properties, used for dropdowns in the application
     * @param items the items to display in the combo box
     * @param x x-coordinate
     * @param y y-coordinate
     * @param w width
     * @param h height
     * @param size font size
     * @return a {@link JComboBox} with the given properties and items
     */
    private JComboBox<String> createComboBox(String[] items, int x, int y, int w, int h, float size) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBounds(x, y, w, h);
        combo.setFont(customFont.deriveFont(Font.PLAIN, size));
        return combo;
    }

    /**
     * helper method to create a styled label that looks like a hyperlink, which runs the given action when clicked
     * 
     * @param text the text to display on the label
     * @param x x-coordinate
     * @param y y-coordinate
     * @param w width
     * @param h height
     * @param size font size
     * @param onClick the action to run when the label is clicked
     * @return a {@link JLabel} that looks like a hyperlink and runs the given action when clicked
     */
    private JLabel createLinkLabel(String text, int x, int y, int w, int h, float size, Runnable onClick) {
        JLabel link = new JLabel("<html><u>" + text + "</u></html>");
        link.setBounds(x, y, w, h);
        link.setFont(customFont.deriveFont(Font.PLAIN, size));
        link.setForeground(TEXT_LIGHT);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
        return link;
    }

    /**
     * helper method to calculate the x-coordinate needed to center an element of the given width on the screen
     * 
     * @param width the width of the element to be centered
     * @return the x-coordinate needed to center the element on the screen
     */
    private int centerX(int width) {
        return screenWidth / 2 - width / 2;
    }

    /**
     * helper method to load the usernames of all player accounts from the accounts file, which is used to populate the 
     * dropdowns for selecting player accounts in various screens
     * @return an array of all player usernames
     */
    private String[] loadAccountUsernames() {
        List<String> usernames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FileManager.ACCOUNT_FILE.toString()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length > 0 && !data[0].isBlank()) {
                    usernames.add(data[0]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (usernames.isEmpty()) {
            return new String[]{"No accounts found"};
        }
        return usernames.toArray(new String[0]);
    }

    // ---------- dialog / refresh ----------
    /**
     * helper method to show a custom popup dialog with the given message, which is used for various notifications 
     * in the application
     * @param message the message to display in the popup dialog
     */
    private void showCustomPopup(String message) {
        JDialog dialog = new JDialog(frame, "Message", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(frame);
        dialog.setUndecorated(true);

        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (popupBackground != null) {
                    g.drawImage(popupBackground, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(BG_DARK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        backgroundPanel.setLayout(null);

        JLabel messageLabel = new JLabel(
                "<html><div style='text-align: center;'>" + message + "</div></html>",
                SwingConstants.CENTER
        );
        messageLabel.setBounds(75, 90, 350, 120);
        messageLabel.setForeground(TEXT_LIGHT);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(customFont.deriveFont(Font.PLAIN, 30f));
        backgroundPanel.add(messageLabel);

        JButton okButton = new JButton("OK");
        if (okButtonIcon != null) {
            okButton.setIcon(okButtonIcon);
        }

        okButton.setHorizontalTextPosition(SwingConstants.CENTER);
        okButton.setVerticalTextPosition(SwingConstants.CENTER);
        okButton.setForeground(TEXT_LIGHT);
        okButton.setFont(customFont.deriveFont(Font.PLAIN, 15f));
        okButton.setBorderPainted(false);
        okButton.setContentAreaFilled(false);
        okButton.setFocusPainted(false);
        okButton.setOpaque(false);
        okButton.setBounds(185, 200, 130, 50);
        okButton.addActionListener(e -> dialog.dispose());
        backgroundPanel.add(okButton);

        dialog.setContentPane(backgroundPanel);
        backgroundPanel.setOpaque(false);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setVisible(true);
    }

    /**
     * helper method to refresh the content panel
     */
    private void refreshContent() {
        content.revalidate();
        content.repaint();
    }

    /**
     * helper method to show a confirmation dialog when the user tries to exit the application
     * which asks the user to confirm that they want to exit
     */
    private void showExitDialog() {
        int option = JOptionPane.showConfirmDialog(
                frame,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION
        );
        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    public static Font getCustomFont(){
        return customFont;
    }
}



