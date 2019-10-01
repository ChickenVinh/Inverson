package com.mahc.custombottomsheet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.squareup.picasso.Picasso;

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
    private Context context = this;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> mAntennaCollection = new ArrayList<>();
    private String user;
    private Antenna selectedAntenna = null;
    private String obj;
    private int objnr;
    TextView bottomSheetTextView;
    View bottomSheet;
    BottomSheetBehavior behavior;
    ProgressDialog progressDialog ;

    //IMG SERVER STUFF
    String ServerURL = "http://gastroconsultung-catering.com/getData.php";
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    boolean check = true;
    private String currentPhotoPath;
    private int page;

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

        //Download Antennas
        downloadCSV();

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

        ImageView Pic = findViewById(R.id.bottom_sheet_pic);
        TextView Title = findViewById(R.id.bottom_sheet_title);
        TextView Address = findViewById(R.id.bottom_sheet_address);
        TextView extTitle = findViewById(R.id.bottom_sheet_ext_title);
        final Spinner spin1 = findViewById(R.id.spinner1);
        spin1.setEnabled(false);
        spin1.setSelection(0);
        final Spinner spin2 = findViewById(R.id.spinner2);
        spin2.setEnabled(false);
        spin2.setSelection(0);
        final Spinner spin3 = findViewById(R.id.spinner3);
        spin3.setEnabled(false);
        spin3.setSelection(0);

        final ImageButton obj1_pic = findViewById(R.id.obj1_pic);
        obj1_pic.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy1));
        final ImageButton obj2_pic = findViewById(R.id.obj2_pic);
        obj2_pic.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy1));
        final ImageButton obj3_pic = findViewById(R.id.obj3_pic);
        obj3_pic.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy1));


        String get_url = getResources().getString(R.string.server_url) + "getLatest.php?antenna_ID=\"" + item.getTitle() + "\"";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String[] res = response.split("\n");
                        if(!response.equals("")) {
                            for (String s : res) {
                                String module = s.split("###")[0];
                                String imgpath = getResources().getString(R.string.server_url) + s.split("###")[1];
                                if(!imgpath.equals(getResources().getString(R.string.server_url))) {
                                    if (module.equals(obj1_pic.getTag().toString()) & !imgpath.equals(getResources().getString(R.string.server_url))) {
                                        Picasso.with(obj1_pic.getContext()).load(imgpath).into(obj1_pic);
                                        spin1.setSelection(Integer.parseInt(s.split("###")[2]));
                                        spin1.setEnabled(true);
                                    }
                                    if (module.equals(obj2_pic.getTag().toString()) && !imgpath.equals(getResources().getString(R.string.server_url))) {
                                        Picasso.with(obj2_pic.getContext()).load(imgpath).into(obj2_pic);
                                        spin2.setSelection(Integer.parseInt(s.split("###")[2]));
                                        spin2.setEnabled(true);
                                    }
                                    if (module.equals(obj3_pic.getTag().toString()) && !imgpath.equals(getResources().getString(R.string.server_url))) {
                                        Picasso.with(obj3_pic.getContext()).load(imgpath).into(obj3_pic);
                                        spin3.setSelection(Integer.parseInt(s.split("###")[2]));
                                        spin3.setEnabled(true);
                                    }
                                }
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });

        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(stringRequest);

        spin1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
                httpGETupdate(selectedAntenna.getTitle(), spin1.getTag().toString(), user, Integer.toString(pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){
                //Another interface callback
            }

        });

        spin2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                httpGETupdate(selectedAntenna.getTitle(), spin2.getTag().toString(), user, Integer.toString(pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spin3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                httpGETupdate(selectedAntenna.getTitle(), spin3.getTag().toString(), user, Integer.toString(pos));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        Pic.setImageDrawable(roundedPic);
        extTitle.setText(item.getExtTitle());
        Title.setText(item.getTitle());
        Address.setText(item.getAddress());
    }
    //DOWNLOAD N PARSE ANTENNA DATA
    private void downloadCSV(){
        progressDialog = ProgressDialog.show(MainActivity.this,"Download Antennas","Please Wait",false,false);
        String get_url = getResources().getString(R.string.URL_antennas);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        String convtmp = "";
                        convtmp = new String(response.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        ArrayList<String>  lstAntennas = new ArrayList<String>(Arrays.asList(convtmp.split("\r\n")));
                        if(!lstAntennas.isEmpty()){
                            //parsePins(lstAntennas);
                            ParseTask pt = new ParseTask();
                            pt.execute(lstAntennas);
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
    //
    private void httpGETupdate(String antenna_ID, String modul, String user, String status){
        String get_url = getResources().getString(R.string.server_url) + "upload.php?status=\"" + status
                                                                        + "\"&antenna_ID=\"" + antenna_ID
                                                                        + "\"&module=\"" + modul
                                                                        + "\"&user=\"" + user
                                                                        + "\"";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
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
    private class ParseTask extends AsyncTask<ArrayList<String>,Void,ArrayList<Antenna>> {
        /*
        private WeakReference<MainActivity> activityReference;
        ParseTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }
         */
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected void onPostExecute(ArrayList<Antenna> antColl) {
            /*MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;*/
            mAntennaCollection = antColl;
            mClusterManager.addItems(antColl);
            fillAntennaSpinner();
        }

        protected ArrayList<Antenna> doInBackground(ArrayList<String>... params) {
            ArrayList<String> tmp = params[0];
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
            return AntennaCollection;
        }
    }
    public void getDirectionsTo(View view) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+selectedAntenna.getPosition().latitude+","+selectedAntenna.getPosition().longitude));
        startActivity(intent);
    }
    public void startObjectActivity(View view){
        Intent intent = new Intent(MainActivity.this, ObjectActivity.class);
        //Pass Object number to get to right tab
        switch (view.getId()) {
            case R.id.obj1_pic:
                page = 0;
                break;
            case R.id.obj2_pic:
                page = 1;
                break;
            case R.id.obj3_pic:
                page = 2;
                break;
        }
        intent.putExtra("obj", page);
        intent.putExtra("AntennaID",selectedAntenna.getTitle());
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
    //MOVE TO OWN LOCATION
    private void getAndMoveToDeviceLocation(){
        Log.d("", "getDeviceLocation: getting the devices current location");

        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful() && task.getResult() != null) {
                            Log.d("", "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM));
                        }else{
                            Log.d("", "current location is null");
                            Toast.makeText(getBaseContext(), "Location not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }else{
                requestLocationPermission();
            }
        }catch (SecurityException e){
            Log.e("", "getDeviceLocation: SecurityException: " +e.getMessage());
        }
    }
    //KEYBOARD HIDE
    public void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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