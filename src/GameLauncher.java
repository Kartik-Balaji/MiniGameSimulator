import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;


public class GameLauncher extends Component {
    public static String player1Name;
    public static String player2Name;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enter Player Names");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);

            JPanel panel = new JPanel(new GridLayout(5, 1));
            JLabel player1Label = new JLabel("Enter Player 1 Name:");
            JTextField player1Field = new JTextField();
            JLabel player2Label = new JLabel("Enter Player 2 Name:");
            JTextField player2Field = new JTextField();

            JButton startButton = new JButton("Start");
            startButton.addActionListener(e -> {
                player1Name = player1Field.getText().trim();
                player2Name = player2Field.getText().trim();
                if (!player1Name.isEmpty() && !player2Name.isEmpty()) {
                    new GamesMenu(); // Launch the games menu
                    frame.dispose();
                } else {
                    JOptionPane.showMessageDialog(frame, "Please enter both player names.");
                }
            });

            panel.add(player1Label);
            panel.add(player1Field);
            panel.add(player2Label);
            panel.add(player2Field);
            panel.add(startButton);

            frame.add(panel);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

// Games Menu Class
class GamesMenu extends JFrame {
    public GamesMenu() {
        setTitle("Games Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(new BorderLayout());

        // Title Panel with Title Label, Feedback Button, and Stats Button
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Choose Your Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JButton feedbackButton = new JButton("Feedback");
        feedbackButton.addActionListener(e -> collectFeedback());

        JButton statsButton = new JButton("Stats");
        statsButton.addActionListener(e -> displayStats());

        titlePanel.add(feedbackButton, BorderLayout.WEST);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(statsButton, BorderLayout.EAST);

        // Games Panel with Buttons in Rows
        JPanel gamesPanel = new JPanel(new GridLayout(5, 3, 0, 10));
        gamesPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton connect4Btn = new JButton("Connect 4");
        JButton unoBtn = new JButton("UNO");
        JButton ticTacToeBtn = new JButton("Tic-Tac-Toe");
        JButton memoryBtn = new JButton("Memory Game");
        JButton TugOWar = new JButton("Tug of War");

        connect4Btn.addActionListener(e -> new Connect4Game());
        unoBtn.addActionListener(e -> new UnoGame());
        ticTacToeBtn.addActionListener(e -> new TicTacToeGame());
        memoryBtn.addActionListener(e -> new MemoryGame());
        TugOWar.addActionListener(e -> new TugOfWarGame());

        gamesPanel.add(connect4Btn);
        gamesPanel.add(unoBtn);
        gamesPanel.add(ticTacToeBtn);
        gamesPanel.add(memoryBtn);
        gamesPanel.add(TugOWar);

        // Add title panel and games panel to frame
        add(titlePanel, BorderLayout.NORTH);
        add(gamesPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void collectFeedback() {
        String feedback = JOptionPane.showInputDialog(null, "Enter your feedback:", "Feedback", JOptionPane.PLAIN_MESSAGE);
        if (feedback != null && !feedback.trim().isEmpty()) {
            DatabaseManager.saveFeedback(GameLauncher.player1Name, feedback.trim());
            JOptionPane.showMessageDialog(null, "Feedback Added Successfully");
        }
    }

    private void displayStats() {
        String stats = DatabaseManager.getPlayerStats(GameLauncher.player1Name, GameLauncher.player2Name);
        JOptionPane.showMessageDialog(null, stats, "Player Statistics", JOptionPane.INFORMATION_MESSAGE);
    }
}

// DatabaseManager Class
class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/GameStats?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    private static final String USER = "Kartik";
    private static final String PASSWORD = "root";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading MySQL JDBC Driver.");
            e.printStackTrace();
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void saveFeedback(String playerName, String feedback) {
        String query = "INSERT INTO feedback (player_name, feedback_text) VALUES (?, ?)";
        try (Connection connection = connect(); PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerName);
            stmt.setString(2, feedback);
            stmt.executeUpdate();
            System.out.println("Feedback saved successfully for " + playerName);
        } catch (SQLException e) {
            System.err.println("Failed to save feedback");
            e.printStackTrace();
        }
    }

    public static void updateStats(String playerName, String gameName, boolean won) {
        String columnToUpdate;
        switch (gameName) {
            case "Connect 4" -> columnToUpdate = "connect4_wins";
            case "UNO" -> columnToUpdate = "uno_wins";
            case "Tic-Tac-Toe" -> columnToUpdate = "tic_tac_toe_wins";
            case "Memory Game" -> columnToUpdate = "memory_game_wins";
            case "Checkers" -> columnToUpdate = "Tug_Of_War_wins";
            default -> {
                System.err.println("Invalid game name: " + gameName);
                return;
            }
        }

        String query = "INSERT INTO player_stats (player_name, total_matches, " + columnToUpdate + ") "
                + "VALUES (?, 1, ?) ON DUPLICATE KEY UPDATE "
                + "total_matches = total_matches + 1, "
                + columnToUpdate + " = " + columnToUpdate + " + ?";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, won ? 1 : 0); // Initial value for game wins
            pstmt.setInt(3, won ? 1 : 0); // Increment value for game wins if duplicate
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update stats for " + playerName);
            e.printStackTrace();
        }
    }

