package com.hrgirdattendanceonline;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

//import SecuGen.FDxSDKPro.JSGFPLib;
//import SecuGen.FDxSDKPro.SGFDxDeviceName;
//import SecuGen.FDxSDKPro.SGFDxErrorCode;

/**
 * Created by admin on 25-11-2016.
 */
public class LogInActivity extends AppCompatActivity
{
    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    public static final String MyPREFERENCES = "MyPrefs" ;
    SharedPreferences shared_pref;
    int PRIVATE_MODE = 0;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    ConnectionDetector cd;
    UserSessionManager session;
    CheckInternetConnection internetConnection;
    Toolbar toolbar;
    GPSTracker gps;
    ProgressDialog progressDialog;

    String Url, logo;
    String url_http;
    String myJSON = null;
    String UserName, Password;
    String Current_Location;
    String android_id;
    String Login_id;

    EditText ed_userName, ed_password;
    TextView txt_forgotPass;
    Button btn_signIn;
    LinearLayout signIn_layout, progress_layout;
    ImageView logo_login;
    LinearLayout poweredby_layout;

    Double latitude = 0.0, longitude = 0.0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner);
        TextView Header = (TextView)findViewById(R.id.header_text);
        ImageView img_logout = (ImageView)findViewById(R.id.img_logout);
        toolbar.setBackgroundColor(getResources().getColor(R.color.GreyBgColor));
        setSupportActionBar(toolbar);

        img_logout.setVisibility(View.GONE);

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Header.setText("");
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            //getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.abc_ic_ab_back_material));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        internetConnection = new CheckInternetConnection(getApplicationContext());

        Login_id = getIntent().getStringExtra("login_id");

        gps = new GPSTracker(getApplicationContext(), LogInActivity.this);

        latitude = gps.getLatitude();
        longitude = gps.getLongitude();
        Current_Location = gps.getlocation_Address();

        /*if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(LogInActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        else
        {
            gps = new GPSTracker(getApplicationContext(), LogInActivity.this);
            if (gps.canGetLocation())
            {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                Current_Location = gps.getlocation_Address();
                //Log.i("Current_Location",Current_Location);
            }
            else
            {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(LogInActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setMessage("Please Enable GPS");
                alertDialog.setCancelable(true);
                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                alertDialog.show();
            }
        }*/

        ed_userName = (EditText)findViewById(R.id.ed_userName);
        ed_password = (EditText)findViewById(R.id.ed_password);
        btn_signIn = (Button) findViewById(R.id.btn_signIn);
        signIn_layout = (LinearLayout)findViewById(R.id.signIn_layout);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout);
        txt_forgotPass = (TextView)findViewById(R.id.forgot_pass);
        poweredby_layout = (LinearLayout)findViewById(R.id.layout_poweredby_login);

        session = new UserSessionManager(getApplicationContext());
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));
        
        logo_login = (ImageView)findViewById(R.id.logo_login);
        Picasso.with(getApplicationContext()).load(logo).into(logo_login);

        txt_forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogInActivity.this, ForgotPassword.class);
                startActivity(intent);
                finish();
            }
        });

        ed_userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poweredby_layout.setVisibility(View.GONE);
            }
        });
        
        btn_signIn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UserName = ed_userName.getText().toString();
                Password = ed_password.getText().toString();
                
                if (internetConnection.hasConnection(getApplicationContext())) 
                {
                    if (UserName.equals("") && Password.equals("")) 
                    {
                        ed_userName.setError("Please enter email/mobile");
                        ed_password.setError("Please enter password");
                        txtChange();
                    } 
                    else if (UserName.equals("")) {
                        ed_userName.setError("Please enter email/mobile");
                        txtChange();
                    }
                    else if (Password.equals("")) {
                        ed_password.setError("Please enter password");
                        txtChange();
                    } 
                    else {
                        if (Current_Location != null)
                        {
                            signIn();
                        }
                        else {
                            Toast.makeText(LogInActivity.this, "Failed to get location", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                else {
                    Toast.makeText(LogInActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
        
        deviceData();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("requestCode",""+requestCode );//1

        switch (requestCode)
        {
            case 1:
            {
                Log.i("grantResults",""+grantResults.length );
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.i("grantResults_in",""+grantResults.length );
                    gps = new GPSTracker(getApplicationContext(), LogInActivity.this);

                    if (gps.canGetLocation())
                    {
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        Current_Location=gps.getlocation_Address();
                        //Log.i("Current_Location",Current_Location);
                    }
                    else
                    {
                        AlertDialog.Builder  builder = new AlertDialog.Builder(LogInActivity.this);
                        builder.setMessage("Please Enable GPS");
                        builder.setCancelable(false);
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int arg1)
                            {
                                d.dismiss();
                            }
                        });
                    }
                }
                else
                {
                    Log.i("grantResults_else",""+grantResults.length );
                }
                return;
            }
        }
    }

    public void deviceData()
    {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
    }

    public void txtChange()
    {
        ed_userName.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ed_userName.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ed_password.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ed_password.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void signIn()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = new ProgressDialog(LogInActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setTitle("Please wait");
                progressDialog.setMessage("Signing In...");
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/signInwithdeviceid/?";
                    
                    String query = String.format("email=%s&password=%s&android_devide_id=%s&devicelocation=%s&signinby=%s&logoutflag=%s",
                            URLEncoder.encode(UserName, "UTF-8"),
                            URLEncoder.encode(Password, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode(Current_Location, "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"));

                    url = new URL(Transurl + query);
                    Log.i("url", "" + url);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(10000);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response += line;
                        }
                    }
                    else {
                        response = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(LogInActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(LogInActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(LogInActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("response", result);
                if (response.equals("[]"))
                {
                    progressDialog.dismiss();
                    Toast.makeText(LogInActivity.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject object = json.getJSONObject(0);

                        String responsecode = object.getString("responseCode");

                        if (responsecode.equals("1"))
                        {
                            progressDialog.dismiss();
                            
                            session.createUserLoginSession(UserName, Password);

                            String uId = object.getString("uId");
                            String firstName = object.getString("firstName");
                            String lastName = object.getString("lastName");
                            String Name = firstName + lastName;
                            String email = object.getString("email");
                            String mobile = object.getString("mobile");
                            String subuserid = object.getString("subuserid");
                            
                            pref = getApplicationContext().getSharedPreferences(MyPREFERENCES, PRIVATE_MODE);
                            editor = pref.edit();
                            editor.putString("password", Password);
                            editor.putString("uId", uId);
                            editor.commit();

                            if (Login_id.equals("1")) {
                                Intent intent = new Intent(LogInActivity.this, RegistrationActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else {
                                Intent intent = new Intent(LogInActivity.this, ResetThumbActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                        else
                        {
                            progressDialog.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(LogInActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            alertDialog.setTitle(message);
                            alertDialog.setCancelable(true);
                            alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }
                            });

                            alertDialog.show();
                        }
                    }
                    catch (JSONException e){
                        progressDialog.dismiss();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
