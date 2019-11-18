package com.mahc.custombottomsheet.ui.main;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
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

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private LinearLayout linearLayout;
    private TextView commentView;
    private String[] obj;
    private int index = 1;
    private String antenna;
    ArrayList<String> imgPathes = new ArrayList<>();

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
        antenna = ((ObjectActivity) getActivity()).getAntennaID();
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
        pageViewModel.setAntenna(antenna);
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
        btnEditSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editSendComment(view);
            }
        });
        pageViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        setCommentObserver();
        ObjectActivity parent = (ObjectActivity) getActivity();
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
    //GET PATHES TO ALL PICTURES
    private void grabPictures(String antenna) {
        String get_url = "http://gastroconsultung-catering.com/getPics.php?ant=\"" + antenna + "\"";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        parseResultAndShowPics(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(stringRequest);
    }

    private void parseResultAndShowPics(String response) {
        for (String s : response.split("###")) {
            String objekt = obj[index-1];
            if (s.contains(objekt)) {
                imgPathes.add(getResources().getString(R.string.server_url)+s.split("#")[4]);
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
            String get_url = getActivity().getResources().getString(R.string.upload_script)+"?antenna_ID=\""
                    + antenna
                    + "\"&module=\"" + obj[index-1]
                    + "\"&comment=\"" + commentView.getText().toString().replaceAll("\n","__NEWLINE__")
                    + "\"";

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
        }
    }

}