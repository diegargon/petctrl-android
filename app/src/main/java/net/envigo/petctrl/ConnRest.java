package net.envigo.petctrl;

import android.content.Context;
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
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by diego on 24/02/18.
 */


class ConnRest extends AsyncTask<HashMap<String, String>, JSONObject, JSONObject> {

    //private static final String domain_uri = "https://envigo.net/inzone/";
    private String server_uri = "https://zoners.envigo.net/";
    private String method;
    private String file_query;
    //private Context context;
    private WeakReference<Context> activityContext;

    public ConnRest(Context context) {
        activityContext = new WeakReference<>(context);
        // activityReference = new WeakReference<>(context);
        //method = mValue;
        //type_query = tValue;
      //  this.context = context;
    }

    public void setServerURL(String url) {
        this.server_uri = url;
    }
    public String getServerURL(String url) {
        return url;
    }

    public void setMethod(String method) {
        this.method = method;
    }
    public void setFileQuery(String query) {
        this.file_query = query;
    }

    @Override
    protected JSONObject doInBackground(HashMap<String, String> ... params) {
        HttpURLConnection conn = null;
        Log.d("Log", "doInBackground execute");
        try {

            URL url = setupURL();
            if (url == null) {
                return null;
            }

            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);

            switch (method) {
                case "POST":
                    conn.setRequestMethod("POST");
                    //break;
                case "GET":
                    conn.setRequestMethod("GET");
                    //break;
            }

            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.connect();
            Log.d("Log", ": url ->" + url.toString());

            String query = getPostDataString(params[0]);

            // mLog.d("ConnRest query", query);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            int status = conn.getResponseCode();
            Log.d("Log", "Conn status: "+ status );
            //TODO: manage 404 response
            InputStream inputStream = conn.getInputStream();

            if (inputStream == null) {
                Log.d("Log", "Connrest->input stream return null");
                conn.disconnect();
                return null;
            } else {
                Log.d("Log", "Connrest->input tiene algo");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            String result = "";

            while ((line = reader.readLine()) != null) {
                result += line;
            }

            inputStream.close();
            Log.d("Log", "ConnRest -> Info result" + result);
            conn.disconnect();
            if(result.isEmpty()) {
                return null;
            }
            JSONObject jsonResult = new JSONObject(result);
            //mLog.d("ConnRest Resultado", jsonResult.toString(4));

            return jsonResult;

        } catch (Exception e) {
            Log.d("Log: Excep ConnRest Bg", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    protected URL setupURL() {
        URL url = null;

        try {
            if (file_query.isEmpty()) {
                Log.d("Log: ", "Incorrect file_query");
                return null;
            }
            url = new URL(server_uri + file_query);

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
