package communicator;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Klasa obsługująca serwer komunikatora
 * @author Mikołaj Soska
 */
public class Server implements Runnable{
    private static ArrayList<String> allUsers = new ArrayList<>(); /**lista wszystkich dostepnych uzytkownikow**/
    private static ArrayList<String> onlineUsers = new ArrayList<>(); /**zalogowani uzytkownicy**/
    private static HashMap<String, Socket> users = new HashMap<>(); /**zawiera wszystkie sockety wraz z identyfikatorami, ktorego uzytkownika sa**/
    private static HashMap<String, ArrayList<Message>> notSentMessages = new HashMap<>(); /**zawiera niewyslane wiadomosci oczekujace na zalogowanie sie uzytkownika**/
    private Socket socket;

    /**
     * Konstruktor przekazujacy sokcet do nowego watku
     * @param socket socket odebrany od uzytkownika
     */
    private Server(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            os.writeObject(allUsers);

            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
            String ID = (String) is.readObject();

            if(ID != null) {
                System.out.println("Połączono z użytkownikiem: " + ID);
                onlineUsers.add(ID);
                os.writeObject(onlineUsers);
                writeUsers(); //synchronizujemy liste dostepnych uzytkownikow u kazdego pozostalego zalogowanego
                users.put(ID, socket);

                if (notSentMessages.containsKey(ID)) { //przechowujemy wiadomosci gdy ktos nie jest online
                    for (Message m : notSentMessages.get(ID)) {
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.writeObject(m);
                    }
                    notSentMessages.remove(ID);
                }

                for (; ; ) {
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) in.readObject();

                    if (message == null) //zakonczenie dzialania
                        break;

                    if(message.transTest != 0){
                        System.out.println("Pole transient zostało przesłane. "); //nie powinno sie wypisywac
                    }

                    String recipientID = message.getRecipientID();
                    if (onlineUsers.contains(recipientID)) {
                        Socket sock = users.get(recipientID);
                        ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
                        out.writeObject(message);
                    } else {
                        if (notSentMessages.containsKey(recipientID))
                            notSentMessages.get(recipientID).add(message);
                        else {
                            ArrayList<Message> list = new ArrayList<>();
                            list.add(message);
                            notSentMessages.put(recipientID, list);
                        }
                    }
                }
                onlineUsers.remove(ID);
                users.remove(ID);
                writeUsers(); //synchronizujemy liste dostepnych uzytkownikow u kazdego pozostalego zalogowanego
                System.out.println("Rozłączono z użytkownikiem: " + ID);
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Błąd wejścia/wyjścia");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Nie znaleziono danej klasy.");
            e.printStackTrace();
        }
    }

    /**
     * Wysyla liste zalogowanych uzytkownikow do klienta
     * @throws IOException w razie bledu wejscia/wyjscia
     */
    private void writeUsers() throws IOException {
        for(Socket s : users.values()){
            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            os.writeObject(onlineUsers);
        }
    }
    public static void main(String args[]){
        try {
            FileInputStream stream = new FileInputStream(new File("users.txt"));
            Scanner scanner = new Scanner(stream);

            while (scanner.hasNext())
                allUsers.add(scanner.nextLine()); //wczytujemy liste uztykownikow ktorzy moga korzystac z komunikatora

            ServerSocket serverSocket = new ServerSocket(88,50);
            System.out.println("Serwer został uruchomiony");
            System.out.println(InetAddress.getLocalHost());
            for(;;){
                Thread sock = new Thread(new Server(serverSocket.accept()));
                sock.start();
            }
        } catch (IOException e) {
            System.out.println("Błąd wejścia/wyjścia");
            e.printStackTrace();
        }
    }
}