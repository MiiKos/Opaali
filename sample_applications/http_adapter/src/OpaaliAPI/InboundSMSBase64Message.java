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

public class InboundSMSBase64Message extends Message {


    public InboundSMSBase64Message(String msg) {
        this.messageType = SMS_BINARY_MESSAGE | SMS_INBOUND_MESSAGE;
        this.message = msg;
    }

    public InboundSMSBase64Message(String msg,
                                   int dataCoding,
                                   int sourcePort,
                                   int destinationPort) {
        this.messageType = SMS_BINARY_MESSAGE | SMS_INBOUND_MESSAGE;
        this.message = msg;
        this.dataCoding = dataCoding;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
    }


    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return
        "         \"inboundSMSBase64Message\":{" + CR +
        "             \"dataCoding\" :" + dataCoding + CR +
        "             \"sourcePort\" :" + sourcePort + CR +
        "             \"destinationPort\" :" + destinationPort + CR +
        "             \"message\":\""+message+"\"" + CR +
        "         }";
    }

    public int getDataCoding() {
        return dataCoding;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    //= end of public part ====================================================

    protected InboundSMSBase64Message() {
        message = null;
    }

    private static final String CR = "\n";


    protected int dataCoding = 4;
    protected int sourcePort = 0;
    protected int destinationPort = 0;
    protected boolean containsUDH = false;

    public static void main(String[] args) {

        String bmsg = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn+AgYKDhA==";

        System.out.println(new InboundSMSBase64Message("AQI=", 4, 0, 0));
        System.out.println(new InboundSMSBase64Message(bmsg, 4, 1, 255));
    }


}
