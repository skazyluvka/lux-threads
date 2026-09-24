package org.clothingstore.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    private String id;
    @SerializedName("first_name") private String firstName;
    @SerializedName("last_name")  private String lastName;
    private String phone;
    private String address;
    private String role;

    public String getId()        { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName()  { return lastName; }
    public String getPhone()     { return phone; }
    public String getAddress()   { return address; }

    public String getRole()      { return role != null ? role : "user"; }

    public void setFirstName(String v) { firstName = v; }
    public void setLastName(String v)  { lastName  = v; }
    public void setPhone(String v)     { phone     = v; }
    public void setAddress(String v)   { address   = v; }

    public void setRole(String v)      { role = v; }

    public String getFullName() {
        String fn = firstName != null ? firstName : "";
        String ln = lastName  != null ? lastName  : "";
        return (fn + " " + ln).trim();
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    public boolean isSeller() {
        return "seller".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }
}