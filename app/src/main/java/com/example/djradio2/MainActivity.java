// DJ Radio 2
// Communicates with Raspberry Pi - based dual radio to display station information after SEEK
// Displays discovered stations and program types
// Click on any button to play station

package com.example.djradio2;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.util.TypedValue;

import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE;
import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

  static int n_rows = 5;
  static int n_cols = 4;
  private static final String TAG = MainActivity.class.getName();
  private RequestQueue queue;
  private StringRequest stringRequest;
  private String ip_address, url;

  private Button currentStation = null;
  private int primaryColor, primaryColorVariant, onPrimaryColor;

  Button[][] button_array;
  Integer[][] button_ids;

  String ip_address_default = "192.168.2.101";

  final static int ACTIVITY_SETTINGS = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Find IP address of Raspberry Pi
    if (Build.FINGERPRINT.contains("generic"))
      ip_address = ip_address_default;   // Emulator
    else {   // Real device
      // Find IP address assigned to Raspberry Pi from Pixel Hotspot
      // ip neigh returns all IPs in neighborhood
      // One we want is 192.168.x.187 dev wlan1
      try {
        Process process = Runtime.getRuntime().exec("ip neigh");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
          output.append(buffer, 0, read);
        }
        reader.close();
        process.waitFor();
        String out = output.toString();
        int end_ind = out.indexOf(" dev wlan1");
        if (end_ind != -1) {
          out = out.substring(0, end_ind);
          int start_ind = out.lastIndexOf("192");
          if (start_ind != -1)
          ip_address = out.substring(start_ind);
          else{
            Toast.makeText(getApplicationContext(), "Can't determine IP address, check Wifi enabled", Toast.LENGTH_LONG).show();
            ip_address = ip_address_default;
          }
        }
        else{
          Toast.makeText(getApplicationContext(), "Can't determine IP address, check wifi enabled", Toast.LENGTH_LONG).show();
          ip_address = ip_address_default;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    Log.i(TAG, "ip address of Raspberry pi = " + ip_address);

    // Set colors from theme
    final TypedValue value = new TypedValue();
    getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
    primaryColor = value.data;
    getTheme().resolveAttribute(R.attr.colorPrimaryVariant, value, true);
    primaryColorVariant = value.data;
    getTheme().resolveAttribute(R.attr.colorOnPrimary, value, true);
    onPrimaryColor = value.data;

    // Set up Start button
    Button startButton = findViewById(R.id.startButton);
    startButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Log.i(TAG, "Start button clicked");
        // Send GET Request to start station seek program
        url = "http://" + ip_address + ":5000/find_stations";
        sendGet(url);
        try {
          sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    // Set up Exit button
    Button exitButton = findViewById(R.id.exitButton);
    exitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        System.exit(0);
      }
    });

    // Initialize button array
    button_array = new Button[n_rows][n_cols];
    button_ids = new Integer[n_rows][n_cols];

    int display_width = Resources.getSystem().getDisplayMetrics().widthPixels;
    int display_height = Resources.getSystem().getDisplayMetrics().heightPixels - 450; // Leave room for taskbar and Exit button
    Log.i(TAG, "Usable display width, height = " + display_width + " x " + display_height);

    GridLayout button_grid_layout = findViewById(R.id.buttonArray);
    button_grid_layout.setRowCount(n_rows);
    button_grid_layout.setColumnCount(n_cols);
    for (int i = 0; i < n_rows; i++) {
      for (int j = 0; j < n_cols; j++) {
        button_array[i][j] = new Button(this);
        button_array[i][j].setId(10 * i + j);
        button_ids[i][j] = button_array[i][j].getId();
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = Math.floorDiv(display_width, n_cols);
        params.height = Math.floorDiv(display_height, n_rows);
        Log.i(TAG, "Button width, height = " + params.width + " x " + params.height);
        params.rowSpec = GridLayout.spec(i);
        params.columnSpec = GridLayout.spec(j);
        button_array[i][j].setLayoutParams(params);
        button_grid_layout.addView(button_array[i][j]);
      }
    }

    // Start queue of network requests
    queue = Volley.newRequestQueue(this);

  } // End OnCreate

  private void sendGet(String url) {
    stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
      @Override
      public void onResponse(String response) {
        String stations = response.toString();
//      Log.i(TAG,"stations string= " + stations);
        String[] station_array = stations.split("\\|", 0);
        int n_stations = station_array.length - 1;  // Nothing after last | in string
        Log.i(TAG, "number of stations found=" + n_stations);
        if (n_stations > n_rows * n_cols) n_stations = n_rows * n_cols;  // Just in case
        for (int i_station=0; i_station<n_stations; i_station++) {
          Log.i(TAG, "station: " + station_array[i_station]);
          String freq = station_array[i_station].substring(0,5);
          String prog_type = station_array[i_station].substring(5);
          Log.i(TAG, "Station[" + i_station + "]:  Freq=" + freq + " Program Type=" + prog_type);
          int i = i_station / n_cols;  // Row
          int j = i_station % n_cols;  // Column
          button_array[i][j].setAllCaps(false);
          SpannableString f = new SpannableString(freq + '\n');
          SpannableString pty = new SpannableString(prog_type);
          f.setSpan(new AbsoluteSizeSpan(22, true), 0, freq.length(), SPAN_INCLUSIVE_INCLUSIVE);
          pty.setSpan(new AbsoluteSizeSpan(15, true), 0, pty.length(), SPAN_INCLUSIVE_INCLUSIVE);
          button_array[i][j].setText(TextUtils.concat(f, pty));
          button_array[i][j].setTextColor(onPrimaryColor);
          button_array[i][j].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              int button_id = view.getId();
              Log.i(TAG, "onClick button_id = " + button_id + ", position [" + button_id/10 + "][" + button_id % 10  + "]");
              String url = "http://" + ip_address + ":5000/playradio";
              String freq_fixed;
              if (freq.substring(0,1).equals(" ")) freq_fixed = freq.substring(1); else freq_fixed = freq;
              String freq_vol = freq_fixed + " 15";
              Log.i(TAG, "In onClick: url = " + url + " freq_vol = " + freq_vol);
              sendPost(url, freq_vol);
              if (currentStation != null) currentStation.setBackgroundTintList(getResources().getColorStateList(R.color.blue, getTheme())); //primaryColor
              button_array[i][j].setBackgroundTintList(getResources().getColorStateList(R.color.darkblue, getTheme()));  //primaryColorVariant
              currentStation = button_array[i][j];
            }
          });
        }
      }
    }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error){
        Log.i(TAG, "Get Response Error: " + error.toString());
      }
    });

    stringRequest.setRetryPolicy(new DefaultRetryPolicy(20000, 5, 1));
//    Log.i(TAG, "Retry policy:" +  stringRequest.getTimeoutMs() + " " +  stringRequest.getRetryPolicy());
    queue.add(stringRequest);
    Toast.makeText(getApplicationContext(), "Searching for stations...please wait", Toast.LENGTH_LONG).show();
  }

  private void sendPost(String url, String freqvol_string) {
    stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
      @Override
      public void onResponse(String response) {
        Log.i(TAG,"Post Response: " + response.toString());
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        Log.i("Info","Post Response Error: " + error.toString());
      }
    }){
      @Override
      protected Map<String, String> getParams() {
        Map<String, String> postData = new HashMap<String, String>();
        postData.put("freqvol", freqvol_string);
        return postData;
      }
    };

    queue.add(stringRequest);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivityForResult(intent, ACTIVITY_SETTINGS);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ACTIVITY_SETTINGS) {
      if (resultCode == RESULT_OK) {
        if (data.getStringExtra("ip_address") != null){
          ip_address= data.getStringExtra("ip_address");
          Log.i(TAG, "Return from SettingsActivity ip_address=" + ip_address);
        }
      }
    }
  } // End OnActivityResult
}  // End MainActivity
