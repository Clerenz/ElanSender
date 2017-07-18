package de.clemensloos.elan.sender;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WifiQueryActivity extends Activity {

    private Button start;
    private Button cancel;
    private Button add;
    private Button okay;
    private TextView devicesList;
    private ProgressBar progressDevices;
    private TextView status;

    private boolean discoveryRunning = false;
    private List<String> lastResults;

    private int defaultPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wifi_query);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        start = ((Button)findViewById(R.id.but_start_query));
        cancel = ((Button)findViewById(R.id.but_cancel_results));
        add = ((Button)findViewById(R.id.but_add_results));
        okay = ((Button)findViewById(R.id.but_use_results));
        devicesList = ((TextView) findViewById(R.id.textView_results));
        progressDevices = ((ProgressBar)findViewById(R.id.progressBar_discovery));
        status = ((TextView)findViewById(R.id.textView_status));

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setEnabled(false);
                add.setEnabled(false);
                okay.setEnabled(false);
                discoveryRunning = true;
                lastResults = null;
                new AsyncSearch().execute("");
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (discoveryRunning) {
                    discoveryRunning = false;
                }
                else {
                    finish();
                }
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastResults != null && lastResults.size() > 0) {
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(WifiQueryActivity.this);
                    String oldValues = sharedPrefs.getString(getResources().getString(R.string.pref_ip_key), "");
                    String result = combine(oldValues, lastResults);
                    SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
                    sharedPrefsEditor.putString(getResources().getString(R.string.pref_ip_key), result);
                    sharedPrefsEditor.commit();
                    finish();
                }
            }
        });

        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastResults != null && lastResults.size() > 0) {
                    String value = "";
                    for (String s : lastResults) {
                        value += s + ",";
                    }
                    SharedPreferences.Editor sharedPrefsEditor = PreferenceManager.getDefaultSharedPreferences(WifiQueryActivity.this).edit();
                    sharedPrefsEditor.putString(getResources().getString(R.string.pref_ip_key), value);
                    sharedPrefsEditor.commit();
                    finish();
                }
            }
        });

        // default port
        String portString = PreferenceManager.getDefaultSharedPreferences(this).getString(getResources().getString(R.string.pref_port_key),
                getResources().getString(R.string.pref_port_default));
        try {
            defaultPort = Integer.parseInt(portString);
        } catch (Exception e) {
            // ignore
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    private String combine(String oldValues, List<String> newValues) {

        Set<String> stringSet = new HashSet<>(Arrays.asList(oldValues.split(",")));
        stringSet.addAll(newValues);

        String[] stringArray = stringSet.toArray(new String[stringSet.size()]);
        Arrays.sort(stringArray);

        String result = "";
        for (String s : stringArray) {
            result += s + ",";
        }
        return result;
    }


    public String getIpAddr() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        return String.format(
                "%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
    }

    class AsyncSearch extends AsyncTask<String, Integer, List<String>> {

        public List<String> doInBackground(String... args) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDevices.setProgress(0);
                    devicesList.setText("Devices:");
                    status.setText("");
                }
            });

            List<String> activeDevices = new ArrayList<>();

            String ipAddress = getIpAddr();

            InetAddress host;

            try {
                host = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                activeDevices.add("Error: " + e.getMessage());
                this.cancel(true);
                return activeDevices;
            }

            byte[] ip = host.getAddress();

            for(int i = 0; i <= 255; i++) {
                if(!discoveryRunning) {
                    break;
                }

                try {
//
                    ip[3] = (byte) i;
                    String hostAddress = InetAddress.getByAddress(ip).getHostAddress();

                    URI uri = new URL("http", hostAddress, defaultPort, "alive").toURI();

                    HttpParams httpParameters = new BasicHttpParams();
                    // Set the timeout in milliseconds until a connection is established.
                    // The default value is zero, that means the timeout is not used.
                    int timeoutConnection = 500;
                    HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                    // Set the default socket timeout (SO_TIMEOUT)
                    // in milliseconds which is the timeout for waiting for data.
                    int timeoutSocket = 500;
                    HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

                    DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);

                    HttpGet httpGet = new HttpGet(uri);


                    // Execute HTTP Get Request
                    HttpResponse response = httpClient.execute(httpGet);
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        activeDevices.add(hostAddress);
                        final String finalHostAddress = hostAddress;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                devicesList.append("\n" + finalHostAddress);
                            }
                        });
                    }
                } catch (URISyntaxException | IOException e) {
                    // ignore
                }

                final int progress = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDevices.setProgress(progress);
                    }
                });
            }


            return activeDevices;
        }


        public void onCancelled(List<String> result) {
            start.setEnabled(true);
            status.setText(result.get(0));
        }


        public void onPostExecute(List<String> result) {
            start.setEnabled(true);
            status.setText("Found " + result.size() + " devices running the receiver.");
            if (result.size() > 0) {
                lastResults = result;
                add.setEnabled(true);
                okay.setEnabled(true);
            }

        }

    }

}
