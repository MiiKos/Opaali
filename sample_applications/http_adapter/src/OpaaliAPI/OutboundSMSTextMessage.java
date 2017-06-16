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

public class OutboundSMSTextMessage extends Message {
    
    
    public OutboundSMSTextMessage(String msg) {
        this.messageType = SMS_TEXT_MESSAGE | SMS_OUTBOUND_MESSAGE;
        this.message = msg;
    }
    
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return 
        "         \"outboundSMSTextMessage\":{" +
        "             \"message\":\""+message+"\"" +
        "         }";
    }
    
    //= end of public part ====================================================

    protected OutboundSMSTextMessage() {
        message = null;
    }
    

}
