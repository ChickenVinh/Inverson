package com.mahc.custombottomsheet;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //CONSTANTS

    private static final int DEFAULT_ZOOM = 12;
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int CREATE_TICKET_REQUEST = 69;
    private static final int EDIT_TICKET_REQUEST = 96;
    private GoogleMap mMap;
    private View mapView;
    private Context context = this;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> mAntennaCollection = new ArrayList<>();
    private String mUser;
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
        //Get the Username from Login activity###MAYBE USE SHARED PREFRENCES?
        Intent suc = super.getIntent();
        mUser = suc.getStringExtra("User");

        RequestQueue queue = RequestQueueSingleton.getInstance(this.getApplicationContext()).getRequestQueue();

        //Permission stuff
        requestLocationPermission();

        //Download Antennas
        //downloadCSV();
        getAndParseAntennas();
        //setting up some Views
        setupBottomSheet();
        setupSearchBox();

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
            layoutParams.addRule(RelativeLayout.BELOW, 0);
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
        requestCameraPermission();
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        boolean granted = false;
        while (!granted) {
            String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
            if (!EasyPermissions.hasPermissions(this, perms)) {
                EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
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
        RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(jsonObjectRequest);
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
    //NAVIGATION INTENT
    public void getDirectionsTo(View view) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+selectedAntenna.getPosition().latitude+","+selectedAntenna.getPosition().longitude));
        startActivity(intent);
    }
    //START TICKET INTENT
    public void startObjectActivity(View view, JSONObject ticketData){
        Intent intent = new Intent(MainActivity.this, ViewTicketActivity.class);

        intent.putExtra("ticketData", ticketData.toString());

        startActivityForResult(intent, EDIT_TICKET_REQUEST);
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
    public void startCreateTicketActivity(View view) {
        Intent intent = new Intent(MainActivity.this, CreateTicket.class);
        intent.putExtra("antenna",selectedAntenna);

        startActivityForResult(intent,CREATE_TICKET_REQUEST);
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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedAntenna != null) {
            displayAntenna(selectedAntenna);
        }
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