# Lexibyrinth

Lexibyrinth is an education typing game about exploring a dungeon-like labyrinth filled with monsters. Each monster has a corresponding word, and the player must type the word correctly to defeat them. To pass the level and reach the next room, the player must defeat all enemies by typing all words correctly.

## What Lexibyrinth Has:

- Provides a full-screen desktop GUI game built with Java Swing.
- Supports account-based login.
- Supports two account types:
	- Student account: play game levels, shop, and view own typing statistics.
	- Admin (parental control) account: everything in student account, view player statistics, create student accounts, reset student passwords/statistics, and reset high-score table.
- Persists data to CSV files:
	- Accounts and player statistics.
	- Leaderboard/high scores.
	- Enemy word lists.

## Required Libraries and Third-Party Tools

The runtime app itself uses only Java standard library APIs, so no external runtime dependencies.

- Java Development Kit (JDK):
	- Required version: 17 or newer.
	- Tested expectation: any modern OpenJDK/Oracle JDK 17+.
- Java standard libraries (bundled with JDK):
	- `java.awt`, `javax.swing`, `java.io`, `java.util`, etc.

Optional development tools:

- IntelliJ IDEA, or
- VSCode with Java extensions.

## Project Structure

Folders/files:

- `src/`: all Java source code.
- `src/Assets/`: image resources.
- `src/Quintessential/`: custom font resource.
- `src/FILES_ALL/`: CSV persistence files (`accounts.csv`, `highscores.csv`, word list files).

The app loads resources using classpath-relative paths, so run commands should be executed with `src/` as the filepath root.

## How to Build (Compile) Lexibyrinth

### 1. Install Java

Install JDK 17 or newer on Windows.

After installation, open a new Command Prompt or PowerShell window and verify:

```powershell
java -version
javac -version
```

It should be version 17 or higher.

### 2. Open terminal in project root

Open Command Prompt or PowerShell in the project root so it contains `README.md` and `src`.

### 3. Compile the application

Compile from inside `src` so resource paths work correctly at runtime.

```powershell
cd src
javac Frame.java
```

This compiles the main entry point and any required dependencies.

## How to Run (After Compiling) Lexibyrinth

After building once (`javac Frame.java`):

```powershell
java Frame
```

Notes:

- The main entry point is `Frame` and not `GameLevel`.
- The game is GUI/full-screen, so run from an environment that supports desktop windows.

## User Guide

### Main menu

- `Login`: go to account login.
- `View High Scores`: view leaderboard.
- `Tutorial`: opens tutorial page (currently placeholder text).
- `Parental Controls`: routes through login, then opens parental controls for admin accounts.
- `Exit`: closes app.

### Login and account guide

- Login with existing username/password.
- If account is admin (`parentalControl=true`), app opens parental controls.
- If account is student (`parentalControl=false`), app opens player screen.
- The `Create admin account` link on login screen creates a new admin account with 3 security-question answers.
- The `Forgot password` link:
	- Enter username.
	- Choose one security question.
	- Provide answer.
	- Set a new password.

### Student/player guide

- `New Game` / `Select Level` to begin gameplay. `New Game` resets the player's points and level progression.
- During gameplay, type words to defeat enemies.
- Level progression unlocks more difficult levels in the level-select screen.
- Visit shop (chest on level select screen) to buy power-ups using points earned from completing levels.
- View own typing statistics in `Typing Statistics`.

Gameplay controls:

- Type letters to defeat enemies by completing words.
- Power-up shortcuts during gameplay:
	- `Ctrl + B`: Freeze attacks.
	- `Ctrl + N`: Score doubler.
	- `Ctrl + M`: Heal.

### Parental controls (admin account)

Accessible after admin login:

- `Player Screen`: switch to player view.
- `Player Statistics`: inspect selected user statistics.
- `Account Management`:
	- Create new student account.
	- Reset student password (with security-question validation).
	- Reset typing statistics for selected student.
- `Reset High Score Table`: clears leaderboard.

## Account Usernames and Passwords
All account information is located in `src/FILES_ALL/accounts.csv`.

The important fields to understand are the first 3 in `accounts.csv`: username, password, and true or false if this account is an admin account.

## Parental Controls Build/Install Details

Parental controls are integrated in the main Lexibyrinth application and are not a separate program.

- No separate install/build is needed.
- Build/run exactly the same as the main app.
- Access path: Main Menu > Parental Controls > Login with an admin account.

## Helpful Notes

- Run from `src/` (`cd src && java Frame`) so resources and CSV files compile correctly.
- Player data changes persist in CSV files under `src/FILES_ALL/`.
- If login/account behavior seems unusual, you can check for unintended edits in `accounts.csv`.
