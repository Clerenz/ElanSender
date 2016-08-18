package de.clemensloos.elan.sender;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.whitebyte.wifihotspotutils.ClientScanResult;
import com.whitebyte.wifihotspotutils.WifiApManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import javax.jmdns.JmDNS;
//import javax.jmdns.ServiceEvent;
//import javax.jmdns.ServiceInfo;
//import javax.jmdns.ServiceListener;

import de.clemensloos.elan.sender.database.DatabaseHandler;
import de.clemensloos.elan.sender.database.Song;

public class ElanSenderActivity extends Activity implements SensorEventListener {

    private static final int ASYNC_IDENTIFIER_NONE = 0;
    private static final int ASYNC_IDENTIFIER_SEND = 1;

    // ui and gui
    private Vibrator myVib;

    private TextView textViewLast;
    private TextView textViewAct;
    private TextView textViewStatus;

    private Button nextButton;
    private Button sendButton;

    private ProgressDialog progressDialog;

    private String act = "";
    private String last = "";

    private WifiApManager wifiApManager = null;
    private List<Client> receiver = new ArrayList<>();


    // properties
    SharedPreferences sharedPreferences;
    // wifiMode
    // 0: enter ip address
    // 1: mobile WiFi hotspot
    private int wifiMode = 0;
    private int defaultPort = -1;
    private boolean stopOnHttpError = true;

    // sensor listener things
    private SensorManager mSensorManager;

    private static final int NEXT_ACTION_DELTA = 2000;
    private static final int TIMEOUT_HAND_OVER = 300;
    private static final float SHAKE_THRESHOLD = 1.8F;

    private long lastAction = 0;
    private long startWipe = 0;
    private boolean handOver = false;

