package com.owen.kiprop.example.smartswitch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private Switch switch1, switch2, switch3;
    private ImageView bulb1, bulb2, bulb3;

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        switch1 = (Switch) findViewById(R.id.switch1);
        switch1.setOnCheckedChangeListener(this);
        switch2 = findViewById(R.id.switch2);
        switch2.setOnCheckedChangeListener(this);
        switch3 = findViewById(R.id.switch3);
        switch3.setOnCheckedChangeListener(this);


        bulb1 = findViewById(R.id.bulb1);
        bulb2 = findViewById(R.id.bulb2);
        bulb3 = findViewById(R.id.bulb3);

        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);

        // receives connected device information from select_device_acivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

        // gui handler for threads
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECTING_STATUS:
                        switch (msg.arg1) {
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                buttonConnect.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                }
            }
        };
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // switches to new activity when clicked
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });


    }

    @Override

    // send data depending on which switch is toggled on/off
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) { // line 129
        String command= null;
        if (compoundButton.getId() == switch1.getId()) {
            if (b) {
                command = "a";
                bulb1.setColorFilter(getResources().getColor(R.color.bright_yellow));
            } else {
                command = "b";
                bulb1.setColorFilter(getResources().getColor(R.color.lights_off));
            }
            connectedThread.write(command);
        } else if (compoundButton.getId() == switch2.getId()) {
            if (b) {
                command="c";
                bulb2.setColorFilter(getResources().getColor(R.color.bright_yellow));
            } else {
                command="d";
                bulb2.setColorFilter(getResources().getColor(R.color.lights_off));
            }
            connectedThread.write(command);
        } else if (compoundButton.getId() == switch3.getId()) {
            if (b) {
                command="e";
                bulb3.setColorFilter(getResources().getColor(R.color.bright_yellow));
            } else {
                command="f";
                bulb3.setColorFilter(getResources().getColor(R.color.lights_off));
            }
            connectedThread.write(command);
        }
    }

 // implement threading to create bluetooth connection. Each socket uses different thread
    public static class CreateConnectThread extends Thread {

        @SuppressLint("MissingPermission")
        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
           // use temporary socket for connections then assign it to the class variable mmsocket.
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            @SuppressLint("MissingPermission") UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
               // create bluetooth socket to connect to devices
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
                Log.e("Socket status", "Socket has been created");

            } catch (IOException e) {
                Log.e(TAG, "socket could not be created", e);
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // connect to remote device using the bluetooth socket

                mmSocket.connect();
               Log.e("Connection Status", "Connected to bluetooth module");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // if unable to connect, close connection and destroy bluetooth socket
                try {
                    mmSocket.close();
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();

                } catch (IOException closeException) {

                }
                return;
            }

            // a separate thread is used to connect to the arduino after connection attempt succeeded
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();

            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

// threading for data transfer to the arduino
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            try {
                tmpOut = socket.getOutputStream();

            } catch (IOException e) { }

            mmOutStream = tmpOut;
        }

        // to send data to the arduino
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
                Log.e("Data transfer", "Data has been sent to connected device");
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        // to close bluetooth connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
// close connection if back button is pressed
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

}