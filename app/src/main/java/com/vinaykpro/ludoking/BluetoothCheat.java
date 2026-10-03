package com.vinaykpro.ludoking;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.widget.EditText;
import java.io.InputStream;
import java.util.UUID;

public class BluetoothCheat {
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "LudoCheat";
    private MainActivity activity;
    private EditText diceInput;
    private BluetoothServerSocket serverSocket;
    private boolean running = true;

    public BluetoothCheat(MainActivity activity, EditText diceInput) {
        this.activity = activity;
        this.diceInput = diceInput;
    }

    public void start() {
        new Thread(() -> {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                while (running) {
                    BluetoothSocket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[64];
                    int bytes = in.read(buffer);
                    String cmd = new String(buffer, 0, bytes).trim();
                    activity.runOnUiThread(() -> handleCommand(cmd));
                    socket.close();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void handleCommand(String cmd) {
        switch (cmd) {
            case "DICE_6": diceInput.setText("6"); break;
            case "DICE_1": diceInput.setText("1"); break;
            case "DICE_RESET": diceInput.setText(""); break;
            case "SKIP": activity.switchPlayers(); break;
            case "KILL_RED": for (MainActivity.Piece p : activity.rp) p.die(); break;
            case "KILL_GREEN": for (MainActivity.Piece p : activity.gp) p.die(); break;
            case "WIN": activity.forceWin(); break;
        }
    }

    public void stop() { running = false; try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {} }
}
