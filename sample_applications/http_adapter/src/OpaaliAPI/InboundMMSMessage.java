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

import OpaaliAPI.ApiCall.Attachment;

public class InboundMMSMessage extends Message {


    public InboundMMSMessage(String msg) {
        this.messageType = SMS_MMS_MESSAGE | SMS_INBOUND_MESSAGE;
        this.message = msg;
    }


    public InboundMMSMessage(String subject,
                             String priority,
                             Attachment[] a) {
        this.messageType = SMS_MMS_MESSAGE | SMS_INBOUND_MESSAGE;
        this.message = this.subject = subject;
        this.priority = priority;
        this.attachment_list = a;
    }


    public Attachment[] getAttachmentList() {
        return attachment_list;
    }


    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "         \"inboundMMSMessage\":{" +
        "             \"message\":\""+message+"\"" +
        "         }";
    }

    //= end of public part ====================================================

    protected InboundMMSMessage() {
        message = null;
    }

    protected Attachment[] attachment_list = null;
    //protected String bodyText;
    protected String priority = null;
    protected String subject = null;


}
