package com.mahc.custombottomsheet;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //CONSTANTS

    private static final int DEFAULT_ZOOM = 12;
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int CREATE_TICKET_REQUEST = 69;
    private static final int EDIT_TICKET_REQUEST = 96;
    static final int REQUEST_IMAGE_CAPTURE = 88;
    private int maxDistance;
    private GoogleMap mMap;
    private View mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private Location mLocation;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private SharedPreferences sharedPreferences;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> mAntennaCollection = new ArrayList<>();
    private ArrayList<String> antennaWithTickets = new ArrayList<>();
    private String mUser, titlePicUrl;
    private Antenna selectedAntenna = null;
    TextView bottomSheetTextView;
    View bottomSheet;
    BottomSheetBehavior behavior;
    ProgressDialog progressDialog;
    ProgressBar progressMaps;
    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.registerReceiver(this.mConnReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        //Logger
        Timber.plant(new DebugTree());
        //Get the Username from Login activity###MAYBE USE SHARED PREFRENCES?
        Intent suc = super.getIntent();
        mUser = suc.getStringExtra("User");
        sharedPreferences = getSharedPreferences("antennas", Context.MODE_PRIVATE);
        RequestQueue queue = RequestQueueSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        progressMaps = findViewById(R.id.progressMaps);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5*1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setFastestInterval(500);
        locationRequest.setSmallestDisplacement(10); //10 meters
        maxDistance = getResources().getInteger(R.integer.fence_radius);
        getAndParseAntennas();
        //setting up some Views
        setupBottomSheet();
        setupSearchBox();

        //Permission stuff
        requestLocationPermission();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        mapView = mapFragment.getView();
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
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
        mMap.setMinZoomPreference(6f);
        mMap.setMyLocationEnabled(true);
        mMap.setLatLngBoundsForCameraTarget(new LatLngBounds(new LatLng(8.486658, 102.931800),new LatLng(23.650701, 109.592596)));
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

            layoutParams.addRule(RelativeLayout.BELOW, findViewById(R.id.Searchbar).getId());
            layoutParams.setMargins(0, findViewById(R.id.Searchbar).getHeight(), 0, 0);
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

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mLocation = location;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),DEFAULT_ZOOM));
                        }else{
                            // Show Vietnam
                            LatLng vietnam = new LatLng(16, 106.5);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnam,6f));
                        }
                    }
                });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_TICKET_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra("result");
                Snackbar.make(findViewById(R.id.mapsCoordLayout), "SUCCESS!"+result, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }else if (resultCode == RESULT_FIRST_USER) {
                String result = data.getStringExtra("result");
                Snackbar.make(findViewById(R.id.mapsCoordLayout), "OFFLINE!"+result, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }else if(requestCode == EDIT_TICKET_REQUEST){
            if (resultCode == RESULT_OK) {
                //-1\0\1 - error\nothing edited\success edit
                int result = data.getIntExtra("result",0);
                if(result == -1){
                    Snackbar.make(findViewById(R.id.mapsCoordLayout), "ERROR uploading Changes!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }else if(result == 1){
                    Snackbar.make(findViewById(R.id.mapsCoordLayout), "Ticket edited!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
                int closed = data.getIntExtra("closed", 0);
                if(closed == 1){
                    Snackbar.make(findViewById(R.id.mapsCoordLayout), "Ticket closed!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }else if(closed == -1){
                    Snackbar.make(findViewById(R.id.mapsCoordLayout), "Ticket not closed!", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            }
        }else if(requestCode == REQUEST_IMAGE_CAPTURE){
            try {
                Snackbar.make(findViewById(R.id.mapsCoordLayout), "Sending Picture...", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Action", null).show();
                final JSONObject json = new JSONObject();
                //Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);



                String url = getResources().getString(R.string.antennaTitleImageScript);
                final StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>()
                        {
                            @Override
                            public void onResponse(String response) {
                                Snackbar.make(findViewById(R.id.mapsCoordLayout), "Sending Success", Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                                final ImageView titlePic = findViewById(R.id.bottom_sheet_pic);
                                Picasso.get().load(response).fit().into(titlePic);
                            }
                        },
                        new Response.ErrorListener()
                        {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Snackbar.make(findViewById(R.id.mapsCoordLayout), "Sending Failed", Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams()
                    {
                        Map<String, String>  params = new HashMap<String, String>();
                        params.put("data", json.toString());

                        return params;
                    }
                };
                Picasso.get().load(photoUri).resize(1000, 1000).centerInside().into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        String b64 = ImageHandler.getBASE64(bitmap);
                        try {
                            json.put("antenna_id", selectedAntenna.getTitle());
                            json.put("imgdata64", b64);
                            RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(postRequest);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    }
                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //PERMISSION STUFF
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestCameraPermission(){
        boolean granted = false;
        while(!granted) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            } else {
                granted = true;
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    logout(mapView);
                }
            }
        }
    }
    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }
    public void requestStoragePermission() {
        boolean granted = false;
        while (!granted) {
            String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this, "Please grant the r-storage permission", REQUEST_STORAGE_PERMISSION, perms);
            }else{
                granted = true;
            }
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
    private void setupSearchBox(){
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
    private void setupBottomSheet(){
        CoordinatorLayout coordinatorLayout = findViewById(R.id.mapsCoordLayout);
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

        final ImageView titlePic = findViewById(R.id.bottom_sheet_pic);
        titlePic.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                createPicture();

                return true;
            }
        });
        titlePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                try {
                    List<String> PATH = new ArrayList<>();
                    PATH.add(titlePicUrl);
                    StfalconImageViewer.Builder stf = new StfalconImageViewer.Builder<>(getApplicationContext(), PATH, new ImageLoader<String>() {
                        @Override
                        public void loadImage(ImageView imageView, String image) {
                            Picasso.get().load(image).into(imageView);
                        }

                    });
                    stf.show();
               /*             //(this, Arrays.asList(titlePicUrl)), { v, image -> Picasso.get().load(image).into(view)};
                Picasso.get().load(titlePicUrl).into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                final ArrayList<Bitmap> tmp = new ArrayList<>();
                                tmp.add(bitmap);
                                new StfalconImageViewer.Builder<>(getBaseContext(), tmp, new ImageLoader<Bitmap>() {
                                    @Override
                                    public void loadImage(ImageView imageView, Bitmap image) {
                                        imageView.setImageBitmap(image);
                                    }
                                }).show();
                            }
                            @Override
                            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                            }
                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {
                            }
                        });*/
                }catch (Exception ex){
                    ex.printStackTrace();
                    //Toast.makeText(getBaseContext(),"Error while opening.",Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    private void createPicture(){
        ImageHandler imageHandler = new ImageHandler(this);
        photoUri = imageHandler.dispatchTakePictureIntent();
    }
    private void grabAllAntennaData(String antenna_id){
        String url = getResources().getString(R.string.getAntennaDataScript)
                +"?antenna_id=" + antenna_id;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        buildPreview(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
        if(RequestQueueSingleton.getInstance(getApplicationContext()).checkForConnection()) {
            RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(jsonObjectRequest);
        }else{
            if(sharedPreferences.contains("antenna_json")) {
                Toast.makeText(MainActivity.this,"No Connection: Showing local copy",Toast.LENGTH_LONG).show();
                new ParsingTask().execute(sharedPreferences.getString("antenna_json", ""));
            }else{//try it atleast
                RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(jsonObjectRequest);
            }
        }
    }
    private void getAntennaTitleImage(){
        String url = getResources().getString(R.string.antennaTitleImageScript)
                +"?antenna_id=" + selectedAntenna.getTitle();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(!response.isEmpty()) {
                            ImageView titlePic = findViewById(R.id.bottom_sheet_pic);
                            titlePicUrl = response;
                            Picasso.get().load(response).fit().into(titlePic);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(stringRequest);
    }
    //DISPLAY ANTENNA-DATA ON BOTTOMSHEET
    private void displayAntenna(Antenna item){
        grabAllAntennaData(item.getTitle());
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
        NestedScrollView bottomsheet = findViewById(R.id.bottom_sheet);
        bottomsheet.smoothScrollTo(0,0);

        ImageView Pic = findViewById(R.id.bottom_sheet_pic);
        TextView Title = findViewById(R.id.bottom_sheet_title);
        TextView Address = findViewById(R.id.bottom_sheet_address);
        TextView extTitle = findViewById(R.id.bottom_sheet_ext_title);
        Pic.setImageDrawable(roundedPic);
        extTitle.setText(item.getExtTitle());
        Title.setText(item.getTitle());
        Address.setText(item.getAddress());

        getAntennaTitleImage();
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mLocation = location;
                            displayDistance(location);
                        }
                    }
                });
    }
    private void monitorLocationChange(){
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && selectedAntenna != null) {
                    for (Location location : locationResult.getLocations()) {
                        mLocation = location;
                        displayDistance(location);
                    }
                }
            }
        };
    }
    private void displayDistance(Location mylocation){
        TextView txtDistance = findViewById(R.id.txtDistance);
        Button btnTicket = findViewById(R.id.btnCreateTicket);
        float[] results = new float[1];
        Location.distanceBetween(mylocation.getLatitude(),
                mylocation.getLongitude(),
                selectedAntenna.getPosition().latitude,
                selectedAntenna.getPosition().longitude,
                results);
        if(results[0] < 5000) {
            txtDistance.setText(String.format(Locale.US,"%.2f m", results[0]));
        }else{
            txtDistance.setText(getResources().getString(R.string.maxDistance));
        }
        if(results[0] < maxDistance){
            //activate Button && paint dinstance green
            btnTicket.setEnabled(true);
            txtDistance.setTextColor(Color.GREEN);
        }else{
            btnTicket.setEnabled(false);
            txtDistance.setTextColor(getColor(R.color.transWhite));
        }
    }
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
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
        //progressDialog = ProgressDialog.show(MainActivity.this,"Download Antennas","Please Wait",false,false);
        progressMaps.setVisibility(View.VISIBLE);
        String get_url = getResources().getString(R.string.getAntennaScript);
        if(sharedPreferences.contains("antenna_json")) {
            try {
                String chk = new JSONObject(sharedPreferences.getString("antenna_json", "")).getString("Checksum");
                get_url = getResources().getString(R.string.getAntennaScript)
                        + "?checksum=" + chk;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        if(!response.isEmpty()){
                            if(response.length()> 20 && response.startsWith("{")) {//json?
                                new ParsingTask().execute(response);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("antenna_json", response);
                                editor.apply();
                            }else{//checksum?
                                new ParsingTask().execute(sharedPreferences.getString("antenna_json", ""));
                            }
                        }else{
                            Toast.makeText(getBaseContext(), "Network Problem! No Antenna Data", Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(sharedPreferences.contains("antenna_json")) {
                    Toast.makeText(MainActivity.this,"NetworkCall Error: Showing local copy",Toast.LENGTH_LONG).show();
                    new ParsingTask().execute(sharedPreferences.getString("antenna_json", ""));
                }
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
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        progressMaps.setVisibility(View.INVISIBLE);
    }
    //NAVIGATION INTENT
    public void getDirectionsTo(View view) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+selectedAntenna.getPosition().latitude+","+selectedAntenna.getPosition().longitude));
        startActivity(intent);
    }
    //START TICKET INTENT
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startObjectActivity(View view, JSONObject ticketData){
        Intent intent = new Intent(MainActivity.this, ViewTicketActivity.class);

        intent.putExtra("ticketData", ticketData.toString());

        startActivityForResult(intent, EDIT_TICKET_REQUEST);

        requestCameraPermission();
    }
    //KEYBOARD HIDE
    public void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    //SHOW TICKET PREVIEW
    public void buildPreview(JSONObject AntennaData){
        //remove
        LinearLayout linlay = findViewById(R.id.vLayStatusPreview);
        linlay.removeAllViews();
        //BUILD THE PREVIEW
        try {
            int countObj = AntennaData.getJSONArray("ticket").length();
            for (int i=0; i < countObj; i++)
            {
                //get all informations out of server responses
                final JSONObject jTicketObj = AntennaData.getJSONArray("ticket").getJSONObject(i);

                String type = jTicketObj.getString("ticket_type");
                String module_id = jTicketObj.getString("module_id");
                String ticket_id = jTicketObj.getString("ticket_id");


                //BUILD UP LAYOUT---------
                final RelativeLayout layPrev = new RelativeLayout(this);
                int[] attrs = new int[]{R.attr.selectableItemBackground};
                TypedArray typedArray = this.obtainStyledAttributes(attrs);
                int backgroundResource = typedArray.getResourceId(0, 0);
                layPrev.setBackgroundResource(backgroundResource);
                layPrev.setTag(ticket_id);
                //arrow
                ImageView arrow = new ImageView(this);
                RelativeLayout.LayoutParams rlp_arrow = new RelativeLayout.LayoutParams(DPtoPX(80), DPtoPX(80));
                arrow.setId(View.generateViewId());
                rlp_arrow.setMarginStart(DPtoPX(5));
                rlp_arrow.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                arrow.setImageResource(R.drawable.ic_arrow_right);
                arrow.setLayoutParams(rlp_arrow);
                arrow.setClickable(false);
                arrow.setFocusable(false);
                layPrev.addView(arrow);
                //Date
                TextView txtDate = new TextView(this);
                RelativeLayout.LayoutParams rlp_date = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                rlp_date.addRule(RelativeLayout.ALIGN_PARENT_END);
                rlp_date.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                //rlp_date.addRule(RelativeLayout.LEFT_OF, arrow.getId());
                txtDate.setText(jTicketObj.getString("open_time"));
                txtDate.setTextSize(8f);
                txtDate.setLayoutParams(rlp_date);
                layPrev.addView(txtDate);
                //Pic
                ImageView pic = new ImageView(this);
                pic.setId(View.generateViewId());
                pic.setFocusable(false);
                pic.setClickable(false);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(DPtoPX(80), DPtoPX(80));
                rlp.setMarginStart(DPtoPX(10));
                rlp.topMargin = DPtoPX(10);
                rlp.setMarginEnd(DPtoPX(16));
                pic.setLayoutParams(rlp);
                pic.setImageResource(R.drawable.ic_dummy1);
                //check for pics
                if( jTicketObj.getJSONArray("picture").length() > 0) {
                    String fullpath = getResources().getString(R.string.server_url)
                            + jTicketObj.getJSONArray("picture")
                            .getJSONObject(0).getString("path");
                    Picasso.get().load(fullpath).into(pic);
                }
                layPrev.addView(pic);
                //ticketID label
                TextView ticketLabel = new TextView(this);
                ticketLabel.setFocusable(false); ticketLabel.setClickable(false);
                ticketLabel.setText(ticket_id);
                ticketLabel.setTypeface(ticketLabel.getTypeface(), Typeface.BOLD);
                ticketLabel.setId(View.generateViewId());
                RelativeLayout.LayoutParams rlp_id = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                rlp_id.topMargin = DPtoPX(10);
                rlp_id.addRule(RelativeLayout.RIGHT_OF, pic.getId());
                ticketLabel.setLayoutParams(rlp_id);
                layPrev.addView(ticketLabel);
                //name-label
                TextView mname = new TextView(this);
                mname.setFocusable(false); mname.setClickable(false);
                mname.setText(module_id);
                mname.setId(View.generateViewId());
                RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                rlp2.addRule(RelativeLayout.RIGHT_OF, pic.getId());
                rlp2.addRule(RelativeLayout.BELOW, ticketLabel.getId());
                mname.setLayoutParams(rlp2);
                layPrev.addView(mname);
                //status-label
                TextView txtStat = new TextView(this);
                txtStat.setFocusable(false); txtStat.setClickable(false);
                txtStat.setId(View.generateViewId());
                txtStat.setTextSize(24);
                txtStat.setAllCaps(true);
                RelativeLayout.LayoutParams rlp3 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                rlp3.addRule(RelativeLayout.RIGHT_OF, pic.getId());
                rlp3.addRule(RelativeLayout.BELOW, mname.getId());
                txtStat.setLayoutParams(rlp3);

                String[] types = getResources().getStringArray(R.array.ticket_type_array);
                if(type.equals(types[0])){
                    txtStat.setText(types[0]);
                    txtStat.setTextColor(Color.RED);
                }else if(type.equals(types[1])){
                    txtStat.setText(types[1]);
                    txtStat.setTextColor(Color.HSVToColor(new float[]{ 16.65f, 63, 100f }));//ORANGE?
                }else if(type.equals(types[2])){
                    txtStat.setText(types[2]);
                    txtStat.setTextColor(Color.YELLOW);
                }else if(type.equals(types[3])){
                    txtStat.setText(types[3]);
                    txtStat.setTextColor(Color.GRAY);
                }

                layPrev.addView(txtStat);
                linlay.addView(layPrev);

                layPrev.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View view) {
                        //layPrev.setBackgroundColor(Color.LTGRAY);
                        startObjectActivity(layPrev, jTicketObj);
                        //layPrev.setBackgroundColor(Color.TRANSPARENT);
                    }
                });
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    //Convert DP input to Pixels
    private int DPtoPX(int dp){
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics());
        return px;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startCreateTicketActivity(View view) {
        Intent intent = new Intent(MainActivity.this, CreateTicket.class);
        intent.putExtra("antenna",selectedAntenna);

        startActivityForResult(intent,CREATE_TICKET_REQUEST);

        requestCameraPermission();
    }
    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    @Override
    protected void onResume() {
        super.onResume();
        monitorLocationChange();
        startLocationUpdates();
        if (selectedAntenna != null) {
            displayAntenna(selectedAntenna);
        }
    }
    private void superBack(){
        super.onBackPressed();
    }
    @Override
    public void onBackPressed() {
        if(RequestQueueSingleton.getInstance(getApplicationContext()).getCacheLength() > 0){
            showClosingDialog();
        }else{
            super.onBackPressed();
        }
    }
    private void showClosingDialog(){
        DialogInterface.OnClickListener closingTicketdialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        RequestQueueSingleton.getInstance(getApplicationContext()).addListToQueue();
                        superBack();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("There are Unsaved Tickets. You will lose them!").setPositiveButton("Yes", closingTicketdialogClickListener)
                .setNegativeButton("No", closingTicketdialogClickListener).show();
    }
    private void sendLocationRequest(){
        Snackbar.make(findViewById(R.id.mapsCoordLayout), "Sending new Location...", Snackbar.LENGTH_LONG)
                .setAction("SEND", null).show();
        final LatLng new_pos = mMap.getCameraPosition().target;
        final LatLng old_pos = selectedAntenna.getPosition();
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon_gray))
                .position(old_pos));
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon_green))
                .position(new_pos)
                .title(selectedAntenna.getExtTitle() + " - New Location")
        ).setSnippet("Needs approval from admins.");

        PolylineOptions line=
                new PolylineOptions().add(new_pos,old_pos)
                        .width(5).color(Color.GREEN);

        mMap.addPolyline(line);

        String url = getResources().getString(R.string.requestNewLocationScript);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        Snackbar.make(findViewById(R.id.mapsCoordLayout), "Finished sending!", Snackbar.LENGTH_SHORT)
                                .setAction("SEND", null).show();
                        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error.Response", error.getMessage());
                        Toast.makeText(getApplicationContext(), "Network Error @ Sending request", Toast.LENGTH_SHORT).show();
                        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("new_latlng", new_pos.latitude + "/" + new_pos.longitude);
                params.put("old_latlng", old_pos.latitude + "/" + old_pos.longitude);
                params.put("antenna_id", selectedAntenna.toString());
                params.put("user_id", mUser);
                return params;
            }
        };
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(postRequest);
    }
    public void changeAntennaPosition(View view) {
        final ImageView newPin = findViewById(R.id.newPin);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        Snackbar.make(findViewById(R.id.mapsCoordLayout), "Choose a new Location for this Antenna!", Snackbar.LENGTH_INDEFINITE)
                .setAction("SET", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        newPin.setVisibility(View.GONE);

                        sendLocationRequest();
                    }
                }).show();

        newPin.setVisibility(View.VISIBLE);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedAntenna.getPosition(),14));


    }
    public void makeAllAntennasEditable(View view) {
        if(mUser.equals("admin")) {
            maxDistance = Integer.MAX_VALUE;
            displayAntenna(selectedAntenna);
        }
    }

    public void logout(View view) {
        sharedPreferences.edit().clear().apply();
        getSharedPreferences("login", Context.MODE_PRIVATE).edit().clear().apply();
        finishAffinity();
        startActivity(new Intent(this, LoginActivity.class));
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
            if(antennaWithTickets.contains(item.getTitle())) {
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon_r)).snippet(item.getTitle());
            }else{
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon)).snippet(item.getTitle());
            }
        }
    }

    private final class ParsingTask extends AsyncTask<String, Void, ArrayList<Antenna>> {

        @Override
        protected ArrayList<Antenna> doInBackground(String... params) {
            try {
                JSONObject responseJSON = new JSONObject(params[0]);
                //save antennas with tickets
                JSONArray antTick = responseJSON.getJSONArray("WithTickets");
                for(int i = 0; i<antTick.length();i++){
                    antennaWithTickets.add(antTick.getString(i));
                }
                //extract Antennas
                ArrayList<Antenna> tmp_antColl = new ArrayList<>();
                JSONArray jArray = responseJSON.getJSONArray("Antennas");
                for (int i = 0; i < jArray.length(); i++) { //start with 1 cause 0 is checksum
                    JSONObject tmpObj = jArray.getJSONObject(i);
                    String ID = tmpObj.getString("antenna_id");
                    String extID = tmpObj.getString("company_site_name");
                    String region = tmpObj.getString("region");
                    String province = tmpObj.getString("province");
                    String Address = tmpObj.getString("site_locate_address");
                    Double klat = tmpObj.getDouble("lat");
                    Double klong = tmpObj.getDouble("lng");
                    //Create Antenna and add to Collection
                    Antenna tmp_ant = new Antenna(klat, klong, ID, Address, extID, region, province);
                    tmp_antColl.add(tmp_ant);
                }
                return tmp_antColl;
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            return null;
        }
        //OnProgress for a parsing progressbar possible

        @Override
        protected void onPostExecute(ArrayList<Antenna> antennas) {
            super.onPostExecute(antennas);
            addAntennasToCollection(antennas);
            //progressDialog.dismiss();
        }
    }

    public BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);

            NetworkInfo currentNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo otherNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            if(currentNetworkInfo.isConnected()){
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                if(RequestQueueSingleton.getInstance(getApplicationContext()).getCacheLength() > 0) {
                    RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue().start();
                    RequestQueueSingleton.getInstance(getApplicationContext()).addListToQueue();
                    if (selectedAntenna != null) {
                        displayAntenna(selectedAntenna);
                    }
                    Snackbar.make(findViewById(R.id.mapsCoordLayout), "Sending offline data...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }else{
                Toast.makeText(getApplicationContext(), "Not Connected", Toast.LENGTH_SHORT).show();
                RequestQueueSingleton.getInstance(getApplicationContext()).getRequestQueue().stop();
            }
        }
    };
}