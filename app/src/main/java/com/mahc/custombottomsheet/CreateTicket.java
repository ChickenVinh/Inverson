package com.mahc.custombottomsheet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateTicket extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private final String ImageNameFieldOnServer = "image_name" ;
    private final String ImagePathFieldOnServer = "image_path" ;
    SharedPreferences sharedpreferences;
    Spinner spin_module, spin_type;
    TextView txt_comment;
    ArrayList<Bitmap> imgList;
    JSONObject ticketJSON;
    Antenna antenna;
    String mUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ticket);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generateTicketJSON(view);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView title = findViewById(R.id.txtViewTicketTitle);
        title.setText("Create Ticket");

        setupSpinnerListener();

        sharedpreferences = getSharedPreferences("login", Context.MODE_PRIVATE);

        mUser = sharedpreferences.getString("user", "");
        antenna = getIntent().getParcelableExtra("antenna");
        ticketJSON = new JSONObject();
        try {
            ticketJSON.put("picture",new JSONArray());
            ticketJSON.put("user", mUser);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        imgList = new ArrayList<>();
    }

    private void generateTicketJSON(View view){
        //Fill JSONObject
        try {
            ticketJSON.put("module_id",antenna.getTitle()+"_"+spin_module.getSelectedItem());
            ticketJSON.put("type",spin_type.getSelectedItem());
            ticketJSON.put("comment",txt_comment.getText().toString()
                    .replaceAll("\n",getResources().getString(R.string.new_line_placeholder))   //see grabComments() function why this
                    .replaceAll("#",getResources().getString(R.string.cross_placeholder)));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sendTicketData(ticketJSON);



    }
    private void setupSpinnerListener(){
        txt_comment = findViewById(R.id.txtCommentCreate);
        spin_module = findViewById(R.id.spinner_module);
        spin_module.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spin_type = findViewById(R.id.spinner_type);
        spin_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    private void addImgToView(Bitmap bmp){
        LinearLayout linearLayout = findViewById(R.id.create_tick_Layout);
        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams((int)this.getResources().getDisplayMetrics().density * 150,
                LinearLayout.LayoutParams.MATCH_PARENT));
        img.setId(imgList.size());
        img.setImageBitmap(bmp);
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openImageFull(imgList.toArray(new Bitmap[0]),v.getId());
            }
        });

        linearLayout.addView(img,1);
    }
    private void openImageFull(final Bitmap[] imgList, int index){
        try {
            new StfalconImageViewer.Builder<>(this, imgList, new ImageLoader<Bitmap>() {
                @Override
                public void loadImage(ImageView imageView, Bitmap image) {
                    imageView.setImageBitmap(image);
                }
            }).withStartPosition(index-1).show();
        }catch (Exception ex){
            ex.printStackTrace();
            Toast.makeText(getBaseContext(),"Error while opening.",Toast.LENGTH_LONG).show();
        }
    }
    private String BMPtoBASE64_BYTE(Bitmap bmp){
        ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
        byte[] b_arr = null;
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStreamObject);
            b_arr = byteArrayOutputStreamObject.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Base64.encodeToString(b_arr, Base64.DEFAULT);
    }
    public void dispatchTakePictureIntent(View view) {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //THUMBNAIL
        if(imageIntent.resolveActivity(this.getPackageManager())!=null){
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imgList.add(imageBitmap);
            addImgToView(imageBitmap);

            //save to JSONObject then to Final JSON
            String img_data = BMPtoBASE64_BYTE(imageBitmap);
            String img_timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            JSONObject picJO = new JSONObject();
            try {
                picJO.put("img_path", img_data);
                picJO.put("timestamp", img_timestamp);
                //add it to the Big one
                ticketJSON.getJSONArray("picture").put(picJO);
            } catch (JSONException e) {
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

    private void sendTicketData(JSONObject jTicket){
        Snackbar.make(findViewById(R.id.coordinator_create), "Sending Ticket...", Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null).show();

        String url = getResources().getString(R.string.parseTicketScript);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result",response);
                        setResult(Activity.RESULT_OK,returnIntent);
                        finish();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        VolleyLog.d("Error.Response", error.getMessage());
                        Snackbar.make(findViewById(R.id.coordinator_create), "ERROR"+error.getMessage(), Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("data",ticketJSON.toString());

                return params;
            }
        };
        RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(postRequest);
    }
}
