package com.example.inverson;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private Button loginButton; //Button initialisieren
    private TextView loginID;
    private TextView loginPassword;
    private LocationManager locationManager;

    public static final String TAG ="Main Activity";
    public static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = (Button) findViewById(R.id.loginButton);
        loginID = (TextView) findViewById(R.id.loginID);
        loginPassword = (TextView) findViewById(R.id.loginPassword);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        loginButton.setOnClickListener(new View.OnClickListener(){  //ActionListener für Button loginButton
            public void onClick(View view){
                if(isMapsOK()) {
                    loginID.setText("Anja stinks"); //LOGIN
                    Intent intent = new Intent(MainActivity.this, MapActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
    @Override
    protected void onStart(){
        super.onStart();

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "GPS Check complete", Toast.LENGTH_SHORT).show();
        }else{
            alertGPSDisabled();
        }
    }

    private void alertGPSDisabled(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        System.exit(0);
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public boolean isMapsOK() { //check google maps version
        Log.d(TAG, "isMapsOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            //version is correct, user can continue with maps
            Log.d(TAG, "isMapsOK: Google Play Services is working");
            Toast.makeText(this, "Google Maps version OK", Toast.LENGTH_SHORT).show();
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //fixable error occured
            Log.d(TAG, "isMapsOK: an error occured but we can fix it");
            //get dialog from google
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "you can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
