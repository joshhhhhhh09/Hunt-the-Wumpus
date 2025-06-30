import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;

public class HuntTheWumpus extends JPanel implements KeyListener {

    private static final int SCREEN_WIDTH = 1000;
    private static final int SCREEN_HEIGHT = 1000;
    private static final int NUM_BATS = 3;
    private static final int NUM_PITS = 3;
    private static final int NUM_ARROWS = 0;

    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;

    private static final Color BROWN = new Color(193, 154, 107);
    private static final Color RED = new Color(138, 7, 7);

    private int playerPos = 0;
    private int wumpusPos = 0;
    private int numArrows = 1;
    private boolean mobileWumpus = true;
    private int wumpusMoveChance = 50;
    private boolean gameOver = false;
    private String gameMessage = "";

    private BufferedImage playerImg, wumpusImg, batImg, arrowImg, pitImg;

    private HashMap<Integer, int[]> cave = new HashMap<>();
    private java.util.List<Integer> batsList = new ArrayList<>();
    private java.util.List<Integer> pitsList = new ArrayList<>();
    private java.util.List<Integer> arrowsList = new ArrayList<>();

    public HuntTheWumpus() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        initCave();
        loadImages();
        resetGame();
    }

    private void initCave() {
        int[][] data = {
            {0,8,2,5},{0,10,3,1},{0,12,4,2},{0,14,5,3},{0,6,1,4},
            {5,0,7,15},{0,17,8,6},{1,0,9,7},{0,18,10,8},{2,0,11,9},
            {0,19,12,10},{3,0,13,11},{0,20,14,12},{4,0,15,13},
            {0,16,6,14},{15,0,17,20},{7,0,18,16},{9,0,19,17},
            {11,0,20,18},{13,0,16,19}
        };
        for (int i = 0; i < data.length; i++) cave.put(i + 1, data[i]);
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("Could not load image: " + path);
            return null;
        }
    }

    private void loadImages() {
        playerImg = loadImage("images/player.png");
        wumpusImg = loadImage("images/wumpus.png");
        batImg = loadImage("images/bat.png");
        arrowImg = loadImage("images/arrow.png");
        pitImg = loadImage("images/pit.png");
    }

    private void playSound(String soundFile) {
        try {
            File file = new File("sounds/" + soundFile);
            if (!file.exists()) {
                System.err.println("Missing sound: " + soundFile);
                return;
            }
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("Error playing sound: " + soundFile + " - " + e.getMessage());
        }
    }

    private void resetGame() {
        gameOver = false;
        gameMessage = "";
        batsList.clear();
        pitsList.clear();
        arrowsList.clear();

        Random rand = new Random();
        playerPos = rand.nextInt(20) + 1;
        do { wumpusPos = rand.nextInt(20) + 1; } while (wumpusPos == playerPos);

        for (int i = 0; i < NUM_BATS; i++) placeEntity(rand, batsList);
        for (int i = 0; i < NUM_PITS; i++) placeEntity(rand, pitsList);
        for (int i = 0; i < NUM_ARROWS; i++) placeEntity(rand, arrowsList);

        numArrows = 1;
        repaint();
    }

    private void placeEntity(Random rand, java.util.List<Integer> list) {
        int pos;
        do {
            pos = rand.nextInt(20) + 1;
        } while (pos == playerPos || list.contains(pos) || pos == wumpusPos);
        list.add(pos);
    }

    private void checkRoom() {
        if (playerPos == wumpusPos) {
            playSound("wumpus.wav");
            endGame("You were eaten by the WUMPUS!");
        } else if (pitsList.contains(playerPos)) {
            playSound("pit.wav");
            endGame("You fell into a bottomless pit! Press 'R' to restart.");
        } else {
            if (batsList.contains(playerPos)) {
                playSound("bats.wav");
                Random rand = new Random();
                batsList.remove((Integer) playerPos);
                int newBatPos;
                do {
                    newBatPos = rand.nextInt(20) + 1;
                } while (batsList.contains(newBatPos) || newBatPos == wumpusPos || pitsList.contains(newBatPos));
                batsList.add(newBatPos);

                int newPlayerPos;
                do {
                    newPlayerPos = rand.nextInt(20) + 1;
                } while (newPlayerPos == playerPos || newPlayerPos == wumpusPos || pitsList.contains(newPlayerPos));
                playerPos = newPlayerPos;
                gameMessage = "Bats picked you up and dropped you elsewhere!";
            }

            if (arrowsList.contains(playerPos)) {
                playSound("arrow.wav");
                numArrows++;
                arrowsList.remove((Integer) playerPos);
                gameMessage = "You found an arrow!";
            }
        }
    }

    private void endGame(String message) {
        gameOver = true;
        gameMessage = message;
        repaint();
    }

    private void shootArrow(int direction) {
        if (numArrows == 0) return;
        numArrows--;
        int targetRoom = cave.get(playerPos)[direction];
        if (targetRoom == wumpusPos) {
            playSound("victory.wav");
            endGame("Your aim was true! You killed the Wumpus!");
        } else {
            playSound("miss.wav");
            Random rand = new Random();
            do { wumpusPos = rand.nextInt(20) + 1; } while (wumpusPos == playerPos);
            if (numArrows == 0) endGame("Out of arrows. You have died! Press 'R' to restart.");
            else gameMessage = "You missed. The Wumpus may have moved...";
        }
    }

    private boolean isNear(java.util.List<Integer> list) {
        int[] exits = cave.get(playerPos);
        for (int room : exits) if (list.contains(room)) return true;
        return false;
    }

    private boolean isWumpusNear() {
        int[] exits = cave.get(playerPos);
        for (int room : exits) if (room == wumpusPos) return true;
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g.setColor(BROWN);
        g.fillOval(SCREEN_WIDTH / 4, SCREEN_HEIGHT / 4, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);

        int[] exits = cave.get(playerPos);
        if (exits[LEFT] > 0) g.fillRect(0, SCREEN_HEIGHT / 2 - 40, SCREEN_WIDTH / 4, 80);
        if (exits[RIGHT] > 0) g.fillRect(SCREEN_WIDTH - SCREEN_WIDTH / 4, SCREEN_HEIGHT / 2 - 40, SCREEN_WIDTH / 4, 80);
        if (exits[UP] > 0) g.fillRect(SCREEN_WIDTH / 2 - 40, 0, 80, SCREEN_HEIGHT / 4);
        if (exits[DOWN] > 0) g.fillRect(SCREEN_WIDTH / 2 - 40, SCREEN_HEIGHT - SCREEN_HEIGHT / 4, 80, SCREEN_HEIGHT / 4);

        if (pitImg != null && pitsList.contains(playerPos)) {
            g.drawImage(pitImg, SCREEN_WIDTH / 2 - pitImg.getWidth() / 2, SCREEN_HEIGHT / 2 - pitImg.getHeight() / 2, null);
        } else if (batImg != null && batsList.contains(playerPos)) {
            g.drawImage(batImg, SCREEN_WIDTH / 2 - batImg.getWidth() / 2, SCREEN_HEIGHT / 2 - batImg.getHeight() / 2, null);
        } else if (wumpusImg != null && playerPos == wumpusPos) {
            g.drawImage(wumpusImg, SCREEN_WIDTH / 2 - wumpusImg.getWidth() / 2, SCREEN_HEIGHT / 2 - wumpusImg.getHeight() / 2, null);
        }

        if (!gameOver && playerImg != null) {
            g.drawImage(playerImg, SCREEN_WIDTH / 2 - playerImg.getWidth() / 2, SCREEN_HEIGHT / 2 - playerImg.getHeight() / 2, null);
        }

        g.setColor(Color.GREEN);
        g.drawString("Position: " + playerPos + "  Arrows: " + numArrows, 10, 20);

        int y = 50;
        if (!gameOver) {
            if (isWumpusNear()) { g.drawString("You see bloodstains on the walls.", 10, y); y += 20; }
            if (isNear(batsList)) { g.drawString("You hear the squeaking of bats.", 10, y); y += 20; }
            if (isNear(pitsList)) { g.drawString("You feel a draft.", 10, y); y += 20; }
        }

        if (!gameMessage.isEmpty()) {
            g.setColor(RED);
            g.drawString(gameMessage, 10, y);
        }

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", SCREEN_WIDTH / 2 - 120, SCREEN_HEIGHT / 2 + 200);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Press 'R' to restart or use Game > Restart menu", SCREEN_WIDTH / 2 - 200, SCREEN_HEIGHT / 2 + 240);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (gameOver && key == KeyEvent.VK_R) {
            resetGame();
            return;
        }

        if (gameOver) return;
        boolean shift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        int[] exits = cave.get(playerPos);

        if (key == KeyEvent.VK_LEFT) {
            if (shift) shootArrow(LEFT);
            else if (exits[LEFT] > 0) playerPos = exits[LEFT];
        } else if (key == KeyEvent.VK_RIGHT) {
            if (shift) shootArrow(RIGHT);
            else if (exits[RIGHT] > 0) playerPos = exits[RIGHT];
        } else if (key == KeyEvent.VK_UP) {
            if (shift) shootArrow(UP);
            else if (exits[UP] > 0) playerPos = exits[UP];
        } else if (key == KeyEvent.VK_DOWN) {
            if (shift) shootArrow(DOWN);
            else if (exits[DOWN] > 0) playerPos = exits[DOWN];
        }

        checkRoom();
        repaint();
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hunt the Wumpus");
            HuntTheWumpus gamePanel = new HuntTheWumpus();

            JMenuBar menuBar = new JMenuBar();
            JMenu gameMenu = new JMenu("Game");
            JMenuItem restartItem = new JMenuItem("Restart");
            restartItem.addActionListener(e -> gamePanel.resetGame());
            gameMenu.add(restartItem);
            menuBar.add(gameMenu);

            frame.setJMenuBar(menuBar);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}