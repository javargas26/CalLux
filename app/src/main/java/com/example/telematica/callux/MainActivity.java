package com.example.telematica.callux;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button btnSensor1, btnSensor2, btnEsperado, btnAction, btnClear;
    TextView sensorView1, sensorView2, sensorEstimated, tvPorcentaje;
    EditText etDistancia1, etDistancia2;
    int btn;


    private static final String TAG = "MainActivity";
    Handler bluetoothIn;
    final int handlerState = 0;             //used to identify handler message
    private BluetoothAdapter btAdapter;
    Context mContext;

    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSensor1=findViewById(R.id.btnSensor1);
        btnSensor2=findViewById(R.id.btnSensor2);
        btnEsperado=findViewById(R.id.btnEsperado);
        btnAction=findViewById(R.id.btnAction);
        btnClear=findViewById(R.id.btnClear);
        sensorView1=findViewById(R.id.sensorView1);
        sensorView2=findViewById(R.id.sensorView2);
        sensorEstimated=findViewById(R.id.sensorEstimated);
        tvPorcentaje=findViewById(R.id.tvPorcentaje);
        etDistancia1=findViewById(R.id.etDistancia1);
        etDistancia2=findViewById(R.id.etDistancia2);


        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {

                Log.e(TAG, "handleMessage: Llega informacion ");
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    if (readMessage.equals("/n")) return;
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        if (recDataString.charAt(0) == '#')                             //if it starts with # we know it is what we are looking for
                        {
                            String sensor0 = recDataString.substring(1, 6);             //get sensor value from string between indices 1-5
                            if (btn==1)
                            {
                                sensorView1.setText( sensor0 );    //update the textviews with sensor values
                            }
                            if (btn==2)
                            {
                                sensorView2.setText( sensor0 );    //update the textviews with sensor values
                            }


                        }
                        recDataString.delete(0, recDataString.length());
                        //clear all string data
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        btnSensor1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                btn=1;
                sensorView1.setText(null);
                mConnectedThread.write("1");   // Envia accion
                Toast.makeText(getBaseContext(), "Pidiendo dato sensor", Toast.LENGTH_SHORT).show();

            }
        });
        btnSensor2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                btn=2;
                sensorView2.setText(null);
                mConnectedThread.write("1");   // Envia accion
                Toast.makeText(getBaseContext(), "Pidiendo dato sensor", Toast.LENGTH_SHORT).show();

            }
        });

        btnEsperado.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                float d1,d2, e1, e2, esp ,error;

                d1=Float.valueOf(etDistancia1.getText().toString());
                d2=Float.valueOf(etDistancia2.getText().toString());
                e1=Float.valueOf(sensorView1.getText().toString());
                e2=Float.valueOf(sensorView2.getText().toString());
                if (d2==0)
                {
                    Toast.makeText(getBaseContext(), "Ingrese valor de la distancia 2", Toast.LENGTH_SHORT).show();
                }
                if(d2!=0)
                {
                    Log.e(TAG, "onClick: haciendo la operacion");
                    esp=((d1*d1*e1)/d2)/d2;
                    Log.e(TAG, "onClick: operacion hecha");
                    sensorEstimated.setText(String.valueOf(esp));
                    error=((Math.abs(esp-e2))/esp)*100;
                    String error_view=String.format("%.3f",error);
                    tvPorcentaje.setText(String.valueOf(error_view));
                }
            }
        });
        btnClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {



                etDistancia1.setText(null);
                etDistancia2.setText(null);
                sensorView1.setText(null);
                sensorView2.setText(null);
                sensorEstimated.setText(null);
                tvPorcentaje.setText(null);

            }
        });

    }

    public void onResume() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }



    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {

                    bytes = mmInStream.read(buffer);         //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();



                } catch (IOException e) {
                    Log.d(TAG, "run: no puede recibir");
                    break;
                }
            }
        }
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }


}
