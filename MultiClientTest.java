package edu.nyu.cs9053.homework11.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import edu.nyu.cs9053.homework11.game.Difficulty;

public class MultiClientTest {
    
    private static final int CLIENT = 5;
    private static final int RETRIES = 10;
    private static final int REQUEST_PERIOD = 1000; // ms
    private static final long TERMINATE_WAIT = 3000L; // ms
    private static final long TEST_LENGTH = 10000L; // ms
    
    public static void main(String[] args) throws IOException {
        Random random = new Random();
        
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GameServer.main(null);
                    System.out.printf("Server has shut down properly%n");
                } catch (Exception e) {
                    System.out.printf("GameServer Error%n");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
        server.start();
        waitServerLive();
        
        ExecutorService executor = Executors.newFixedThreadPool(CLIENT);
        
        Runnable client = new Runnable() {
            private final AtomicInteger existedClientCount = new AtomicInteger(0);
            
            @Override
            public void run() {
                GameClient client = GameClient.construct(Difficulty.Hard);
                int clientID = existedClientCount.incrementAndGet();
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(random.nextInt(REQUEST_PERIOD));
                        System.out.printf("[Client %d]: nextMove is %s, nextFoe is %d%n"
                                , clientID
                                , client.getRandomNextMove().toString()
                                , client.getRandomNumberOfNextFoes());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        System.out.printf("Unexpected client error - %s%n", e.getMessage());
                    }
                }
            }
        };
        
        for (int i = 0; i < CLIENT; i++) {
            executor.submit(client);
        }
        
        try {
            Thread.sleep(TEST_LENGTH);
        } catch (InterruptedException ie) {
            System.out.printf("Test Interrputed%n");
            System.exit(1);
        }
        
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(TERMINATE_WAIT, TimeUnit.MILLISECONDS)) {
                System.out.printf("Executor failed to complete all task%n");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        
        server.interrupt();
        try {
            server.join(TERMINATE_WAIT);
            if (server.isAlive()) {
                System.out.printf("Server failed to shutdown%n");
            }
            System.out.printf("Finished test%n");
        } catch (InterruptedException ie) {
            System.out.printf("Shutdown Interrputed%n");
        }
        
        System.exit(0);
    }
    
    private static void waitServerLive() {
        int retries = RETRIES;
        Socket socket = new Socket();
        while(retries > 0) {
            try
            {
                socket.connect(new InetSocketAddress(GameServer.SERVER_HOST, GameServer.SERVER_PORT));
                socket.close();
                return;
            } catch(IOException ioe) {
                System.out.printf("Server socket unreachable, retrying...%n");
                try {
                    Thread.sleep(1000L);
                    retries--;
                } catch (InterruptedException ie) {
                    System.out.printf("Initialization interrupted%n");
                    System.exit(1);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }
        throw new RuntimeException("Server cannot be reached");
    }
    
}
