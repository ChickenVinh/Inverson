package com.mahc.custombottomsheet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //CONSTANTS
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int DEFAULT_ZOOM = 12;

    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private GoogleMap mMap;
    private View mapView;
    private Context context = this;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> mAntennaCollection = new ArrayList<>();
    private String user;
    private Antenna selectedAntenna = null;
    private String obj;
    private int counter = 0;
    TextView bottomSheetTextView;
    View bottomSheet;
    BottomSheetBehavior behavior;
    ProgressDialog progressDialog;
    //IMG SERVER STUFF
    String ServerURL = "http://gastroconsultung-catering.com/getData.php";
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    boolean check = true;
    private String currentPhotoPath;
    private int page = 0;

    String statusResponse = "";
    String pictureResponse = "";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Logger
        Timber.plant(new DebugTree());
        //Get the Username from Login activity
        Intent suc = super.getIntent();
        user = suc.getStringExtra("User");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        RequestQueue queue = RequestQueueSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        mapView = mapFragment.getView();
        //Download Antennas
        //downloadCSV();
        getAndParseAntennas();

        //Request Permissions
        requestCameraPermission();
        requestLocationPermission();

        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorlayout);
        bottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        final RelativeLayout searchbar = coordinatorLayout.findViewById(R.id.Searchbar);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        searchbar.setVisibility(View.GONE);
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        searchbar.setVisibility(View.VISIBLE);
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
        bottomSheetTextView = bottomSheet.findViewById(R.id.bottom_sheet_title);
        //ItemPagerAdapter adapter = new ItemPagerAdapter(this,mDrawables);
        //ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        //viewPager.setAdapter(adapter);

        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        //behavior.setCollapsible(false);

        final TextView searchTextView = findViewById(R.id.txt_search);

        searchTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    if(!searchMarker(textView.getText().toString())){
                        Toast.makeText(getApplicationContext(), getString(R.string.error_antenna_not_found), Toast.LENGTH_SHORT).show();
                    }
                    hideSoftKeyboard();
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mClusterManager = new ClusterManager<Antenna>(this,mMap);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                //behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
            }
        });
        mMap.setMyLocationEnabled(true);
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            layoutParams.setMargins(0, 120, 0, 0);
        }
        final CustomClusterRenderer renderer = new CustomClusterRenderer(this, mMap, mClusterManager);

        mClusterManager.setRenderer(renderer);

        //MARKER LISTENER
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<Antenna>() {
            @Override
            public boolean onClusterItemClick(Antenna item) {
                displayAntenna(item);
                return true;
            }
        });
        // Show Vietnam
        LatLng vietnam = new LatLng(16, 106.5);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnam,5.5f));
    }
    //PERMISSION STUFF
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestCameraPermission(){
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }
    //SEARCH FOR ANTENNA
    private boolean searchMarker(String searchtext){
        for (Antenna ant : mAntennaCollection) {
            String tit = ant.getTitle().toLowerCase(Locale.ROOT);
            String ext = ant.getExtTitle().toLowerCase(Locale.ROOT);
            if(searchtext.length() > 4 && (tit.contains(searchtext.toLowerCase(Locale.ROOT))
                    || ext.contains(searchtext.toLowerCase(Locale.ROOT)) )){
                displayAntenna(ant);
                return true;
            }
        }
        return false;
    }
    //DISPLAY ANTENNA-DATA ON BOTTOMSHEET
    private void displayAntenna(Antenna item){
        selectedAntenna = item;
        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(item.getPosition().latitude - 0.02,item.getPosition().longitude))
                .zoom(DEFAULT_ZOOM).build();
        //Zoom in and animate the camera.
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        //BOTTOMSHEET STUFF
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_antenne);
        RoundedBitmapDrawable roundedPic = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        final float roundPx = (float) bitmap.getWidth() * 0.06f;
        roundedPic.setCornerRadius(roundPx);

        counter=0;
        statusResponse = "";
        pictureResponse = "";
        grabStatuses();
        grabLatestPictures();

        ImageView Pic = findViewById(R.id.bottom_sheet_pic);
        TextView Title = findViewById(R.id.bottom_sheet_title);
        TextView Address = findViewById(R.id.bottom_sheet_address);
        TextView extTitle = findViewById(R.id.bottom_sheet_ext_title);
        Pic.setImageDrawable(roundedPic);
        extTitle.setText(item.getExtTitle());
        Title.setText(item.getTitle());
        Address.setText(item.getAddress());
    }
    private void grabLatestPictures(){

        //http://gastroconsultung-catering.com/set_picture.php?action=get&antID=20LSN1002&module=Object-1
        String get_url = getResources().getString(R.string.picture_script) + "?action=get&antID=" + selectedAntenna.getTitle();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        pictureResponse = response;
                        if(!statusResponse.trim().isEmpty()) {
                            buildPreview();
                        }else{//wait a bit and do it
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    buildPreview();
                                }
                            }, 500);  //the time is in miliseconds
                        }
                        /*
                        try {
                            JSONArray jArray = new JSONArray(response);
                            for (int i=0; i < jArray.length(); i++)
                            {
                                JSONObject tmpObj = jArray.getJSONObject(i);
                                String fullpath = getResources().getString(R.string.server_url) + tmpObj.getString("path");

                                if (fullpath.contains(obj1_pic.getTag().toString())) {
                                    Picasso.with(obj1_pic.getContext()).load(fullpath).into(obj1_pic);
                                    //spin1.setSelection(Integer.parseInt(s.split("###")[2]));
                                    //spin1.setEnabled(true);
                                }
                                if (fullpath.contains(obj2_pic.getTag().toString())) {
                                    Picasso.with(obj2_pic.getContext()).load(fullpath).into(obj2_pic);
                                    //spin2.setSelection(Integer.parseInt(s.split("###")[2]));
                                    //spin2.setEnabled(true);
                                }
                                if (fullpath.contains(obj3_pic.getTag().toString())) {
                                    Picasso.with(obj3_pic.getContext()).load(fullpath).into(obj3_pic);
                                    //spin3.setSelection(Integer.parseInt(s.split("###")[2]));
                                    //spin3.setEnabled(true);
                                }
                            }
                        }catch (JSONException ex){
                            ex.printStackTrace();
                        }
                        */
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
            }
        }){
            @Override
            public Priority getPriority() {
                return Priority.LOW;
            }
        };

        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(stringRequest);

    }
    //DOWNLOAD N PARSE ANTENNA DATA
    private void downloadCSV(){
        progressDialog = ProgressDialog.show(MainActivity.this,"Download Antennas","Please Wait",false,false);
        String get_url = getResources().getString(R.string.URL_antennasCSV);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        String convtmp = "";
                        convtmp = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        ArrayList<String>  lstAntennas = new ArrayList<String>(Arrays.asList(convtmp.split("\r\n")));
                        if(!lstAntennas.isEmpty()){
                            parseCSV(lstAntennas);
                            //ParseTask pt = new ParseTask();
                            //pt.execute(lstAntennas);
                        }else{
                            Toast.makeText(getBaseContext(), "Network Problem! No Antenna Data", Toast.LENGTH_LONG).show();
                        }
                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(stringRequest);
    }
    private void addAntennasToCollection(ArrayList<Antenna> tmp_ant){
        if(tmp_ant.size()>0) {
            mAntennaCollection = tmp_ant;
            mClusterManager.addItems(mAntennaCollection);
            fillAntennaSpinner();
        }else{
            Toast.makeText(getApplicationContext(), "No Antenna Data Received!", Toast.LENGTH_LONG).show();
        }
    }
    private void getAndParseAntennas(){
        progressDialog = ProgressDialog.show(MainActivity.this,"Download Antennas","Please Wait",false,false);
        String get_url = getResources().getString(R.string.URL_antennas);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        if(!response.isEmpty()){
                            try {
                                ArrayList<Antenna> tmp_antColl = new ArrayList<>();
                                JSONArray jArray = new JSONArray(response);
                                for (int i=0; i < jArray.length(); i++)
                                {
                                    JSONObject tmpObj = jArray.getJSONObject(i);
                                    String ID = tmpObj.getString("AntennaID");
                                    String extID = tmpObj.getString("Company_site_name");
                                    String region = tmpObj.getString("Region");
                                    String province = tmpObj.getString("Province");
                                    String Address = tmpObj.getString("Site_locate_Address");
                                    Double klat = tmpObj.getDouble("Lat");
                                    Double klong = tmpObj.getDouble("Long");
                                    //Create Antenna and add to Collection
                                    Antenna tmp_ant = new Antenna(klat, klong, ID, Address, extID, region, province);
                                    tmp_antColl.add(tmp_ant);
                                }
                                addAntennasToCollection(tmp_antColl);
                            }catch (JSONException ex){
                                ex.printStackTrace();
                            }
                        }else{
                            Toast.makeText(getBaseContext(), "Network Problem! No Antenna Data", Toast.LENGTH_LONG).show();
                        }
                        progressDialog.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(stringRequest);
    }
    //FILL ANTENNASPINNER
    public void fillAntennaSpinner() {

        //ArrayList<Marker> MarkerList = new ArrayList<Marker>(mClusterManager.getMarkerCollection().getMarkers());
        ArrayList<String> IDstrings = new ArrayList<String>();
        IDstrings.add("");
        for (Antenna a: mAntennaCollection) {
            IDstrings.add(a.getTitle());
        }
        Spinner spinner_antenna = findViewById(R.id.spinner_antennas);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,IDstrings);
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);

        spinner_antenna.setAdapter(adapter);

        //Antenna selected
        spinner_antenna.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
                TextView txt_search = findViewById(R.id.txt_search);
                txt_search.setText(parent.getItemAtPosition(pos).toString());
                try {
                    displayAntenna(mAntennaCollection.get(pos-1));
                }catch(Exception ex){}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){
                //Another interface callback
            }
        });
    }
    //PARSE CSV TO ANTENNA LISTS
    private void parseCSV(ArrayList<String> strlst){
        ArrayList<String> tmp = strlst;
        String csvDelimiter = ";";
        //ArrayList<String> tmp = FileHelper.ReadFile(this, filepath);
        ArrayList<Antenna> AntennaCollection = new ArrayList<>();
        tmp.remove(0);
        for (String line : tmp) {
            String[] lineArr = line.split(csvDelimiter);
            try {
                String ID = lineArr[0];
                String extID = lineArr[1];
                String Address = lineArr[3] + ", " + lineArr[4] + ", " + lineArr[5];
                String klat = lineArr[7].replace(',', '.');
                String klong = lineArr[6].replace(',', '.');

                if (!klat.equalsIgnoreCase("#NV") && !klong.equalsIgnoreCase("#NV")) {
                    //Create Antenna and add to Collection
                    Antenna tmp_ant = new Antenna(Double.parseDouble(klat), Double.parseDouble(klong), ID, Address, extID);

                    AntennaCollection.add(tmp_ant);
                    //change clustered Icons: https://stackoverflow.com/questions/36522305/android-cluster-manager-icon-depending-on-type
                }
            }catch(Exception e){
                System.out.println(">>>>>>Error @ Parsing");
            }
        }
        mAntennaCollection = AntennaCollection;
        mClusterManager.addItems(mAntennaCollection);
        fillAntennaSpinner();
    }
    public void getDirectionsTo(View view) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+selectedAntenna.getPosition().latitude+","+selectedAntenna.getPosition().longitude));
        startActivity(intent);
    }
    public void startObjectActivity(View view){
        Intent intent = new Intent(MainActivity.this, ObjectActivity.class);

        intent.putExtra("obj", 0);
        intent.putExtra("Antenna",selectedAntenna);
        intent.putExtra("user",user);
        startActivity(intent);
    }
    //CREATING EMPTY IMG FILE
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    //KEYBOARD HIDE
    public void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    public void buildPreview(){
        if(!statusResponse.trim().isEmpty()){
            //remove
            LinearLayout prevList = findViewById(R.id.vLayStatusPreview);
            prevList.removeAllViews();
            //BUILD THE PREVIEW
            try {
                JSONArray jstatArray = new JSONArray(statusResponse);
                JSONArray jpicArray = new JSONArray(pictureResponse);
                int countObj = jstatArray.length();
                for (int i=0; i < countObj; i++)
                {
                    //get all informations out of server responses
                    JSONObject jstatObj = jstatArray.getJSONObject(i);
                    JSONObject jpicObj = jpicArray.getJSONObject(i);
                    int status = jstatObj.getInt("status");
                    String module_id = jstatObj.getString("module_id");
                    String fullpath = getResources().getString(R.string.server_url) + jpicObj.getString("path");

                    if(status != 0) {
                        //BUILD UP LAYOUT---------
                        RelativeLayout layPrev = new RelativeLayout(this);
                        //Pic
                        ImageView pic = new ImageView(this);
                        pic.setId(View.generateViewId());
                        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(DPtoPX(80), DPtoPX(80));
                        rlp.setMarginStart(DPtoPX(10));
                        rlp.topMargin = DPtoPX(10);
                        rlp.setMarginEnd(DPtoPX(16));
                        pic.setLayoutParams(rlp);
                        pic.setImageResource(R.drawable.ic_dummy1);
                        Picasso.with(pic.getContext()).load(fullpath).into(pic);
                        layPrev.addView(pic);

                        //name-label
                        TextView mname = new TextView(this);
                        mname.setText(module_id);
                        mname.setId(View.generateViewId());
                        RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        rlp2.addRule(RelativeLayout.RIGHT_OF, pic.getId());
                        mname.setLayoutParams(rlp2);
                        layPrev.addView(mname);
                        //status-label
                        TextView txtStat = new TextView(this);
                        txtStat.setId(View.generateViewId());
                        txtStat.setTextSize(24);
                        txtStat.setAllCaps(true);
                        RelativeLayout.LayoutParams rlp3 = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        rlp3.addRule(RelativeLayout.RIGHT_OF, pic.getId());
                        rlp3.addRule(RelativeLayout.BELOW, mname.getId());
                        txtStat.setLayoutParams(rlp3);
                        if (status == 1) {
                            txtStat.setText("Checkup needed!");
                            txtStat.setTextColor(Color.YELLOW);
                        }
                        if (status == 2) {
                            txtStat.setText("Service required!");
                            txtStat.setTextColor(Color.RED);
                        }
                        layPrev.addView(txtStat);

                        LinearLayout linlay = findViewById(R.id.vLayStatusPreview);
                        linlay.addView(layPrev);
                    }
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }else{
            //WAIT 2s AND RETRY
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(++counter > 3) {
                        buildPreview();
                    }else{
                        LinearLayout linL = findViewById(R.id.vLayStatusPreview);
                        linL.removeAllViews();
                        TextView txt = new TextView(getApplicationContext());
                        txt.setText("No Data");
                        txt.setTextSize(35);
                        txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        linL.addView(txt);
                    }

                }
            }, 500);  //the time is in miliseconds
        }
    }
    private int DPtoPX(int dp){
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics());
        return px;
    }

    private void grabStatuses() {
        String get_url = getApplication().getResources().getString(R.string.statusScript)
                +"?antID=" + selectedAntenna.getTitle();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        if(!response.trim().isEmpty()) {
                            statusResponse = response.trim();
                            /*
                            try {
                                JSONArray jArray = new JSONArray(response);
                                int countObj = jArray.length();
                                for (int i=0; i < countObj; i++)
                                {
                                    JSONObject tmpObj = jArray.getJSONObject(i);
                                    int status = tmpObj.getInt("status");
                                    String module_id = tmpObj.getString("module_id");
                                    //fillandColorize status text
                                    //drawStatusText(module_id,status);
                                }
                            }catch (JSONException ex){
                                ex.printStackTrace();
                            }
                             */
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(stringRequest);
    }

    //CUSTOM_MARKER_ICON
    public class CustomClusterRenderer extends DefaultClusterRenderer<Antenna> {

        private final Context mContext;

        public CustomClusterRenderer(Context context, GoogleMap map,
                                     ClusterManager<Antenna> clusterManager) {
            super(context, map, clusterManager);

            mContext = context;
        }

        @Override protected void onBeforeClusterItemRendered(Antenna item,
                                                             MarkerOptions markerOptions) {
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon)).snippet(item.getTitle());
        }
    }


}