package com.hrgirdattendanceonline;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class AttendanceActivity extends AppCompatActivity implements MFS100Event
{
    byte[] Enroll_Template;
    byte[] Verify_Template;
    int mfsVer = 41;
    SharedPreferences settings;
    Context context;
    CommonMethod.ScannerAction scannerAction = CommonMethod.ScannerAction.Capture;

    int minQuality = 40;
    int timeout = 5000;//capture time out 10000
    MFS100 mfs100 = null;

    public static String _testKey = "t7L8wTG/iv02t+pgYrMQ7tt8qvU1z42nXpJDfAfsW592N4sKUHLd8A0MEV0GRxH+f4RgefEaMZALj7mgm/Thc0jNhR2CW9BZCTgeDPjC6q0W";

    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences  shared_pref;

    String myJSON = null, myJson2;
    String RegisteredBase64;
    String EmpId;
    String Sign_InOut_id = "1";
    String ResponseCode, Message;
    String Url, url_http;
    String android_id, logo;
    String empattDid, flag;
    String TabID;

    int result_match = 0;

    Toolbar toolbar;
    CoordinatorLayout snackbarCoordinatorLayout;
    TextView txt_date, txt_time, txt_time_a, txt_result, txt_quality_per, txt_quality_success,
        txt_empname, txt_att_deviceid;
    ImageView img_thumb_result, img_in_mark, img_out_mark;
    Button btn_signIn,btn_signOut;
    ProgressBar progress_quality;
    Snackbar snackbar, snackbar1;

    TextToSpeech textToSpeech;
    ProgressDialog progressDialog;
    Handler someHandler;
    ConnectionDetector cd;
    UserSessionManager session;

    CheckInternetConnection internetConnection;
    public static NetworkChange receiver;
    DatabaseHandler db;

    ArrayList<String> thumb_list = new ArrayList<String>();
    ArrayList<String> empId_list = new ArrayList<String>();
    ArrayList<String> empattDid_arr = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_new);
        context = AttendanceActivity.this.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mfsVer = Integer.parseInt(settings.getString("MFSVer", String.valueOf(mfsVer)));

        PubVar.sharedPrefernceDeviceMode = (SharedPreferences) context.getSharedPreferences(PubVar.strSpDeviceKey, Context.MODE_PRIVATE);

        mfs100 = new MFS100(this, mfsVer);
        mfs100.SetApplicationContext(this);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner_att);
        ImageView img_logo = (ImageView)findViewById(R.id.img_logo_att);
        setSupportActionBar(toolbar);

        db = new DatabaseHandler(this);
        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());

        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));
        logo = (shared_pref.getString("logo", ""));

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Picasso.with(AttendanceActivity.this).load(logo).into(img_logo);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(getResources().getDrawable(R.drawable.abc_ic_ab_back_material));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Initialisation();
        deviceData();

        if (internetConnection.hasConnection(AttendanceActivity.this))
        {
            //getThumbExpressionAll();
            flag = "1";
            empattDid = "";
            getUserData();
        }
        else
        {
            internetConnection.showNetDisabledAlertToUser(AttendanceActivity.this);
        }

        receiver = new NetworkChange()
        {
            @Override
            protected void onNetworkChange()
            {
                if (receiver.isConnected)
                {
                    if (snackbar != null)
                    {
                        snackbar.dismiss();
                    }
                }
                else
                {
                    snackbar = Snackbar.make(snackbarCoordinatorLayout, "", Snackbar.LENGTH_INDEFINITE);

                    Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
                    TextView textView = (TextView) layout.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setVisibility(View.INVISIBLE);
                    LayoutInflater inflater = LayoutInflater.from(snackbar.getContext());
                    View snackView = inflater.inflate(R.layout.snackbar_layout, null);
                    layout.addView(snackView, 0);
                    layout.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                    snackbar.show();
                }
            }
        };
    }

    public void Initialisation()
    {
        txt_date = (TextView)findViewById(R.id.txt_att_date);
        txt_time = (TextView)findViewById(R.id.txt_att_time);
        txt_time_a = (TextView)findViewById(R.id.txt_att_time_a);
        txt_result = (TextView)findViewById(R.id.txt_att_result);
        txt_empname = (TextView)findViewById(R.id.txt_att_name);
        txt_quality_per = (TextView)findViewById(R.id.txt_att_quality_per);
        txt_quality_success = (TextView)findViewById(R.id.txt_att_quality_success);
        txt_att_deviceid = (TextView)findViewById(R.id.txt_att_deviceid);

        img_thumb_result = (ImageView)findViewById(R.id.img_thumb_result);
        img_in_mark = (ImageView)findViewById(R.id.img_in);
        img_out_mark = (ImageView)findViewById(R.id.img_out);

        btn_signIn = (Button)findViewById(R.id.btn_att_signIn);
        btn_signOut = (Button)findViewById(R.id.btn_att_signOut);

        snackbarCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.snackbarCoordinatorLayout_att);

        progress_quality = (ProgressBar)findViewById(R.id.progressBar_quality);
        progress_quality.setMax(100);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if(status != TextToSpeech.ERROR)
                {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy");
        String currentDate = sdf.format(calendar.getTime());

        try
        {
            Date date = sdf.parse(currentDate);
            SimpleDateFormat outFormat = new SimpleDateFormat("EEE");
            String day = outFormat.format(date);
            Log.i("current_date", day+", "+currentDate);

            txt_date.setText(day+", "+currentDate);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        img_in_mark.setVisibility(View.GONE);
        img_out_mark.setVisibility(View.GONE);
        txt_result.setText("");
        txt_quality_success.setVisibility(View.INVISIBLE);

        btn_signIn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sign_InOut_id = "1";
                img_in_mark.setVisibility(View.VISIBLE);
                img_out_mark.setVisibility(View.GONE);
                if (internetConnection.hasConnection(AttendanceActivity.this))
                {
                    btn_signIn.setEnabled(false);
                    btn_signOut.setEnabled(false);
                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    StartSyncCapture();
                }
                else {
                    Toast.makeText(AttendanceActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_signOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sign_InOut_id = "2";
                img_in_mark.setVisibility(View.GONE);
                img_out_mark.setVisibility(View.VISIBLE);
                if (internetConnection.hasConnection(AttendanceActivity.this))
                {
                    btn_signIn.setEnabled(false);
                    btn_signOut.setEnabled(false);
                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    StartSyncCapture();
                }
                else {
                    Toast.makeText(AttendanceActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
                SimpleDateFormat sdf1 = new SimpleDateFormat("a");
                sdf.setLenient(false);
                Date today = new Date();
                String time = sdf.format(today);
                String time_a = sdf1.format(today);
                String str = time_a.replace("AM", "am").replace("PM","pm");
                txt_time.setText(time);
                txt_time_a.setText(str);
                someHandler.postDelayed(this, 1000);
            }
        }, 10);
    }

    public void deviceData()
    {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        //txt_att_deviceid.setText(android_id);
        //GetDeviceName(android_id);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
    }

    Handler handler2;
    Runnable runnable;
    int i = 0;

    public void onControlClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.btnForLoop:
                Toast.makeText(AttendanceActivity.this, "Loop for init->uninit->init... 500 times", Toast.LENGTH_LONG).show();
                i = 0;
                handler2 = new Handler();
                runnable = new Runnable()
                {
                    @Override
                    public void run() {
                        // Log.e("1", ""+ (i+1));
                        if (i >= 500)
                        {
                            handler2.removeCallbacks(runnable);
                        }
                        else
                        {
                            if (i % 2 == 0) {
                                InitScanner();
                            }
                            else {
                                UnInitScanner();
                            }
                            i++;
                            handler2.postDelayed(runnable, 100);
                        }
                    }
                };
                handler2.post(runnable);
                break;

            default:
                break;
        }
    }

    private void InitScanner()
    {
        try
        {
            int ret = mfs100.Init();
            if (ret != 0)
            {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
                Log.i("info", "fail - "+(mfs100.GetErrorMsg(ret)));
            }
            else
            {
                SetTextonuiThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                Log.i("info", info);
            }
        }
        catch (Exception ex)
        {
            Toast.makeText(this, "Init failed, unhandled exception", Toast.LENGTH_LONG).show();
            SetTextonuiThread("Init failed, unhandled exception");
        }
    }

    private void StartSyncCapture()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                SetTextonuiThread("");
                try
                {
                    FingerData fingerData = new FingerData();

                    int ret = mfs100.StartCapture(minQuality, timeout, true);
                    if (ret != 0)
                    {
                        SetTextonuiThread(mfs100.GetErrorMsg(ret));
                    }
                    else
                    {
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(
                                fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);

                        SetTextonuiThread("Capture Success");
                        String log = "\nQuality: " + fingerData.Quality()
                                + "\nNFIQ: " + fingerData.Nfiq()
                                + "\nWSQ Compress Ratio: "
                                + fingerData.WSQCompressRatio()
                                + "\nImage Dimensions (inch): "
                                + fingerData.InWidth() + "\" X "
                                + fingerData.InHeight() + "\""
                                + "\nImage Area (inch): " + fingerData.InArea()
                                + "\"" + "\nResolution (dpi/ppi): "
                                + fingerData.Resolution() + "\nGray Scale: "
                                + fingerData.GrayScale() + "\nBits Per Pixal: "
                                + fingerData.Bpp() + "\nWSQ Info: "
                                + fingerData.WSQInfo();
                        //SetLogOnUIThread(log);

                        //////////////////// Extract ISO Image
                        byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height())+1078];
                        byte[] isoImage = null;
                        int dataLen = mfs100.ExtractISOImage(fingerData.RawData(),tempData);
                        if(dataLen<=0)
                        {
                            if(dataLen==0)
                            {
                                SetTextonuiThread("Failed to extract ISO Image");
                            }
                            else
                            {
                                SetTextonuiThread(mfs100.GetErrorMsg(dataLen));
                            }
                            return;
                        }
                        else
                        {
                            isoImage = new byte[dataLen];
                            System.arraycopy(tempData, 0, isoImage, 0,
                                    dataLen);
                        }

                        //getThumbExpression(fingerData);
                        //SetData2(fingerData,ansiTemplate,isoImage,wsqImage);
                    }
                }
                catch (Exception ex) {
                    SetTextonuiThread("Error");
                }
            }
        }).start();
    }

    private void UnInitScanner()
    {
        try
        {
            int ret = mfs100.UnInit();
            if (ret != 0)
            {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
            }
            else {
                //SetLogOnUIThread("Uninit Success");
                SetTextonuiThread("Uninit Success");
            }
        }
        catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    private void SetTextonuiThread(final String str)
    {
        txt_quality_per.post(new Runnable()
        {
            public void run()
            {
                if (str.equalsIgnoreCase("Capture Success"))
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    txt_quality_success.setText(str);
                }
                else if (str.equalsIgnoreCase("Error: -1140(Timeout)"))
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText("Please press thumb properly");
                    img_in_mark.setVisibility(View.GONE);
                    img_out_mark.setVisibility(View.GONE);
                    mfs100.StopCapture();
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                            progress_quality.getProgressDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.DST);
                        }
                    }, 2000);
                }
                else if (str.equalsIgnoreCase("No Device Connected"))
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText(str);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_in_mark.setVisibility(View.GONE);
                    img_out_mark.setVisibility(View.GONE);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mfs100.StopCapture();
                            Log.i("start", "start");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                }
                else if (str.equalsIgnoreCase("Permission denied"))
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText(str);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_in_mark.setVisibility(View.GONE);
                    img_out_mark.setVisibility(View.GONE);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mfs100.StopCapture();
                            Log.i("start", "start");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                }
                else if (str.equalsIgnoreCase("Device removed"))
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    txt_quality_success.setVisibility(View.VISIBLE);
                    txt_quality_success.setText(str);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    img_in_mark.setVisibility(View.GONE);
                    img_out_mark.setVisibility(View.GONE);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mfs100.StopCapture();
                            Log.i("start", "start");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                }
                else if (str.contains("Error: -1319(Capturing stopped)"))
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);
                    txt_result.setText("");
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                }
                else if (str.contains("Error"))
                {
                    txt_result.setText("");
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                }
                else
                {
                    txt_quality_success.setVisibility(View.INVISIBLE);
                    txt_quality_per.setText(str+"%");
                }

                Log.i("str",str);

                String regexStr = "^[0-9]*$";
                try
                {
                    int progress = Integer.parseInt(str);
                    progress_quality.setProgress(progress);
                    if (progress < 40)
                    {
                        progress_quality.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.ADD);
                    }
                    else
                    {
                        progress_quality.getProgressDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.DST);
                    }
                }
                catch (NumberFormatException e) {
                    Log.i(""," is not a number");
                }
            }
        });
    }

    @Override
    public void OnPreview(FingerData fingerData)
    {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0, fingerData.FingerImage().length);

        // Log.e("OnPreview.Quality", String.valueOf(fingerData.Quality()));
        SetTextonuiThread(""+fingerData.Quality());
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData)
    {
        Log.i("capture_cmplt", "capture_cmplt");
        if (status)
        {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(
                    fingerData.FingerImage(), 0,
                    fingerData.FingerImage().length);

            SetTextonuiThread("Capture Success");
            String log = "\nQuality: " + fingerData.Quality() + "\nNFIQ: "
                    + fingerData.Nfiq() + "\nWSQ Compress Ratio: "
                    + fingerData.WSQCompressRatio()
                    + "\nImage Dimensions (inch): " + fingerData.InWidth()
                    + "\" X " + fingerData.InHeight() + "\""
                    + "\nImage Area (inch): " + fingerData.InArea() + "\""
                    + "\nResolution (dpi/ppi): " + fingerData.Resolution()
                    + "\nGray Scale: " + fingerData.GrayScale()
                    + "\nBits Per Pixal: " + fingerData.Bpp() + "\nWSQ Info: "
                    + fingerData.WSQInfo();
            Log.i("capture_cmplt_log", log);

            //getThumbExpression(fingerData);
            //matchThumb(fingerData);
            MatchThumb(fingerData);
        }
        else
        {
            SetTextonuiThread("Error: " + errorCode + "(" + errorMsg + ")");
        }
    }

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission)
    {
        int ret = 0;
        if (!hasPermission)
        {
            SetTextonuiThread("Permission denied");
            return;
        }
        if (vid == 1204 || vid == 11279)
        {
            if (pid == 34323)
            {
                ret = mfs100.LoadFirmware();
                if (ret != 0)
                {
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                } else {
                    SetTextonuiThread("Loadfirmware success");
                }
            }
            else if (pid == 4101)
            {
                //Added by Milan Sheth on 19-Dec-2016
                String strDeviceMode = PubVar.sharedPrefernceDeviceMode.getString(PubVar.strSpDeviceKey, "public");
                if (strDeviceMode.toLowerCase().equalsIgnoreCase("public"))
                {
                    ret = mfs100.Init("");
                    if (ret == -1322)
                    {
                        ret = mfs100.Init(_testKey);
                        if (ret == 0)
                        {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                            //showSuccessLog();
                        }
                    }
                    else if (ret == 0) {
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                        //showSuccessLog();
                    }
                }
                else
                {
                    ret = mfs100.Init(_testKey);
                    if (ret == -1322)
                    {
                        ret = mfs100.Init("");
                        if (ret == 0)
                        {
                            PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "public").apply();
                            //showSuccessLog();
                        }
                    }
                    else if (ret == 0)
                    {
                        PubVar.sharedPrefernceDeviceMode.edit().putString(PubVar.strSpDeviceKey, "protected").apply();
                        //showSuccessLog();
                    }
                }

                if (ret != 0)
                {
                    SetTextonuiThread(mfs100.GetErrorMsg(ret));
                }
            }
        }
    }

    @Override
    public void OnDeviceDetached()
    {
        UnInitScanner();
        SetTextonuiThread("Device removed");
    }

    @Override
    public void OnHostCheckFailed(String err)
    {
        try
        {
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        }
        catch (Exception ex) {

        }
    }

    public void getUserData()
    {
        class GetUserData extends AsyncTask<String, Void, String>
        {
            String response1;

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(AttendanceActivity.this, "Please wait", "Getting Thumb data...", true);
                progressDialog.show();
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
                    URL url = new URL(leave_url + query3);
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
                            Toast.makeText(AttendanceActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AttendanceActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AttendanceActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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

                    GetDeviceName(android_id);

                    progressDialog.dismiss();

                    if (result.contains("<HTML><HEAD>"))
                    {
                        Toast.makeText(AttendanceActivity.this, "Login to captive portal", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        if (myJson2.equals("[]"))
                        {
                            Toast.makeText(AttendanceActivity.this, "No New Records", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            try
                            {
                                // [{"empattDid":29,"uId":4,"firstName":"Amit","lastName":"Mhaske","cid":"Hrsaas2","mobile":"1202152102","status":"1","attendancetype":3,
                                JSONArray jsonArray = new JSONArray(myJson2);
                                Log.i("jsonArray123", "" + jsonArray);

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

                                        //Log.i("Insert: ", "Inserting ..");
                                        db.addContact(new UserDetails_Model(null, get_uId, get_cid, get_attType, get_firstName, get_lastName, get_mobile, t1, t2, t3, t4));
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
                                        if (db.checkEmpId(get_uId)) {
                                            db.UpdateContactAttType(new UserDetails_Model(t1, t2, t3, t4, get_attType), get_uId);
                                        }
                                    }
                                    else if (get_status.equals("3"))
                                    {
                                        String get_uId = object.getString("uId");
                                        //Log.i("get_uId",get_uId);

                                        String get_mobile = object.getString("mobile");
                                        //Log.i("get_mobile",get_mobile);
                                        if (db.checkEmpId(get_uId)) {
                                            db.deleteContact(get_mobile);
                                        }
                                    }
                                }

                                Toast.makeText(AttendanceActivity.this, "Data updated successfully", Toast.LENGTH_LONG).show();

                                //Log.i("Reading: ", "Reading all contacts..");
                                List<UserDetails_Model> contacts = db.getAllContacts();

                                for (UserDetails_Model cn : contacts)
                                {
                                    String log = "PrimaryKey: "+cn.getPrimaryKey()+",uId: "+cn.getUid()+",cId: "+cn.getCid()+", Type: "+cn.getAttType()+" ,Name: " + cn.getFirstname() + " ,Phone: " + cn.getMobile_no();
                                    Log.i("Name: ", log);
                                }

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

    public void MatchThumb(final FingerData fingerData)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                List<UserDetails_Model> contacts = db.getAllContacts();
                Log.i("MFS_Log contacts", "" + contacts);

                result_match = 0;

                for (UserDetails_Model cn : contacts)
                {
                    if (result_match < 1400)
                    {
                        String t1 = cn.getThumb1();
                        String t2 = cn.getThumb2();
                        String t3 = cn.getThumb3();
                        String t4 = cn.getThumb4();

                        String thumbs[] = {t1, t2, t3, t4};
                        Log.i("thumbs",""+  Arrays.toString(thumbs));

                        for (int i = 0; i < thumbs.length; i++)
                        {
                            if (result_match < 1400)
                            {
                                RegisteredBase64 = thumbs[i];
                                if (RegisteredBase64 != null)
                                {
                                    Log.i("MFS_ RegisteredBase64", RegisteredBase64);

                                    EmpId = cn.getUid();
                                    Log.i("MFS_Log EmpId", EmpId);

                                    Enroll_Template = Base64.decode(RegisteredBase64, Base64.DEFAULT);
                                    Log.i("MFS_Log Enroll_Template", "" + Enroll_Template);

                                    Verify_Template = new byte[fingerData.ISOTemplate().length];
                                    System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                                            fingerData.ISOTemplate().length);

                                    Log.i("MFS_Log Verify_Template", "" + Verify_Template);

                                    result_match = mfs100.MatchISO(Enroll_Template, Verify_Template);
                                    Log.i("MFS_Log result_match", "" + result_match);

                                    if (result_match >= 1400)
                                    {
                                        Log.i("MFS_Log MATCHED!!", "MATCHED!!");
                                        makeAttendance();
                                        break;
                                    }
                                    else
                                    {
                                        Log.i("MFS_Log NOT MATCHED!!", "NOT MATCHED!!");
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        break;
                    }
                }

                if (result_match < 1400)
                {
                    btn_signIn.setEnabled(true);
                    btn_signOut.setEnabled(true);

                    img_in_mark.setVisibility(View.GONE);
                    img_out_mark.setVisibility(View.GONE);
                    txt_quality_per.setText("0%");
                    progress_quality.setProgress(0);
                    txt_result.setText("Sorry thumb not matched");
                    txt_result.setTextColor(getColor(R.color.RedTextColor));
                    img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                    textToSpeech.speak("Sorry thumb not matched!!", TextToSpeech.QUEUE_FLUSH, null);
                    Log.i("THUMB NOT MATCHED!!", "THUMB NOT MATCHED!!");

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            txt_result.setText("");
                            txt_quality_success.setVisibility(View.INVISIBLE);
                            img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                        }
                    }, 3000);
                }
            }
        });
    }

    public void makeAttendance()
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                progressDialog = ProgressDialog.show(AttendanceActivity.this, "", "Please wait...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    //String Transurl = ""+url_http+""+Url+"/owner/hrmapi/makeattendance/?";
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/makeattendancehitm/?";

                    String query = String.format("empId=%s&signId=%s",
                            URLEncoder.encode(EmpId, "UTF-8"),
                            URLEncoder.encode(Sign_InOut_id, "UTF-8"));

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
                            Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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

                btn_signIn.setEnabled(true);
                btn_signOut.setEnabled(true);

                if (result == null)
                {
                    progressDialog.dismiss();
                    Toast.makeText(AttendanceActivity.this, "Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject jsonObj = json.getJSONObject(0);

                        ResponseCode = jsonObj.getString("responsecode");
                        Message = jsonObj.getString("msg");

                        if (Sign_InOut_id.equals("1"))
                        {
                            progressDialog.dismiss();

                            if (ResponseCode.equals("1"))
                            {
                                String firstName = jsonObj.getString("firstName");

                                img_in_mark.setVisibility(View.GONE);
                                img_out_mark.setVisibility(View.GONE);
                                txt_quality_per.setText("0%");
                                progress_quality.setProgress(0);
                                txt_empname.setVisibility(View.VISIBLE);
                                txt_empname.setText(firstName);
                                txt_result.setText(Message);
                                txt_result.setTextColor(getColor(R.color.TextGreenColor));
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_green));
                                textToSpeech.speak("Welcome "+firstName, TextToSpeech.QUEUE_FLUSH, null);
                            }
                            else
                            {
                                img_in_mark.setVisibility(View.GONE);
                                img_out_mark.setVisibility(View.GONE);
                                txt_quality_per.setText("0%");
                                progress_quality.setProgress(0);
                                txt_empname.setVisibility(View.GONE);
                                txt_result.setText(Message);
                                txt_result.setTextColor(getColor(R.color.RedTextColor));
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                                textToSpeech.speak(Message, TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }
                        else if (Sign_InOut_id.equals("2"))
                        {
                            progressDialog.dismiss();
                            if (ResponseCode.equals("1"))
                            {
                                String firstName = jsonObj.getString("firstName");

                                img_in_mark.setVisibility(View.GONE);
                                img_out_mark.setVisibility(View.GONE);
                                txt_quality_per.setText("0%");
                                progress_quality.setProgress(0);
                                txt_empname.setVisibility(View.VISIBLE);
                                txt_empname.setText(firstName);
                                txt_result.setText(Message);
                                txt_result.setTextColor(getColor(R.color.TextGreenColor));
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_green));
                                textToSpeech.speak("Bye Bye "+firstName, TextToSpeech.QUEUE_FLUSH, null);
                            }
                            else
                            {
                                img_in_mark.setVisibility(View.GONE);
                                img_out_mark.setVisibility(View.GONE);
                                txt_quality_per.setText("0%");
                                progress_quality.setProgress(0);
                                txt_empname.setVisibility(View.GONE);
                                txt_result.setText(Message);
                                txt_result.setTextColor(getColor(R.color.RedTextColor));
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_red));
                                textToSpeech.speak(Message, TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }

                        Log.i("Successful", "Successful");

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                txt_result.setText("");
                                txt_empname.setText("");
                                txt_quality_success.setVisibility(View.INVISIBLE);
                                img_thumb_result.setImageDrawable(getDrawable(R.drawable.thumb_black));
                            }
                        }, 3000);
                    }
                    catch (JSONException e)
                    {
                        progressDialog.dismiss();
                        //Toast.makeText(AttendanceActivity.this, "Sorry...Json exception", Toast.LENGTH_LONG).show();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
    }

    protected void onStop()
    {
        UnInitScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        if (mfs100 != null)
        {
            mfs100.Dispose();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                InitScanner();
            }
        }).start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed()
    {
        mfs100.StopCapture();
        Intent intent = new Intent(AttendanceActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    public void GetDeviceName(final String device_id)
    {
        class SendDeviceID extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                /*progressDialog1 = new ProgressDialog(AttendanceActivity.this, ProgressDialog.THEME_HOLO_LIGHT);
                progressDialog1.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog1.setTitle("Please wait");
                progressDialog1.setMessage("Receiving Device ID...");
                progressDialog1.show();*/
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/getnameofdeviceid/?";

                    String query = String.format("android_devide_id=%s",
                            URLEncoder.encode(device_id, "UTF-8"));

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
                            //progressDialog1.dismiss();
                            Toast.makeText(AttendanceActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("SocketTimeoutException", e.toString());
                }
                catch (ConnectTimeoutException e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog1.dismiss();
                            Toast.makeText(AttendanceActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e("ConnectTimeoutException", e.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog1.dismiss();
                            Toast.makeText(AttendanceActivity.this, "Slow internet / Login to captive portal", Toast.LENGTH_SHORT).show();
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
                    //progressDialog1.dismiss();
                    Toast.makeText(AttendanceActivity.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
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
                            //progressDialog1.dismiss();

                            TabID = object.getString("responseMessage");
                            Log.i("TabID",TabID);
                            txt_att_deviceid.setText("Tab"+TabID);
                            Log.i("Device Name",txt_att_deviceid.getText().toString());
                        }
                        else
                        {
                            //progressDialog1.dismiss();

                            String msg = object.getString("responseMessage");
                            String message = msg.substring(2, msg.length()-2);

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(AttendanceActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
                        //progressDialog1.dismiss();
                        Log.i("Exception", e.toString());
                    }
                }
            }
        }
        SendDeviceID sendid = new SendDeviceID();
        sendid.execute();
    }
}
