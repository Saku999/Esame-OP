package com.example.openvctest;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.Toast;

import com.example.opencvtest.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.File;
import java.nio.file.Files;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    int ServerPort = 4747;
    ServerSocket serverSocket;
    Mat mRgba;  //matrice dei pixel ricevuti dalla fotocamera
    Bitmap bpm = null;
    Thread Thread1 = null;
    String IPAddress;
    TextView textView;
    AlertDialog alertDialog;
    private OutputStream output;
    DataOutputStream dos;
    JavaCameraView camera;  //view della fotocamera
    BaseLoaderCallback baseLoaderCallback;//bo
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_BACK; //selezione la camera da utilizzare (posteriore)
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    //
    int cont = 0;
    File files;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.Ciao);
        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        camera = findViewById(R.id.javaCameraView);
        baseLoaderCallback = new BaseLoaderCallback(this) { //FA PARTIRE LA VIDEOCAMERA SE OPENCV E' PARTITO BENE

            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.d("OPENCV", "SIIIII");
                        System.out.println("PARTITO!!!");
                        camera.enableView(); //attivo la fotocamera se tutto è andato bene
                        break;
                    }
                    default: {
                        super.onManagerConnected(status);
                        System.out.println("FUCKKKKK!!!");
                        Log.d("OPENCV","NOOOO");
                        break;
                    }
                }
            }
        };

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
                    output = socket.getOutputStream();  //creazione del mittente
                    dos = new DataOutputStream(output); //creo il canale per la trasmissioni dei dati (PNG)
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

                            //CHIEDO I PERMESSI DELLA FOTOCAMERA E LA INIZIALIZZO
                            if(permission()) {
                                //inizializzo la camera
                                initializeCamera(camera, activeCamera);
                                openCamera();   //apro la fotocamera
                            }else{
                                //metto un allert per avvertire l'utente che non ha accettato i permessi
                                alertDialog.setTitle(":(");
                                alertDialog.setMessage("Non hai accettato i permessi :'(");
                                alertDialog.setCancelable(false);
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                finish();
                                                System.exit(0);
                                            }
                                        });
                                alertDialog.show();
                            }
                        }
                    });
                    //new Thread(new Thread2()).start();  //FACCIO PARTIRE LO SCAMBIO DI INFORMAZIONI
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //DOVREI INVIARE IL VIDEDO-FRAME CORRENTE AL PC
    private class Thread2 implements Runnable {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            try{
                //creo il Bitmap prima di spedire l'arrey di byte
                bpm = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                //lo converto in File
                bitmapToVideoEncoder.queueFrame(bpm);
            }catch(CvException e){Log.d("Exception",e.getMessage());}
        }
    }

    BitmapToVideoEncoder bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
        @Override
        public void onEncodingComplete(File outputFile) {
            //Toast.makeText(this,  "Encoding complete!", Toast.LENGTH_LONG).show();
        }
    });

    //apro la fotocamera a tutto schermo
    private void openCamera() {
            initializeCamera(camera, activeCamera);
            textView.setVisibility(View.GONE);
            camera.enableView();
            System.out.println("BELAAAAA!!!");
    }

    //INIZIALIZZO LA FOTOCAMERA
    private void initializeCamera(JavaCameraView camera, int activeCamera){
        camera.setCameraPermissionGranted();
        camera.setCameraIndex(activeCamera);
        camera.setVisibility(SurfaceView.VISIBLE);
        camera.setCvCameraViewListener(this);
    }

    //CONTROLLA CHE CI SIA IL PERMESSO DELLA FOTOCAMERA, ALTRIMENTI LO CHIEDE ALL'UTENTE
    private boolean permission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //chiedo i permessi della fotocamera nel caso non li abbia ancora
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 50);
        } else {
            initializeCamera(camera, activeCamera);
            return true;
        }
        return false;
    }

    //METODI DA IMPLEMENTARE DELL'INTERFACCIA
    @Override
    public void onCameraViewStarted(int width, int height) {
            mRgba.release();
    }

    @Override
    public void onCameraViewStopped() {

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba=inputFrame.rgba();
        //faccio partire l'encoding
        if(cont == 0){
            files = new File(getFilesDir(), "files");
            bitmapToVideoEncoder.startEncoding(mRgba.cols(), mRgba.rows(), files);
        }
        cont++;
        new Thread(new Thread2()).start();
        //fermo l'encoding
        if(cont == 30){
            bitmapToVideoEncoder.stopEncoding();
            //creo byte array
            try {
                System.out.println("IL FILE:" + files.getTotalSpace());
                byte[] array = Files.readAllBytes(files.toPath());
                dos.writeInt(array.length);             //dimensiono l'array dei dati in uscita
                dos.write(array, 0, array.length);  //scrivo sull'outputstream dei dati
            }catch(IOException e) {e.printStackTrace();}
            System.out.println("Ciao");
            if(files.delete())
                System.out.println("Cancellato OK");
            cont=0;
        }
        return mRgba;
    }

    //INIZIALIZZAZIONE OPENCV
    @Override
    public void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()) {
            Log.d("OPENCV","OpenCV caricato correttamente");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS); //chiama baseLoaderCallback (definito in alto, fa attivare la videocamera se tutto va bene)
        }
        else{
            Log.d("OPENCV","Errore OpenCV non caricato");
        }
    }
}
