package com.hrgirdattendanceonline;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import javax.net.ssl.HttpsURLConnection;

public class AttendanceNew extends AppCompatActivity implements MFS100Event {

    TextView lblMessage;
    EditText txtEventLog;

    byte[] Enroll_Template;
    byte[] Verify_Template;
    int mfsVer = 41;
    SharedPreferences settings;
    Context context;
    CommonMethod.ScannerAction scannerAction = CommonMethod.ScannerAction.Capture;

    int minQuality = 40;
    int timeout = 10000;
    MFS100 mfs100 = null;

    public static String _testKey = "t7L8wTG/iv02t+pgYrMQ7tt8qvU1z42nXpJDfAfsW592N4sKUHLd8A0MEV0GRxH+f4RgefEaMZALj7mgm/Thc0jNhR2CW9BZCTgeDPjC6q0W";

    public static final String MyPREFERENCES_url = "MyPrefs_url" ;
    SharedPreferences  shared_pref;
    SharedPreferences.Editor editor1;

    public static final String MyPREFERENCES = "MyPrefs" ;
    int PRIVATE_MODE = 0;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    String myJSON = null;
    String RegisteredBase64, MobileNo;
    String CaptureBase64;
    String EmpId;
    String Sign_InOut_id = "1";
    String logout_id = "0";

    LinearLayout atndnc_logout;
    EditText ed_atndncLogout;
    Button btn_atndncLogout;
    Toolbar toolbar;
    EditText ed_MobNo;
    TextView txt_matchMsg, txt_Time, txt_success;
    ImageView img_Match;
    Button btn_signIn, btn_signOut;
    RadioButton rd_signIn, rd_signOut;
    RelativeLayout content_frame;
    CoordinatorLayout snackbarCoordinatorLayout;

    Calendar c;
    TextToSpeech textToSpeech;
    String ResponseCode, Message;

    ProgressDialog progressDialog;

    static boolean logout_status = true;
    Timer timer;
    //AttendanceActivity.MyTimerTask myTimerTask;
    Handler someHandler;
    ConnectionDetector cd;
    String Url;
    String url_http;
    UserSessionManager session;
    CheckInternetConnection internetConnection;
    public static NetworkChange receiver;
    Snackbar snackbar, snackbar1;

    String android_id;
    boolean device_info = false;