    public static String getPlayerStats(String player1Name, String player2Name) {
        StringBuilder stats = new StringBuilder();
        String query = "SELECT player_name, total_matches, connect4_wins, uno_wins, tic_tac_toe_wins, "
                + "memory_game_wins, Tug_Of_War_wins FROM player_stats WHERE player_name IN (?, ?)";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, player1Name);
            pstmt.setString(2, player2Name);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                stats.append("Player: ").append(rs.getString("player_name"))
                        .append("\nTotal Matches: ").append(rs.getInt("total_matches"))
                        .append("\nConnect 4 Wins: ").append(rs.getInt("connect4_wins"))
                        .append("\nUNO Wins: ").append(rs.getInt("uno_wins"))
                        .append("\nTic-Tac-Toe Wins: ").append(rs.getInt("tic_tac_toe_wins"))
                        .append("\nMemory Game Wins: ").append(rs.getInt("memory_game_wins"))
                        .append("\nTug Of War Wins: ").append(rs.getInt("Tug_Of_War_wins"))
                        .append("\n\n");
            }
        } catch (SQLException e) {
            System.err.println("Failed to retrieve player stats");
            e.printStackTrace();
        }

        if (stats.length() == 0) {
            stats.append("No stats available for the selected players.");
        }
        return stats.toString();
    }
}




 class TugOfWarGame extends JFrame {
    private static final int MAX_POSITION = 10; // The maximum position on either side
    private int position = 0; // The tug indicator position, starting in the center

    private JLabel tugLabel;

    public TugOfWarGame() {
        setTitle("Tug of War (Button Mashing Game)");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        tugLabel = new JLabel(getTugStatus(), SwingConstants.CENTER);
        tugLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(tugLabel, BorderLayout.NORTH);


        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Left-click: Player 1 action
                    updateTugPosition(-1);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // Right-click: Player 2 action
                    updateTugPosition(1);
                }
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private String getTugStatus() {
        StringBuilder status = new StringBuilder("Player 1 ");
        for (int i = -MAX_POSITION; i <= MAX_POSITION; i++) {
            if (i == position) {
                status.append("|"); // The current tug position
            } else {
                status.append(" ");
            }
        }
        status.append(" Player 2");
        return status.toString();
    }

    private void updateTugPosition(int change) {
        position += change;

        // Update the label
        tugLabel.setText(getTugStatus());

        // Check if a player has won
        if (position >= MAX_POSITION) {
            JOptionPane.showMessageDialog(this, GameLauncher.player2Name + " wins!");
            DatabaseManager.updateStats(GameLauncher.player2Name, "TugOfWar", true);
            DatabaseManager.updateStats(GameLauncher.player1Name, "TugOfWar", false);
            dispose();
        } else if (position <= -MAX_POSITION) {
            JOptionPane.showMessageDialog(this, GameLauncher.player1Name + " wins!");
            DatabaseManager.updateStats(GameLauncher.player1Name, "TugOfWar", true);
            DatabaseManager.updateStats(GameLauncher.player2Name, "TugOfWar", false);
            dispose();
        }
    }
}










class Connect4Game extends JFrame {
    private static final int ROWS = 7;
    private static final int COLS = 7;
    private final JButton[][] board = new JButton[ROWS][COLS];
    private boolean isPlayer1Turn = true;
    private final Color EMPTY = Color.WHITE;
    private final Color P1_COLOR = Color.RED;
    private final Color P2_COLOR = Color.YELLOW;

