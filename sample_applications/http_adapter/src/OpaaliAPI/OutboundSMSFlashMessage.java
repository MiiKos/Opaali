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

public class OutboundSMSFlashMessage extends Message {


    public OutboundSMSFlashMessage(String msg) {
        this.messageType = SMS_FLASH_MESSAGE | SMS_OUTBOUND_MESSAGE;
        this.message = msg;
    }


    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "         \"outboundSMSFlashMessage\":{" +
        "             \"flashMessage\":\""+message+"\"" +
        "         }";
    }

    //= end of public part ====================================================

    protected OutboundSMSFlashMessage() {
        message = null;
    }


}
