package CgwCompatibility;

import OpaaliAPI.InboundMMSMessage;
import OpaaliAPI.InboundMessage;
import OpaaliAPI.Message;

/*
 * an SMS message in CGW format (as converted from OpaaliAPI format)
 */
public class CgwMessage {


    private String msg = "";
    private String udh = null;        // null when not a binary message
    private String from = null;
    private String to = null;
    private String date = null;
    private String keyword = null;
    private String[] words = null;

    //public String buildSmartMessageString(int port, String msg, boolean bin) {
           /*
            * TODO: to be implemented
            *
            * if smart without bin: msg is text, which will be fragmented and port udh header will be added
            * if smart with bin: msg is hex string (binary)
            * if smart with bin: strip all non hex characters and treat the rest as a binary hex string
            */
        //return null;
     //}


    /*
     * create a CgwMessage from parameters
     */
    public CgwMessage(String to,
                      String from,
                      String date,
                      String udh,
                      String msg) {
        this.to = removeTelPrefix(to);
        this.from = removeTelPrefix(from);
        this.date = date;
        this.udh = udh;
        this.msg = msg;
    }


    /*
     * create a CgwMessage from an InboundMessage
     */
    public CgwMessage(InboundMessage msg) {
        this.to = removeTelPrefix(msg.getDestinationString());
        this.from = removeTelPrefix(msg.getSenderAddress());
        this.msg = msg.getMessage();
        if ((msg.getMessageType() & Message.SMS_BINARY_MESSAGE) != 0) {
            /*
             * TODO: to be implemented
             */
            /*
            InboundSMSBase64Message bMsg = (InboundSMSBase64Message) msg.getMsg();
            BinaryContent b = new BinaryContent(bMsg);
            this.udh = b.udh;
            this.msg = b.payload;
            */
        }
        else if ((msg.getMessageType() & Message.SMS_MMS_MESSAGE) != 0) {
            // MMS Message
            InboundMMSMessage mMsg = (InboundMMSMessage) msg.getMsg();
            if (mMsg.getAttachmentList() != null) {
                // replace subject/message with resourceUrl
                this.msg = msg.getResourceUrl();
            }
        }
        else {
            // SMS text message
            this.udh = null;
        }
    }


    public String getMsg() {
        return msg != null ? msg : "";
    }

    public String getUdh() {
        return udh != null ? udh : "";
    }

    public String getFrom() {
        return from != null ? from : "";
    }

    public String getTo() {
        return to != null ? to : "";
    }

    public String getDate() {
        return date != null ? date : "";
    }

    public String getKeyword() {
        return getWord(1);
    }

    public String getWord(int index) {
        if (index == 0) {
            return msg;
        }
        if (udh == null && words == null) {
            words = parseWords(msg);
            keyword = words[0];
        }
        return (words != null && index > 0 && index <= words.length) ? words[index-1] : "";
    }

    public int getWordCount() {
        if (udh == null && words == null) {
            words = parseWords(msg);
            keyword = words[0];
        }
        return words != null ? words.length : 0;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "from="+getFrom()+"&to="+getTo()+"&msg="+getMsg()+(udh != null ? "&udh="+getUdh() : "");
    }

    // = end of public part ===============================================

    /*
     * split message text into words if message is as hexstring parse it first
     * into text string
     */
    private static String[] parseWords(String msg) {
        // split into words at space or line-delimiter
        String[] words = msg.split(" |\\r\\n");
        return words != null ? words : new String[]{msg};
    }


     private CgwMessage() {}



    /*
     * convert international numbers to required format
     */
    private String removeTelPrefix(String msisdn) {
        return (msisdn != null && msisdn.startsWith("tel:")) ? msisdn.substring("tel:".length()) : msisdn;
    }

}
