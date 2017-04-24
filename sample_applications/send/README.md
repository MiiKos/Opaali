# Send
Send is a command line bash-script modeled after the Content Gateway (CGW) *send* command line utility. It is used for sending SMS messages to mobile terminals.

## Configuration
Before you can use this script for sending messages you need to configure your service/application specific credentials.
Set variables <code>applicationUserName</code> and <code>applicationPassword</code> either in the script itself or, preferrably, in a separate file which is not visible to outsiders. (Anyone who knows your credentials can send messages on your account.)
The default filename for the credentials configuration file is <code>.opaalicredentials</code> in your current working directory.

An example of .opaalicredentials (replace your own username and password):

    applicationUserName="b535b0c5e5ae815cea82db6b32b5095a"
    applicationPassword='1%AMCC?u'

## Sending SMS Messages
With Send you can send SMS messages from a computer to a mobile terminal. To send a message, go to
command prompt and enter the following command on the command line:

`./send.sh parameters`

The _parameters_ are listed in the table below. The parameters in brackets are not required.

|Parameter|Value|Definition|Comment|
|---------|-----|----------|-------|
|-s|Sender number|Your Short Number or other phone number| |
|-r|Recipient number|The recipient’s mobile terminal number| |
|[-t]|Timeout|The delivery notification timeout <p>The time (in seconds) that Send waits for a delivery notification.|*NOT IMPLEMENTED*|
|[-bin \| -text]|Bin \| text|The message content is a binary or text message.|*NOT IMPLEMENTED*|
|[-h]|Host - Provider Server address|Provider Server’s address <p>If Provider Server is installed in another computer, add this parameter to the command.|*NOT IMPLEMENTED*|
|[-nrq]| |The delivery notification request <p>If you wish to receive a notification when a message has been delivered, add this parameter to the command. The notification tells you that a message has been delivered or that it is buffered to wait for a later delivery.|*NOT IMPLEMENTED*|
|[-vp]|Minutes|The validity period of the message in minutes|*NOT IMPLEMENTED*|
|[-ddt]|Minutes / DDMMYYYYhhmm|The delivery time of the message<p>Define the time either in minutes from the time the message is sent or in the format DDMMYYYYhhmm.<p>DD = The day with two digits<p>MM = The month with two digits<p>YYYY = The year with four digits<p>hh = The hour with two digits<p>mm = The minutes with two digits<p>For example, if you wish the message to be sent at 12:33 on October 6th 2000, define the time as 061020001233.|*NOT IMPLEMENTED*|
|[-m]|Message|The message<p>~~Use -m as the last parameter in the command. Send includes everything after the –m in the message.~~|In this implementation -m does not need to be the last parameter and only text upto next parameter is included in the message. |
|[-smart]|Smart messaging port number|The smart messaging destination and originator ports. Remember to use the bin parameter if the content is a binary message.|*NOT IMPLEMENTED*|
|[-udh]|UDH in hex|The User Data Header information in hexadecimal format<p>To find out more about the UDH parameter, see the Nokia Smart Messaging documentation. Do not use this parameter if you do not know how it affects the mobile terminal’s functions.|*NOT IMPLEMENTED*|
|[-c]|Class number|The message class<p>Defines where the received message is stored in the mobile terminal. The classes are as follows:<p>0 = Displays the message (flash) like a cell broadcast, but does not store the message<p>1 = Mobile terminal specific<p>2 = The message is stored on the SIM card.<p>3 = Terminal equipment specific<p>Do not add this parameter to the command if you do not know how the mobile terminal functions with this parameter.|*NOT IMPLEMENTED*|
|[-f]|Filename|The name of the file that contains the message<p>The message is sent from the file specified in the parameter. If you omit the –m or –f parameters from the command, Send sends the text from the console as the message.| |
|[-hex]|Message in hex|The message content in hexadesimal format.<p>For example:<p>-hex 98FAE4E412BC|*NOT IMPLEMENTED*|

Currently only a subset of the parameters are implemented (and some of the parameters are not relevant at all with Opaali API).
