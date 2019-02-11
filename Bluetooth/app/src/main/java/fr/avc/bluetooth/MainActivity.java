package fr.avc.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import fr.avc.bluetooth.R;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "App";
    public static final String Adresse ="20:14:12:03:07:82";//Address  de ES32
    private UUID UUId;
    private byte Commande = 0;
    String Nom ="Nom";
    BluetoothAdapter Blue_Adapte = BluetoothAdapter.getDefaultAdapter(); // Objet support Bluetooth
    private BluetoothDevice ESP;
    Set<BluetoothDevice> AppareilAssoci; //Objet de stokage de la liste des pareille connecté
    Threard_Echange EchangeTh;
    Thread_Connection ConnectionTh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UUId = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        if (Blue_Adapte.isDiscovering()) {
            Blue_Adapte.cancelDiscovery();
        }

        Verif_Blue(Blue_Adapte);
        Activation(Blue_Adapte);
        Toast("Connexion à Ceasar");

        AppareilAssoci = Blue_Adapte.getBondedDevices();
        if(AppareilAssoci.size() > 0 ){
            ESP = null;
            for ( BluetoothDevice Appareil : AppareilAssoci) { //Recherche du serveur bluetooth
                if (Appareil.getAddress().equals(Adresse)) {
                    ESP = Appareil;
                    break;
                }
            }
            Log.d(TAG,"Création du Thread de Co");
            ConnectionTh = new Thread_Connection(ESP);
            ConnectionTh.run();
            Toast("La connection a été effectué");
            EchangeTh.Ecriture(Nom);
        }else{
            Log.d(TAG,"Aucun appareil associer !");
            finish();
        }
    }
    void Verif_Blue(BluetoothAdapter Bl){
        if (Bl == null) {// Verification de la présence du bluetooth
            CharSequence text = "Il n'y a pas bluetooth";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        }

    }
    void Activation(BluetoothAdapter Bl){
        if (!Bl.isEnabled()) {
            CharSequence text = "Activer le Bluetooth";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(this, text, duration);
            toast.show();

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }
    }

    void Connection(BluetoothSocket Blue_Socket) {
        EchangeTh = new Threard_Echange(Blue_Socket);
    }

    void Toast(CharSequence text){
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

    private class Threard_Echange extends Thread {
        private final BluetoothSocket Socket;
        private final InputStream InStream;
        private final OutputStream OutStream;
        private byte[] Buffer;

        Threard_Echange(BluetoothSocket Sckt) {
            Socket = Sckt;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = Socket.getInputStream();
            }
            catch (IOException e) {
                Log.e(TAG, "Une erreure un survenue lors de la creation de l'entré du flux", e);
            }
            try {
                tmpOut = Socket.getOutputStream();
            }
            catch (IOException e) {
                Log.e(TAG, "Une erreure un survenue lors de la creation de la sortie du flux", e);
            }
            InStream = tmpIn;
            OutStream = tmpOut;
        }

        public void run() {
            Buffer = new byte[1024];//
            int NbrOct;

            while (true ) {
                try {
                    NbrOct = InStream.read(Buffer);
                    if(NbrOct >0){
                        Log.d(TAG, String.valueOf(Buffer[0]));
                    }
                }
                catch(IOException e) {
                    Log.d(TAG, "L'entré du flux était déconnecter", e);
                    break;
                }
            }
        }

        void Ecriture(String Msg) {
            byte[] Oct = new byte[Msg.length()];
            for(int i = 0; i<Oct.length;i++){
                Oct[i] = (byte) Msg.charAt(i);
            }
            try {
                OutStream.write(Oct);
            }
            catch(IOException e) {
                Log.e(TAG, " Une errueur est surbenue lors de l'envoie du message", e);
            }
        }
        void Annuler() {
            try {
                Socket.close();
            }
            catch(IOException e) {
                Log.e(TAG, "On ne peut fermer le client Socket");
            }
        }
    }

    private class Thread_Connection extends Thread {
        private final BluetoothSocket Socket;
        private final BluetoothDevice Appa;

        Thread_Connection(BluetoothDevice Appareil) {

            BluetoothSocket tmp = null;
            Appa = Appareil;
            try {
                Log.d(TAG,"Teantative de co !");
                tmp = Appa.createRfcommSocketToServiceRecord(UUId);
            }
            catch(IOException e) {
                Log.e(TAG, "Erreur lors de creation du Socket", e);
            }
            Socket = tmp;
        }

        public void run () {
            Log.d(TAG,"RUN !");
            Blue_Adapte.cancelDiscovery();
            try {
                Log.d(TAG,"Socket.connect()");
                Socket.connect();
            }
            catch(IOException connectExecption) {
                try {
                    Log.d(TAG,"Skct Close ?");
                    Socket.close();
                }
                catch(IOException closeException) {
                    Log.e(TAG, "On ne peut fermer le client Socket", closeException);
                    Log.d(TAG,"On ne peut fermer le client Socket");
                }
                return;
            }
            Log.d(TAG,"Connection ?");
            Connection(Socket);
        }

        void Annuler() {
            try {
                Socket.close();
            }
            catch(IOException e) {
                Log.e(TAG, "On ne peut fermer le client Socket", e);
            }
        }
    }

}