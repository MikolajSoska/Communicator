package communicator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Klasa obsługująca proces klienta komunikatora.
 * @author Mikołaj Soska
 */
public class Client extends Application {
    private String ID; /**identyfikator użytkownika **/
    private Socket socket;/** socket do połączenia**/
    private ArrayList<String> allUsers; /**lista wszystkich uzytkownikow **/
    private HashMap<String, Messanger> openMessangers; /**zawiera wszystkie otwarte okna komunikatora **/
    private HashMap<String,ArrayList<Message>> notSentMessages; /**wiadomosci nie wyswietlone przez uzytkownika **/
    private ListView<String> onlineUsers; /**lista uzytkownikow online **/
    private ListView<String> offlineUsers; /**lista uzytkownikow offlinem **/

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Metoda otwiera okno służace do zalogowania sie do komunikatora
     * @param primaryStage scena
     */
    @Override
    public void start(Stage primaryStage) {
        GridPane root = new GridPane();
        TextField textField = new TextField();
        Label label = new Label("Podaj ID użytkownika");
        Label wrongID = new Label("Wprowadzono nieprawidłowe ID");
        Button button = new Button("ZALOGUJ");

        wrongID.setVisible(false);
        button.setPrefWidth(150);
        button.setOnAction(event -> {
            ID = textField.getText();
            if(canLogIn()) { //sprawdzamy czy podane ID jest prawidlowe
                primaryStage.close();
                primaryStage.close();
                createMainWindow();
            }
            else
                wrongID.setVisible(true); //wyswietlamy komunikat jesli nie jest
        });

        root.addColumn(0,label,textField,wrongID,button);
        root.setAlignment(Pos.CENTER);
        GridPane.setHalignment(button, HPos.CENTER);
        GridPane.setHalignment(label, HPos.CENTER);
        root.setVgap(10);

        primaryStage.setTitle("Komunikator");
        primaryStage.setScene(new Scene(root,220,140));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Funkcja sprawdza czy wprowadzono prawidłowy identyfikator użytkowika
     * @return true jesli ID jest prawidłowe; false w przeciwym wypadku
     */
    private boolean canLogIn(){
        try {
            socket = new Socket("LAPTOP-0HUTO8E1",88); //nazwa komputera na którym jest serwer
            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
            allUsers = (ArrayList<String>) is.readObject();
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            if(allUsers.contains(ID)) {
                allUsers.remove(ID);
                os.writeObject(ID);
                onlineUsers = new ListView<>();
                offlineUsers = new ListView<>();
                getUsers((ArrayList<String>) is.readObject());
                return true;
            }
            else{
                os.writeObject(null);
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Błąd wejścia/wyjścia.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Nie znaleziono danej klasy.");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Funckja wyświetla główne okno komunikatora, z listą wszystkich uzytkowników
     */
    private void createMainWindow(){
        openMessangers = new HashMap<>();
        GridPane pane = new GridPane();
        onlineUsers.setPrefSize(250,200);
        offlineUsers.setPrefSize(300,200);
        pane.addColumn(0,new TitledPane("Zalogowani użytkownicy:", onlineUsers),new TitledPane("Niezalogowani użytkownicy:", offlineUsers));

        onlineUsers.setOnMouseClicked(event -> openWindow(event,onlineUsers));
        offlineUsers.setOnMouseClicked(event -> openWindow(event,offlineUsers));

        ClientReader reader = new ClientReader();
        reader.start();
        Stage stage = new Stage();
        stage.setOnCloseRequest(event -> stop(reader));
        stage.setTitle(ID);
        stage.setScene(new Scene(pane,250,400));
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Funckja uruchamia okno do rozmowy z innym użytkownikiem
     * @param event wydarzenie kliknięcia myszką
     * @param users dana lista użytkowników (online lub offline)
     */
    private void openWindow(MouseEvent event, ListView<String> users){
        if(event.getClickCount() == 2){
            try {
                String recipientID = users.getSelectionModel().getSelectedItem();
                Messanger messanger = new Messanger(ID,recipientID,socket,notSentMessages.get(recipientID), openMessangers);
                openMessangers.put(recipientID, messanger);
                messanger.start(new Stage());
                notSentMessages.remove(recipientID);
            } catch (Exception e) {
                System.out.println("Błąd podczas otwierania okna komunikatora.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Funkcja aktualizuje listę użytkowników
     * @param online lista dostępnych użytkowników
     */
    private void getUsers(ArrayList<String> online) {
        ArrayList<String> offline = new ArrayList<>();
        online.remove(ID);
        for(String s : allUsers){
            if(!online.contains( s))
                offline.add(s);
        }
        onlineUsers.setItems(FXCollections.observableList(online));
        offlineUsers.setItems(FXCollections.observableList(offline));
    }

    /**
     * Funckja wyłowywana przy zamykaniu programu, kończy wszystkie wątki i zamyka pozostałe okna
     * @param reader klasa czytająca wiadomości
     */
    private void stop(ClientReader reader){
        try {
            reader.stop();
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            os.writeObject(null);
            socket.close();
            Platform.exit(); //zamykamy wszystkie otwarte okna
        } catch (IOException e) {
            System.out.println("Błąd wejścia/wyjścia.");
            e.printStackTrace();
        }
    }

    /**
     * Klasa czytająca wiadomości przysyłane do użytkownika i wyświetla je w odpowiednich oknach
     */
    private class ClientReader extends Thread {
        @Override
        public void run() {
            try {
                notSentMessages = new HashMap<>();
                for (;;) {
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Object object = in.readObject();
                    if(object.getClass().getName().equals("communicator.Message")) //jesli dostalismy wiadomosc
                        readMessage((Message) object);
                    else { //jesli dostalismy liste uzytkownikow
                        Platform.runLater(() -> getUsers((ArrayList) object));
                    }
                }
            } catch (IOException e) {
                System.out.println("Błąd wejścia/wyjścia");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Nie znaleziono danej klasy.");
                e.printStackTrace();
            }
        }

        /**
         * Funckja czyta wiadomość i przesyła ją do otwartego okna albo przechowuje oczekując na jego otwarcie
         * @param message przełana wiadomość
         */
        private void readMessage(Message message) {
            String ID = message.getSenderID();
            if(openMessangers.containsKey(ID))
                openMessangers.get(ID).showMessage(message.toString());
            else{
                if(notSentMessages.containsKey(ID))
                    notSentMessages.get(ID).add(message);
                else{
                    ArrayList<Message> list = new ArrayList<>();
                    list.add(message);
                    notSentMessages.put(ID,list);
                }
            }
        }
    }
}