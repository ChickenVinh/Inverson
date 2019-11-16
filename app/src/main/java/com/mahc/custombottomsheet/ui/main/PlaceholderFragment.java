package com.mahc.custombottomsheet.ui.main;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private LinearLayout linearLayout;
    private String[] obj;
    private int index = 1;

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
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_main, container, false);
        final TextView textView = root.findViewById(R.id.section_label);
        linearLayout = root.findViewById(R.id.imgLayout);
        pageViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        ObjectActivity parent = (ObjectActivity) getActivity();
        grabPictures(((ObjectActivity) getActivity()).getAntennaID());
        //Get the Photos taken here
        return root;
    }

    //GET PATHES TO ALL PICTURES
    private void grabPictures(String antenna) {
        String get_url = "http://gastroconsultung-catering.com/getPics.php?ant=\"" + antenna + "\"";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        parsePicResult(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getActivity().getApplicationContext()).addToRequestQueue(stringRequest);
    }

    private void parsePicResult(String response) {
        ArrayList<String> pathes = new ArrayList<>();

        for (String s : response.split("###")) {
            String objekt = obj[index-1];
            if (s.contains(objekt)) {
                pathes.add(s.split("#")[4]);
            }
        }

        for (int i = 0; i < pathes.size(); i++) {
            ImageView img = new ImageView(getActivity());
            img.setLayoutParams(new LinearLayout.LayoutParams((int)getContext().getResources().getDisplayMetrics().density * 150,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            //img.setImageResource(R.drawable.ic_ex_img);
            Picasso.with(img.getContext()).load(getResources().getString(R.string.server_url) + pathes.get(i)).into(img);
            linearLayout.addView(img);
        }
    }
}