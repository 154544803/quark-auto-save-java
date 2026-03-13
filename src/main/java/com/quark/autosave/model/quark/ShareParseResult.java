package com.quark.autosave.model.quark;

public class ShareParseResult {

    private String pwdId;
    private String passcode;
    private String pdirFid = "0";

    public String getPwdId() {
        return pwdId;
    }

    public void setPwdId(String pwdId) {
        this.pwdId = pwdId;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public String getPdirFid() {
        return pdirFid;
    }

    public void setPdirFid(String pdirFid) {
        this.pdirFid = pdirFid;
    }
}
