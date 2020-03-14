package com.mahc.custombottomsheet;

import androidx.collection.SimpleArrayMap;

public class Ticket {
    private String ticket_id,module_id,open_time,user_id,type,comment;
    SimpleArrayMap<String, String> picture;

    Ticket(String ID, String module, String time, String user, String type, String comment, String[] pic_path, String[] pic_time){
        ticket_id = ID;
        module_id = module;
        open_time = time;
        user_id = user;
        this.type = type;
        this.comment = comment;
        picture = new SimpleArrayMap<>();

        for (int i = 0; i<pic_path.length;i++)
            picture.put(pic_path[i], pic_time[i]);
    }

}
