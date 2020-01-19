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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PageViewModel extends AndroidViewModel {

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private String antenna = "";
    private String module = "";
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
            grabComments();
        }
        return mComment;
    }
    private MutableLiveData<String> mStatus;
    LiveData<String> getStatus() {
        if (mStatus == null) {
            mStatus = new MutableLiveData<>();
            grabStatus();
        }
        return mStatus;
    }

    public void setAntennaModule(String antenna, String module) {
        this.antenna = antenna;
        this.module = module;
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
    private void grabComments() {
        String get_url = getApplication().getResources().getString(R.string.comment_script)
                +"?antID=" + antenna
                + "&module=" + module
                +"&action=get";
        //http://gastroconsultung-catering.com/comment.php?antID=10AGG1001&module=Modul69&action=get
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        mComment.setValue(response
                                .replaceAll(getApplication().getResources().getString(R.string.new_line_placeholder),"\n")  //New lines(\n) dissappear in DB
                                .replaceAll(getApplication().getResources().getString(R.string.cross_placeholder),"#"));    //Hashtags also cause problems
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(stringRequest);
    }

    private void grabStatus() {
        String get_url = getApplication().getResources().getString(R.string.statusScript)
                +"?antID=" + antenna;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, get_url,
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray jArray = new JSONArray(response);

                            for (int i=0; i < jArray.length(); i++)
                            {
                                JSONObject tmpObj = jArray.getJSONObject(i);
                                if(tmpObj.getString("module_id").contains(module)){
                                    int status = tmpObj.getInt("status");
                                    mStatus.setValue(Integer.toString(status));
                                }
                            }
                        }catch (JSONException ex){
                            ex.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        // Add the request to the RequestQueue.
        RequestQueueSingleton.getInstance(getApplication()).addToRequestQueue(stringRequest);

    }

}