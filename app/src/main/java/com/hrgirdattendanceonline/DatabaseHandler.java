package com.hrgirdattendanceonline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by infogird47 on 08/07/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper
{
    // Database Version
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_NAME = "attendance_offline_db_";
    private static final String TABLE_User_Details = "User_Details";

    //Columns names
    private static final String KEY_PrimaryKey1 = "Primary_key1";
    private static final String KEY_ID = "User_ID";
    private static final String KEY_FirstNAME = "User_FirstName";
    private static final String KEY_LastNAME = "User_LastName";
    private static final String KEY_PH_NO = "U_Mobile_Number";
    private static final String KEY_CID = "CId";
    private static final String KEY_AttType = "attType";
    private static final String KEY_Thumb1 = "Thumb1";
    private static final String KEY_Thumb2 = "Thumb2";
    private static final String KEY_Thumb3 = "Thumb3";
    private static final String KEY_Thumb4 = "Thumb4";

    public DatabaseHandler(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String CREATE_UserDetails_TABLE = "CREATE TABLE " + TABLE_User_Details + "("
                + KEY_PrimaryKey1 + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_ID + " TEXT," + KEY_FirstNAME + " TEXT,"
                + KEY_LastNAME + " TEXT,"+ KEY_PH_NO + " TEXT,"
                + KEY_CID + " TEXT," + KEY_AttType + " TEXT,"
                + KEY_Thumb1 + " TEXT," + KEY_Thumb2 + " TEXT,"
                + KEY_Thumb3 + " TEXT," + KEY_Thumb4 + " TEXT" + ")";
        db.execSQL(CREATE_UserDetails_TABLE);
        Log.i("data","table userdetails created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Drop older table if existed
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_User_Details);
        // Create tables again
        // onCreate(db);
    }

    public void addContact(UserDetails_Model contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, contact.getUid());
        values.put(KEY_FirstNAME, contact.getFirstname());
        values.put(KEY_LastNAME, contact.getLastname());
        values.put(KEY_PH_NO, contact.getMobile_no());
        values.put(KEY_CID, contact.getCid());
        values.put(KEY_AttType, contact.getAttType());
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());

        db.insert(TABLE_User_Details, null, values);
        db.close();
    }

    public void UpdateContact(UserDetails_Model contact, String uid)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());

        db.update(TABLE_User_Details, values, KEY_ID+"="+uid, null);
        db.close();
    }

    public void UpdateContactAttType(UserDetails_Model contact, String uid)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_Thumb1, contact.getThumb1());
        values.put(KEY_Thumb2, contact.getThumb2());
        values.put(KEY_Thumb3, contact.getThumb3());
        values.put(KEY_Thumb4, contact.getThumb4());
        values.put(KEY_AttType, contact.getAttType());

        db.update(TABLE_User_Details, values, KEY_ID+"="+uid, null);
        db.close();
    }

    public List<UserDetails_Model> getAllContacts()
    {
        List<UserDetails_Model> contactList = new ArrayList<UserDetails_Model>();
        String selectQuery = "SELECT  * FROM " + TABLE_User_Details;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null)
        {
            if (cursor.moveToFirst())
            {
                do
                {
                    UserDetails_Model contact = new UserDetails_Model();
                    contact.setPrimaryKey(cursor.getString(0));
                    contact.setUid(cursor.getString(1));
                    contact.setFirstname(cursor.getString(2));
                    contact.setLastname(cursor.getString(3));
                    contact.setMobile_no(cursor.getString(4));
                    contact.setCid(cursor.getString(5));
                    contact.setAttType(cursor.getString(6));
                    contact.setThumb1(cursor.getString(7));
                    contact.setThumb2(cursor.getString(8));
                    contact.setThumb3(cursor.getString(9));
                    contact.setThumb4(cursor.getString(10));

                    contactList.add(contact);

                } while (cursor.moveToNext());
            }
        }
        return contactList;
    }

    public int getContactsCount()
    {
        String countQuery = "SELECT  * FROM " + TABLE_User_Details;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }

    public int updateContact(UserDetails_Model contact)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ID, contact.getUid());
        values.put(KEY_Thumb1, contact.getThumb1());

        return db.update(TABLE_User_Details, values, KEY_ID + " = ?",
                new String[] { String.valueOf(contact.getUid()) });
    }

    public void deleteContact(String mob)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from "+ TABLE_User_Details +"where" + KEY_PH_NO +"="+mob);
        db.execSQL("DELETE FROM " + TABLE_User_Details + " WHERE " + KEY_PH_NO + "= '" + mob + "'");
        //db.delete(TABLE_User_Details, KEY_PH_NO + "=" + mob, null);
        db.close();
    }

    public void delete_record()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from "+ TABLE_User_Details);
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_User_Details);

        String CREATE_UserDetails_TABLE = "CREATE TABLE " + TABLE_User_Details + "("
                + KEY_PrimaryKey1 + " INTEGER PRIMARY KEY AUTOINCREMENT,"+ KEY_ID + " TEXT," + KEY_FirstNAME + " TEXT,"
                + KEY_LastNAME + " TEXT,"+ KEY_PH_NO + " TEXT,"
                + KEY_CID + " TEXT," + KEY_AttType + " TEXT,"
                + KEY_Thumb1 + " TEXT," + KEY_Thumb2 + " TEXT,"
                + KEY_Thumb3 + " TEXT," + KEY_Thumb4 + " TEXT" + ")";
        db.execSQL(CREATE_UserDetails_TABLE);
        Log.i("data","table userdetails created");
    }

    public UserDetails_Model check_userData()
    {
        String selectQuery = "SELECT * FROM " +TABLE_User_Details+" ORDER BY "+  KEY_PrimaryKey1 +" DESC LIMIT 1";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        UserDetails_Model contact = new UserDetails_Model();

        if(cursor.getCount() ==0)
        {
            Log.i("Empty","Table EMpty");
        }
        else
        {
            cursor.moveToFirst();
            contact.setPrimaryKey(cursor.getString(0));
            Log.i("last entry_db",cursor.getString(0));
        }
        return contact;
    }

    public boolean checkEmpId(String uId)
    {
        String sql = "SELECT "+ KEY_ID+" FROM "+TABLE_User_Details+" WHERE "+ KEY_ID+"="+uId;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(sql,null);

        if(cursor.getCount() > 0)
        {
            cursor.close();
            return true;
        }
        else
        {
            cursor.close();
            return false;
        }
    }
}
