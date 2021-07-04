package com.example.djradio2;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
  private static final String TAG = SettingsActivity.class.getName();
  String ip_address;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    // Set up edit for ip address
    EditText ipAddressEdit = (EditText)findViewById(R.id.ipAddressEditText);
    ipAddressEdit.setText("192.168.x.y");
    ipAddressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          ip_address = view.getText().toString();
//        Toast.makeText(SettingsActivity.this, ip_address, Toast.LENGTH_SHORT).show();
          return true;
        }
        return false;
      }
    });

    Button cancelButton = findViewById(R.id.cancelButton);
    cancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent cancelIntent = new Intent();
        setResult(RESULT_CANCELED, cancelIntent);
        finish();
      }
    });
    
    Button setButton = findViewById(R.id.setButton);
    setButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent setIntent = new Intent();
        // Check ip_address
        boolean ip_address_good = true;
        if (ip_address != null) {
          String[] octet_str = ip_address.split("\\.", 4);
          for (int i = 0; i < 4; i++) {
//            Log.i(TAG, "octet_str[" + i + "]=" + octet_str[i]);
            int octet_int = Integer.parseInt(octet_str[i]);
//            Log.i(TAG, "octet_int=" + octet_int);
            if (octet_int < 0 | octet_int > 255) ip_address_good = false;
          }
        }
        if (!ip_address_good) {
          Toast.makeText(SettingsActivity.this, "Bad IP address", Toast.LENGTH_SHORT).show();
        }
        else{
          setIntent.putExtra("ip_address", ip_address);
          setResult(RESULT_OK, setIntent);
          finish();
        }
      }
    });
  }
} // End SettingsActivity