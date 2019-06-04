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
import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.mahc.custombottomsheetbehavior.BottomSheetBehaviorGoogleMapsLike;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //CONSTANTS
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int DEFAULT_ZOOM = 12;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private GoogleMap mMap;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> AntennaCollection = new ArrayList<>();
    private String user;
    private Antenna selectedAntenna = null;
    TextView bottomSheetTextView;
    View bottomSheet;
    BottomSheetBehaviorGoogleMapsLike behavior;
    ProgressDialog progressDialog ;

    //IMG SERVER STUFF
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    boolean check = true;
    private String currentPhotoPath;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the Username from Login activity
        Intent suc = super.getIntent();
        user = suc.getStringExtra("User");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Download Antennas
        DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
        downloadFilesTask.execute();

        //Request Permissions
        requestCameraPermission();
        requestLocationPermission();

        //BottomSheet Callbacks
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorlayout);
        bottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehaviorGoogleMapsLike.from(bottomSheet);
        final RelativeLayout searchbar = coordinatorLayout.findViewById(R.id.Searchbar);
        behavior.addBottomSheetCallback(new BottomSheetBehaviorGoogleMapsLike.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED:
                        Log.d("bottomsheet-", "STATE_COLLAPSED");
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_DRAGGING:
                        Log.d("bottomsheet-", "STATE_DRAGGING");
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_EXPANDED:
                        Log.d("bottomsheet-", "STATE_EXPANDED");
                        searchbar.setVisibility(View.GONE);
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT:
                        Log.d("bottomsheet-", "STATE_ANCHOR_POINT");
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN:
                        Log.d("bottomsheet-", "STATE_HIDDEN");
                        searchbar.setVisibility(View.VISIBLE);
                        break;
                    default:
                        Log.d("bottomsheet-", "STATE_SETTLING");
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        bottomSheetTextView = (TextView) bottomSheet.findViewById(R.id.bottom_sheet_title);
        //ItemPagerAdapter adapter = new ItemPagerAdapter(this,mDrawables);
        //ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        //viewPager.setAdapter(adapter);

        behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
        //behavior.setCollapsible(false);

        final TextView searchTextView = (TextView)findViewById(R.id.txt_search);

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
/*
    private void createSpinner() {

            Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
            Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
            Spinner spinner3 = (Spinner) findViewById(R.id.spinner3);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.status_array, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner1.setAdapter(adapter);
            spinner2.setAdapter(adapter);
            spinner3.setAdapter(adapter);
    }
*/
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


    private boolean searchMarker(String searchtext){
        for (Antenna ant : AntennaCollection) {
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
        behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(item.getPosition().latitude - 0.02,item.getPosition().longitude))
                .zoom(DEFAULT_ZOOM).build();
        //Zoom in and animate the camera.
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));


        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_antenne);
        RoundedBitmapDrawable roundedPic = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        final float roundPx = (float) bitmap.getWidth() * 0.06f;
        roundedPic.setCornerRadius(roundPx);

        ImageView Pic = (ImageView)findViewById(R.id.bottom_sheet_pic);
        TextView Title = (TextView)findViewById(R.id.bottom_sheet_title);
        TextView Address = (TextView)findViewById(R.id.bottom_sheet_address);
        TextView extTitle = (TextView)findViewById(R.id.bottom_sheet_ext_title);

        ImageView obj1_pic = (ImageView)findViewById(R.id.obj1_pic);
        ImageView obj2_pic = (ImageView)findViewById(R.id.obj2_pic);
        ImageView obj3_pic = (ImageView)findViewById(R.id.obj3_pic);
        obj1_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_dummy1));
        obj2_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_dummy2));
        obj3_pic.setImageDrawable(getResources().getDrawable(R.drawable.ic_dummy3));

        Pic.setImageDrawable(roundedPic);
        extTitle.setText(item.getExtTitle());
        Title.setText(item.getTitle());
        Address.setText(item.getAddress());

    }
    public void parsePins(ArrayList<String> tmp) {
        String csvDelimiter = ";";
        //ArrayList<String> tmp = FileHelper.ReadFile(this, filepath);

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
                    mClusterManager.addItem(tmp_ant);
                    //change clustered Icons: https://stackoverflow.com/questions/36522305/android-cluster-manager-icon-depending-on-type
                }
            }catch(Exception e){
                System.out.println(">>>>>>Error @ Parsing");
            }
        }
        //ArrayList<Marker> MarkerList = new ArrayList<Marker>(mClusterManager.getMarkerCollection().getMarkers());
        ArrayList<String> IDstrings = new ArrayList<String>();
        IDstrings.add("");
        for (Antenna a: AntennaCollection) {
            IDstrings.add(a.getTitle());
        }
        Spinner spinner_antenna = (Spinner)findViewById(R.id.spinner_antennas);
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
                    displayAntenna(AntennaCollection.get(pos-1));
                }catch(Exception ex){}
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){
                //Another interface callback
            }

        });

    }

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
        if(EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }

    public void showList(View view) {

    }

    //START CAMERA
    public void dispatchTakePictureIntent(View view) {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
/*
        if (imageIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                try {
                    Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName(), photoFile);
                    imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(imageIntent, REQUEST_IMAGE_CAPTURE);
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }

      */
        if(imageIntent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(imageIntent, REQUEST_IMAGE_CAPTURE);
        }

    }

    //CAMERA RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
            ImageView imageView = (ImageView) findViewById(R.id.cam_pic);
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);

            try {
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStreamObject);
                byte[] byteArrayVar = byteArrayOutputStreamObject.toByteArray();
                uploadImgByteArray(byteArrayVar);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
