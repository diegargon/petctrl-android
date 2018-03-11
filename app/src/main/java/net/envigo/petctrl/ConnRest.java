package net.envigo.petctrl;

import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by diego on 8/03/18.
 */

public class ConnRest extends AsyncTask<HashMap<String, String>, JSONObject, JSONObject> {

    private iConnResult<JSONObject> mCallBack;
    //private Context mContext;
    public Exception mException;

    //public ConnRest(Context context, iConnResult callback) {
    public ConnRest(iConnResult callback) {
        mCallBack = callback;
        //mContext = context;
    }

    @Override
    protected JSONObject doInBackground(HashMap<String, String> ... params) {
        Log.d("Log", "doInBackground execute");

        HttpURLConnection conn = null;
        HashMap<String, String> conn_details = params[0];

        String method = conn_details.get("method");
        String location = conn_details.get("url");

        URL url = setupURL(location);
        if (url == null || method == null) {
            Log.d("Log", "ConnRest Inbackground URL/Method null");
            return null;
        } else {
            Log.d("Log", ": url ->" + url.toString());
        }

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);

            conn.setRequestMethod(method);
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.connect();

            String query = getPostDataString(params[1]);

            Log.d("Log:", "ConnRest query" + query);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            int status = conn.getResponseCode();
            //TODO: manage better 404 response
            Log.d("Log", "Conn status: "+ status );
            if (status == 404) {
                conn.disconnect();
                return null;
            }

            InputStream inputStream = conn.getInputStream();

            if (inputStream == null) {
                Log.d("Log", "Connrest->input stream return null");
                conn.disconnect();
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            String result = "";

            while ((line = reader.readLine()) != null) result += line;

            inputStream.close();
            Log.d("Log", "ConnRest -> Info result" + result);
            conn.disconnect();

            if(result.isEmpty()) return null;

            //JSONObject jsonResult = new JSONObject(result);
            //Log.d("ConnRest Resultado", jsonResult.toString(4));
            //return jsonResult;
            return new JSONObject(result);

        } catch (Exception e) {
            //Log.d("Log: Excep ConnRest Bg", e.getMessage());
            //e.printStackTrace();
            mException = e;
        }


        return null;
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        //super.onPostExecute(jsonObject);
        if (mCallBack != null) {
            if (mException == null) {
                mCallBack.onSuccess(jsonObject);
            } else {
                mCallBack.onFailure(mException);
            }
        }
    }

    private URL setupURL(String location) {
        URL url;

        try {
            if (location.isEmpty()) {
                Log.d("Log: ", "Incorrect file_query");
                return null;
            }
            url = new URL(location);

        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
        return url;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        Log.d("Log", "CONNSEND: " + result.toString());
        return result.toString();
    }

}
