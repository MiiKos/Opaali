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

import javax.xml.bind.DatatypeConverter;

public class OutboundSMSBinaryMessage extends Message {

    /*
     * create binary message from a Base64 encoded string
     */
    public OutboundSMSBinaryMessage(String msg) {
        this.messageType = SMS_BINARY_MESSAGE | SMS_OUTBOUND_MESSAGE;
        this.message = msg;
    }

    /*
     * udh and msg are given as hex strings, udh is optional (may be null)
     */
    public OutboundSMSBinaryMessage(String udh, String msg) {
        this.messageType = SMS_BINARY_MESSAGE | SMS_OUTBOUND_MESSAGE;
        this.message = DatatypeConverter.printBase64Binary(DatatypeConverter.parseHexBinary((udh != null ? udh+msg : msg)));
    }


    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "         \"outboundSMSBinaryMessage\":{" +
        "             \"message\":\""+message+"\"" +
        "         }";
    }

    //= end of public part ====================================================

    protected OutboundSMSBinaryMessage() {
        message = null;
    }


}
