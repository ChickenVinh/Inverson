package com.mahc.custombottomsheet;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
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

import org.json.JSONException;
import org.json.JSONObject;

public class ObjectActivity extends AppCompatActivity {
    private int page;
    private String user;
    private Antenna selectedAntenna;
    private String[] obj;
    private int[] objnr;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    String ticketID;
    ProgressDialog progressDialog;
    String ServerURL = "http://gastroconsultung-catering.com/getData.php";
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    ImageView[] objImg = new ImageView[4];
    TabLayout tabs;
    ViewPager viewPager;
    JSONObject ticketData;

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

        obj = getResources().getStringArray(R.array.module_array);
        objnr = new int[]{R.string.Object1, R.string.Object2, R.string.Object3};
        //select right Tab
        int defaultValue = 0;
        //GET INTENT EXTRAS
        if(getIntent().hasExtra("json")) {
            try {
                ticketData = new JSONObject(getIntent().getStringExtra("json"));
                ticketID = ticketData.getString("ticket_id");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        TextView txtID = findViewById(R.id.txtAntID);
        txtID.setText(getAntennaID());
        TextView txtTickID = findViewById(R.id.txtTickID);
        txtTickID.setText(ticketID);

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

    public String getAntennaID(){
        return selectedAntenna.getTitle();
    }
    public String getUser() {
        return user;
    }
    public String getTicketID(){return ticketID;}

    public void reloadTab(){
        try {
            Thread.sleep(1000);
        }catch (Exception ex){}
        Intent intent = new Intent(ObjectActivity.this, ObjectActivity.class);
        intent.putExtra("obj", page);
        intent.putExtra("Antenna",selectedAntenna);
        intent.putExtra("mUser",user);
        startActivity(intent);
        finish();
    }
    private void openTicket(){
        String get_url = getResources().getString(R.string.openclose_ticket)
                +"?ticketID="+getTicketID()
                +"&mUser="+getUser()
                +"&action="+"open"
                +"&antID="+getAntennaID();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(final String response) {
                        if(response.contains("AlreadyOpen")){//there is already a ticket on that antenna!
                            //EDIT TICKET DIALOG?
                            DialogInterface.OnClickListener editTicketdialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        case DialogInterface.BUTTON_POSITIVE:
                                            ticketID=response.split(":")[1];
                                            Toast.makeText(getBaseContext(),"Editing Ticket: " + ticketID,Toast.LENGTH_LONG).show();
                                            TextView txtTickID = findViewById(R.id.txtTickID);
                                            txtTickID.setText(ticketID);
                                            break;

                                        case DialogInterface.BUTTON_NEGATIVE:
                                            finish();
                                            break;
                                    }
                                }
                            };
                            AlertDialog.Builder builder = new AlertDialog.Builder(ObjectActivity.this);
                            builder.setMessage("Antenna already has a open ticket!\nWant to edit that?").setPositiveButton("Yes", editTicketdialogClickListener)
                                    .setNegativeButton("No", editTicketdialogClickListener).show();
                        }else{//create new Ticket
                            Toast.makeText(getBaseContext(),response,Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getBaseContext(),"Ticket generation Failed:" + error.getMessage(),Toast.LENGTH_LONG).show();
                finish();
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);
    }

    public void closeTicket(View view) {
        DialogInterface.OnClickListener closingTicketdialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        String get_url = getResources().getString(R.string.openclose_ticket)
                                +"?ticketID="   +getTicketID()
                                +"&mUser="       +getUser()
                                +"&action="     +"close";
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                                new Response.Listener<String>() {
                                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                    @Override
                                    public void onResponse(final String response) {
                                        Toast.makeText(getBaseContext(),response,Toast.LENGTH_LONG).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(ObjectActivity.this);
        builder.setMessage("Do you really want to close this ticket?").setPositiveButton("Yes", closingTicketdialogClickListener)
                .setNegativeButton("No", closingTicketdialogClickListener).show();



    }

    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }
}