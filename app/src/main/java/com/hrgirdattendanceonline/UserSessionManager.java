package com.hrgirdattendanceonline;

/**
 * Created by adminsitrator on 08/03/2016.
 */
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class UserSessionManager {

    SharedPreferences pref, pref1;
    Editor editor, editor1;
    Context _context;
    int PRIVATE_MODE = 0;

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String MyPREFERENCES_Url = "MyPrefs_url" ;
    private static final String IS_USER_LOGIN = "IsUserLoggedIn";
    private static final String IS_URL_LOGIN = "IsUrlLoggedIn";
    public static final String KEY_NAME = "";
    public static final String KEY_PASS = "";
    public static final String KEY_URL = "";

    public UserSessionManager(Context context){
        this._context = context;
        pref = _context.getSharedPreferences(MyPREFERENCES, PRIVATE_MODE);
        editor = pref.edit();
        pref1 = _context.getSharedPreferences(MyPREFERENCES_Url, PRIVATE_MODE);
        editor1 = pref1.edit();
    }
   
    public void createUserLoginSession(String name, String pass){
        editor.putBoolean(IS_USER_LOGIN, true);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_PASS, pass);
        editor.commit();
    }
    
    public void createUrlLogin(String url)
    {
        editor1.putBoolean(IS_URL_LOGIN, true);
        editor1.putString(KEY_URL, url);
        editor1.commit();
    }
    
    public void logoutUser()
    {
        editor.clear();
        editor.commit();

        Intent i = new Intent(_context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(i);
    }
   
    public void logout_url()
    {
        editor1.clear();
        editor1.commit();

        Intent i = new Intent(_context, UrlActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(i);
    }
    
    public boolean isUrlPresent()
    {
        return pref1.getBoolean(IS_URL_LOGIN, false);
    }
    
    public boolean isUserLoggedIn()
    {
        return pref.getBoolean(IS_USER_LOGIN, false);
    }
}