    private MulticastLock lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elan_sender);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        myVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Typeface font = Typeface.createFromAsset(getAssets(), getResources().getString(R.string.font_name));

        textViewLast = (TextView) findViewById(R.id.textViewLast);
        textViewLast.setTypeface(font);
        textViewAct = (TextView) findViewById(R.id.textViewAct);
        textViewAct.setTypeface(font);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);

        findViewById(R.id.button1).setOnClickListener(new MyOnClickListener(1));
        findViewById(R.id.button2).setOnClickListener(new MyOnClickListener(2));
        findViewById(R.id.button3).setOnClickListener(new MyOnClickListener(3));
        findViewById(R.id.button4).setOnClickListener(new MyOnClickListener(4));
        findViewById(R.id.button5).setOnClickListener(new MyOnClickListener(5));
        findViewById(R.id.button6).setOnClickListener(new MyOnClickListener(6));
        findViewById(R.id.button7).setOnClickListener(new MyOnClickListener(7));
        findViewById(R.id.button8).setOnClickListener(new MyOnClickListener(8));
        findViewById(R.id.button9).setOnClickListener(new MyOnClickListener(9));
        findViewById(R.id.button0).setOnClickListener(new MyOnClickListener(0));

        findViewById(R.id.buttonC).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                myVib.vibrate(25);
                act = "";
                textViewAct.setText(act);
                sendButton.setEnabled(false);
            }
        });

        sendButton = (Button) findViewById(R.id.buttonS);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                myVib.vibrate(25);
                sendHttp();
            }
        });
        sendButton.setEnabled(false);

        findViewById(R.id.buttonBlack).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                myVib.vibrate(25);
                act = "";
                textViewAct.setText(act);
                sendHttp();
            }
        });

        nextButton = (Button) findViewById(R.id.buttonNext);
        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSong();
            }
        });
        nextButton.setEnabled(false);

        // Keep the screen always on
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register the sensor listeners
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sharedPreferences.getBoolean(getResources().getString(R.string.pref_use_wipe_key), false)) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (sharedPreferences.getBoolean(getResources().getString(R.string.pref_use_shake_key), false)) {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        // refresh preferences

        // use hotspot
        wifiApManager = new WifiApManager(ElanSenderActivity.this);
        if (wifiApManager.isWifiApEnabled()) {
            wifiMode = 1;
        } else {
            wifiMode = 0;
        }

        // default port
        String portString = sharedPreferences.getString(getResources().getString(R.string.pref_port_key),
                getResources().getString(R.string.pref_port_default));
        try {
            defaultPort = Integer.parseInt(portString);
        } catch (Exception e) {
            showMessage(R.string.message_port_error);
        }

        // stop on error
        stopOnHttpError = sharedPreferences.getBoolean(getResources().getString(R.string.pref_notify_on_error_key),
                true);

        // Get clients (and send pending message, if there is one)
        //if (receiver.size() == 0) {
            refreshClients();
        //}
    }

    public void refreshClients() {
        switch (wifiMode) {
            case (0):
                getClientsFromSettings();
                break;
            case (1):
                new AsyncGetClients().execute(ASYNC_IDENTIFIER_NONE);
                break;
//            case (2):
//                callAsyncGetService();
//                break;
        }
    }

    @Override
    protected void onPause() {

        // stop sensor listening
        mSensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (lock != null) {
            lock.release();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_elan_sender, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        if (item.getItemId() == R.id.menu_refresh_clients) {
            refreshClients();
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_search_clients) {
            Intent intent = new Intent(this, WifiQueryActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_spec_1) {
            act = getResources().getString(R.string.spec_value1);
            encodeAndShow();
            sendHttp();
        } else if (item.getItemId() == R.id.menu_spec_2) {
            act = getResources().getString(R.string.spec_value2);
            encodeAndShow();
            sendHttp();
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Get current time
        long now = System.currentTimeMillis();

        // Handle "hand over" event (proximity sensor)
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {

            // if hand entered
            if (event.values[0] < 5.0) {

                // if hand was inside before or if last hand over was too close
                if (handOver || lastAction + NEXT_ACTION_DELTA > now) {
                    // dismiss
                    return;
                }

                // start wipe
                handOver = true;
                startWipe = now;
                return;
            }

            // if hand leaving
            else if (event.values[0] >= 5.0) {

                // if hand not entered before or if too much time passed
                if (!handOver || startWipe + TIMEOUT_HAND_OVER < now) {
                    // stop hand over
                    handOver = false;
                    return;
                }

                // successful hand over -> next song
                lastAction = now;
                handOver = false;

                nextSong();
            }
        }

        // Handle shake event
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // get values
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // normalize
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // G-Force will be 1 when there is no movement. (gravity)
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // if gForce is larger than threshold and last action was not too
            // close
            if (gForce > SHAKE_THRESHOLD && lastAction + NEXT_ACTION_DELTA < now) {
                // --> next song
                lastAction = now;
                nextSong();
            }
        }

    }


    private void encodeAndShow() {

        String encVal = act;
        if (encVal.startsWith(getResources().getString(R.string.spec_ident))) {
            encVal = encVal.replace(getResources().getString(R.string.spec_ident), "");
            int i = Integer.parseInt(encVal, 16);
            encVal = Character.toString((char) i);
        }
        textViewAct.setText(encVal);
    }

    /**
     * Does what it says it does
     */
    private void sendHttp() {

        // verify connectivity
        if (receiver.size() == 0) {
            refreshClients();
            return;
        }

        // send http message
        new AsyncSendHttp().execute(receiver.toArray(new Client[receiver.size()]));

    }

    /**
     * If possible, send next number
     */
    private void nextSong() {
        if (!last.equals("")) {
            try {
                int i = Integer.parseInt(last);
                myVib.vibrate(25);
                i++;
                if (i == 1000) {
                    i = 0;
                }
                act = "" + i;
                textViewAct.setText(act);
                sendHttp();
            } catch (NumberFormatException e) {
                // ignore, maybe last one was a smiley :-)
            }
        }
    }

    /*
     * Set a new status by resource id
	 */
    private void setStatus(int id) {
        setStatus(getResources().getString(id));
    }

    /**
     * Set new status
     *
     * @param status
     */
    private void setStatus(final String status) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText(status + "   ");
            }
        });
    }

    /**
     * Show a new message by resource id
     *
     * @param id
     */
    private void showMessage(int id) {
        showMessage(getResources().getString(id));
    }

    /**
     * Show a new message
     *
     * @param message
     */
    private void showMessage(final String message) {

        runOnUiThread(new Runnable() {
            public void run() {
                Utils.showDialog(message, ElanSenderActivity.this, myVib);
            }
        });
    }


    private void getClientsFromSettings() {

        receiver = new ArrayList<>();

        if (!Utils.isWiFiNetworkAvailable(this)) {
            showMessage(R.string.message_no_network);
            return;
        }

        String ipString = sharedPreferences.getString(getResources().getString(R.string.pref_ip_key),
                getResources().getString(R.string.pref_ip_default));

        if (ipString.equals("")) {
            setStatus(getResources().getString(R.string.status_connected_devices) + " 0");
            ElanSenderActivity.this.showMessage(R.string.message_no_device_configured);
            return;
        }

        String[] addresses = ipString.split(",");
        if (addresses.length == 0) {
            setStatus(getResources().getString(R.string.status_connected_devices) + " 0");
            ElanSenderActivity.this.showMessage(R.string.message_no_device_configured);
            return;
        }

        for (String s : addresses) {
            receiver.add(new Client(s));
        }

        setStatus(getResources().getString(R.string.status_connected_devices) + " " + receiver.size());
    }


    /**
     * Class that fetches the clients from the wifi ap
     */
    private class AsyncGetClients extends AsyncTask<Integer, String, Boolean> {

        private int flag = 0;

        @Override
        protected Boolean doInBackground(Integer... params) {

            flag = params[0];

            // clear old results
            receiver = new ArrayList<>();

            // create manager
            wifiApManager = new WifiApManager(ElanSenderActivity.this);
            // is access point (ap) enabled?
            if (!wifiApManager.isWifiApEnabled()) {
                setStatus(R.string.status_ap_not_enabled);
                ElanSenderActivity.this.showMessage(R.string.message_ap_not_enabled);
                return false;
            }
            // get reachable clients
            ArrayList<ClientScanResult> clients = wifiApManager.getClientList(true, 500);
            if (clients.size() == 0) {
                if (wifiApManager.getClientList(false, 500).size() > 0) {
                    setStatus(getResources().getString(R.string.status_connected_devices) + " 0");
                    ElanSenderActivity.this.showMessage(R.string.message_device_not_reachable);
                } else {
                    setStatus(getResources().getString(R.string.status_connected_devices) + " 0");
                    ElanSenderActivity.this.showMessage(R.string.message_no_device);
                }
                return false;
            }
            // get the ips
            for (ClientScanResult clientScanResult : clients) {
                receiver.add(new Client(clientScanResult.getIpAddr()));
                publishProgress(clientScanResult.getIpAddr());
            }

            setStatus(getResources().getString(R.string.status_connected_devices) + " " + receiver.size());
            return true;
        }

        @Override
        protected void onProgressUpdate(String... foundIp) {
            // handle find one new ip
        }

        @Override
        protected void onPostExecute(Boolean successful) {

            if (successful && flag == ASYNC_IDENTIFIER_SEND) {
                new AsyncSendHttp().execute(receiver.toArray(new Client[receiver.size()]));
            }
        }

    }


    /**
     * Class that sends the http requests to the clients
     */
    private class AsyncSendHttp extends AsyncTask<Client, Client, Boolean> {

        @Override
        protected Boolean doInBackground(Client... receivers) {

            int retry = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.pref_retry_key), "0"));

            // Prepare httpClient
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            int timeoutConfig = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.pref_timeout_key), "2"));
            int timeoutConnection = 1000 * timeoutConfig;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            int timeoutSocket = 1000 * timeoutConfig;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

            //Prepare httpRequest
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("song", act));

            DatabaseHandler dbh = new DatabaseHandler(ElanSenderActivity.this);
            Song song = null;
            try {
                song = dbh.getSongById(Integer.parseInt(act));
            } catch (NumberFormatException e) {
                // ignore
            }
            nameValuePairs.add(new BasicNameValuePair("title", (song == null) ? "" : song.getTitle()));
            nameValuePairs.add(new BasicNameValuePair("artist", (song == null) ? "" : song.getArtist()));

            for (Client client : receivers) {

                boolean clientSuc = false;

                for (int i=0; i<=retry; i++) {

                    Log.d("ElanSender", "Retry " + i);

                    try {

                        URI uri = new URL("http", client.ip, client.port, "").toURI();
                        HttpPost httpPost = new HttpPost(uri);

                        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                        // Execute HTTP Post Request
                        HttpResponse response = httpClient.execute(httpPost);
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            clientSuc = true;
                            break;
                        }

                    } catch (Exception e) {
                        // ignore
                    }
                }

                if (!clientSuc && stopOnHttpError) {
                    publishProgress(client);
                    return false;
                }
            }
            return true;
        }

        @Override
        // only called if an error occurred
        protected void onProgressUpdate(Client... client) {

            showMessage(getResources().getString(R.string.message_error_device) + " " + client[0].ip);
        }

        @Override
        protected void onPostExecute(Boolean successful) {

            if (successful) {

                last = act;
                // if(textViewAct.getText().equals("")) {
                // textViewLast.setText(act);
                // }
                textViewLast.setText(textViewAct.getText());

                act = "";
                textViewAct.setText(act);
                sendButton.setEnabled(false);
            }

            boolean canNext = false;
            try {
                Integer.parseInt(last);
                canNext = true;
            } catch (NumberFormatException nfe) {
                // ignore
            } finally {
                nextButton.setEnabled(canNext);
            }

        }
    }

    /**
     * OnClickListener for the numpad keys
     */
    private class MyOnClickListener implements OnClickListener {

        private int num;

        public MyOnClickListener(int num) {
            super();
            this.num = num;
        }

        @Override
        public void onClick(View v) {
            myVib.vibrate(25);
            if (act.length() > 2) {
                act = act.substring(1);
            }
            act += num;
            textViewAct.setText(act);
            sendButton.setEnabled(true);
        }

    }

    private class Client {

        protected String ip;
        protected int port;

        Client(String ip) {
            this.ip = ip;
            this.port = defaultPort;
        }

        Client(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

    }

}
