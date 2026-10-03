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
                    // Har client ke liye alag thread - connection alive rahega
                    new Thread(() -> handleClient(socket)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClient(BluetoothSocket socket) {
        try {
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            // Connection alive rakho - jab tak client disconnect na kare
            while (running && socket.isConnected()) {
                int bytes = in.read(buffer);
                if (bytes <= 0) break; // client disconnect

                String data = new String(buffer, 0, bytes).trim();
                // Multiple commands ek saath aa sakti hain - split karo
                String[] cmds = data.split("[\r\n]+");
                for (String c : cmds) {
                    final String cmd = c.trim();
                    if (!cmd.isEmpty()) {
                        activity.runOnUiThread(() -> handleCommand(cmd));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void handleCommand(String rawCmd) {
        String cmd = rawCmd.replace("\n", "").replace("\r", "").trim().toUpperCase();
        if (cmd.isEmpty()) return;

        if (cmd.equals("RESET") || cmd.equals("CLEAR") || cmd.equals("DICE_RESET")) {
            MainActivity.cheatDiceValue = -1;
            MainActivity.cheatTargetColor = null;
            return;
        }

        if (cmd.equals("SKIP")) { activity.switchPlayers(); return; }
        if (cmd.equals("WIN")) { activity.forceWin(); return; }

        if (cmd.startsWith("KILL_")) {
            String c = cmd.substring(5);
            if (c.equals("RED")) { for (MainActivity.Piece p : activity.rp) p.die(); }
            else if (c.equals("GREEN")) { for (MainActivity.Piece p : activity.gp) p.die(); }
            else if (c.equals("BLUE")) { for (MainActivity.Piece p : activity.bp) p.die(); }
            else if (c.equals("YELLOW")) { for (MainActivity.Piece p : activity.yp) p.die(); }
            return;
        }

        String payload = cmd;
        if (payload.startsWith("DICE_")) payload = payload.substring(5);

        String s = payload;
        String color = null;
        int num = -1;

        if (s.startsWith("RED")) { color = "red"; s = s.substring(3); }
        else if (s.startsWith("GREEN")) { color = "green"; s = s.substring(5); }
        else if (s.startsWith("BLUE")) { color = "blue"; s = s.substring(4); }
        else if (s.startsWith("YELLOW")) { color = "yellow"; s = s.substring(6); }
        else if (s.startsWith("R")) { color = "red"; s = s.substring(1); }
        else if (s.startsWith("G")) { color = "green"; s = s.substring(1); }
        else if (s.startsWith("B")) { color = "blue"; s = s.substring(1); }
        else if (s.startsWith("Y")) { color = "yellow"; s = s.substring(1); }

        s = s.replace("_", "").replace(" ", "").trim();

        try {
            int v = Integer.parseInt(s);
            if (v >= 1 && v <= 6) num = v;
        } catch (Exception e) {}

        if (num >= 1 && num <= 6) {
            MainActivity.cheatDiceValue = num;
            MainActivity.cheatTargetColor = color;
        }
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) {}
    }
          }
