package io.kickflip.sdk.exception;

import java.io.IOException;

/**
 * Kickflip Exception
 */
public class KickflipException extends IOException{
    private String mMessage;
    private int mCode;
    private boolean missingPrivilegesError;

    public KickflipException(){
        mMessage = "An unknown error occurred";
        mCode = 0;
    }

    public KickflipException(String message, int code){
        mMessage = message;
        mCode = code;
    }

    public KickflipException(String message, boolean isMissingPrivilegesError){
        mMessage = message;
        missingPrivilegesError = isMissingPrivilegesError;
    }

    public String getMessage() {
        return mMessage;
    }

    public int getCode() {
        return mCode;
    }

    public boolean isMissingPrivilegesError() {
        return missingPrivilegesError;
    }

    public void setMissingPrivilegesError(boolean missingPrivilegesError) {
        this.missingPrivilegesError = missingPrivilegesError;
    }
}
