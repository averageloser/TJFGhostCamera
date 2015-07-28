package com.example.tj.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tj on 7/26/2015.
 */
public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,
        Camera.PictureCallback {

    private boolean surfaceReady;
    private Camera camera;
    private TextureView texture;
    private Bitmap boo;
    private Paint booPaint;
    private AtomicInteger count;
    private boolean saving;
    private SharedPreferences.Editor prefsEditor;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        texture = new TextureView(this);
        texture.setOnClickListener(cameraListener);

        texture.setSurfaceTextureListener(this);

        setContentView(texture);

        boo = BitmapFactory.decodeResource(getResources(), R.drawable.boo);

        booPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        booPaint.setAlpha(55);

        prefs = getPreferences(Context.MODE_PRIVATE);

        prefsEditor = prefs.edit();

        count = new AtomicInteger(prefs.getInt("count", 0)); // on first run this will be 0.
    }

    public void onPause() {
        super.onPause();
    Log.i("onpause", "called");
        camera.release();

        //save count to shared preferences.
        prefsEditor.putInt("count", count.get());
        prefsEditor.commit();
    }

    public void onResume() {
        super.onResume();
        if (camera == null && surfaceReady) {
            camera = Camera.open();
        }


    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i("surface", "ready");
        surfaceReady = true;

        //I can now instantiate the camera.
        camera = Camera.open();

        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);

        camera.enableShutterSound(true);
        camera.setParameters(params);
        try {
            camera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private View.OnClickListener cameraListener = new View.OnClickListener() {
        public void onClick(View view) {
            if (camera != null && !saving) {
                saving = true;

                camera.takePicture(null, null, CameraActivity.this);
            }
        }
    };

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        /*
        Can all of this happen before the activity is destroyed during an orientation change, or
        should this be put into a headless fragment with retained state?  Shoud this activity be
        a fragment that does all of this?
         */
        class PictureSaver extends AsyncTask<byte[], Void, Void> {
            @Override
            protected Void doInBackground(byte[]... params) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;

                Bitmap image = BitmapFactory.decodeByteArray(params[0], 0, params[0].length, options);

                Canvas canvas = new Canvas(image);

                canvas.drawBitmap(boo, 100, 100, booPaint);

                //Save the image to external file storage.
                File externalStorage = Environment.getExternalStorageDirectory();

                //This is not such a great way to generate a filename.  Append the date to the image, instead.
                File pictureSaved = new File(externalStorage.getPath() + "/" + "booimage" + count.getAndIncrement() + ".jpg");

                try {
                    //try to keep the file size small.
                    image.compress(Bitmap.CompressFormat.JPEG, 25, new BufferedOutputStream(new FileOutputStream(pictureSaved)));


                } catch (IOException e) {
                    Log.e("error saving image", e.getMessage());
                }

                //MediaStore.Images.Media.insertImage(getContentResolver(), image, "Boo", "ghost cam boo");

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                saving = false;

                Toast.makeText(CameraActivity.this, "Image Saved", Toast.LENGTH_LONG).show();
            }
        }

        new PictureSaver().execute(data);
    }




}
