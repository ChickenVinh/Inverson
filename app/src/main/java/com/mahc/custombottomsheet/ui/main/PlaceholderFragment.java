package com.mahc.custombottomsheet.ui.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mahc.custombottomsheet.ObjectActivity;
import com.mahc.custombottomsheet.R;
import com.mahc.custombottomsheet.RequestQueueSingleton;
import com.squareup.picasso.Picasso;
import com.stfalcon.imageviewer.StfalconImageViewer;
import com.stfalcon.imageviewer.loader.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    String ImageNameFieldOnServer = "image_name" ;
    String ImagePathFieldOnServer = "image_path" ;
    private ObjectActivity parent;
    private PageViewModel pageViewModel;
    private LinearLayout linearLayout;
    private TextView commentView;
    private Spinner spinStatus;
    private String[] obj;
    private int index = 1;
    private String antenna;
    private String module;
    ArrayList<String> imgPathes;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        obj = new String[]{getResources().getString(R.string.Object1), getResources().getString(R.string.Object2), getResources().getString(R.string.Object3)};
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        parent = (ObjectActivity) getActivity();
        antenna = parent.getAntennaID();
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        module = obj[index-1];
        pageViewModel.setIndex(index);
        pageViewModel.setAntennaModule(antenna,module);
        //Picasso.with(getContext()).setIndicatorsEnabled(true);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        final TextView textView = root.findViewById(R.id.section_label);
        linearLayout = root.findViewById(R.id.imgLayout);
        commentView = root.findViewById(R.id.commentView);
        Button btnEditSave = root.findViewById(R.id.btnEditSave);
        ImageButton btnTakePic = root.findViewById(R.id.btnTakePicture);
        spinStatus = root.findViewById(R.id.spinnerStatus);
        pageViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        btnEditSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editSendComment(view);
            }
        });
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent(view);
            }
        });
        setCommentObserver();
        setStatusObserver();
        imgPathes = new ArrayList<>();
        grabPictures(antenna);
        //Get the Photos taken here
        return root;
    }
    private void setCommentObserver(){
        pageViewModel.getComments().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s.isEmpty()){
                    commentView.setHint(R.string.no_comment);
                }else{
                    commentView.setText(s);
                }
            }
        });
    }
    private void setStatusObserver(){
        pageViewModel.getStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s.isEmpty()){
                    spinStatus.setSelection(0,false);
                }else{
                    int pos = Integer.parseInt(s.replaceAll(" ",""));
                    spinStatus.setSelection(pos,false);
                }
                spinStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        updateStatus(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
            }
        });
    }
    //GET PATHES TO ALL PICTURES
    private void grabPictures(String antenna) {
        String get_url = getResources().getString(R.string.picture_script) + "?action=get&antID=" + antenna;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseResultAndShowPics(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity(),"NetworkCall Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });

        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getContext()).addToRequestQueue(stringRequest);
    }
    private void parseResultAndShowPics(String response) {
        String[] pathes = response.trim().split("###");//Split the pathes
        if(!response.equals("")) {
            for (String path : pathes) {
                if(!path.isEmpty() && path.contains(obj[index-1])) {
                    String fullpath = getResources().getString(R.string.server_url) + path;
                    imgPathes.add(fullpath);
                }
            }
        }

        for (int i = 0; i < imgPathes.size(); i++) {
            ImageView img = new ImageView(getActivity());
            img.setLayoutParams(new LinearLayout.LayoutParams((int)getContext().getResources().getDisplayMetrics().density * 150,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            //img.setImageResource(R.drawable.ic_ex_img);
            Picasso.with(img.getContext()).load(imgPathes.get(i)).into(img);
            img.setId(i);
            img.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openImageFull(imgPathes.toArray(new String[0]),v.getId());
                }
            });
            linearLayout.addView(img);

        }
    }
    private void openImageFull(final String[] imgList, int index){
        new StfalconImageViewer.Builder<>(getContext(), imgList, new ImageLoader<String>() {

            @Override
            public void loadImage(ImageView imageView, String image) {
                Picasso.with(getContext()).load(image).into(imageView);
            }
        }).withStartPosition(index).show();
    }
    public void editSendComment(View v) {
        if(!commentView.isEnabled()){
            commentView.setEnabled(true);
            ((Button) v).setText("Save");
        }else{
            commentView.setEnabled(false);
            ((Button) v).setText("Edit");
            //upload Comment
            //http://gastroconsultung-catering.com/comment.php?antID=10AGG1001&module=Modul69&action=add&comment=I got a Feeling&ticketID=54321

            String url = getActivity().getResources().getString(R.string.comment_script);
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>()
                    {
                        @Override
                        public void onResponse(String response) {
                            Toast.makeText(getContext(),response,Toast.LENGTH_LONG).show();
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            Log.d("Error.Response", error.getMessage());
                            Toast.makeText(getContext(),"Network Error, comment not uploaded",Toast.LENGTH_LONG).show();
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams()
                {
                    Map<String, String>  params = new HashMap<String, String>();
                    params.put("antID",antenna);
                    params.put("action","add");
                    params.put("module",module);
                    params.put("ticketID",parent.getTicketID());
                    params.put("comment",commentView.getText().toString()
                                                    .replaceAll("\n",getResources().getString(R.string.new_line_placeholder))   //see grabComments() function why this
                                                    .replaceAll("#",getResources().getString(R.string.cross_placeholder)));
                    return params;
                }
            };
            RequestQueueSingleton.getInstance(getContext()).addToRequestQueue(postRequest);
            /*String get_url = getActivity().getResources().getString(R.string.comment_script)
                    +"?antID="      + antenna
                    + "&module="    + module
                    + "&comment="   + commentView.getText().toString()
                    .replaceAll("\n",getResources().getString(R.string.new_line_placeholder))   //see grabComments() function why this
                    .replaceAll("#",getResources().getString(R.string.cross_placeholder))
                    + "&action=add";

            StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                    new Response.Listener<String>() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void onResponse(String response) {
                            Toast.makeText(getContext(),response,Toast.LENGTH_LONG).show();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getContext(),"Network Error, comment not uploaded",Toast.LENGTH_LONG).show();
                }
            });
            // Add the request to the RequestQueue.
            RequestQueueSingleton.getInstance(getContext()).addToRequestQueue(stringRequest);
            */
        }
    }
    private void updateStatus(int status){
        String get_url = getResources().getString(R.string.statusScript)
                + "?status=" + status
                + "&antID=" + antenna
                + "&modName=" + module
                + "&user=" + parent.getUser();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(getContext(), response, Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getContext()).addToRequestQueue(stringRequest);
    }
    public void dispatchTakePictureIntent(View view) {
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //THUMBNAIL
        if(imageIntent.resolveActivity(getActivity().getPackageManager())!=null){
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
        String url = getString(R.string.picture_script);
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
                        Toast.makeText(getContext(), response, Toast.LENGTH_SHORT).show();
                        //parent.reloadTab();
                        reloadActivity();
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
                params.put(ImageNameFieldOnServer, antenna
                        + "_" + obj[index-1] //ADD MODULE NAME
                        + "_" + parent.getUser());
                params.put(ImagePathFieldOnServer, ConvertImage);
                params.put("ticketID", parent.getTicketID());
                params.put("action", "add");
                params.put("module", module);
                params.put("antID", antenna);
                return params;
            }
        };
        RequestQueueSingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(postRequest);
    }

    private void reloadActivity(){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false);
        }
        ft.detach(this).attach(this).commit();
    }
}