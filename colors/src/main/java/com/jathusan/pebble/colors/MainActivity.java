package com.jathusan.pebble.colors;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private Socket socket;

    // ip address (will be retrieved from input)
    private static String ip_Address;

    // port value (assigned as 1234)
    private static final int PORT = 1234;

    // Textview used for presenting information in header
    private TextView tvInfo;

    // ListView of all the values from the python server
    private ListView rgbListView;

    // thread for retrieving data from server.py
    private Thread clientThread;

    private EditText ipField;
    private Button goButton;
    private ArrayList<RGBObject> rgbObjects;
    private ArrayAdapter<RGBObject> rgbAdapter;

    // default value for the R, G and B fields
    private static final int DEFAULT_RGB_VALUE = 127;

    // used to update the user interface, from the client thread
    private Handler interfaceUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = (TextView) findViewById(R.id.info);
        goButton = (Button) findViewById(R.id.goButton);
        ipField = (EditText) findViewById(R.id.ipField);
        rgbListView = (ListView) findViewById(R.id.listView);
        rgbListView.setItemsCanFocus(false);

        // set input type to numeric keyboard layout
        ipField.setRawInputType(Configuration.KEYBOARD_12KEY);

        rgbObjects = new ArrayList<RGBObject>();
        rgbAdapter = new RGBArrayAdapter(this, R.layout.list_row, rgbObjects);
        rgbListView.setAdapter(rgbAdapter);

        rgbListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                //exit multi choice mode if number of selected items is 0
                if (!rgbAdapter.getItem(position).isSelected()) {
                    //mark the item as selected
                    rgbAdapter.getItem(position).setSelected(true);
                } else {
                    //deselect the item form the list
                    rgbAdapter.getItem(position).setSelected(false);
                }

                rgbAdapter.notifyDataSetChanged();
                calculateAndUpdateCurrentValues();
            }
        });

        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ip_Address = ipField.getText().toString();
                if (ip_Address != null && !ip_Address.isEmpty()) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Connecting to " + ip_Address + "...", Toast.LENGTH_SHORT);
                    toast.show();

                    tvInfo.setText("R: " + DEFAULT_RGB_VALUE + " G: " + DEFAULT_RGB_VALUE + " B: " + DEFAULT_RGB_VALUE);

                    // Hide the soft-keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(ipField.getWindowToken(), 0);

                    // hide the edittext and button
                    goButton.setVisibility(View.GONE);
                    ipField.setVisibility(View.GONE);

                    // used to update the user interface from the client thread
                    interfaceUpdater = new Handler();

                    //start client thread to retrieve commands from python server
                    clientThread = new Thread(new ClientThread(socket));
                    clientThread.start();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            // interrupt the client thread, to stop the while loop
            clientThread.interrupt();
            socket.close();
        } catch (Exception e) {
            Log.e("RGB", "Unknown Exception when closing socket");
        }
    }

    class ClientThread implements Runnable {

        private Socket mSocket;

        public ClientThread(Socket s) {
            this.mSocket = s;
        }

        @Override
        public void run() {
            try {

                // initialize the socket with the provided ip address and port
                InetAddress ipAddress = InetAddress.getByName(ip_Address);
                mSocket = new Socket(ipAddress, PORT);

                DataInputStream input;

                // run this thread until it's interrupted
                while (!(Thread.currentThread().isInterrupted())) {
                    // input stream from socket
                    input = new DataInputStream(new BufferedInputStream(mSocket.getInputStream()));

                    // create a new rgbObject to add to our list of rgbObjects (for the log)
                    RGBObject rgbObject = new RGBObject();

                    // if the first byte is 2, then we know this is an absolute command,
                    // otherwise, it must be relative.
                    rgbObject.setAbsolute(input.readByte() == 2);

                    if (!rgbObject.isAbsolute()) {
                        // relative command (3, 16-bit signed int's)
                        rgbObject.setRValue(input.readByte() << 8 | input.readByte());
                        rgbObject.setGValue(input.readByte() << 8 | input.readByte());
                        rgbObject.setBValue(input.readByte() << 8 | input.readByte());
                    } else {
                        // absolute command (3, 8-bit unsigned int's)
                        rgbObject.setRValue(input.readUnsignedByte());
                        rgbObject.setGValue(input.readUnsignedByte());
                        rgbObject.setBValue(input.readUnsignedByte());
                    }

                    //tell the interface updater handler to update the interface
                    // with the new rgb object
                    interfaceUpdater.post(new UIThread(rgbObject));
                }

                // close the socket when the thread is interrupted
                mSocket.close();

            } catch (Exception e) {
                // update ui thread that there was a problem connecting to the host
                interfaceUpdater.post(new UIThread(null));
                Log.e("RGB", "Unknown exception on client thread");
            }
        }
    }

    class UIThread implements Runnable {
        private RGBObject rgbObject;

        public UIThread(RGBObject rbjO) {
            this.rgbObject = rbjO;
        }

        @Override
        public void run() {

            if (rgbObject == null) {
                tvInfo.setText(R.string.invalid_ip);
                return;
            }

            if (rgbObject.isAbsolute()) {
                // clear all the previously selected items in the list
                clearAllSelected();
            }

            rgbObject.setSelected(true);

            // add the new RGBObject to the list
            rgbObjects.add(0, rgbObject);
            // post updates to the UI
            rgbAdapter.notifyDataSetChanged();
            // update the current R,G,B values
            calculateAndUpdateCurrentValues();
        }
    }

    private void calculateAndUpdateCurrentValues() {
        int currentR = DEFAULT_RGB_VALUE;
        int currentG = DEFAULT_RGB_VALUE;
        int currentB = DEFAULT_RGB_VALUE;

        for (int i = rgbObjects.size() - 1; i >= 0; i--) {
            RGBObject rgbObject = rgbObjects.get(i);
            if (rgbObject.isSelected()) {
                if (rgbObject.isAbsolute()) {
                    currentR = rgbObject.getRValue();
                    currentG = rgbObject.getGValue();
                    currentB = rgbObject.getBValue();
                } else {
                    currentR = (currentR + rgbObject.getRValue()) % 255;
                    // adjust values to be positive if negative
                    if (currentR < 0) {
                        currentR += 255;
                    }
                    currentG = (currentG + rgbObject.getGValue()) % 255;
                    // adjust values to be positive if negative
                    if (currentG < 0) {
                        currentG += 255;
                    }
                    currentB = (currentB + rgbObject.getBValue()) % 255;
                    // adjust values to be positive if negative
                    if (currentB < 0) {
                        currentB += 255;
                    }
                }
            }
        }

        // update the header with the current values for R G and B
        tvInfo.setText("R: " + currentR + ", G: " + currentG + ", B: " + currentB);
    }

    // sets all the items in the list to be deselected
    private void clearAllSelected() {
        for (int i = 0; i < rgbObjects.size(); i++) {
            rgbObjects.get(i).setSelected(false);
        }
    }

}