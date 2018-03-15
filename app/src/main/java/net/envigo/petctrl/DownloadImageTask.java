package net.envigo.petctrl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import java.io.InputStream;

/**
 * Created by diego on 14/03/18.
 *
 * @ https://stackoverflow.com/questions/5776851/load-image-from-url
 */

class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private DownloadImageTask() {}

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Log", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    /*
    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }
    */
}