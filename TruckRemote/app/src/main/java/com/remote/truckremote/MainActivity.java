package com.remote.truckremote;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.lang.Integer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 0;
    final HashMap<String, String> addressList = new HashMap<>();
    Slider speedSlider, steerSlider;
    SwitchMaterial headlightSwitch, parkingSwitch;
    Button hornButton,handbrakeButton,directionButton,musicButton;
    FloatingActionButton connectBluetooth;
    Chip chip;
    CircularProgressIndicator circularProgressIndicator;

    BluetoothSocket btSocket = null;
    BluetoothDevice btDevice = null;
    String btName = null, btAddress = null;
    BluetoothAdapter bluetoothAdapter;
    boolean isBluetoothConnected = false;
    boolean isBluetoothConnectedFirstTime = false;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speedSlider = findViewById(R.id.sliderspeed);
        steerSlider = findViewById(R.id.slidersteer);
        directionButton = findViewById(R.id.directionbutton);
        handbrakeButton = findViewById(R.id.handbrakeButton);
        musicButton = findViewById(R.id.musicButton);
        headlightSwitch = findViewById(R.id.headlightswitch);
        hornButton = findViewById(R.id.hornButton);
        parkingSwitch = findViewById(R.id.parkingswitch);
        connectBluetooth = findViewById(R.id.connectbluetooth);
        chip = findViewById(R.id.chip);
        circularProgressIndicator = findViewById(R.id.batteryindicator);
        circularProgressIndicator.setProgress(50);

        final MediaPlayer screech = MediaPlayer.create(this, R.raw.handbrake);
        final MediaPlayer buttonClick = MediaPlayer.create(this,R.raw.buttonclicksound);
        final MediaPlayer connSuccess = MediaPlayer.create(this, R.raw.bluetoothconnsuccess);
        final MediaPlayer listDevices = MediaPlayer.create(this, R.raw.selectbluetoothdevice);

        //create listview & fetch all paired devices to arraylist
        Set<BluetoothDevice> pairedDevices;
        ListView listView = null;
        ArrayList<String> deviceList = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            listView = new ListView(this);
            deviceList = new ArrayList<>();
            pairedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bt : pairedDevices) {
                deviceList.add(bt.getName());
                addressList.put(bt.getName(), bt.getAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //create array adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList);
        listView.setAdapter(adapter);
        //add listView to alert dialog

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setView(listView);
        builder.setTitle("Select Bluetooth Device :");
        AlertDialog dialog = builder.create();

        //add onClicklistener to the floating button
        connectBluetooth.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //noinspection deprecation
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                listDevices.start();
            } else {
                dialog.show();
                listDevices.start();
            }
        });

        //add onClicklistener to listView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            isBluetoothConnectedFirstTime = true;
            btName = adapter.getItem(position);
            btAddress = addressList.get(btName);
            dialog.dismiss();
            if (btSocket == null || !btSocket.isConnected()) {
                try {
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    //connects to the device's address and checks if it's available
                    btDevice = bluetoothAdapter.getRemoteDevice(btAddress);
                    //create a RFCOMM (SPP) connection
                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(mUUID);
                    btSocket.connect();
                    chip.setText("Connected");
                    connSuccess.start();
                    isBluetoothConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    isBluetoothConnected = false;
                    chip.setText("Cannot be Connected");
                }
            } else if (btSocket != null || btSocket.isConnected()) {
                isBluetoothConnected = true;
                msg("Bluetooth Already Connected");
            } else {
                isBluetoothConnected = false;
                msg("Bluetooth Disconnected");
            }
        });

        //setting up onClick listeners on all the CONTROLS                                          //CONTROLS LISTENERS
        //forward-reverse button
        directionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (directionButton.getText().equals("STOP") || directionButton.getText().equals("REVERSE")){
                        btSocket.getOutputStream().write("f".getBytes());
                        directionButton.setText("DRIVE");
                        directionButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_forward,0,0,0);
                        buttonClick.start();
                    }
                    else if (directionButton.getText().equals("DRIVE")){
                        btSocket.getOutputStream().write("b".getBytes());
                        directionButton.setText("REVERSE");
                        directionButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_backward,0,0,0);
                        buttonClick.start();
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Truck not Connected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //handbrake button
        handbrakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (handbrakeButton.getText().equals("HAND BRAKE -> ON")){
                        btSocket.getOutputStream().write("y".getBytes());
                        handbrakeButton.setText("HAND BRAKE -> OFF");
                        if (directionButton.getText().equals("STOP")){
                            directionButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_forward,0,0,0);
                            directionButton.setText("DRIVE");
                        }
                    }
                    else if (handbrakeButton.getText().equals("HAND BRAKE -> OFF")){
                        btSocket.getOutputStream().write("x".getBytes());
                        screech.start();
                        handbrakeButton.setText("HAND BRAKE -> ON");
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }  catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Truck not Connected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //music button playing GOT theme song
        musicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    btSocket.getOutputStream().write("g".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Truck not Connected", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //headlights switch
        headlightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                if (isChecked) {
                    buttonClick.start();
                    btSocket.getOutputStream().write("l".getBytes());
                } else {
                    buttonClick.start();
                    btSocket.getOutputStream().write("m".getBytes());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Truck not Connected", Toast.LENGTH_SHORT).show();
            }
        });
        //horn button
        hornButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    btSocket.getOutputStream().write("h".getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        //parking lights switch
        parkingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                if (isChecked) {
                    buttonClick.start();
                    btSocket.getOutputStream().write("p".getBytes());
                } else {
                    buttonClick.start();
                    btSocket.getOutputStream().write("q".getBytes());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Truck not Connected", Toast.LENGTH_SHORT).show();
            }
        });
        //speed Slider
        speedSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                String str = String.valueOf(Math.round(value));
                str=str.concat("a");
                try {
                    btSocket.getOutputStream().write(str.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
        //set speed to 0 after touch remove
        speedSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                slider.setValue(0);
                try {
                    btSocket.getOutputStream().write("c".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
        //angle slider
        steerSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                String str = String.valueOf(Math.round(value));
                str=str.concat("t");
                try {
                    btSocket.getOutputStream().write(str.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
        //set angle back to FRONT = 95
        steerSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                slider.setValue(95);
                float value=slider.getValue();
                String str = String.valueOf(Math.round(value));
                str=str.concat("t");
                try {
                    btSocket.getOutputStream().write(str.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
    }
//OnCreate END

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Called  finish() function here
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBluetoothConnected) {
            chip.setText("Connected");
        } else if (!isBluetoothConnectedFirstTime) {
            chip.setText("Connect Bluetooth First");
        } else {
            chip.setText("Disconnected");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //send command to arduino to stop car
        try {
            isBluetoothConnectedFirstTime = true;
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //destroy all objects
        try {
            isBluetoothConnectedFirstTime = true;
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btSocket = null;
        bluetoothAdapter = null;
    }

    //new thread for checking if bluetooth disconnected & notifying user
    public class MyAsyncTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            if(!btSocket.isConnected()){
                chip.setText("BT Disconnected");
            }
            return null;
        }
    }
}