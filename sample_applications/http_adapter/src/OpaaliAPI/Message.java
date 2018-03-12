/*
 * Opaali (Telia Operator Service Platform) sample code
 *
 * Copyright(C) 2017 Telia Company
 *
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 *
 * Author: jlasanen
 *
 */


package OpaaliAPI;

public class Message {


    public static final int SMS_UNKNOWN_MESSAGE = 0;
    public static final int SMS_TEXT_MESSAGE = 1;
    public static final int SMS_FLASH_MESSAGE = 3;
    public static final int SMS_BINARY_MESSAGE = 4;
    public static final int SMS_INBOUND_MESSAGE = 8;
    public static final int SMS_OUTBOUND_MESSAGE = 16;
    public static final int SMS_LOGO_MESSAGE = 32;
    public static final int SMS_RINGTONE_MESSAGE = 64;
    public static final int SMS_MMS_MESSAGE = 128;
    public static final int SMS_NOTIFICATION = 256;
    public static final int SMS_LIST = 512;


    public Message(String message) {
        this.message = message;
    }


    public int getMessageType() {
        return messageType();
    }

    @Deprecated
    public int messageType() {
        return messageType;
    }


    public boolean isMessageType(int msgType) {
        return (msgType & messageType) != 0;
    }


    public String getMessage() {
        return message();
    }

    @Deprecated
    public String message() {
        return message;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "             \"message\":\""+message+"\"";
    }


    //= end of public part ====================================================

    protected Message() {message = null;}

    protected int messageType = SMS_UNKNOWN_MESSAGE;

    protected String message;



}
