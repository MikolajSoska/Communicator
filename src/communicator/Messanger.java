package communicator;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Klasa wsywietla okienko do rozmowy z innym uzytkownikiem
 * @author Mikołaj Soska
 */
public class Messanger extends Application {
    private String recipientID;
    private Button button;
    private TextArea textArea;
    private ListView<String> messages;
    private ArrayList<String> list;
    private ArrayList<Message> unseenMessages;
    private Socket socket;
    private HashMap<String, Messanger> clients;

    /**
     * Konstruktor klasy
     * @param senderID identyfikator wysylajacego
     * @param recipientID identyfikator odbiorcy
     * @param socket socket wysylajacego
     * @param unseenMessages lista nieprzeczytanych wiadomosci
     * @param messangers lista wszystkich otwartych okienek
     */
    Messanger(String senderID, String recipientID, Socket socket, ArrayList<Message> unseenMessages, HashMap<String, Messanger> messangers){
        this.recipientID = recipientID;
        this.socket = socket;
        this.unseenMessages = unseenMessages;
        this.clients = messangers;

        textArea = new TextArea();
        textArea.setMinSize(500,100);

        button = new Button("WYŚLIJ");
        button.setMinSize(100,100);
        button.setFont(new Font(20));
        button.setOnAction(event -> send(new Message(senderID, recipientID, textArea.getText())));

        messages = new ListView<>();
        messages.setMinSize(600,500);

        list = new ArrayList<>();
    }

    /**
     * Funckja wysyła wiadomość do użytkownika
     * @param message wiadomość do wysłania
     */
    private void send(Message message) {
        try {
            if(message != null) {
                textArea.clear();
                showMessage(message.toString());
                message.transTest = 10;
            }
            ObjectOutputStream stream = new ObjectOutputStream(socket.getOutputStream());
            stream.writeObject(message);
        } catch (IOException e) {
            System.out.println("Błąd wejścia/wyjścia");
            e.printStackTrace();
        }
    }

    /**
     * Funkcja wyświetla wiadomości w okienku
     * @param message wiadomosc do wyswietlenia
     */
    void showMessage(String message){
        list.add(message);
        messages.setItems(FXCollections.observableList(list));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SplitPane root = new SplitPane();
        SplitPane pane = new SplitPane();
        pane.setOrientation(Orientation.HORIZONTAL);
        pane.getItems().addAll(textArea,button);
        root.setOrientation(Orientation.VERTICAL);
        root.getItems().addAll(messages,pane);

        primaryStage.setTitle(recipientID);
        primaryStage.setScene(new Scene(root,600,600));
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> clients.remove(recipientID));

        if(unseenMessages != null){
            for(Message m : unseenMessages)
                showMessage(m.toString());
        }
    }
}