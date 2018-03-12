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

public class InboundSMSTextMessage extends Message {


    public InboundSMSTextMessage(String msg) {
        this.messageType = SMS_TEXT_MESSAGE | SMS_INBOUND_MESSAGE;
        this.message = msg != null ? msg : "";    // replace missing message with an empty message
    }


    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "         \"inboundSMSTextMessage\":{" +
        "             \"message\":\""+message+"\"" +
        "         }";
    }

    //= end of public part ====================================================

    protected InboundSMSTextMessage() {
        message = null;
    }

}
