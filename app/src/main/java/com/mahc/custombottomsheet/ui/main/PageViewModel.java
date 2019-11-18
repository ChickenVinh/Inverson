package com.mahc.custombottomsheet.ui.main;

import android.app.Application;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.mahc.custombottomsheet.R;
import com.mahc.custombottomsheet.RequestQueueSingleton;

public class PageViewModel extends AndroidViewModel {

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private String antenna = "";
    private String[] obj = {getApplication().getResources().getString(R.string.Object1),
                            getApplication().getResources().getString(R.string.Object2),
                            getApplication().getResources().getString(R.string.Object3)};
    private LiveData<String> mText = Transformations.map(mIndex, new Function<Integer, String>() {
        @Override
        public String apply(Integer input) {
            return obj[input-1];
        }
    });
    private MutableLiveData<String> mComment;

    LiveData<String> getComments() {
        if (mComment == null) {
            mComment = new MutableLiveData<>();
            grabComments(antenna);
        }
        return mComment;
    }

    public void setAntenna(String antenna) {
        this.antenna = antenna;
    }

    public PageViewModel(@NonNull Application application) {
        super(application);
    }

    public void setIndex(int index) {
        mIndex.setValue(index);
    }

    public LiveData<String> getText() {
        return mText;
    }

    //GET PATHES TO ALL PICTURES
    private void grabComments(String antenna) {
        String get_url = getApplication().getResources().getString(R.string.getComment_script)+"?antenna_ID=\"" + antenna + "\"&module=\"" + obj[mIndex.getValue()-1] + "\"";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        mComment.setValue(response.replaceAll("__NEWLINE__","\n"));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(stringRequest);
    }
}