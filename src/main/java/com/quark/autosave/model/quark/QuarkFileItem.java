package com.quark.autosave.model.quark;

public class QuarkFileItem {

    private String fid;
    private String shareFidToken;
    private String fileName;
    private boolean dir;
    private String fileNameAfterRename;

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getShareFidToken() {
        return shareFidToken;
    }

    public void setShareFidToken(String shareFidToken) {
        this.shareFidToken = shareFidToken;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isDir() {
        return dir;
    }

    public void setDir(boolean dir) {
        this.dir = dir;
    }

    public String getFileNameAfterRename() {
        return fileNameAfterRename;
    }

    public void setFileNameAfterRename(String fileNameAfterRename) {
        this.fileNameAfterRename = fileNameAfterRename;
    }
}
