package com.openvctest.opencvtest;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    int ServerPort = 4747;
    ServerSocket serverSocket;
    Thread Thread1 = null;
    String IPAddress;
    TextView textView;
    AlertDialog alertDialog;
    private PrintWriter output;
    private BufferedReader input;
    JavaCameraView camera;  //view della fotocamera
    BaseLoaderCallback baseLoaderCallback;//bo


    @Override
    protected void onCreate(Bundle savedInstanceState) implements CvCameraViewListener2 {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.Ciao);
        alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        if(permission()) {  //chiedo i permessi per la fotocamera
            //inizializzo la camera
            camera = findViewById(R.id.javaCameraView);
            camera.setCameraPermissionGranted();    //permessi camera
            camera.setCvCameraViewListener(this);
        }

        Thread1 = new Thread(new Thread1());
        Thread1.start();
    }

    class Thread1 implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(ServerPort);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                        IPAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                        textView.setText("Ip Address: " + IPAddress + "\n Port: " + ServerPort);
                    }
                });
                try {
                    socket = serverSocket.accept();
                    output = new PrintWriter(socket.getOutputStream());
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { //qui metto semplicemente un allert che mi avvisa l'avvenuta connessione
                            alertDialog.setTitle("Connect!");
                            alertDialog.setMessage("Hey! È avvenuta la connessione");
                            alertDialog.setCancelable(false);
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            openCamera();
                        }
                    });
                    new Thread(new Thread2()).start();  //FACCIO PARTIRE LO SCAMBIO DI INFORMAZIONI
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    if (message != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.append("client:" + message + "\n");
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void openCamera() {
            textView.setVisibility(View.GONE);
            camera.setVisibility(SurfaceView.VISIBLE);
            baseLoaderCallback = new BaseLoaderCallback(this) { //FA PARTIRE LA VIDEOCAMERA SE OPENCV E' PARTITO BENE
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS: {
                            Log.d("OPENCV","SIIIII");
                            camera.enableView(); //attivo la fotocamera se tutto è andato bene
                        }
                        break;
                        default: {
                            super.onManagerConnected(status);
                            Log.d("OPENCV","NOOOO");
                        }
                        break;
                    }
                }
            };
    }

    //CONTROLLA CHE CI SIA IL PERMESSO DELLA FOTOCAMERA, ALTRIMENTI LO CHIEDE ALL'UTENTE
    private boolean permission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 50);
        } else {
            return true;
        }
        return false;
    }
}