/*
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
            Uri uri = data.getData();
            //show Thumbnail
            ImageView imageView = (ImageView) findViewById(R.id.cam_pic);
            imageView.setImageBitmap((Bitmap)data.getExtras().get("data"));

            try {
                // Adding captured image in bitmap.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStreamObject);
                byte[] byteArrayVar = byteArrayOutputStreamObject.toByteArray();
                uploadImgByteArray(byteArrayVar);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        */
    }

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

    private void uploadImgByteArray(byte[] b_arr){
        final String ConvertImage = Base64.encodeToString(b_arr, Base64.DEFAULT);

        class AsyncTaskUploadClass extends AsyncTask<Void,Void,String> {

            @Override
            protected void onPreExecute() {

                super.onPreExecute();

                // Showing progress dialog at image upload time.
                progressDialog = ProgressDialog.show(MainActivity.this,"Image is Uploading","Please Wait",false,false);
            }

            @Override
            protected void onPostExecute(String string1) {

                super.onPostExecute(string1);

                // Dismiss the progress dialog after done uploading.
                progressDialog.dismiss();

                // Printing uploading success message coming from server on android app.
                Toast.makeText(MainActivity.this,string1,Toast.LENGTH_LONG).show();
            }

            @Override
            protected String doInBackground(Void... params) {

                ImageProcessClass imageProcessClass = new ImageProcessClass();

                HashMap<String,String> HashMapParams = new HashMap<String,String>();

                HashMapParams.put(ImageNameFieldOnServer, selectedAntenna.getTitle()
                                                            + "_" + "Modul1" //ADD MODULE NAME
                                                            + "_" + user
                                                            + "_" + new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date()));

                HashMapParams.put(ImagePathFieldOnServer, ConvertImage);

                String FinalData = imageProcessClass.ImageHttpRequest(getString(R.string.upload_script), HashMapParams);

                return FinalData;
            }
        }


        AsyncTaskUploadClass AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();
        AsyncTaskUploadClassOBJ.execute();
    }

    public class ImageProcessClass{

        public String ImageHttpRequest(String requestURL,HashMap<String, String> PData) {

            StringBuilder stringBuilder = new StringBuilder();

            try {

                URL url;
                HttpURLConnection httpURLConnectionObject ;
                OutputStream OutPutStream;
                BufferedWriter bufferedWriterObject ;
                BufferedReader bufferedReaderObject ;
                int RC ;

                url = new URL(requestURL);

                httpURLConnectionObject = (HttpURLConnection) url.openConnection();

                httpURLConnectionObject.setReadTimeout(19000);

                httpURLConnectionObject.setConnectTimeout(19000);

                httpURLConnectionObject.setRequestMethod("POST");

                httpURLConnectionObject.setDoInput(true);

                httpURLConnectionObject.setDoOutput(true);

                OutPutStream = httpURLConnectionObject.getOutputStream();

                bufferedWriterObject = new BufferedWriter(

                        new OutputStreamWriter(OutPutStream, "UTF-8"));

                bufferedWriterObject.write(bufferedWriterDataFN(PData));

                bufferedWriterObject.flush();

                bufferedWriterObject.close();

                OutPutStream.close();

                RC = httpURLConnectionObject.getResponseCode();

                if (RC == HttpsURLConnection.HTTP_OK) {

                    bufferedReaderObject = new BufferedReader(new InputStreamReader(httpURLConnectionObject.getInputStream()));

                    stringBuilder = new StringBuilder();

                    String RC2;

                    while ((RC2 = bufferedReaderObject.readLine()) != null){

                        stringBuilder.append(RC2);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return stringBuilder.toString();
        }

        private String bufferedWriterDataFN(HashMap<String, String> HashMapParams) throws UnsupportedEncodingException {

            StringBuilder stringBuilderObject;

            stringBuilderObject = new StringBuilder();

            for (Map.Entry<String, String> KEY : HashMapParams.entrySet()) {

                if (check)

                    check = false;
                else
                    stringBuilderObject.append("&");

                stringBuilderObject.append(URLEncoder.encode(KEY.getKey(), "UTF-8"));

                stringBuilderObject.append("=");

                stringBuilderObject.append(URLEncoder.encode(KEY.getValue(), "UTF-8"));
            }

            return stringBuilderObject.toString();
        }

    }
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


    //DOWNLOAD ANTENNA-DATA
    private ArrayList<String> downloadAntennasCSV(){
        URL mUrl = null;
        ArrayList<String> content = new ArrayList<>();
        try {
            mUrl = new URL(getString(R.string.URL_antennas));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            assert mUrl != null;
            URLConnection connection = mUrl.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "";
            while((line = br.readLine()) != null){
                content.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }



    private class DownloadFilesTask extends AsyncTask<URL, Void, ArrayList<String>> {
        protected ArrayList<String> doInBackground(URL... urls) {
            return downloadAntennasCSV();
        }
        protected void onPostExecute(ArrayList<String> result) {
            if(!result.isEmpty()){
                parsePins(result);
            }else{
                Toast.makeText(getBaseContext(), "Network Problem! No Antenna Data", Toast.LENGTH_LONG).show();
            }
        }
    }

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