package edu.nyu.cs9053.homework11.network;

import edu.nyu.cs9053.homework11.game.Difficulty;
import edu.nyu.cs9053.homework11.game.screen.InputMove;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * User: blangel
 *
 * A NIO implementation of a NetworkGameProvider.
 *
 * The server takes the following commands:
 * <pre>
 *     foes Difficulty
 * </pre>
 * <pre>
 *     move
 * </pre>
 * where the String "foes Easy" would be a call to {@linkplain NetworkGameProvider#getRandomNumberOfNextFoes(String)}
 * with "Easy"
 * and a call using String "move" would be a call to {@linkplain NetworkGameProvider#getRandomNextMove()}
 */
public class GameServer implements NetworkGameProvider, Runnable {

    public static final String SERVER_HOST = "localhost";

    public static final int SERVER_PORT = 8080;

    public static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(SERVER_HOST, SERVER_PORT);

    private final Random random;

    private final Selector selector;

    private final ServerSocketChannel listeningChannel;

    private final Map<SocketChannel, ByteBuffer> writeBuffers;

    public static void main(String[] args) throws IOException {
        GameServer server = new GameServer();
        server.run();
    }

    public GameServer() throws IOException {
        this.random = new Random();
        this.selector = Selector.open();
        this.listeningChannel = ServerSocketChannel.open();
        configureChannel();
        this.writeBuffers = new HashMap<>();
    }

    private void configureChannel() throws IOException{
        listeningChannel.configureBlocking(false);
        listeningChannel.bind(SERVER_ADDRESS);
        listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public String getRandomNumberOfNextFoes(String difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("Difficulty cannot be null");
        }
        if (difficulty.equals("Easy")) {
            return Integer.toString(random.nextInt(Difficulty.Easy.getLevel() + 1));      //Because .nextInt() method return value between 0(inclusive) and the specified value(exclusive)
        } else if (difficulty.equals("Medium")) {
            return Integer.toString(random.nextInt(Difficulty.Medium.getLevel() + 1));
        } else if (difficulty.equals("Hard")) {
            return Integer.toString(random.nextInt(Difficulty.Hard.getLevel() + 1));
        } else {
            return null;        //default
        }
    }

    @Override
    public String getRandomNextMove() {
        if (random.nextBoolean()) {
            if (random.nextBoolean()) {
                return InputMove.Up.toString();
            } else {
                return InputMove.Down.toString();
            }
        } else {
            if (random.nextInt(100) < 95) {
                return InputMove.Left.toString();
            } else {
                return InputMove.Right.toString();
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                runMainProcess();
            } catch (IOException ioe) {
                try {
                    selector.close();
                    listeningChannel.close();
                } catch (IOException ioe2) {
                    throw new RuntimeException(ioe2);
                }
                Thread.currentThread().interrupt();
                throw new RuntimeException(ioe);
            }
        }
    }

    private void runMainProcess() throws IOException {
        int readyCount = selector.select();
        if (readyCount == 0) {
            return;
        }
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            if (key.isAcceptable()) {
                acceptChannelSocket(key);
            } else if (key.isReadable()) {
                readChannelSocket(key);
            } else if (key.isWritable()) {
                writeChannelSocket(key);
            }
            keyIterator.remove();
        }
    }

    private void acceptChannelSocket(SelectionKey key) throws IOException {
        SocketChannel clientConnection = listeningChannel.accept();
        clientConnection.configureBlocking(false);
        clientConnection.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        System.out.printf("[%s] is accepted.%n", clientConnection.getRemoteAddress().toString());
    }

    private void readChannelSocket(SelectionKey key) throws IOException {
        SocketChannel clientConnection = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(64);
        clientConnection.read(readBuffer);
        readBuffer.flip();
        String clientCommand = new String(readBuffer.array(), "UTF-8").split("\n")[0].trim();
        String response = "";
        if (!clientCommand.equals("")) {
            if (clientCommand.contains("foes")) {
                response = getRandomNumberOfNextFoes(clientCommand.split(" ")[1]);
            } else if (clientCommand.contains("move")) {
                response = getRandomNextMove();
            }
            writeBuffers.put(clientConnection, ByteBuffer.wrap(response.getBytes("UTF-8")));
            System.out.printf("[%s] ---> Client Command: %s%n", clientConnection.getRemoteAddress().toString(), clientCommand);
        }
    }

    private void writeChannelSocket(SelectionKey key) throws IOException {
        SocketChannel clientConnection = (SocketChannel) key.channel();
        synchronized (writeBuffers) {
            if (writeBuffers.get(clientConnection) != null) {
                ByteBuffer writeBuffer = ByteBuffer.allocate(64);
                writeBuffer.put(writeBuffers.get(clientConnection));
                writeBuffer.put((byte) '\n');
                writeBuffer.flip();
                clientConnection.write(writeBuffer);
                writeBuffers.remove(clientConnection);
                System.out.printf("[%s] ---> Server Response: %s%n", clientConnection.getRemoteAddress().toString(), new String(writeBuffer.array(), "UTF-8").trim());
            }
        }
    }

}
