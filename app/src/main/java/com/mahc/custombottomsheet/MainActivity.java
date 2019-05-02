package com.mahc.custombottomsheet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.mahc.custombottomsheetbehavior.BottomSheetBehaviorGoogleMapsLike;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private ClusterManager<Antenna> mClusterManager;
    private ArrayList<Antenna> AntennaCollection = new ArrayList<>();
    TextView bottomSheetTextView;
    View bottomSheet;
    BottomSheetBehaviorGoogleMapsLike behavior;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Download Antennas
        DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
        downloadFilesTask.execute();

        requestCameraPermission();


        //If we want to listen for states callback

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorlayout);
        bottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehaviorGoogleMapsLike.from(bottomSheet);
        behavior.addBottomSheetCallback(new BottomSheetBehaviorGoogleMapsLike.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED:
                        Log.d("bottomsheet-", "STATE_COLLAPSED");
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_DRAGGING:
                        Log.d("bottomsheet-", "STATE_DRAGGING");
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_EXPANDED:
                        Log.d("bottomsheet-", "STATE_EXPANDED");
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT:
                        Log.d("bottomsheet-", "STATE_ANCHOR_POINT");
                        break;
                    case BottomSheetBehaviorGoogleMapsLike.STATE_HIDDEN:
                        Log.d("bottomsheet-", "STATE_HIDDEN");
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
                behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_COLLAPSED);
            }
        });

        final CustomClusterRenderer renderer = new CustomClusterRenderer(this, mMap, mClusterManager);

        mClusterManager.setRenderer(renderer);

        //MARKER LISTENER
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<Antenna>() {
            @Override
            public boolean onClusterItemClick(Antenna item) {
                behavior.setState(BottomSheetBehaviorGoogleMapsLike.STATE_ANCHOR_POINT);
                displayAntenna(item);
                return true;
            }
        });

        // Show Vietnam
        LatLng vietnam = new LatLng(16, 106.5);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vietnam,5.5f));
    }

    public static class Antenna implements ClusterItem {
        private final LatLng mPosition;
        private final String mTitle;
        private final String mSnippet;

        public Antenna(double lat, double lng) {
            mPosition = new LatLng(lat, lng);
            mTitle = null;
            mSnippet = null;
        }

        public Antenna(double lat, double lng, String title, String snippet) {
            mPosition = new LatLng(lat, lng);
            mTitle = title;
            mSnippet = snippet;
        }
        @Override
        public LatLng getPosition() {
            return mPosition;
        }
        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getSnippet() {
            return mSnippet;
        }
    }

    //DISPLAY ANTENNA-DATA ON BOTTOMSHEET
    private void displayAntenna(Antenna item){
        TextView Title = (TextView)findViewById(R.id.bottom_sheet_title);
        TextView Address = (TextView)findViewById(R.id.bottom_sheet_subtitle);

        Title.setText(item.mTitle);
        Address.setText(item.mSnippet);

    }
    public void parsePins(ArrayList<String> tmp) {
        String csvDelimiter = ";";
        //ArrayList<String> tmp = FileHelper.ReadFile(this, filepath);

        tmp.remove(0);
        for (String line : tmp) {
            String[] lineArr = line.split(csvDelimiter);
            try {
                String ID = lineArr[0];
                String Address = lineArr[3] + ", " + lineArr[4] + ", " + lineArr[5];
                String klat = lineArr[7].replace(',', '.');
                String klong = lineArr[6].replace(',', '.');

                if (!klat.equalsIgnoreCase("#NV") && !klong.equalsIgnoreCase("#NV")) {
                    //Create Antenna and add to Collection
                    Antenna tmp_ant = new Antenna(Double.parseDouble(klat), Double.parseDouble(klong), ID, Address);

                    AntennaCollection.add(tmp_ant);
                    mClusterManager.addItem(tmp_ant);
                    //change clustered Icons: https://stackoverflow.com/questions/36522305/android-cluster-manager-icon-depending-on-type
                }
            }catch(Exception e){
                System.out.println(">>>>>>Error @ Parsing");
            }
        }
        ArrayList<Marker> MarkerList = new ArrayList<Marker>(mClusterManager.getMarkerCollection().getMarkers());
    }

    public void requestCameraPermission(){
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    MY_CAMERA_REQUEST_CODE);
        }
    }
    //START CAMERA
    public void dispatchTakePictureIntent(View view) {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(imageIntent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(imageIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    //CAMERA RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView imageView = (ImageView) findViewById(R.id.cam_pic);
            imageView.setImageBitmap(imageBitmap);
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
            }
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
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_antenna_icon)).snippet(item.mTitle);
        }
    }
}