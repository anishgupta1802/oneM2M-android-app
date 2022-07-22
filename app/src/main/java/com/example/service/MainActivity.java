package com.example.service;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import  javax.net.ssl.TrustManager;

import javax.net.ssl.SSLSocketFactory;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.keymanager.DummyX509ExtendedKeyManager;
import nl.altindag.ssl.util.IOUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CertificatePinner;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class MainActivity extends AppCompatActivity implements SensorEventListener {

    TextView textView;
    Button button1;
    Button button2;
    SensorManager sensorManager;
    Sensor stepcounter;
    Sensor accelerometer;
    boolean iscounter;
    int stepcount =0;




    //this method connects using sslcontext after genereting keystore and make a trustmanager factory
    private SSLContext connect() {
        SSLContext sslContext = null;
        CertificateFactory certificateFactory = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        try {
//            AssetManager am = this
            InputStream caInput = getAssets().open("cert.crt");
//            .open("kym.cer");
            BufferedInputStream bis = new BufferedInputStream(caInput);
            Log.d("caInput", String.valueOf(bis));
            Certificate ca = null;
            try {
                ca = certificateFactory.generateCertificate(bis);
                Log.d("Certificate", String.valueOf(ca));
            } catch (CertificateException e) {
                e.printStackTrace();
            } finally {
                caInput.close();
                bis.close();
            }
            //Create a KeyStore containing trusted CAs
            String keyStoteType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoteType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            //Create a TrustManager that trusts tha CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            //Create an SSLContext that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            Log.d("debug", String.valueOf(sslContext));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return sslContext;
    }


    //gives string from inputstream
    public static String getStringFromInputStream(InputStream stream) throws IOException
    {
        int n = 0;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = new InputStreamReader(stream, "UTF8");
        StringWriter writer = new StringWriter();
        while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
        return writer.toString();
    }


    //methods for android sensors
    @Override
    protected void onPause() {
        super.onPause();
        if(sensorManager.getDefaultSensor((Sensor.TYPE_PRESSURE))!=null) {
            sensorManager.unregisterListener(this,stepcounter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(sensorManager.getDefaultSensor((Sensor.TYPE_PRESSURE))!=null) {
            sensorManager.registerListener(this,stepcounter,sensorManager.SENSOR_DELAY_NORMAL);
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button1=findViewById(R.id.button);
        button2=findViewById(R.id.button2);
        textView=findViewById(R.id.textView1);
        textView.setText("hey");
        Log.d(TAG, "onCreate: sinnerman ");

        OkHttpClient client = new OkHttpClient();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);




        try{
        SSLContext sslContext = connect();
        URL url = new URL("https://ccspdev2.m2mlab.cdot.in");
        HostnameVerifier hostnameVerifier = (hostname, session) -> {
            HostnameVerifier hv =
                    HttpsURLConnection.getDefaultHostnameVerifier();
            // return hv.verify("test1.ceir.gov.in:8443", session);
            return true;
        };
//        final String basicAuth = "Basic" + Base64.encodeToString("cms-
//                key:password".getBytes(), Base64.NO_WRAP);
        HttpsURLConnection httpsURLConnection = (HttpsURLConnection)
                url.openConnection();
        httpsURLConnection.setHostnameVerifier(hostnameVerifier);
        httpsURLConnection.setRequestMethod("GET");
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setUseCaches(false);
//        httpsURLConnection.setRequestProperty("Authorization", basicAuth);
//        Log.d("debug", basicAuth);
        httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        Log.d("debug", String.valueOf(httpsURLConnection));
        OutputStream outputStream = httpsURLConnection.getOutputStream();
        BufferedWriter bufferedWriter = new BufferedWriter(new
                OutputStreamWriter(outputStream, "UTF-8"));
        JSONObject post_data_json = new JSONObject();
//        post_data_json.put("IMEInumber", IMEInumber);
//        post_data_json.put("UDID", myIMEI);
        bufferedWriter.write(String.valueOf(post_data_json));
        bufferedWriter.flush();
        bufferedWriter.close();
        outputStream.close();
        }
        catch (Exception e){
            Log.d(TAG, "onCreate: 2222"+ e);
        }









        try {
//            CertificatePinner certificatePinner= new CertificatePinner.Builder()
//                    .add(
//                            "https://ccspdev2.m2mlab.cdot.in",
//                            "sha256/Gsbn13ldubNMUjUU4eCnpxNUTdbUurbklZXL1+s2rFk="
//                    ).build();
            client=new OkHttpClient.Builder().build();

//            Log.d(TAG, "aaaaaaaaaaaaaaaaaaaaaa ");

//            InputStream inputStream = Resources.getSystem().openRawResource(R.raw.cert);

//            KeyStore keyStore = null;
//            keyStore = KeyStore.getInstance("JKS");
//            keyStore.load(new FileInputStream("C:\\Users\\anish\\AndroidStudioProjects\\service\\app\\src\\main\\res\\raw\\pk.p12"), "cdot@123".toCharArray());

            KeyManagerFactory trustManagerFactory = KeyManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(null,"cdot@123".toCharArray());


//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init( trustManagerFactory.getKeyManagers(), null,null);

            Log.d(TAG, "onCreate: keystore made");


            KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
            caKs.load(null, null);
//            caKs.setCertificateEntry("ca-certificate", R.raw.ca);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caKs);

//            // client key and certificates are sent to server so it can authenticate us
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
//            ks.setCertificateEntry("certificate", cert);
//            ks.setKeyEntry("private-key", privateKey, password.toCharArray(), new java.security.cert.Certificate[]{cert});
//            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//            kmf.init(ks, password.toCharArray());

            // finally, create SSL socket factory
            SSLContext context = SSLContext.getInstance("TLSv1.2");
//            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            URL url = new URL("https://ccspdev2.m2mlab.cdot.in");
//            SSLSocketFactory factory = sslContext.getSocketFactory();
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
//            urlConnection.setSSLSocketFactory(factory);
//            urlConnection.setHostnameVerifier(new HostnameVerifier() {
//                @Override
//                public boolean verify(String hostname, SSLSession session) {
////                        return true;
//                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
//                    return hv.verify("your_domain.com", session);
//                }
//            });
            Log.d(TAG, "onCreate: satrting url ");
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(10000);
            urlConnection.connect();
            InputStream inputStream1 = urlConnection.getInputStream();
            String jsonr = getStringFromInputStream(inputStream1);
            textView.setText(jsonr);
            Log.d(TAG, "onCreate hi: "+jsonr);

        }
        catch (Exception e){
            Log.d(TAG, "onCreate exception: "+e.toString());
        }



//
//        OkHttpClient okHttpClient= new OkHttpClient();
//            KeyStore ksTrust = KeyStore.getInstance("BKS");
//            InputStream instream = getResources().openRawResource(R.raw.cert);
//            ksTrust.load(instream, "secret".toCharArray());
//
//            // TrustManager decides which certificate authorities to use.
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            tmf.init(ksTrust);
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, tmf.getTrustManagers(), null);
//
//            okHttpClient.new  .setSslSocketFactory(sslContext.getSocketFactory());
//
//        okHttpClient.
//        HandshakeCertificates certificates = new HandshakeCertificates.Builder()
//                .addTrustedCertificate(letsEncryptCertificateAuthority)
//                .addTrustedCertificate(entrustRootCertificateAuthority)
//                .addTrustedCertificate(comodoRsaCertificationAuthority)
//                // Uncomment if standard certificates are also required.
//                //.addPlatformTrustedCertificates()
//                .build();

        String get_url ="https://ccspdev2.m2mlab.cdot.in/D1";
        String post_url ="http://192.168.105.33:8080/save";

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Request request = new Request.Builder()
//                        .header("X-M2M-Origin","S0_testAE1")
//                        .header("X-M2M-RI","anish")
//                        .header("Accept","application/json")
//                        .url(get_url)
//                        .build();
//                client.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    @Override
//                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                        if(response.isSuccessful()){
//                            String myresponse=response.body().string();
//                            MainActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    textView.setText(myresponse);
//                                }
//                            });
//                        }
//                    }
//                });
            }
        });


        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                RequestBody formBody = new FormBody.Builder()
//                        .add("firstName","akshat")
//                        .add("lastName","goyal")
//                        .add("age","25")
//                        .add("occupation", "manager")
//                        .build();
//                Log.d(TAG, "onClick: "+ formBody.contentType());
//                Request request = new Request.Builder()
//                        .url(post_url)
//                        .post(formBody)
//                        .build();
//                Log.d(TAG, "onClick: "+request.toString());
//                client.newCall(request).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                        Log.d(TAG, "onFailure: "+e);
//                        e.printStackTrace();
//                    }
//
//                    @Override
//                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                        Log.d(TAG, "onResponse: HEY "+ response);
//                        if(response.isSuccessful()){
//                            String myresponse=response.body().string();
//                            Log.d(TAG, "onResponse: "+myresponse);
//                            MainActivity.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    textView.setText(myresponse);
//                                }
//                            });
//                        }
//                    }
//                });
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        textView=findViewById(R.id.textView1);
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(MainActivity.this,accelerometer,sensorManager.SENSOR_DELAY_NORMAL);

//
//        if(sensorManager.getDefaultSensor((Sensor.TYPE_STEP_COUNTER))!=null){
//            stepcounter=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
//            Log.d(TAG, "step counter registered");
//            iscounter=true;
//        }
//        else{
//            iscounter=false;
//            textView.setText("not present");
//        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d(TAG, "onSensorChanged: x: "+event.values[0]+" Y: "+ event.values[1]+" Z: "+event.values[2]);
//        Log.d(TAG, "onstepcounterChanged: "+ String.valueOf(stepcount));
        if(event.sensor.getType()==Sensor.TYPE_PRESSURE){
            stepcount=(int)event.values[0];
//            textView.setText("x: "+event.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}