    int result_match = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);
        context = AttendanceNew.this.getApplicationContext();
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        mfsVer = Integer.parseInt(settings.getString("MFSVer",
                String.valueOf(mfsVer)));

        //CommonMethod.DeleteDirectory();
        //CommonMethod.CreateDirectory();

        PubVar.sharedPrefernceDeviceMode = (SharedPreferences) context.getSharedPreferences(PubVar.strSpDeviceKey, Context.MODE_PRIVATE);

        mfs100 = new MFS100(this, mfsVer);
        mfs100.SetApplicationContext(this);

        toolbar = (Toolbar)findViewById(R.id.toolbar_inner);
        TextView Header = (TextView)findViewById(R.id.header_text);
        ImageView img_logout = (ImageView)findViewById(R.id.img_logout);
        setSupportActionBar(toolbar);

        img_logout.setVisibility(View.GONE);

        session = new UserSessionManager(getApplicationContext());
        internetConnection = new CheckInternetConnection(getApplicationContext());

        cd = new ConnectionDetector(getApplicationContext());
        url_http = cd.geturl();

        shared_pref = getSharedPreferences(MyPREFERENCES_url, MODE_PRIVATE);
        Url = (shared_pref.getString("url", ""));

        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setTitle("");
            Header.setText("ATTENDANCE");
            img_logout.setVisibility(View.GONE);
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

        if (internetConnection.hasConnection(AttendanceNew.this))
        {

        }
        else {
            internetConnection.showNetDisabledAlertToUser(AttendanceNew.this);
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
                    //internetConnection.showNetDisabledAlertToUser(AttendanceNew.this);
                    snackbar = Snackbar.make(snackbarCoordinatorLayout, "Please check your internet connection", Snackbar.LENGTH_INDEFINITE);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                    snackbar.show();
                }
            }
        };
    }

    public void Initialisation()
    {
        atndnc_logout = (LinearLayout)findViewById(R.id.atndnc_logout);
        ed_atndncLogout = (EditText)findViewById(R.id.ed_atndnc_logout);
        btn_atndncLogout = (Button)findViewById(R.id.btn_atndnc_logout);
        content_frame = (RelativeLayout)findViewById(R.id.content_frame);
        txt_Time = (TextView)findViewById(R.id.txtTime);
        snackbarCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.snackbarCoordinatorLayout);

        ed_MobNo = (EditText)findViewById(R.id.ed_match_mobNo);
        txt_matchMsg = (TextView)findViewById(R.id.txtMatch);

        txt_success = (TextView)findViewById(R.id.tv_signinsuccess);

        img_Match = (ImageView)findViewById(R.id.match_fingerprint);
        btn_signIn = (Button)findViewById(R.id.btn_atndnc_signIn);
        btn_signOut = (Button)findViewById(R.id.btn_atndnc_signOut);
        rd_signIn = (RadioButton)findViewById(R.id.radio_signIn);
        rd_signOut = (RadioButton)findViewById(R.id.radio_signOut);

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

        rd_signIn.setChecked(true);

        rd_signIn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sign_InOut_id = "1";
                rd_signIn.setButtonDrawable(getResources().getDrawable(R.drawable.checkedradiobtn));
                rd_signOut.setButtonDrawable(getResources().getDrawable(R.drawable.uncheckedradiobtn));
            }
        });

        rd_signOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sign_InOut_id = "2";
                rd_signIn.setButtonDrawable(getResources().getDrawable(R.drawable.uncheckedradiobtn));
                rd_signOut.setButtonDrawable(getResources().getDrawable(R.drawable.checkedradiobtn));
            }
        });

        deviceData();

        someHandler = new Handler(getMainLooper());
        someHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy   hh:mm a");
                sdf.setLenient(false);
                Date today = new Date();
                String time = sdf.format(today);
                txt_Time.setText(time);
                someHandler.postDelayed(this, 10000);
            }
        }, 10);

        ed_MobNo.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                String mobNo = ed_MobNo.getText().toString();
                Log.i("mobNo", mobNo);
                Log.i("mobNo_length", ""+mobNo.length());

                MobileNo = ed_MobNo.getText().toString();
                Log.i("MobileNo", MobileNo);

                if (mobNo.length() == 0)
                {
                    mfs100.StopCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }

                if (mobNo.length() == 1)
                {
                    if (internetConnection.hasConnection(AttendanceNew.this))
                    {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                InitScanner();
                            }
                        }).start();

                        mfs100.StopCapture();
                        scannerAction = CommonMethod.ScannerAction.Capture;
                        MobileNo = ed_MobNo.getText().toString();
                        StartSyncCapture();
                        Log.i("ed_MobNo", ed_MobNo.getText().toString());
                    }
                    else {
                        Toast.makeText(AttendanceNew.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                }
                else if (mobNo.length() == 4)
                {
                    mfs100.StopCapture();

                    if (internetConnection.hasConnection(AttendanceNew.this))
                    {
                        scannerAction = CommonMethod.ScannerAction.Capture;
                        MobileNo = ed_MobNo.getText().toString();
                        StartSyncCapture();
                        Log.i("ed_MobNo", ed_MobNo.getText().toString());
                    }
                    else {
                        Toast.makeText(AttendanceNew.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                }
                else if (mobNo.length() == 5 || mobNo.length() == 6 || mobNo.length() == 7 ||mobNo.length() == 8||mobNo.length() == 9)
                {
                    mfs100.StopCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 10)
                {
                    if (internetConnection.hasConnection(AttendanceNew.this))
                    {
                        scannerAction = CommonMethod.ScannerAction.Capture;
                        MobileNo = ed_MobNo.getText().toString();
                        StartSyncCapture();
                        Log.i("ed_MobNo", ed_MobNo.getText().toString());
                    }
                    else {
                        Toast.makeText(AttendanceNew.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                /*String mobNo = ed_MobNo.getText().toString();
                Log.i("mobNo", mobNo);
                Log.i("mobNo_length", ""+mobNo.length());

                if (mobNo.length() == 1)
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InitScanner();
                        }
                    }).start();

                    mfs100.StopCapture();
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    MobileNo = ed_MobNo.getText().toString();
                    StartSyncCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 0)
                {
                    mfs100.StopCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 5 || mobNo.length() == 6 || mobNo.length() == 7 ||mobNo.length() == 8||mobNo.length() == 9)
                {
                    mfs100.StopCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }
                else if (mobNo.length() == 10)
                {
                    scannerAction = CommonMethod.ScannerAction.Capture;
                    MobileNo = ed_MobNo.getText().toString();
                    StartSyncCapture();
                    Log.i("ed_MobNo", ed_MobNo.getText().toString());
                }*/
               /* else if (mobNo.length() == 1)
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InitScanner();
                        }
                    }).start();
                }*/
                /*else
                {
                    Log.i("ed_MobNo_else", ed_MobNo.getText().toString());
                    mfs100.StopCapture();
                }*/
            }
        });
    }

    public void deviceData()
    {
        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;

        String Androidversion = manufacturer + model + version + versionRelease;
    }

    Handler handler2;
    Runnable runnable;
    int i = 0;

    public void onControlClicked(View v)
    {
        switch (v.getId())
        {
            case R.id.btnForLoop:
                Toast.makeText(AttendanceNew.this, "Loop for init->uninit->init... 500 times", Toast.LENGTH_LONG).show();
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
                //SetLogOnUIThread(info);
                Log.i("info", info);
            }
        }
        catch (Exception ex) {
            Toast.makeText(this, "Init failed, unhandled exception", Toast.LENGTH_LONG).show();
            SetTextonuiThread("Init failed, unhandled exception");
        }
    }

    private void StartAsyncCapture()
    {
        SetTextonuiThread("");
        try
        {
            int ret = mfs100.StartCapture(minQuality, timeout, true);
            if (ret != 0)
            {
                SetTextonuiThread(mfs100.GetErrorMsg(ret));
            }
            else {
                SetTextonuiThread("Place finger on scanner");
            }
        }
        catch (Exception ex) {
            SetTextonuiThread("Error");
        }
    }

    private void StopAsynCapture()
    {
        mfs100.StopCapture();
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
                    //int ret = mfs100.AutoCapture(fingerData, timeout, true, false);

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
                        img_Match.post(new Runnable() {
                            @Override
                            public void run() {
                                img_Match.setImageBitmap(bitmap);
                                img_Match.refreshDrawableState();
                            }
                        });

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

                        getThumbExpression(fingerData);
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

    private void WriteFile(String filename, byte[] bytes)
    {
        try
        {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists())
            {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists())
            {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    private void SetTextonuiThread(final String str)
    {
        /*lblMessage.post(new Runnable() {
            public void run() {
                lblMessage.setText(str);
            }
        });*/
    }

    private void SetLogOnUIThread(final String str)
    {
        txtEventLog.post(new Runnable() {
            public void run() {
                txtEventLog.append("\n" + str);
            }
        });
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mfs100_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Launch settings activity
                Intent i = new Intent(getBaseContext(), Preference.class);
                startActivity(i);
                break;
            // more code...
        }
        return true;
    }*/

    @Override
    public void OnPreview(FingerData fingerData)
    {
        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0, fingerData.FingerImage().length);
        img_Match.post(new Runnable() {
            @Override
            public void run() {
                img_Match.setImageBitmap(bitmap);
                img_Match.refreshDrawableState();

            }
        });
        // Log.e("OnPreview.Quality", String.valueOf(fingerData.Quality()));
        SetTextonuiThread("Quality: " + fingerData.Quality());
    }

    @Override
    public void OnCaptureCompleted(boolean status, int errorCode, String errorMsg, FingerData fingerData)
    {
//		SetLogOnUIThread("EndTime: " + getCurrentTime());
        Log.i("capture_cmplt", "capture_cmplt");
        if (status)
        {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(
                    fingerData.FingerImage(), 0,
                    fingerData.FingerImage().length);
            img_Match.post(new Runnable() {
                @Override
                public void run() {
                    img_Match.setImageBitmap(bitmap);
                    img_Match.refreshDrawableState();
                }
            });
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
            //SetLogOnUIThread(log);
            Log.i("capture_cmplt_log", log);

            //SetData(fingerData);
            getThumbExpression(fingerData);
        }
        else {
            SetTextonuiThread("Error: " + errorCode + "(" + errorMsg + ")");
        }
    }

    public void SetData(FingerData fingerData)
    {
        if (scannerAction.equals(CommonMethod.ScannerAction.Capture))
        {
            Enroll_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0,
                    fingerData.ISOTemplate().length);
        }
        else if (scannerAction.equals(CommonMethod.ScannerAction.Verify))
        {
            if (Enroll_Template == null) {
                SetTextonuiThread("Enrolled template not found.");
                return;
            }
            if (Enroll_Template.length <= 0) {
                SetTextonuiThread("Enrolled template not found.");
                return;
            }
            Verify_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                    fingerData.ISOTemplate().length);
            int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
            if (ret < 0)
            {
                SetTextonuiThread("Error: " + ret + "("
                        + mfs100.GetErrorMsg(ret) + ")");
            }
            else {
                if (ret >= 1400)
                {
                    SetTextonuiThread("Finger matched with score: " + ret);
                }
                else {
                    SetTextonuiThread("Finger not matched, score: " + ret);
                }
            }
        }
    }

    public void SetData2(FingerData fingerData, byte[] ANSITemplate, byte[] IsoImage, byte[] WsqImage)
    {
        if (scannerAction.equals(CommonMethod.ScannerAction.Capture))
        {
            Enroll_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0,
                    fingerData.ISOTemplate().length);
        }
        else if (scannerAction.equals(CommonMethod.ScannerAction.Verify))
        {
            Verify_Template = new byte[fingerData.ISOTemplate().length];
            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                    fingerData.ISOTemplate().length);
            int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
            if (ret < 0)
            {
                SetTextonuiThread("Error: " + ret + "("
                        + mfs100.GetErrorMsg(ret) + ")");
            }
            else
            {
                if (ret >= 1400)
                {
                    SetTextonuiThread("Finger matched with score: " + ret);
                }
                else {
                    SetTextonuiThread("Finger not matched, score: " + ret);
                }
            }
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

    private void showSuccessLog()
    {
        SetTextonuiThread("Init success");
        String info = "Serial: "
                + mfs100.GetDeviceInfo().SerialNo() + " Make: "
                + mfs100.GetDeviceInfo().Make() + " Model: "
                + mfs100.GetDeviceInfo().Model()
                + "\nCertificate: " + mfs100.GetCertification();
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

    public void getThumbExpression(final FingerData fingerData)
    {
        class GetDataJSON extends AsyncTask<String, Void, String>
        {
            private URL url;
            private String response = "";

            @Override
            protected void onPreExecute()
            {
                super.onPreExecute();
                if (snackbar1 != null)
                {
                    snackbar1.dismiss();
                }
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    //String Transurl = ""+url_http+""+Url+"/owner/hrmapi/getthumexpression/?";//85bca464ad3399eb
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/getthumexpression/?";

                    String query = String.format("mobile=%s&android_devide_id=%s",
                            URLEncoder.encode(MobileNo, "UTF-8"),
                            URLEncoder.encode(android_id, "UTF-8"));

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
                    Log.i("responseCode", ""+responseCode);

                    if (responseCode == HttpsURLConnection.HTTP_OK)
                    {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = br.readLine()) != null)
                        {
                            response += line;
                        }
                    }
                    else
                    {
                        response = "";
                    }
                }
                catch (SocketTimeoutException e1)
                {
                    /*AlertDialog.Builder alertDialog = new AlertDialog.Builder(AttendanceNew.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    alertDialog.setTitle("No Internet");
                    alertDialog.setMessage("Please Login to captive portal");
                    alertDialog.setCancelable(true);
                    alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    alertDialog.show();*/
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            snackbar1 = Snackbar.make(snackbarCoordinatorLayout, "Slow internet / Login to captive portal", Snackbar.LENGTH_INDEFINITE);
                            View sbView = snackbar1.getView();
                            sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                            snackbar1.show();

                            //progressDialog.dismiss();
                            //Toast.makeText(AttendanceNew.this, "Slow internet connection / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("SocketTimeoutException", ""+e1.toString());
                }
                catch (ConnectTimeoutException e1)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            snackbar1 = Snackbar.make(snackbarCoordinatorLayout, "Slow internet / Login to captive portal", Snackbar.LENGTH_INDEFINITE);
                            View sbView = snackbar1.getView();
                            sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                            snackbar1.show();

                           /* final Handler handler = new Handler();
                            handler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    if (snackbar1 != null)
                                    {
                                        snackbar1.dismiss();
                                    }
                                }
                            }, 3000);*/

                            //progressDialog.dismiss();
                            //Toast.makeText(AttendanceNew.this, "Slow internet connection /  Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("SocketTimeoutException", ""+e1.toString());
                }
                catch (Exception e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            snackbar1 = Snackbar.make(snackbarCoordinatorLayout, "Slow internet / Login to captive portal", Snackbar.LENGTH_INDEFINITE);
                            View sbView = snackbar1.getView();
                            sbView.setBackgroundColor(getResources().getColor(R.color.RedTextColor));
                            snackbar1.show();

                            //progressDialog.dismiss();
                            //Toast.makeText(AttendanceNew.this, "Slow internet connection / Login to captive portal", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.e("Exception", ""+e.toString());
                    //e.printStackTrace();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                myJSON = result;
                Log.i("result", "" + result);
                if (result.equals("[]"))
                {
                    Toast.makeText(AttendanceNew.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
                }
                else
                {
                    try
                    {
                        JSONArray json = new JSONArray(result);
                        //Log.i("json", "" + json);

                        JSONObject jsonObj = json.getJSONObject(0);

                        String responsecode = jsonObj.getString("responsecode");
                        result_match = 0;

                        if (responsecode.equals("1"))
                        {
                            try
                            {
                                JSONArray array = jsonObj.getJSONArray("thumexpression");
                                Log.i("array", ""+array);

                                String thumb_data = array.toString();
                                if (thumb_data.equals("[]"))
                                {
                                    result_match = 1200;
                                    txt_matchMsg.setText("Unsuccessful");
                                    //textToSpeech.speak("Sorry Thumb Not Matched", TextToSpeech.QUEUE_FLUSH, null);
                                    txt_matchMsg.setTextColor(Color.RED);
                                    textToSpeech.speak("Thumb Registration Not Exist!!", TextToSpeech.QUEUE_FLUSH, null);
                                    txt_success.setText("Thumb Registration Not Exist!!\n");
                                    Log.i("NOT MATCHED!!", "NOT MATCHED!!");

                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Log.i("start", "start");
                                            txt_matchMsg.setText("");
                                            txt_success.setText("");
                                            ed_MobNo.setText("");
                                            img_Match.setImageResource(R.drawable.imagefinger);
                                            //img_Match.setBackground(getResources().getDrawable(R.drawable.imagefinger));
                                        }
                                    }, 3000);
                                }
                                else
                                {
                                    for (int i = 0; i < array.length(); i++)
                                    {
                                        JSONObject object = array.getJSONObject(i);
                                        //Log.i("object", ""+object);

                                        if (result_match <= 1000)
                                        {
                                            RegisteredBase64 = object.getString("thumexpression");
                                            Log.i("RegisteredBase64", RegisteredBase64);

                                            EmpId = object.getString("empId");
                                            Log.i("EmpId", EmpId);

                                            Enroll_Template = Base64.decode(RegisteredBase64, Base64.DEFAULT);
                                            Log.i("Enroll_Template", "" + Enroll_Template);

                                            Verify_Template = new byte[fingerData.ISOTemplate().length];
                                            System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                                                    fingerData.ISOTemplate().length);

                                            CaptureBase64 = android.util.Base64.encodeToString(Verify_Template, android.util.Base64.NO_WRAP);
                                            Log.i("CaptureBase64", CaptureBase64);
                                            Log.i("Verify_Template", ""+Verify_Template);

                                            result_match = mfs100.MatchISO(Enroll_Template, Verify_Template);

                                            //sgfplib.MatchIsoTemplate(mRegisterTemplate, 0, mVerifyTemplate, 0, SGFDxSecurityLevel.SL_NORMAL, matched);

                                            Log.i("result_match",""+result_match);

                                            if (result_match >= 1400)
                                            {
                                                makeAttendance();
                                                Log.i("match_result_match",""+result_match);
                                            }
                                            else
                                            {
                                                Log.i("NOT MATCHED!!", "NOT MATCHED!!");
                                            }
                                        }
                                    }
                                }
                            }
                            catch (JSONException e)
                            {
                                e.printStackTrace();
                            }

                            if (result_match <= 1000)
                            {
                                txt_matchMsg.setText("Unsuccessful");
                                //textToSpeech.speak("Sorry Thumb Not Matched", TextToSpeech.QUEUE_FLUSH, null);
                                txt_matchMsg.setTextColor(Color.RED);
                                textToSpeech.speak("Sorry thumb not matched!!", TextToSpeech.QUEUE_FLUSH, null);
                                txt_success.setText("Sorry thumb not matched!!\n");
                                Log.i("NOT MATCHED!!", "NOT MATCHED!!");

                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        Log.i("start", "start");
                                        txt_matchMsg.setText("");
                                        txt_success.setText("");
                                        //ed_MobNo.setText("");
                                        img_Match.setImageResource(R.drawable.imagefinger);
                                        scannerAction = CommonMethod.ScannerAction.Capture;
                                        StartSyncCapture();
                                        //img_Match.setBackground(getResources().getDrawable(R.drawable.imagefinger));
                                    }
                                }, 3000);
                            }
                        }
                        else
                        {
                            String msg = jsonObj.getString("msg");
                            String message = msg.substring(2, msg.length()-2);
                            ed_MobNo.setText("");
                            txt_matchMsg.setText("Unsuccessful");
                            txt_success.setText(message);
                            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
                            txt_matchMsg.setTextColor(Color.RED);

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Log.i("start", "start");
                                    txt_matchMsg.setText("");
                                    txt_success.setText("");
                                    //ed_MobNo.setText("");
                                    img_Match.setImageResource(R.drawable.imagefinger);
                                    //img_Match.setBackground(getResources().getDrawable(R.drawable.imagefinger));
                                }
                            }, 5000);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        GetDataJSON getDataJSON = new GetDataJSON();
        getDataJSON.execute();
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
                progressDialog = ProgressDialog.show(AttendanceNew.this, "Please wait", "Matching thumb...", true);
                progressDialog.show();
            }

            @Override
            protected String doInBackground(String... params)
            {
                try
                {
                    //String Transurl = ""+url_http+""+Url+"/owner/hrmapi/makeattendance/?";
                    String Transurl = ""+url_http+""+Url+"/owner/hrmapi/makeattendancehitm/?";

                    String query = String.format("empId=%s&signId=%s", URLEncoder.encode(EmpId, "UTF-8"), URLEncoder.encode(Sign_InOut_id, "UTF-8"));
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
                            Toast.makeText(AttendanceNew.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AttendanceNew.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(AttendanceNew.this, "Slow internet connection", Toast.LENGTH_SHORT).show();
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

                if (result.equals(null))
                {
                    progressDialog.dismiss();
                    Toast.makeText(AttendanceNew.this, "Sorry... Slow internet connection", Toast.LENGTH_LONG).show();
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

                        if (result_match >= 1400)
                        {
                            if (Sign_InOut_id.equals("1"))
                            {
                                if (ResponseCode.equals("1"))
                                {
                                    String firstName = jsonObj.getString("firstName");
                                    String lastName = jsonObj.getString("firstName");
                                    String empName = firstName + lastName;

                                    progressDialog.dismiss();
                                    ed_MobNo.setText("");
                                    txt_matchMsg.setText("Successful");
                                    txt_matchMsg.setTextColor(getResources().getColor(R.color.GreenColor));
                                    txt_success.setText(Message);
                                    textToSpeech.speak("Welcome "+firstName, TextToSpeech.QUEUE_FLUSH, null);
                                    Log.i("MATCHED", "MATCHED");
                                    ed_MobNo.setText("");
                                }
                                else
                                {
                                    progressDialog.dismiss();
                                    ed_MobNo.setText("");
                                    txt_matchMsg.setText("Unsuccessful");
                                    txt_success.setText(Message);
                                    textToSpeech.speak(Message, TextToSpeech.QUEUE_FLUSH, null);
                                    txt_matchMsg.setTextColor(Color.RED);
                                }
                            }
                            else if (Sign_InOut_id.equals("2"))
                            {
                                if (ResponseCode.equals("1"))
                                {
                                    String firstName = jsonObj.getString("firstName");
                                    String lastName = jsonObj.getString("firstName");
                                    String empName = firstName + lastName;

                                    progressDialog.dismiss();
                                    ed_MobNo.setText("");
                                    txt_matchMsg.setText("Successful");
                                    txt_matchMsg.setTextColor(getResources().getColor(R.color.GreenColor));
                                    txt_success.setText(Message);
                                    textToSpeech.speak("Bye Bye "+firstName, TextToSpeech.QUEUE_FLUSH, null);
                                    Log.i("MATCHED", "MATCHED");
                                    ed_MobNo.setText("");
                                }
                                else
                                {
                                    progressDialog.dismiss();
                                    ed_MobNo.setText("");
                                    txt_matchMsg.setText("Unsuccessful");
                                    txt_success.setText(Message);
                                    textToSpeech.speak(Message, TextToSpeech.QUEUE_FLUSH, null);
                                    txt_matchMsg.setTextColor(Color.RED);
                                }
                            }
                        }
                        else
                        {
                            progressDialog.dismiss();
                            Log.i("NOT MATCHED!!", "NOT MATCHED!!");
                        }

                        Log.i("Successful", "Successful");

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Log.i("start", "start");
                                txt_matchMsg.setText("");
                                txt_success.setText("");
                                img_Match.setImageResource(R.drawable.imagefinger);
                            }
                        }, 5000);
                    }
                    catch (JSONException e)
                    {
                        progressDialog.dismiss();
                        Toast.makeText(AttendanceNew.this, "Sorry...Json exception", Toast.LENGTH_LONG).show();
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
        //UnInitScanner();
        mfs100.StopCapture();
        Intent intent = new Intent(AttendanceNew.this, MainActivity.class);
        startActivity(intent);
        finish();

    }
}