    public Connect4Game() {
        setTitle("Connect 4");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(ROWS, COLS));

        // Initialize the board
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                JButton button = new JButton();
                button.setBackground(EMPTY);
                final int currentCol = col;
                button.addActionListener(e -> dropPiece(currentCol));
                board[row][col] = button;
                add(button);
            }
        }

        setSize(700, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void dropPiece(int col) {
        // Find the lowest empty row
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][col].getBackground() == EMPTY) {
                board[row][col].setBackground(isPlayer1Turn ? P1_COLOR : P2_COLOR);
                if (checkWin(row, col)) {
                    JOptionPane.showMessageDialog(this,
                            (isPlayer1Turn ? "Player 1" : "Player 2") + " wins!");

                    dispose();
                }
                isPlayer1Turn = !isPlayer1Turn;
                break;
            }
        }
    }

    private boolean checkWin(int row, int col) {
        Color currentColor = board[row][col].getBackground();

        // Check horizontal
        if (countConsecutive(row, col, 0, 1, currentColor) >= 4) return true;
        // Check vertical
        if (countConsecutive(row, col, 1, 0, currentColor) >= 4) return true;
        // Check diagonal down-right
        if (countConsecutive(row, col, 1, 1, currentColor) >= 4) return true;
        // Check diagonal up-right
        if (countConsecutive(row, col, -1, 1, currentColor) >= 4) return true;



        // Update stats in the database
        String winner = isPlayer1Turn ? GameLauncher.player1Name : GameLauncher.player2Name;
        String loser = isPlayer1Turn ? GameLauncher.player2Name : GameLauncher.player1Name;
        JOptionPane.showMessageDialog(this, winner + " wins!");

        // Update stats in the database
        DatabaseManager.updateStats(winner, "Connect 4", true);
        DatabaseManager.updateStats(loser, "Connect 4", false);

        dispose();
        return false;
    }

    private int countConsecutive(int row, int col, int rowDelta, int colDelta, Color color) {
        int count = 1; // Start with the current position

        // Check in the positive direction
        int r = row + rowDelta;
        int c = col + colDelta;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c].getBackground() == color) {
            count++;
            r += rowDelta;
            c += colDelta;
        }

        // Check in the negative direction
        r = row - rowDelta;
        c = col - colDelta;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c].getBackground() == color) {
            count++;
            r -= rowDelta;
            c -= colDelta;
        }

        return count;
    }
}
class TicTacToeGame extends JFrame {
    private final JButton[][] board = new JButton[3][3];
    private boolean isPlayer1Turn = true;

    public TicTacToeGame() {
        setTitle("Tic-Tac-Toe");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(3, 3));

