package com.mahc.custombottomsheet;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageHandler {
    static final int REQUEST_IMAGE_CAPTURE = 88;
    String currentPhotoPath;
    Context context;

    ImageHandler(Context ctx){
        context = ctx;
    }

    static void deleteFile(Uri uri){
        File fdelete = new File(uri.getPath());
        if (fdelete.exists()) {
            fdelete.delete();
        }
    }
//doesnt work yet, but most efficient in theory
    static Bitmap getDownsizedBitmap(Uri uri, int size){
        /*
        final Bitmap[] bmp = {null};
        Picasso.get().load(uri).resize(size, size).centerInside().into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                bmp[0] = bitmap;
            }
            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
        return bmp[0];

        BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
        mBitmapOptions.inSampleSize = inSampleSize;

        return BitmapFactory.decodeFile(uri.getEncodedPath(),mBitmapOptions);

         */
        return null;
    }
    static String getBASE64(Bitmap bmp){
        ByteArrayOutputStream byteArrayOutputStreamObject = new ByteArrayOutputStream();
        byte[] b_arr = null;
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStreamObject);
            b_arr = byteArrayOutputStreamObject.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Base64.encodeToString(b_arr, Base64.DEFAULT);
    }

    Uri dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(context.getApplicationContext(),
                        context.getPackageName()+".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                ((Activity)context).startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                return photoURI;
            }
            return null;
        }
        return null;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public Bitmap getBitmapFromUri(Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

}
