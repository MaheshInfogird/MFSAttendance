package com.hrgirdattendanceonline;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences pref, shared_pref;

    Button btn_attendance, btn_registration, btn_resetThumb;
    CheckInternetConnection internetConnection;
    UserSessionManager session;
    ConnectionDetector cd;
    ProgressDialog progressDialog;
    DatabaseHandler db;
    PopupWindow pw;
    GPSTracker gps;
    
    String response_version, myJson1, myJson2, Url;
    String Packagename;
    String url_http, logo;
    String UserName, Password;
    String android_id, outid;
    String empattDid, flag;
    String Current_Location;
    
    int version_code;
    
    ImageView main_logo;
    ImageButton app_destroy;
    LinearLayout progress_layout;
    EditText ed_userName, ed_password;
    Button btn_login, btn_Cancel;

    boolean check = false;

    ArrayList<String> empattDid_arr = new ArrayList<String>();

    Double latitude = 0.0, longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());
        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();

        db = new DatabaseHandler(this);
        
       /* WifiManager wm = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int speed = wm.getConnectionInfo().getLinkSpeed();
        Log.i("internet_speed", ""+speed);*/

        pref = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE);
        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));

        Initialization();
        deviceData();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        else
        {
            gps = new GPSTracker(getApplicationContext(), MainActivity.this);
            if (gps.canGetLocation())
            {
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
                Current_Location = gps.getlocation_Address();
                if (internetConnection.hasConnection(MainActivity.this))
                {
                    /*flag = "1";
                    empattDid = "";
                    getUserData();*/
                    getCheckVersion();
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check internet connection", Toast.LENGTH_SHORT).show();
                }
            }
            /*else
            {
                Log.i("Current_Location","Current_Location");

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setMessage("Please Enable GPS");
                alertDialog.setCancelable(true);
                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                alertDialog.show();
            }*/
        }
    }

    public void deviceData()
    {
        try
        {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version_code = info.versionCode;
            Packagename = info.packageName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public void Initialization()
    {
        btn_attendance = (Button)findViewById(R.id.btn_attendance);
        btn_registration = (Button)findViewById(R.id.btn_registration);
        btn_resetThumb = (Button)findViewById(R.id.btn_resetThumb);
        app_destroy = (ImageButton)findViewById(R.id.app_destroy);
        main_logo = (ImageView)findViewById(R.id.main_logo);
        progress_layout = (LinearLayout)findViewById(R.id.progress_layout_main);

        Picasso.with(MainActivity.this).load(logo).into(main_logo);

        app_destroy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                alertDialog.setMessage("Do you want to close app?");
                alertDialog.setCancelable(false);
                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        outid = "0";
                        popup_window(v);
                    }
                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        });
        //getCheckVersion();

        main_logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outid = "1";
                popup_window(v);
            }
        });

        btn_attendance.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btn_attendance.setBackgroundResource(R.drawable.attendance_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.admin_button);
                btn_attendance.setTextColor(getResources().getColor(R.color.RedTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.WhiteTextColor));

                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    Intent intent = new Intent(MainActivity.this, AttendanceActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.attendance_button);
                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.RedTextColor));

                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (session.isUserLoggedIn())
                    {
                        Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                        intent.putExtra("login_id", "1");
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                        intent.putExtra("login_id", "1");
                        startActivity(intent);
                        finish();
                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });


        btn_resetThumb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                btn_attendance.setBackgroundResource(R.drawable.admin_button);
                btn_registration.setBackgroundResource(R.drawable.admin_button);
                btn_resetThumb.setBackgroundResource(R.drawable.attendance_button);
                btn_attendance.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_registration.setTextColor(getResources().getColor(R.color.WhiteTextColor));
                btn_resetThumb.setTextColor(getResources().getColor(R.color.RedTextColor));

                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (session.isUserLoggedIn())
                    {
                        Intent intent = new Intent(MainActivity.this, ResetThumbActivity.class);
                        intent.putExtra("login_id", "2");
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
                        intent.putExtra("login_id", "2");
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
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
                    gps = new GPSTracker(getApplicationContext(), MainActivity.this);

                    if (gps.canGetLocation())
                    {
                        latitude = gps.getLatitude();
                        longitude = gps.getLongitude();
                        Current_Location = gps.getlocation_Address();
                        if (internetConnection.hasConnection(MainActivity.this))
                        {
                            /*flag = "1";
                            empattDid = "";
                            getUserData();*/
                            getCheckVersion();
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Please check internet connection", Toast.LENGTH_SHORT).show();
                        }
                    }
                   /* else
                    {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                        alertDialog.setMessage("Please Enable GPS");
                        alertDialog.setCancelable(true);
                        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        alertDialog.show();
                    }*/
                }
                else
                {
                    Log.i("grantResults_else",""+grantResults.length );
                }
                return;
            }
        }
    }

    public void popup_window(View v)
    {
        try {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.destroy_app_login, (ViewGroup) findViewById(R.id.destroy_login_layout));

            pw = new PopupWindow(layout, width, LinearLayout.LayoutParams.WRAP_CONTENT, true);
            pw.setWidth(width-40);
            pw.showAtLocation(v, Gravity.CENTER, 0, 0);

            dimBehind(pw);

            ed_userName = (EditText)layout.findViewById(R.id.ed_userName_dest);
            ed_password = (EditText)layout.findViewById(R.id.ed_password_dest);
            btn_login = (Button) layout.findViewById(R.id.btn_signIn_dest);
            btn_Cancel = (Button) layout.findViewById(R.id.btn_Cancel_dest);

            if (outid.equals("1"))
            {
                btn_login.setText("Logout");
            }
            else
            {
                btn_login.setText("Close App");
            }

            btn_Cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pw.dismiss();
                }
            });

            btn_login.setOnClickListener(new View.OnClickListener()
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
                            Toast.makeText(MainActivity.this, "Please enter username & password", Toast.LENGTH_LONG).show();
                        }
                        else if (UserName.equals(""))
                        {
                            Toast.makeText(MainActivity.this, "Please enter username", Toast.LENGTH_LONG).show();
                        }
                        else if (Password.equals(""))
                        {
                            Toast.makeText(MainActivity.this, "Please enter password", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            pw.dismiss();
                            signIn();
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dimBehind(PopupWindow popupWindow)
    {
        View container;
        if (popupWindow.getBackground() == null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                container = (View) popupWindow.getContentView().getParent();
            }
            else {
                container = popupWindow.getContentView();
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container = (View) popupWindow.getContentView().getParent().getParent();
            }
            else {
                container = (View) popupWindow.getContentView().getParent();
            }
        }

        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.6f;
        wm.updateViewLayout(container, p);
    }

    public void signIn()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            ProgressDialog progressDialog;
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = new ProgressDialog(MainActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Please wait...");
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
                            URLEncoder.encode("", "UTF-8"),
                            URLEncoder.encode("1", "UTF-8"),
                            URLEncoder.encode("2", "UTF-8"));

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
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                Log.i("response", result);
                if (response.equals("[]"))
                {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
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

                            if (outid.equals("1"))
                            {
                                db.deleteAllEmpRecord();
                                session.logout_url();
                                Intent intent = new Intent(MainActivity.this, UrlActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else {
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }
                        else
                        {
                            progressDialog.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
                    catch (JSONException e)
                    {
                        progressDialog.dismiss();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    public void getCheckVersion()
    {
        class GetCheckVersion extends AsyncTask<String, Void, String>
        {
            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Getting Thumb data...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/getversion/?";

                    String query3 = String.format("apptype=%s", URLEncoder.encode("5", "UTF-8"));
                    URL url = new URL(leave_url + query3);
                    Log.i("url", ""+ url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setDoOutput(true);
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response_version = "";
                            response_version += line;
                        }
                    }
                    else
                    {
                        response_version = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    if (progressDialog != null && progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("Exception", e.toString());
                }

                return response_version;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson1 = result;
                    Log.i("myJson", myJson1);

                    if (myJson1.equals("[]"))
                    {
                        if (progressDialog != null && progressDialog.isShowing())
                        {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MainActivity.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(myJson1);
                            //Log.i("jsonArray", "" + jsonArray);

                            JSONObject object = jsonArray.getJSONObject(0);
                            
                            int get_version = object.getInt("Version");
                            
                            if (version_code != get_version)
                            {
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setTitle("New Update");
                                alertDialog.setMessage("Please update your app");
                                alertDialog.setCancelable(false);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.hrgirdattendanceonline"));
                                        startActivity(intent);
                                        finish();
                                    }
                                });

                                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(startMain);
                                        finish();
                                    }
                                });

                                alertDialog.show();
                            }
                            else
                            {
                                check = true;
                                flag = "1";
                                empattDid = "";
                                getUserData();
                            }
                        }
                        catch (JSONException e)
                        {
                            if (progressDialog != null && progressDialog.isShowing())
                            {
                                progressDialog.dismiss();
                            }
                            Log.e("JsonException", e.toString());
                        }
                    }
                }
                else
                {
                    if (progressDialog != null && progressDialog.isShowing())
                    {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(MainActivity.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetCheckVersion getCheckVersion = new GetCheckVersion();
        getCheckVersion.execute();
    }

    public void getUserData()
    {
        class GetUserData extends AsyncTask<String, Void, String>
        {
            String response1;

            @Override
            protected void onPreExecute()
            {
                if (!check)
                {
                    progressDialog = ProgressDialog.show(MainActivity.this, "Please wait", "Getting Thumb data...", true);
                    progressDialog.show();
                }
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/getallempdatadevicewise/?";
                    String query3 = String.format("deviceid=%s&flag=%s&empdevicearr=%s",
                            URLEncoder.encode(android_id, "UTF-8"),
                            URLEncoder.encode(flag, "UTF-8"),
                            URLEncoder.encode(empattDid, "UTF-8"));

                    query3 = query3.replace("%2C+",",");
                    URL url = new URL(leave_url+query3);
                    Log.i("url123", ""+ url);

                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    connection.setReadTimeout(10000);
                    connection.setConnectTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setUseCaches(false);
                    connection.setAllowUserInteraction(false);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response1 = "";
                            response1 += line;
                        }
                    }
                    else
                    {
                        response1 = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("Exception", e.toString());
                }

                return response1;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson2 = result;
                    Log.i("myJson", myJson2);

                    if (result.contains("<HTML><HEAD>"))
                    {
                        if (progressDialog != null && progressDialog.isShowing())
                        {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(MainActivity.this, "Login to captive portal", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        if (myJson2.equals("[]"))
                        {
                            if (progressDialog != null && progressDialog.isShowing())
                            {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(MainActivity.this, "No New Records", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            try
                            {
            // [{"empattDid":29,"uId":4,"firstName":"Amit","lastName":"Mhaske","cid":"Hrsaas2","mobile":"1202152102","status":"1","attendancetype":3,
                                JSONArray jsonArray = new JSONArray(myJson2);
                                Log.i("jsonArray123", "" + jsonArray);

//"empattDid":3,"uId":4,"firstName":"Harshad","lastName":"Patil","cid":"Z3",
// "mobile":"8888113788","status":"1","attendancetype":1,"applyshift":1,"Thumexp":
                                empattDid_arr.clear();

                                for(int i=0; i <jsonArray.length(); i++)
                                {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    //Log.i("object123", "" + object);

                                    String get_status = object.getString("status");
                                    //Log.i("get_status",get_status);

                                    String empattDid = object.getString("empattDid");
                                    //Log.i("empattDid",empattDid);

                                    empattDid_arr.add(empattDid);

                                    if (get_status.equals("1"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);
                                        String get_firstName = object.getString("firstName");
                                        //Log.i("get_firstName",get_firstName);
                                        String get_lastName = object.getString("lastName");
                                        //Log.i("get_lastName",get_lastName);
                                        String get_cid = object.getString("cid");
                                        //Log.i("get_cid",get_cid);
                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);

                                        String get_attType = object.getString("attendancetype");
                                        //Log.i("get_attType",get_attType);

                                        JSONArray thumbexpr = object.getJSONArray("Thumexp");
                                        //Log.i("thumbexpr1143",thumbexpr+"");

                                        String t1="",t2="",t3="",t4="";

                                        for(int j = 0; j < thumbexpr.length(); j++)
                                        {
                                            JSONObject object_thumb = thumbexpr.getJSONObject(j);
                                            //Log.i("object_thumb", "" + object_thumb);
                                            String get_thumb = object_thumb.getString(j+1+"");
                                            //Log.i("get_thumb",get_thumb);

                                            if(j+1 == 1)
                                            {
                                                t1 = get_thumb;
                                                //Log.i("t1",t1);
                                            }
                                            else if(j+1 == 2)
                                            {
                                                t2 = get_thumb;
                                                //Log.i("t2",t2);
                                            }
                                            else if(j+1 == 3)
                                            {
                                                t3 = get_thumb;
                                                //Log.i("t3",t3);
                                            }
                                            else if(j+1 == 4)
                                            {
                                                t4 = get_thumb;
                                                //Log.i("t4",t4);
                                            }
                                            else{
                                                t1 = "";
                                                t2 = "";
                                                t3 = "";
                                                t4 = "";
                                            }
                                        }

                                        if (db.checkEmpId(get_uId))
                                        {
                                            db.UpdateEmpAttType(new UserDetails_Model(t1, t2, t3, t4, get_attType, get_applyshift), get_uId);
                                        }
                                        else
                                        {
                                            db.addEmpData(new UserDetails_Model(null, get_uId, get_cid, get_attType, get_firstName, get_lastName, get_mobile, t1, t2, t3, t4,get_applyshift));
                                        }
                                    }
                                    else if (get_status.equals("2"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);

                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);

                                        String get_attType = object.getString("attendancetype");
                                        //Log.i("get_attType",get_attType);

                                        JSONArray thumbexpr = object.getJSONArray("Thumexp");
                                        //Log.i("thumbexpr1143",thumbexpr+"");

                                        String t1="",t2="",t3="",t4="";

                                        for(int j = 0; j < thumbexpr.length(); j++)
                                        {
                                            JSONObject object_thumb = thumbexpr.getJSONObject(j);
                                            //Log.i("object_thumb", "" + object_thumb);
                                            String get_thumb = object_thumb.getString(j+1+"");
                                            //Log.i("get_thumb",get_thumb);

                                            if(j+1 == 1)
                                            {
                                                t1 = get_thumb;
                                                //Log.i("t1",t1);
                                            }
                                            else if(j+1 == 2)
                                            {
                                                t2 = get_thumb;
                                                //Log.i("t2",t2);
                                            }
                                            else if(j+1 == 3)
                                            {
                                                t3 = get_thumb;
                                                //Log.i("t3",t3);
                                            }
                                            else if(j+1 == 4)
                                            {
                                                t4 = get_thumb;
                                                //Log.i("t4",t4);
                                            }
                                            else{
                                                t1 = "";
                                                t2 = "";
                                                t3 = "";
                                                t4 = "";
                                            }
                                        }

                                        //Log.i("Insert: ", "Inserting ..");
                                        if (db.checkEmpId(get_uId))
                                        {
                                            db.UpdateEmpAttType(new UserDetails_Model(t1, t2, t3, t4, get_attType), get_uId);
                                        }
                                    }
                                    else if (get_status.equals("3"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);

                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);
                                        if (db.checkEmpId(get_uId))
                                        {
                                            db.deleteEmpRecord(get_mobile);
                                        }
                                    }
                                }

                                Toast.makeText(MainActivity.this, "Data updated successfully", Toast.LENGTH_LONG).show();

                                //Log.i("Reading: ", "Reading all contacts..");
                                List<UserDetails_Model> contacts = db.getAllEmpDetails();

                                for (UserDetails_Model cn : contacts)
                                {
                                    String log = "PrimaryKey: "+cn.getPrimaryKey()+",uId: "+cn.getUid()+",cId: "+cn.getCid()+", Type: "+cn.getAttType()+" ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();
                                    Log.i("Name: ", log);
                                }

                                if (progressDialog != null && progressDialog.isShowing())
                                {
                                    progressDialog.dismiss();
                                }

                                check = false;
                                flag = "2";
                                empattDid = empattDid_arr.toString();
                                empattDid = empattDid.substring(1, (empattDid.length() -1));
                                Log.i("empattDid", empattDid);

                                getUserData();
                            }
                            catch (JSONException e)
                            {
                                if (progressDialog != null && progressDialog.isShowing())
                                {
                                    progressDialog.dismiss();
                                }
                                Log.e("JsonException123", e.toString());
                            }
                        }
                    }
                }
                else
                {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            }
        }

        GetUserData getUrlData = new GetUserData();
        getUrlData.execute();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public void onBackPressed()
    {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
}
