package com.mahc.custombottomsheet;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.mahc.custombottomsheet.ui.main.PageViewModel;
import com.mahc.custombottomsheet.ui.main.SectionsPagerAdapter;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ObjectActivity extends AppCompatActivity {
    private int page;
    private String user;
    private Antenna selectedAntenna;
    private String[] obj;
    private int[] objnr;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    ProgressDialog progressDialog;
    String ServerURL = "http://gastroconsultung-catering.com/getData.php";
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    ImageView[] objImg = new ImageView[4];
    TabLayout tabs;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object);
        PageViewModel model = ViewModelProviders.of(this).get(PageViewModel.class);
        RequestQueue queue = RequestQueueSingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = findViewById(R.id.fab);
        obj = new String[]{getResources().getString(R.string.Object1), getResources().getString(R.string.Object2), getResources().getString(R.string.Object3)};
        objnr = new int[]{R.string.Object1, R.string.Object2, R.string.Object3};
        //select right Tab
        int defaultValue = 0;
        //GET INTENT EXTRAS
        page = getIntent().getIntExtra("obj", defaultValue);
        selectedAntenna = getIntent().getParcelableExtra("Antenna");
        user = getIntent().getStringExtra("user");

        //GET SELECTED TAB
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //ON TAB SWITCH
                page = tab.getPosition();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setCurrentItem(page);

/*
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

 */

    }
    //START CAMERA
    public void dispatchTakePictureIntent(View view) {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //THUMBNAIL
        if(imageIntent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(imageIntent, REQUEST_IMAGE_CAPTURE);
        }
/*
        //FULL SIZE PHOTO
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

    }
    //CAMERA RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ImageButton campic = findViewById(objnr[page]);
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            //campic.setImageBitmap(imageBitmap);
            uploadImgByteArray(imageBitmap);

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
    //UPLOAD IMG TO SERVER
    private void uploadImgByteArray(Bitmap bmp){
        ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
        byte[] b_arr = null;
        String url = getString(R.string.upload_script);
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStreamObject);
            b_arr = byteArrayOutputStreamObject.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final String ConvertImage = Base64.encodeToString(b_arr, Base64.DEFAULT);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        Toast.makeText(getBaseContext(), response, Toast.LENGTH_SHORT).show();
                        reloadTab();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.getMessage());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put(ImageNameFieldOnServer, selectedAntenna
                        + "_" + obj[page] //ADD MODULE NAME
                        + "_" + user);
                params.put(ImagePathFieldOnServer, ConvertImage);
                return params;
            }
        };
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(postRequest);
    }
    public String getAntennaID(){
        return selectedAntenna.getTitle();
    }
    private void reloadTab(){
        try {
            Thread.sleep(1000);
        }catch (Exception ex){}

        this.recreate();
    }

}