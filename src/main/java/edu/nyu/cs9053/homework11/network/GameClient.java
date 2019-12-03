package edu.nyu.cs9053.homework11.network;

import edu.nyu.cs9053.homework11.game.Difficulty;
import edu.nyu.cs9053.homework11.game.GameProvider;
import edu.nyu.cs9053.homework11.game.screen.InputMove;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * User: blangel
 *
 * A blocking IO implementation of a client which requests moves from a remote server implementing the
 * {@linkplain edu.nyu.cs9053.homework11.network.NetworkGameProvider}
 */
public class GameClient implements GameProvider {

    public static GameClient construct(Difficulty difficulty) {
        try {
            Socket gameClientSocket = new Socket(GameServer.SERVER_HOST, GameServer.SERVER_PORT);
            try {
                gameClientSocket.setKeepAlive(true);
                return new GameClient(difficulty, gameClientSocket.getInputStream(), gameClientSocket.getOutputStream());
            } catch (IOException ioe) {
                gameClientSocket.close();
                throw new RuntimeException(ioe);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private final Difficulty difficulty;

    private final InputStream serverInput;

    private final OutputStream serverOutput;

    public GameClient(Difficulty difficulty, InputStream serverInput, OutputStream serverOutput) {
        this.difficulty = difficulty;
        this.serverInput = serverInput;
        this.serverOutput = serverOutput;
    }

    @Override
    public Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public int getRandomNumberOfNextFoes() {
        String tempReadLine = "";
        PrintWriter writerToServer = new PrintWriter(serverOutput, true);
        BufferedReader readerFromServer = new BufferedReader(new InputStreamReader(serverInput, StandardCharsets.UTF_8));
        try {
            writerToServer.println("foes " + getDifficulty());
            tempReadLine = readerFromServer.readLine();
        } catch (IOException ioe) {
            try {
                serverOutput.close();
                serverInput.close();
            } catch (IOException ioe2) {
                throw new RuntimeException(ioe2);
            }
            throw new RuntimeException(ioe);
        }
        return Integer.parseInt(tempReadLine);
    }

    @Override
    public InputMove getRandomNextMove() {
        String tempReadLine = "";
        PrintWriter writerToServer = new PrintWriter(serverOutput, true);
        BufferedReader readerFromServer = new BufferedReader(new InputStreamReader(serverInput, StandardCharsets.UTF_8));
        try {
            writerToServer.println("move");
            tempReadLine = readerFromServer.readLine();
        } catch (IOException ioe) {
            try {
                serverOutput.close();
                serverInput.close();
            } catch (IOException ioe2) {
                throw new RuntimeException(ioe2);
            }
            throw new RuntimeException(tempReadLine);
        }
        switch (tempReadLine) {
            case "Up":
                return InputMove.Up;
            case "Down":
                return InputMove.Down;
            case "Left":
                return InputMove.Left;
            case "Right":
                return InputMove.Right;
            default:
                return null;
        }
    }

}
