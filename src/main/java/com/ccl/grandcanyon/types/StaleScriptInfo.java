package com.ccl.grandcanyon.types;

import com.ccl.grandcanyon.types.District;

public class StaleScriptInfo {
    private District district;
    private String adminEmail;
    private boolean adminLoginEnabled;

    public StaleScriptInfo(District d, boolean l, String e){
        district = d;
        adminEmail = e;
        adminLoginEnabled = l;
    }

    public District getDistrict()  {
        return district;
    }

    public void setDistrict(District d){
        district = d;
    }

    public String getAdminEmail()  {
        return adminEmail;
    }
    
    public void setAdminEmail(String e){
        adminEmail = e;
    }

    public boolean getAdminLoginEnabled()  {
        return adminLoginEnabled;
    }
    
    public void setAdminLoginEnabled (boolean e){
        adminLoginEnabled = e;
    }
}