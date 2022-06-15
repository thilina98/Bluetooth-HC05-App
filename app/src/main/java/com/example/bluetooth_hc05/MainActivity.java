package com.example.bluetooth_hc05;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.icu.text.SymbolTable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String relativeContacts[] = {"+94711261361"};

    Button btn_connect;
    TextView txt_data, txt_sts;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice hc05;
    BluetoothSocket bluetoothSocket;

    SendReceive sendReceive;

    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    static final int STATE_CONNECTED = 1;
    static final int STATE_CONNECTION_FAILED = 2;
    static final int STATE_MESSAGE_RECEIVED = 3;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PackageManager.PERMISSION_GRANTED);

        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btn_connect = findViewById(R.id.btn_connect);
        txt_data = findViewById(R.id.txt_data);
        txt_sts = findViewById(R.id.txt_status);

        connect();
    }

    /*
    MESSAGE FORMAT
    -------
    message
    -------
    D,HHMMSS.SSS,TTTT.TTTT,GGGGG.GGGG

    -------
    content
    -------
    D - Accident Detection Flag
    HHMMSS.SSS - Time
    T - Latitude / G - Longitude

    -------
    example
    -------
    0,104534.000,7791.0381,06727.4434

    */

    Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("HandlerLeak")
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case STATE_CONNECTED:
                    txt_sts.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    txt_sts.setText("Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    txt_data.setText((CharSequence) msg.obj);

                    //=====================================================
                    String dataString = (String) msg.obj;
                    String dataArray[] = dataString.split(",");

                    System.out.println(dataString);
                    System.out.println(Arrays.toString(dataArray));

//                    storeData(dataArray);    // to be implemented to store data in a database.

                    String strTime = dataArray[1].substring(0,2)+"."+dataArray[1].substring(2,4);

                    String version = "2.0";

                    if (dataArray[0].equals("1")){
                        SmsManager smsManagerSend = SmsManager.getDefault();
                        String strMessage = version+"ACCIDENT DETECTED\n@"+strTime+"\n"+"view the location on google maps:\nhttps://www.google.com/maps/search/?api=1&query="+dataArray[2]+","+dataArray[3];
                        System.out.println(strMessage);         // testing
                        for (String contact: relativeContacts) {
                            smsManagerSend.sendTextMessage(contact, null, strMessage, null, null);
                        }
                    }

                    //=====================================================


                    break;
            }



            return false;
        }
    }){
    };


    @SuppressLint("MissingPermission")
    private void connect() {

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                hc05 = bluetoothAdapter.getRemoteDevice("00:22:01:00:14:80");

                bluetoothSocket = null;

                try {
                    bluetoothSocket = hc05.createRfcommSocketToServiceRecord(mUUID);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    bluetoothSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(bluetoothSocket.isConnected()==true){
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                }
                else{
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                InputStream inputStream = null;

                try {
                    inputStream = bluetoothSocket.getInputStream();
                    inputStream.skip(inputStream.available());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    sendReceive = new SendReceive(inputStream);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                sendReceive.start();

            }
        });

    }

    private class SendReceive extends Thread{
        private final InputStream inputStream;

        public SendReceive(InputStream inputStream) throws IOException {
            this.inputStream = inputStream;
        }

        public void run(){

            byte[] buffer = new byte[1024];
            int bytes;

            while (true){
                try {
                    bytes = inputStream.read(buffer);
                    String tmp_string = new String(buffer,0,bytes);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes, -1, tmp_string).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

//                while (true){
//
//                    byte[] buffer = new byte[10];
//                    int bytes;
//                    String tmp_string = null;
//                    try {
//                        bytes = inputStream.read(buffer);
//                        tmp_string = new String(buffer,0,bytes);
//                        System.out.println(tmp_string);
//                        txt_data.setText(tmp_string);
//                        Toast.makeText(MainActivity.this, tmp_string, Toast.LENGTH_SHORT).show();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    sleep(1000);
//                }