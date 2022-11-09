import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static Map<String, Connection> clients;
    private static Lock clientsMapLock;
    private static Map<String, Integer> bets;
    private static Lock betLock;
    private ServerSocket serverSocket;
    private String startRoundMessage = "A new game starts. You can make your bet.\n" +
            "Input \"@bet number\" ( number is a value from 0 to 9).";

    private int roundTime = 5;


    public Server() throws IOException, InterruptedException {
        betLock = new ReentrantLock();
        clientsMapLock = new ReentrantLock();
        serverSocket = new ServerSocket(50003);

        Thread connection = new Thread() {
            public void run() {
                try {
                    waitForConnection();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Thread game = new Thread() {
            public void run() {
                try {
                    startRound();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        connection.start();
        game.start();
        connection.join();
        game.join();

        clients = new HashMap<>();
    }

    private void waitForConnection() throws IOException {
        Integer counter = 0;
        clients = new HashMap<>();
        while (true) {
            Socket socket = serverSocket.accept();
            clients.put(counter.toString(), new Connection(counter.toString(), socket));
            clients.get(counter.toString()).sendMessage(startRoundMessage);
            System.out.println("Info: Client " + counter + " was connected");
            counter++;
        }
    }


    private void startRound() throws IOException, InterruptedException {
        Random random = new Random();
        int value;
        while (true) {
            bets = new HashMap<>();
            value = random.nextInt(10);
            System.out.println("Round starts, value is: " + value);

            Thread.sleep(roundTime * 1000);
            if(!bets.isEmpty())
                checkWinner(value);
        }
    }

    private void checkWinner(int value) throws IOException {
        clientsMapLock.lock();
        for (String name : bets.keySet()) {
            if (bets.get(name).equals(value))
                clients.get(name).sendMessage("You win!!!\n\n" + startRoundMessage);
            else
                clients.get(name).sendMessage("Yobana rot etogo casino. You lose((( Value was " + value + ".\n\n" + startRoundMessage);
        }
        clientsMapLock.unlock();
    }

    protected static void addBet(String name, int bet){
        betLock.lock();
        bets.put(name, bet);
        betLock.unlock();
    }



    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server();
    }

    protected static void deleteUser(String name){
        clientsMapLock.lock();
        betLock.lock();
        clients.remove(name);
        bets.remove(name);
        System.out.println("Info: Client " + name + " was disconnected");
        betLock.unlock();
        clientsMapLock.unlock();
    }


}

class Connection extends Thread {
    private String name;
    private Socket socket;
    private InputStreamReader in;
    private OutputStreamWriter out;

    public Connection(String name, Socket socket) throws IOException {
        this.name = name;
        this.socket = socket;
        getMessage();
    }

    public void getMessage() throws IOException {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedReader reader;
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    int bet;
                    while (true) {
                        try {
                            bet = Integer.parseInt(reader.readLine());
                            Server.addBet(name, bet);
                        }
                        catch (Exception e){
                            reader.close();
                            socket.close();
                            Server.deleteUser(name);
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        thread.start();
    }

    public void sendMessage(String message) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        writer.write(message);
        writer.newLine();
        writer.flush();
    }
}
