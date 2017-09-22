package com.hrgirdattendanceonline;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

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

public class ForgotPassword extends AppCompatActivity
{
    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences shared_pref;

    ProgressDialog progressDialog, progressDialog2;
    ConnectionDetector cd;
    UserSessionManager session;
    CheckInternetConnection internetConnection;

    String Url;
    String url_http;
    String mail_mob, otp, newPass, cnfPass, uId;
    String myJson, myJson2, response_mail, response_pass;

    EditText ed_mail_mob, ed_otp, ed_newPass, ed_cnfPass;
    LinearLayout mail_mob_layout, pass_layout;
    Button btn_forgot, btn_otp_submit, btn_pass_reset;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();
       
        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());
        
        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));

        ed_mail_mob = (EditText)findViewById(R.id.ed_email_mob);
        ed_otp = (EditText)findViewById(R.id.ed_otp);
        ed_newPass = (EditText)findViewById(R.id.ed_new_pass);
        ed_cnfPass = (EditText)findViewById(R.id.ed_cnfrm_pass);
        btn_forgot = (Button)findViewById(R.id.btn_frgt_submit);
        btn_otp_submit = (Button)findViewById(R.id.btn_submit_otp);
        btn_pass_reset = (Button)findViewById(R.id.btn_reset_pass);
        mail_mob_layout = (LinearLayout)findViewById(R.id.mail_mob_layout);
        pass_layout = (LinearLayout)findViewById(R.id.otp_layout);

        btn_forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    mail_mob = ed_mail_mob.getText().toString();
                    if (!mail_mob.equals(""))
                    {
                        sendMailMobData();
                    }
                    else
                    {
                        ed_mail_mob.setError("Please enter Email Id / Mob no.");
                    }
                }
                else {
                    Toast.makeText(ForgotPassword.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });

        btn_pass_reset.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (internetConnection.hasConnection(getApplicationContext()))
                {
                    if (checkPassword())
                    {
                        resetPassword();
                    }
                }
                else {
                    Toast.makeText(ForgotPassword.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public boolean checkPassword()
    {
        otp = ed_otp.getText().toString();
        newPass = ed_newPass.getText().toString();
        cnfPass = ed_cnfPass.getText().toString();

        if (otp.equals(""))
        {
            ed_otp.setError("Please enter otp");
            return false;
        }

        if (newPass.equals(""))
        {
            ed_newPass.setError("Please enter new password");
            return false;
        }

        if (cnfPass.equals(""))
        {
            ed_cnfPass.setError("Please enter confirm password");
            return false;
        }

        if (newPass.compareTo(cnfPass) != 0)
        {
            ed_cnfPass.setError("Password do not match");
            return false;
        }
        return true;
    }

    public void sendMailMobData()
    {
        class GetLogoData extends AsyncTask<String, Void, String>
        {
            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(ForgotPassword.this, "", "Please wait...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/forgetPass/?";
                    String query3 = String.format("emailmob=%s", URLEncoder.encode(mail_mob, "UTF-8"));
                    URL url = new URL(leave_url + query3);
                    Log.i("url", "" + url);

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
                            response_mail = "";
                            response_mail += line;
                        }
                    }
                    else
                    {
                        response_mail = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(ForgotPassword.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ForgotPassword.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(ForgotPassword.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response_mail;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson = result;
                    Log.i("myJson", myJson);

                    progressDialog.dismiss();


                    if (myJson.equals("[]"))
                    {
                        Toast.makeText(ForgotPassword.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(myJson);
                            //Log.i("jsonArray", "" + jsonArray);

                            JSONObject object = jsonArray.getJSONObject(0);

                            String responseCode = object.getString("responseCode");
                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length() - 2);

                            if (responseCode.equals("1"))
                            {
                                mail_mob_layout.setVisibility(View.GONE);
                                pass_layout.setVisibility(View.VISIBLE);

                                uId = object.getString("uId");

                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ForgotPassword.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setMessage(message);
                                alertDialog.setCancelable(true);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });

                                alertDialog.show();
                            }
                            else
                            {
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ForgotPassword.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setMessage(message);
                                alertDialog.setCancelable(true);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });

                                alertDialog.show();
                            }

                        }
                        catch (JSONException e) {
                            Log.e("JsonException", e.toString());
                        }
                    }
                }
                else {
                    progressDialog.dismiss();
                    Toast.makeText(ForgotPassword.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetLogoData getUrlData = new GetLogoData();
        getUrlData.execute();
    }

    public void resetPassword()
    {
        class GetLogoData extends AsyncTask<String, Void, String>
        {
            @Override
            protected void onPreExecute()
            {
                progressDialog2 = ProgressDialog.show(ForgotPassword.this, "", "Please wait...", true);
                progressDialog2.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String leave_url = ""+url_http+""+Url+"/owner/hrmapi/updatePassword/?";
                    String query3 = String.format("otp=%s&newpassword=%s&uid=%s",
                            URLEncoder.encode(otp, "UTF-8"),
                            URLEncoder.encode(newPass, "UTF-8"),
                            URLEncoder.encode(uId, "UTF-8"));
                    URL url = new URL(leave_url + query3);
                    Log.i("url", "" + url);

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
                            response_pass = "";
                            response_pass += line;
                        }
                    }
                    else
                    {
                        response_pass = "";
                    }
                }
                catch (SocketTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog2.dismiss();
                            Toast.makeText(ForgotPassword.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog2.dismiss();
                            Toast.makeText(ForgotPassword.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog2.dismiss();
                            Toast.makeText(ForgotPassword.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("Exception", e.toString());
                }

                return response_pass;
            }

            @Override
            protected void onPostExecute(String result)
            {
                if (result != null)
                {
                    myJson2 = result;
                    Log.i("myJson", myJson2);

                    progressDialog2.dismiss();
                    
                    if (myJson2.equals("[]"))
                    {
                        Toast.makeText(ForgotPassword.this, "Sorry... Data not available", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(myJson2);
                            //Log.i("jsonArray", "" + jsonArray);

                            JSONObject object = jsonArray.getJSONObject(0);

                            String responseCode = object.getString("responseCode");
                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length() - 2);

                            if (responseCode.equals("1"))
                            {
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ForgotPassword.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setMessage(message);
                                alertDialog.setCancelable(false);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(ForgotPassword.this, LogInActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });

                                alertDialog.show();
                            }
                            else
                            {
                                AlertDialog.Builder alertDialog = new AlertDialog.Builder(ForgotPassword.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                                alertDialog.setMessage(message);
                                alertDialog.setCancelable(true);
                                alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });

                                alertDialog.show();
                            }

                        }
                        catch (JSONException e) {
                            Log.e("JsonException", e.toString());
                        }
                    }
                }
                else {
                    progressDialog2.dismiss();
                    Toast.makeText(ForgotPassword.this, "Sorry...Bad internet connection", Toast.LENGTH_LONG).show();
                }
            }
        }

        GetLogoData getUrlData = new GetLogoData();
        getUrlData.execute();
    }
}
