package communicator;

import java.io.Serializable;

/**
 * Klasa wiadomosci wysylanej między użytkownikami
 * @author Mikołaj Soska
 */
public class Message implements Serializable{
    private String senderID;
    private String recipientID;
    private String message;
    transient int transTest; /**pole transient, do sprawdzenia czy nie zostaje przesylane**/

    /**
     * Konstruktor tworzący nową wiadomość
     * @param senderID identyfikator wysylajacego
     * @param recipientID identyfikator odbiorcy
     * @param message tresc wiadomosci
     */
    Message(String senderID, String recipientID, String message){
        this.senderID = senderID;
        this.recipientID = recipientID;
        this.message = message;
    }

    /**
     * Zwraca identyfikator odbiorcy
     * @return identyfikator odbiorcy
     */
    String getRecipientID() {
        return recipientID;
    }

    /**
     * Zwraca identyfikator wysylajacego
     * @return identyfikator wysylajacego
     */
    String getSenderID() {
        return senderID;
    }

    /**
     * Zwraca treść wiadomości wraz z informacją od kogo
     * @return treść wiadomości wraz z informacją od kogo
     */
    @Override
    public String toString() {
        return senderID + ": " + message;
    }
}