package com.mfsattendance;

/**
 * Created by infogird47 on 07/07/2017.
 */

public class UserDetails_Model {
    String Uid,Cid,Firstname,Lastname,mobile_no,thumb1,thumb2,thumb3,thumb4,Type, Key, Shift;


    public UserDetails_Model() {
    }

    public UserDetails_Model(String thumb1, String thumb2, String thumb3, String thumb4, String attType) {
        this.thumb1 = thumb1;
        this.thumb2 = thumb2;
        this.thumb3 = thumb3;
        this.thumb4 = thumb4;
        this.Type   = attType;
    }

    public UserDetails_Model(String thumb1, String thumb2, String thumb3, String thumb4) {
        this.thumb1 = thumb1;
        this.thumb2 = thumb2;
        this.thumb3 = thumb3;
        this.thumb4 = thumb4;
    }

    public UserDetails_Model(String key, String uid, String cid, String type, String firstname, String lastname, String mobile_no, String thumb1, String thumb2, String thumb3, String thumb4) {
        Key = key;
        Uid = uid;
        Cid = cid;
        Type = type;
        Firstname = firstname;
        Lastname = lastname;
        this.mobile_no = mobile_no;
        this.thumb1 = thumb1;
        this.thumb2 = thumb2;
        this.thumb3 = thumb3;
        this.thumb4 = thumb4;
    }

    public String getPrimaryKey() {
        return Key;
    }

    public void setPrimaryKey(String key) {
        Key = key;
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public String getCid() {
        return Cid;
    }

    public void setCid(String cid) {
        Cid = cid;
    }

    public String getAttType() {
        return Type;
    }

    public void setAttType(String type) {
        Type = type;
    }

    public String getFirstname() {
        return Firstname;
    }

    public void setFirstname(String firstname) {
        Firstname = firstname;
    }

    public String getLastname() {
        return Lastname;
    }

    public void setLastname(String lastname) {
        Lastname = lastname;
    }

    public String getMobile_no() {
        return mobile_no;
    }

    public void setMobile_no(String mobile_no) {
        this.mobile_no = mobile_no;
    }

    public String getShift() {
        return Shift;
    }

    public void setShift(String shift) {
        this.Shift = shift;
    }

    public String getThumb1() {
        return thumb1;
    }

    public void setThumb1(String thumb1) {
        this.thumb1 = thumb1;
    }

    public String getThumb2() {
        return thumb2;
    }

    public void setThumb2(String thumb2) {
        this.thumb2 = thumb2;
    }

    public String getThumb3() {
        return thumb3;
    }

    public void setThumb3(String thumb3) {
        this.thumb3 = thumb3;
    }

    public String getThumb4() {
        return thumb4;
    }

    public void setThumb4(String thumb4) {
        this.thumb4 = thumb4;
    }
}
