package com.mahc.custombottomsheet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ViewTicketActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 88;
    JSONObject ticketData,editedData = new JSONObject();
    JSONArray picture = new JSONArray();
    String ticket_id, user, type, module_id, comment;
    private final String ImageNameFieldOnServer = "image_name" ;
    private final String ImagePathFieldOnServer = "image_path" ;
    ArrayList<Bitmap> imgList;
    Uri imgUri;
    ImageHandler photoHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_ticket);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        imgList = new ArrayList<>();
/*
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeTicket(view);
            }
        });
*/
        parseBaseData();
        setTicketInfos();
    }
    private void parseBaseData(){
        //GET INTENT EXTRAS
        if(getIntent().hasExtra("ticketData")) {
            try {
                ticketData = new JSONObject(getIntent().getStringExtra("ticketData"));
                ticket_id = ticketData.getString("ticket_id");
                module_id = ticketData.getString("module_id");
                type = ticketData.getString("ticket_type");
                user = ticketData.getString("user_id");
                comment = ticketData.getString("comment");
                picture = ticketData.getJSONArray("picture");
                for(int i = 0;i<picture.length();i++) {
                    String path = getResources().getString(R.string.server_url)
                            + ticketData.getJSONArray("picture")
                            .getJSONObject(i).getString("path");
                    addImgToView(path);
                    Picasso.get().load(path).into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            imgList.add(0,bitmap);
                        }
                        @Override
                        public void onBitmapFailed(Exception e, Drawable errorDrawable) {}
                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {}
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
                //ticket_id = module_id = type = user = "NULL";
            }
        }
    }
    private void setTicketInfos(){
        TextView txtID = findViewById(R.id.txtViewTicketTitle);
        txtID.setText(ticket_id);
        TextView txtTickID = findViewById(R.id.txtViewTicketSubtitle);
        txtTickID.setText(module_id);
        TextView com = findViewById(R.id.txtCommentEdit);
        com.setText(comment);

    }
    public void closeTicket(View view) {
        DialogInterface.OnClickListener closingTicketdialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        String get_url = getResources().getString(R.string.close_ticket)
                                +"?ticket_id="   +ticket_id;
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                                new Response.Listener<String>() {
                                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                    @Override
                                    public void onResponse(final String response) {

                                        Intent returnIntent = new Intent();
                                        returnIntent.putExtra("closed", 1);
                                        setResult(Activity.RESULT_OK, returnIntent);
                                        finish();
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(getBaseContext(),"Ticket closing Failed:" + error.getMessage(),Toast.LENGTH_LONG).show();
                            }
                        });

                        // Add the request to the RequestQueue.
                        RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:

                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you really want to close this ticket?").setPositiveButton("Yes", closingTicketdialogClickListener)
                .setNegativeButton("No", closingTicketdialogClickListener).show();
    }
    public void dispatchTakePictureIntent(View view) {
        photoHandler = new ImageHandler(this);
        imgUri = photoHandler.dispatchTakePictureIntent();
    }
    private void addImgToView(String path){
        LinearLayout linearLayout = findViewById(R.id.viewTicketImgLayout);
        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams((int)this.getResources().getDisplayMetrics().density * 150,
                LinearLayout.LayoutParams.MATCH_PARENT));
        img.setId(imgList.size());
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openImageFull(imgList.toArray(new Bitmap[0]),v.getId());
            }
        });
        linearLayout.addView(img,1);
        Picasso.get().load(path).fit().into(img);
    }
    void getAndSaveBMP(){
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
            imgList.add(0,bitmap);
            if(!editedData.has("picture")){
                editedData.put("picture", new JSONArray());
            }
            //save to JSONObject then to Final JSON
            String img_data = ImageHandler.getBASE64(bitmap);
            String img_timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            JSONObject picJO = new JSONObject();
            picJO.put("img_path", img_data);
            picJO.put("timestamp", img_timestamp);
            //add it to the Big one
            editedData.getJSONArray("picture").put(picJO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void addUriToView(){
        LinearLayout linearLayout = findViewById(R.id.viewTicketImgLayout);
        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams((int)this.getResources().getDisplayMetrics().density * 150,
                LinearLayout.LayoutParams.MATCH_PARENT));
        img.setId(imgList.size());
        linearLayout.addView(img,1);
        img.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openImageFull(imgList.toArray(new Bitmap[0]),v.getId());
            }
        });
        Picasso.get().load(imgUri).fit().into(img);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if(imgUri != null) {
                getAndSaveBMP();
                addUriToView();
                ImageHandler.deleteFile(imgUri);
            }
        }
    }
    private void openImageFull(final Bitmap[] imgList, int index){
        try {
            new StfalconImageViewer.Builder<>(this, imgList, new ImageLoader<Bitmap>() {
                @Override
                public void loadImage(ImageView imageView, Bitmap image) {
                    imageView.setImageBitmap(image);
                }
            }).withStartPosition(imgList.length-(index+1)).show();
        }catch (Exception ex){
            ex.printStackTrace();
            Toast.makeText(getBaseContext(),"Error while opening.",Toast.LENGTH_LONG).show();
        }
    }
    private void sendTicketData() {
        TextView com = findViewById(R.id.txtCommentEdit);
        try {
            editedData.put("module_id", module_id);
            editedData.put("ticket_id", ticket_id);
            editedData.put("user", user);
            editedData.put("comment", com.getText().toString());
            editedData.put("edit", "1");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Snackbar.make(findViewById(R.id.coordinator_edit), "Sending Ticket...", Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null).show();

        String url = getResources().getString(R.string.parseTicketScript);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        if(!response.trim().isEmpty()) {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("result", 1);
                            setResult(Activity.RESULT_OK, returnIntent);
                            finish();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        VolleyLog.d("Error.Response", error.getMessage());
                        Snackbar.make(findViewById(R.id.coordinator_edit), "ERROR"+error.getMessage(), Snackbar.LENGTH_SHORT)
                                .setAction("Action", null).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<>();
                params.put("data",editedData.toString());
                return params;
            }
        };
        if(checkForConnection()){
            RequestQueueSingleton.getInstance(this.getApplicationContext()).addToRequestQueue(postRequest);
        }else{
            RequestQueueSingleton.getInstance(this.getApplicationContext()).addToCache(postRequest);
            Intent returnIntent = new Intent();
            returnIntent.putExtra("result","Send Ticket when online!");
            setResult(Activity.RESULT_FIRST_USER,returnIntent);
            finish();
        }
    }
    boolean checkForConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        //we are connected to a network
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;
    }
    public void onBackPressed(){
        TextView com = findViewById(R.id.txtCommentEdit);
        String newcom = com.getText().toString();
        if(!comment.trim().equals(newcom.trim()) || picture.length() != imgList.size()) {
            DialogInterface.OnClickListener closingTicketdialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            sendTicketData();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            finish();
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want to save the Changes?").setPositiveButton("Yes", closingTicketdialogClickListener)
                    .setNegativeButton("No", closingTicketdialogClickListener).show();
        }else{
            super.onBackPressed();
        }
    }
}


