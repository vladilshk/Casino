import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    Socket clientSocket;
    BufferedWriter writer;
    BufferedReader reader;

    public Client() throws IOException, InterruptedException {
        try {
            clientSocket = new Socket("localhost", 50003);
        }
        catch (Exception e){
            System.out.println("Casino doesn't work(((");
            System.exit(0);
        }
        writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        Thread sendThread = new Thread() {
            public void run() {
                while (clientSocket.isConnected()) {
                    try {
                        sendMessage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        Thread receiveThread = new Thread() {
            public void run() {
                try {
                    getMessage();
                } catch (IOException e) {

                }
            }
        };

        sendThread.start();
        receiveThread.start();
        sendThread.join();
    }

    private void sendMessage() throws IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String bet = getBet();
            writer.write(bet);
            writer.newLine();
            writer.flush();
        }
    }

    private String getBet() throws IOException {
        while (true) {
            String regex = "^@bet +[0-9]$";
            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            if (message.equals("@quit")) {
                System.out.println("Come back if you want to lose some more money))");
                closeAll();
            }
            if (message.matches(regex)) {
                return String.valueOf(message.charAt(5));
            }
            System.out.println("Wrong input, try again.");
        }
    }

    public void getMessage() throws IOException {
        while (true) {
            String message = reader.readLine();
            if (message == null) {
                System.out.println("Casino was closed(((");
                closeAll();
            } else
                System.out.println(message);
        }
    }

    private void closeAll() throws IOException {
            reader.close();
            writer.close();
            clientSocket.close();
            System.exit(0);
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client();
    }
}