        // Initialize Tic-Tac-Toe board
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                board[row][col] = new JButton("");
                board[row][col].setFont(new Font("Arial", Font.BOLD, 60));
                board[row][col].setFocusPainted(false);
                final int r = row, c = col;
                board[row][col].addActionListener(e -> makeMove(r, c));
                add(board[row][col]);
            }
        }

        setSize(400, 400);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void makeMove(int row, int col) {
        if (!board[row][col].getText().equals("")) {
            return; // Invalid move
        }

        board[row][col].setText(isPlayer1Turn ? "X" : "O");
        board[row][col].setForeground(isPlayer1Turn ? Color.RED : Color.BLUE);

        if (checkWin()) {
            String winner = isPlayer1Turn ? GameLauncher.player1Name : GameLauncher.player2Name;
            String loser = isPlayer1Turn ? GameLauncher.player2Name : GameLauncher.player1Name;
            JOptionPane.showMessageDialog(this, "Player " + (isPlayer1Turn ? "1 (X)" : "2 (O)") + " wins!");

            // Update stats in the database
            DatabaseManager.updateStats(winner, "Tic-Tac-Toe", true);
            DatabaseManager.updateStats(loser, "Tic-Tac-Toe", false);

            dispose();
            return;
        } else if (isBoardFull()) {
            JOptionPane.showMessageDialog(this, "It's a draw!");
            dispose();
            return;
        }

        isPlayer1Turn = !isPlayer1Turn;
    }

    private boolean checkWin() {
        String currentPlayerSymbol = isPlayer1Turn ? "X" : "O";

        // Check rows, columns, and diagonals
        for (int i = 0; i < 3; i++) {
            if (board[i][0].getText().equals(currentPlayerSymbol) &&
                    board[i][1].getText().equals(currentPlayerSymbol) &&
                    board[i][2].getText().equals(currentPlayerSymbol)) return true;

            if (board[0][i].getText().equals(currentPlayerSymbol) &&
                    board[1][i].getText().equals(currentPlayerSymbol) &&
                    board[2][i].getText().equals(currentPlayerSymbol)) return true;
        }

        // Check diagonals
        if (board[0][0].getText().equals(currentPlayerSymbol) &&
                board[1][1].getText().equals(currentPlayerSymbol) &&
                board[2][2].getText().equals(currentPlayerSymbol)) return true;

        if (board[0][2].getText().equals(currentPlayerSymbol) &&
                board[1][1].getText().equals(currentPlayerSymbol) &&
                board[2][0].getText().equals(currentPlayerSymbol)) return true;

        return false;
    }

    private boolean isBoardFull() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (board[row][col].getText().equals("")) {
                    return false;
                }
            }
        }
        return true;
    }
}
class UnoGame extends JFrame {
    private static final String[] COLORS = {"Red", "Blue", "Green", "Yellow"};
    private static final String[] VALUES = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "Skip", "Reverse", "+2", "Wild", "Wild+4"};

    private ArrayList<Card> deck = new ArrayList<>();
    private ArrayList<Card> player1Hand = new ArrayList<>();
    private ArrayList<Card> player2Hand = new ArrayList<>();
    private Card topCard;
    private boolean isPlayer1Turn = true;

    private JPanel player1Panel;
    private JPanel player2Panel;
    private JPanel centerPanel;
    private JButton drawButton;
    private JLabel topCardLabel;
    private JLabel turnLabel;

    public UnoGame() {
        setTitle("UNO Game");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        initializeGame();
        createGUI();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeGame() {
        // Create deck
        for (String color : COLORS) {
            for (String value : VALUES) {
                if (value.equals("Wild") || value.equals("Wild+4")) {
                    deck.add(new Card("Black", value));
                } else {
                    deck.add(new Card(color, value));
                }
            }
        }

        // Shuffle deck
        Collections.shuffle(deck);

        // Deal initial cards
        for (int i = 0; i < 7; i++) {
            player1Hand.add(deck.remove(0));
            player2Hand.add(deck.remove(0));
        }

        // Set initial top card
        do {
            topCard = deck.remove(0);
        } while (topCard.getColor().equals("Black"));
    }

    private void createGUI() {
        // Create player panels
        player1Panel = new JPanel(new FlowLayout());
        player2Panel = new JPanel(new FlowLayout());
        updatePlayerPanels();

        // Create center panel
        centerPanel = new JPanel(new FlowLayout());
        topCardLabel = new JLabel();
        updateTopCardLabel();

        // Create turn label
        turnLabel = new JLabel("Player 1's turn", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 18));

        // Create draw button with UNO logo and set a larger size
        drawButton = new JButton();
        ImageIcon unoLogo = new ImageIcon("uno_logo.png"); // Ensure this path is correct
        drawButton.setIcon(unoLogo);
        drawButton.setPreferredSize(new Dimension(100, 150));
        drawButton.addActionListener(e -> drawCard());

        centerPanel.add(turnLabel);
        centerPanel.add(topCardLabel);
        centerPanel.add(drawButton);

        add(player1Panel, BorderLayout.SOUTH);
        add(player2Panel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void updatePlayerPanels() {
        player1Panel.removeAll();
        player2Panel.removeAll();

        // Update Player 1's panel
        for (Card card : player1Hand) {
            JButton cardButton = createCardButton(card, isPlayer1Turn); // Show cards only if it's Player 1's turn
            player1Panel.add(cardButton);
        }

        // Update Player 2's panel
        for (Card card : player2Hand) {
            JButton cardButton = createCardButton(card, !isPlayer1Turn); // Show cards only if it's Player 2's turn
            player2Panel.add(cardButton);
        }

        revalidate();
        repaint();
    }

    private JButton createCardButton(Card card, boolean isVisible) {
        JButton button = new JButton();
        if (isVisible) {
            button.setText(card.toString());
            button.setBackground(getColorFromString(card.getColor()));
            button.addActionListener(e -> playCard(card));
        } else {
            button.setText(""); // Hide text
            button.setBackground(Color.BLACK); // Black out the card
        }
        button.setPreferredSize(new Dimension(80, 120));
        button.setOpaque(true);
        button.setForeground(Color.WHITE); // Ensure text is readable if shown
        return button;
    }


    private void playCard(Card card) {
        ArrayList<Card> currentHand = isPlayer1Turn ? player1Hand : player2Hand;

        if (isValidPlay(card)) {
            currentHand.remove(card);
            topCard = card;

            // Handle special cards
            handleSpecialCard(card);

            // Check for win
            if (currentHand.isEmpty()) {
                String winner = isPlayer1Turn ? GameLauncher.player1Name : GameLauncher.player2Name;
                String loser = isPlayer1Turn ? GameLauncher.player2Name : GameLauncher.player1Name;
                JOptionPane.showMessageDialog(this, winner + " wins!");

                // Update stats in the database
                DatabaseManager.updateStats(winner, "UNO", true);
                DatabaseManager.updateStats(loser, "UNO", false);

                dispose();
                return;
            }

            isPlayer1Turn = !isPlayer1Turn;
            turnLabel.setText("Player " + (isPlayer1Turn ? "1" : "2") + "'s turn");
            updatePlayerPanels();
            updateTopCardLabel();
        }
    }

    private boolean isValidPlay(Card card) {
        return card.getColor().equals(topCard.getColor()) ||
                card.getValue().equals(topCard.getValue()) ||
                card.getColor().equals("Black");
    }

    private void handleSpecialCard(Card card) {
        // Handle Skip, Reverse, +2, Wild, and Wild+4
        switch (card.getValue()) {
            case "Skip":
                isPlayer1Turn = !isPlayer1Turn; // Skip the next player's turn
                break;
            case "+2":
                ArrayList<Card> opponentHand = isPlayer1Turn ? player2Hand : player1Hand;
                for (int i = 0; i < 2 && !deck.isEmpty(); i++) {
                    opponentHand.add(deck.remove(0));
                }
                break;
            case "Wild":
                topCard.setColor(chooseColor());
                break;
            case "Wild+4":
                topCard.setColor(chooseColor());
                ArrayList<Card> opponentHandWild = isPlayer1Turn ? player2Hand : player1Hand;
                for (int i = 0; i < 4 && !deck.isEmpty(); i++) {
                    opponentHandWild.add(deck.remove(0));
                }
                break;
        }
    }

    private String chooseColor() {
        String[] options = COLORS;
        return (String) JOptionPane.showInputDialog(
                this,
                "Choose a color:",
                "Color Selection",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
    }

    private void drawCard() {
        if (!deck.isEmpty()) {
            ArrayList<Card> currentHand = isPlayer1Turn ? player1Hand : player2Hand;
            currentHand.add(deck.remove(0));
            updatePlayerPanels();
            isPlayer1Turn = !isPlayer1Turn; // Switch turn after drawing
            turnLabel.setText("Player " + (isPlayer1Turn ? "1" : "2") + "'s turn");
        }
    }

    private void updateTopCardLabel() {
        topCardLabel.setText("Current Card: " + topCard.toString());

        // Set background color based on top card color
        switch (topCard.getColor()) {
            case "Red":
                topCardLabel.setBackground(Color.RED);
                break;
            case "Blue":
                topCardLabel.setBackground(Color.BLUE);
                break;
            case "Green":
                topCardLabel.setBackground(Color.GREEN);
                break;
            case "Yellow":
                topCardLabel.setBackground(new Color(255, 165, 0)); // Dark orange/gold for yellow
                break;
            default:
                topCardLabel.setBackground(Color.BLACK); // Black for wild cards
                break;
        }
        topCardLabel.setOpaque(true);
        topCardLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topCardLabel.setForeground(Color.WHITE); // Text color on colored background
    }

    private void setPlayerHandsEnabled() {
        for (Component button : player1Panel.getComponents()) {
            button.setEnabled(isPlayer1Turn);
        }
        for (Component button : player2Panel.getComponents()) {
            button.setEnabled(!isPlayer1Turn);
        }
    }

    private Color getColorFromString(String colorStr) {
        switch (colorStr) {
            case "Red": return Color.RED;
            case "Blue": return Color.BLUE;
            case "Green": return Color.GREEN;
            case "Yellow": return Color.YELLOW;
            default: return Color.BLACK;
        }
    }
}
class Card {
    private String color;
    private final String value;

    public Card(String color, String value) {
        this.color = color;
        this.value = value;
    }

    public String getColor() { return color; }
    public String getValue() { return value; }
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() {
        return color + " " + value;
    }
}






class MemoryGame extends JFrame {
    private static final int SIZE = 6;
    private final JButton[][] buttons = new JButton[SIZE][SIZE];
    private final String[][] tileValues = new String[SIZE][SIZE];
    private boolean isPlayer1Turn = true;
    private int flippedCount = 0;
    private int player1Matches = 0;
    private int player2Matches = 0;
    private JButton firstButton = null;
    private JButton secondButton = null;

    public MemoryGame() {
        setTitle("Memory Game");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(SIZE, SIZE));
        setSize(600, 600);

        // Initialize tile values (pairs from "A" to "R")
        ArrayList<String> values = new ArrayList<>();
        for (char c = 'A'; c <= 'R'; c++) {
            values.add(String.valueOf(c));
            values.add(String.valueOf(c));
        }
        Collections.shuffle(values);

        // Set up buttons and assign values
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                JButton button = new JButton();
                button.setFont(new Font("Arial", Font.BOLD, 24));
                button.setBackground(Color.LIGHT_GRAY);
                button.setFocusPainted(false);
                tileValues[i][j] = values.remove(0);
                button.addActionListener(new TileClickListener(i, j, button));
                buttons[i][j] = button;
                add(button);
            }
        }

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void flipTile(int row, int col, JButton button) {
        if (flippedCount == 0) {
            // First tile flipped
            firstButton = button;
            firstButton.setText(tileValues[row][col]);
            firstButton.setEnabled(false);
            flippedCount = 1;
        } else if (flippedCount == 1) {
            // Second tile flipped
            secondButton = button;
            secondButton.setText(tileValues[row][col]);
            secondButton.setEnabled(false);
            flippedCount = 2;

            // Check for match after a short delay
            Timer timer = new Timer(500, e -> checkMatch());
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void checkMatch() {
        if (firstButton.getText().equals(secondButton.getText())) {
            // Matching pair found, keep both tiles revealed
            firstButton.setBackground(isPlayer1Turn ? Color.RED : Color.BLUE);
            secondButton.setBackground(isPlayer1Turn ? Color.RED : Color.BLUE);
            if (isPlayer1Turn) {
                player1Matches++;
            } else {
                player2Matches++;
            }
            firstButton = null;
            secondButton = null;
            flippedCount = 0;

            // Check if the game is won
            checkWin();
        } else {
            // No match, flip both tiles back over
            firstButton.setText("");
            secondButton.setText("");
            firstButton.setEnabled(true);
            secondButton.setEnabled(true);
            firstButton = null;
            secondButton = null;
            flippedCount = 0;

            // Switch turn
            isPlayer1Turn = !isPlayer1Turn;
            JOptionPane.showMessageDialog(this,
                    (isPlayer1Turn ? "Player 1's" : "Player 2's") + " turn");
        }
    }

    private void checkWin() {
        int totalPairs = (SIZE * SIZE) / 2;
        int matchedPairs = player1Matches + player2Matches;

        if (matchedPairs == totalPairs) {
            String winner = (player1Matches>player2Matches) ? GameLauncher.player1Name : GameLauncher.player2Name;
            String loser = (player1Matches>player2Matches) ? GameLauncher.player2Name : GameLauncher.player1Name;
            JOptionPane.showMessageDialog(this, "Player " + (isPlayer1Turn ? GameLauncher.player1Name : GameLauncher.player2Name + " wins!"));

            // Update stats in the database
            DatabaseManager.updateStats(winner, "memory_game", true);
            DatabaseManager.updateStats(loser, "memory_game", false);
            dispose();
        }
    }



    private class TileClickListener implements ActionListener {
        private final int row;
        private final int col;
        private final JButton button;

        public TileClickListener(int row, int col, JButton button) {
            this.row = row;
            this.col = col;
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (flippedCount < 2) {
                flipTile(row, col, button);
            }
        }
    }
}
