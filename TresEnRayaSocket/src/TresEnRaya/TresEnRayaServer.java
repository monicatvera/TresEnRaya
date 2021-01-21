/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TresEnRaya;

/**
 *
 * @author Mónica
 */
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.swing.*;

public class TresEnRayaServer extends JFrame {

    private byte board[];
    private boolean xMove;
    private JTextArea output;
    public Player players[];
    private ServerSocket server;
    private int currentPlayer;
    public byte winner;

    public TresEnRayaServer() {
        super("Tres en raya Server");
        //pnlButton
        board = new byte[9];
        xMove = true;
        players = new Player[2];
        currentPlayer = 0;
        try {
            server = new ServerSocket(5000, 2);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        output = new JTextArea();
        getContentPane().add(output, BorderLayout.CENTER);
        output.setText("El servidor esta esperando jugadores.\n");

        setSize(400, 200);
        show();
    }

    public void execute() {
        for (int i = 0; i < players.length; i++) {
            try {
                players[i]
                        = new Player(server.accept(), this, i);
                players[i].start();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        synchronized (players[0]) {
            players[0].threadSuspended = false;
            players[0].notify();
        }

    }

    public void display(String s) {
        output.append(s + "\n");
    }

    public synchronized boolean validMove(int loc,
            int player) {
        boolean moveDone = false;

        while (player != currentPlayer) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!isOccupied(loc)) {
            board[loc]
                    = (byte) (currentPlayer == 0 ? 'X' : 'O');
            currentPlayer = (currentPlayer + 1) % 2;
            players[currentPlayer].otherPlayerMoved(loc);
            notify();    // tell waiting player to continue
            return true;
        } else {
            return false;
        }
    }

    public boolean isOccupied(int loc) {
        if (board[loc] == 'X' || board[loc] == 'O') {
            return true;
        } else {
            return false;
        }
    }

    public boolean gameOver() {
        if (test(board[0], board[1], board[2])) {
            return true;
        }
        if (test(board[3], board[4], board[5])) {
            return true;
        }
        if (test(board[6], board[7], board[8])) {
            return true;
        }
        if (test(board[0], board[3], board[6])) {
            return true;
        }
        if (test(board[1], board[4], board[7])) {
            return true;
        }
        if (test(board[2], board[5], board[8])) {
            return true;
        }
        if (test(board[0], board[4], board[8])) {
            return true;
        }

        return false;
    }

    private boolean test(byte x, byte y, byte z) {
        if (((x == y) && (y == z)) && ((x == 'X') || (x == 'O'))) {
            winner = x;
            return true;
        } else {
            return false;
        }
    }

    public static void main(String args[]) {
        TresEnRayaServer game = new TresEnRayaServer();

        game.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        }
        );

        game.execute();
    }
}

class Player extends Thread {

    private Socket connection;
    private DataInputStream input;
    public DataOutputStream output;
    private TresEnRayaServer control;
    private int number;
    private char mark;
    protected boolean threadSuspended = true;

    public Player(Socket s, TresEnRayaServer t, int num) {
        mark = (num == 0 ? 'X' : 'O');

        connection = s;

        try {
            input = new DataInputStream(
                    connection.getInputStream());
            output = new DataOutputStream(
                    connection.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        control = t;
        number = num;
    }

    public void otherPlayerMoved(int loc) {
        try {
            output.writeUTF("El oponente ha movido.");
            output.writeInt(loc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        boolean done = false;

        try {
            control.display("Jugador "
                    + (number == 0 ? 'X' : 'O') + " conectado");
            output.writeChar(mark);
            output.writeUTF("Jugador "
                    + (number == 0 ? "X conectado\n"
                            : "O connected, Por favor espere\n"));

            // wait for another player to arrive
            if (mark == 'X') {
                output.writeUTF("Esperando por el jugador O");

                try {
                    synchronized (this) {
                        while (threadSuspended) {
                            wait();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                output.writeUTF(
                        "El jugador O se ha conectado. Es tu turno.");
            }

            // Play game
            while (!done) {
                int location = input.readInt();
                if (location == -1) {
                   
                } else if (control.validMove(location, number)) {
                    control.display("loc: " + location);
                    output.writeUTF("Movimiento valido.");
                } else {
                    output.writeUTF("Movimiento invalido, intentalo de nuevo");
                }

                if (control.gameOver()) {
                    control.display("Ganador: " + (char) control.winner + "\n");
                    control.players[0].output.writeUTF("Ganador: " + (char) control.winner);
                    control.players[1].output.writeUTF("Ganador: " + (char) control.winner);
                    //done = true;
                }
            }

            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
