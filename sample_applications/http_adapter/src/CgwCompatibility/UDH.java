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

package CgwCompatibility;

import javax.xml.bind.DatatypeConverter;

/*
 * User Data Header for binary SMS messages
 *
 * (practically the only UDH data available in Opaali is port addressing)
 */
public class UDH {

    UDH(int sourcePort, int destinationPort) {
        this.sourcePort =sourcePort;
        this.destinationPort = destinationPort;
    }

    int sourcePort;
    int destinationPort;

    /*
     * generates a UDH as a hex string
     */
    public static String udh(int sourcePort, int destinationPort) {
        String udh="";
        if (sourcePort < 256 && destinationPort < 256) {
            // use 8-bit port numbers
            byte[] val = {0x04, 0x04, 0x02, 0x00, 0x00};
            val[3] = (byte)sourcePort;
            val[4] = (byte)destinationPort;
            udh = DatatypeConverter.printHexBinary(val);
        }
        else {
            // use 16-bit port numbers
            byte[] val = {0x06, 0x05, 0x04, 0x00, 0x00, 0x00, 0x00};
            val[3] = (byte)((sourcePort & 0xff00) >> 8);
            val[4] = (byte)(sourcePort & 0x00ff);
            val[5] = (byte)((destinationPort & 0xff00) >> 8);
            val[6] = (byte)(destinationPort & 0x00ff);
            udh = DatatypeConverter.printHexBinary(val);
        }
        return udh;
    }

    @Override
    public String toString() {
        return udh(sourcePort, destinationPort);
    }

}
