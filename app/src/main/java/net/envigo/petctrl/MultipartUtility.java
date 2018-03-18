package net.envigo.petctrl;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @ https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
 * @ https://gist.github.com/Antarix/a36faeaff3092b1fd977#file-usage-java-L16
 */

/*
NOTE : put this code below in non-ui-thread to get response.

String charset = "UTF-8";
String requestURL = "YOUR_URL";

MultipartUtility multipart = new MultipartUtility(requestURL, charset);
multipart.addFormField("param_name_1", "param_value");
multipart.addFormField("param_name_2", "param_value");
multipart.addFormField("param_name_3", "param_value");
multipart.addFilePart("file_param_1", new File(file_path));
String response = multipart.finish(); // response from server.

 */

@SuppressWarnings({"unused", "SameParameterValue"})
public class MultipartUtility {

    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;

    MultipartUtility(String requestURL, String charset)
            throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";

        URL url = new URL(requestURL);
        //Log.e("URL", "URL : " + requestURL.toString());
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
        httpConn.setRequestProperty("Test", "Bonjour");
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    public void addFormField(String name, String value) {
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"")
                .append(name).append("\"").append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=")
                .append(charset).append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(LINE_FEED)
                .append("Content-Disposition: form-data; name=\"")
                .append(fieldName).append("\"; filename=\"")
                .append(fileName).append("\"").append(LINE_FEED)
                .append("Content-Type: ")
                .append(URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED)
                .append("Content-Transfer-Encoding: binary").append(LINE_FEED);
                //.append(LINE_FEED); //this add a fucking space to file (esp8266+arduino)
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[512]; // new byte[4096];
        int bytesRead;// = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
    }

    public void addHeaderField(String name, String value) {
        writer.append(name).append(": ").append(value).append(LINE_FEED);
        writer.flush();
    }

    String finish() throws IOException {
        String response;

        writer.append(LINE_FEED).flush();
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {

            BufferedInputStream in = new BufferedInputStream(httpConn.getInputStream());
            response = inputStreamToString(in);

            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }

    private static String inputStreamToString(InputStream in) {
        String result = "";
        if (in == null) {
            return result;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            result = out.toString();
            reader.close();

            return result;
        } catch (Exception e) {
            Log.e("InputStream", "Error : " + e.toString());
            return result;
        }
    }
}