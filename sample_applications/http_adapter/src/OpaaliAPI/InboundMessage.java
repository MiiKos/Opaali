/*
 * Opaali (Telia Operator Service Platform) sample code
 *
 * Copyright(C) 2018 Telia Company
 *
 * Telia Operator Service Platform and Telia Opaali Portal are trademarks of Telia Company.
 *
 * Author: jlasanen
 *
 */


package OpaaliAPI;


/*
 * InboundMessage
 */
public class InboundMessage extends Message {


    /*
     * Create a SMSText InboundMessage
     */
    public InboundMessage(String destinationString,
                          String senderAddress,
                          String dateTime,
                          String resourceURL,
                          String messageId,
                          InboundSMSTextMessage msg) {
        //this.messageType = SMS_INBOUND_MESSAGE | SMS_TEXT_MESSAGE;
        this.messageType = msg.messageType;
        this.destinationString = destinationString;
        this.senderAddress = senderAddress;
        this.dateTime = dateTime;
        this.resourceURL = resourceURL;
        this.messageId = messageId;
        this.msg = msg;
        this.message = msg.message;
    }


    /*
     * Create a MMS InboundMessage
     */
    public InboundMessage(String destinationString,
                          String senderAddress,
                          String dateTime,
                          String resourceURL,
                          String messageId,
                          InboundMMSMessage msg) {
        //this.messageType = SMS_INBOUND_MESSAGE | SMS_MMS_MESSAGE;
        this.messageType = msg.messageType;
        this.destinationString = destinationString;
        this.senderAddress = senderAddress;
        this.dateTime = dateTime;
        this.resourceURL = resourceURL;
        this.messageId = messageId;
        this.msg = msg;
        this.message = msg.message;
    }


    /*
     * Create a binary InboundMessage
     */
    public InboundMessage(String destinationString,
                          String senderAddress,
                          String dateTime,
                          String resourceURL,
                          String messageId,
                          InboundSMSBase64Message msg) {
        //this.messageType = SMS_INBOUND_MESSAGE | SMS_BINARY_MESSAGE;
        this.messageType = msg.messageType;
        this.destinationString = destinationString;
        this.senderAddress = senderAddress;
        this.dateTime = dateTime;
        this.resourceURL = resourceURL;
        this.messageId = messageId;
        this.msg = msg;
        this.message = msg.message;
    }

    private static final String CR = "\n";

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "{"+CR+
        "    \"inboundMessageNotification\" : {"+CR+
        "        \"inboundMessage\" : {"+CR+
        "            \"destinationAddress\" : \""+destinationString+"\","+CR+
        "            \"senderAddress\" : \""+senderAddress+"\","+CR+
        "            \"dateTime\" : \""+dateTime+"\","+CR+
        "            \"resourceURL\" : \""+resourceURL+"\","+CR+
        "            \"messageId\" : \""+messageId+"\","+CR+
        /*
        "            \"inboundSMSTextMessage\":{" +CR+
        "                \"message\":\""+message+"\"" +CR+
        "             }";
        */
        "            "+msg+CR+
        "        }"+CR+
        "    }"+CR+
        "}";
    }

    public String getDestinationString() {
        return destinationString;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getResourceUrl() {
        return resourceURL;
    }

    public Message getMsg() {
        return msg;
    }

    public String getMessageId() {
        return messageId;
    }

    //= end of public part ====================================================

    protected InboundMessage() {
        message = null;
    }

    protected String destinationString;
    protected String senderAddress;
    protected /*Date*/ String dateTime;
    protected String resourceURL;
    protected String messageId;
    protected Message msg;

}
