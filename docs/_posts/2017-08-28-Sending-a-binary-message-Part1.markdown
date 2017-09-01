---
layout:     post
title:      "Sending a binary message - Part 1"
subtitle:   "(like, a vCalendar appointment)"
date:       2017-08-28 10:10:00
author:     "JPLa"
header-img: "img/ApiConsoleExampleBin1.png"
---
In article [Getting Started with Opaali API Part 2]({{ site.baseurl }}{% post_url 2017-07-27-Getting-Started-Part2 %}) we learned how to send a MT _text_ message. Lets now see how to send a _binary_ message. First, we'll revisit the API Reference.

# API Reference

The API Reference used an example of sending a binary message:

![Outbound Message Request]({{ site.url }}{{ site.ref_path }}/img/APIReferenceMTSMS.png)
*API Reference for Outbound Message Requests*

I am not going to use this particular example, as I do not have a handset that can decode this message. In general, if a handset (=phone) cannot decode a message it will just show no evidence of the message having arrived at all (_that is: if you make a mistake in the data of a binary message, it will seem like it was never sent and you will complain about it to [{{site.opaali_tuki}}](mailto:{{site.opaali_tuki}})_).

## More API Reference

![Outbound Message Request/Binary]({{ site.url }}{{ site.ref_path }}/img/APIReferenceMTSMSbinary.png)
*API Reference for Outbound Message Requests, continued*

So it seems we need to construct a UDH (_User Data Header_) and _Base64 encode_ the binary data payload.

# Trust me, I know what I am doing!

If you want to send a _binary text message_ you probably have a reason for doing this and know what kind of data to send and how to construct the message.

For a simple example, lets try sending a calendar appointment in _vCalendar_ format to a compatible handset. 

## Back to the nineties!

Symbian based Nokia smartphones (like my old Nokia E5) supported _Smart Messaging_ (see [Wikipedia: Smart Messaging](https://en.wikipedia.org/wiki/Smart_Messaging)). If you _Google_ for _nokia smart messaging specification_ you can hopefully find the __Smart Messaging Specification - Revision 3.0.0__ from year 2000 (or even the 1.0.0 version from 1997!) which in chapter 3.5 tells us what we need to know:
```text
Calendar information transfer is based on the Versit vCalendar specification. The vCalendar specification defines a format for electronic calendaring and scheduling. This format is suitable to be used as an interchange format between applications or systems, and it is independent of the method used to transport it.
...
The vCalendar reader is listening to on WDP port 9205 decimal (23F5 hexadecimal) or WTLS-secured WDP
port 9207 decimal (23F7 hexadecimal).
```

vCalendar has since been superceded by iCalendar (see [Wikipedia: iCalendar](https://en.wikipedia.org/wiki/ICalendar)) which is quite a similar format. But there is an example we can use in the Smart Messaging document:

![vCalendar example]({{ site.url }}{{ site.ref_path }}/img/SmartMessaging.png)

If we save the example vCalendar message to a file _example.vcal_
```vCal
BEGIN:VCALENDAR
VERSION:1.0
BEGIN:VEVENT
DESCRIPTION:Steering Group meeting in Portal
DTSTART:20000906T100000
DTEND:20000906T120000
END:VEVENT
END:VCALENDAR
```
we can see that the content as hex encoded matches what was in the Smart Messaging document (_at least if you are using Windows which uses CR+LF as line endings_):
```bash
$ xxd example.vcal
00000000: 4245 4749 4e3a 5643 414c 454e 4441 520d  BEGIN:VCALENDAR.
00000010: 0a56 4552 5349 4f4e 3a31 2e30 0d0a 4245  .VERSION:1.0..BE
00000020: 4749 4e3a 5645 5645 4e54 0d0a 4445 5343  GIN:VEVENT..DESC
00000030: 5249 5054 494f 4e3a 5374 6565 7269 6e67  RIPTION:Steering
00000040: 2047 726f 7570 206d 6565 7469 6e67 2069   Group meeting i
00000050: 6e20 506f 7274 616c 0d0a 4454 5354 4152  n Portal..DTSTAR
00000060: 543a 3230 3030 3039 3036 5431 3030 3030  T:20000906T10000
00000070: 300d 0a44 5445 4e44 3a32 3030 3030 3930  0..DTEND:2000090
00000080: 3654 3132 3030 3030 0d0a 454e 443a 5645  6T120000..END:VE
00000090: 5645 4e54 0d0a 454e 443a 5643 414c 454e  VENT..END:VCALEN
000000a0: 4441 520d 0a                             DAR..

$
```
## UDH

The Smart Messaging Specification shows how to send smart messages in an old, text message compatible format. There is a reference to WDP which defines a binary format which uses UDH. Instructions for constructing a UDH can be found e.g. in _3GPP TS 23.040 V5.3.0 (2002-03)
Technical Specification_, but I think the easiest way for us is just to read this Wikipedia article: [Wikipedia: User Data Header](https://en.wikipedia.org/wiki/User_Data_Header). We will use _Application port addressing scheme, 16 bit address_, the target port we already found out in the Smart Messaging document: 9205 decimal (23f5 hexadecimal).

### Our UDH

|offset|value(hex)|Meaning|
|------|----------|-------|
| 0    | 06       | UDH Length |
| 1    | 05       | IEI: Application port addressing scheme, 16 bit address|
| 2    | 04       | IE Length |
| 3    | 23       | target port, first byte |
| 4    | f5       | target port, second byte |
| 5    | 00       | source port, first byte |
| 6    | 00       | source port, second byte |

To construct the _binary message payload_, we will first combine the hex representations of the _UDH_ and the _vCal message_:
 

```bash
$ vcal_udh=06050423f50000

$ vcal_msg=$(cat example.vcal | xxd -p | tr -d "\n")

$
```
(_the -p switch in <code>xxd</code> generates a plain hexdump which is folded into several lines, therefore we use <code>tr</code> to delete the line breaks_)

Now we will concatenate the hex representations of the _UDH_ and the _vCal_ into a single string, decode the string into binary representation using <code>xxd -r -p</code> and base64 encode this using <code>base64</code> (_and still removing any line breaks so that we get a continuous string_).

```bash
$ bin_msg=$(echo -n $vcal_udh$vcal_msg | xxd -r -p | base64 | tr -d "\n")

$ echo $bin_msg
BgUEI/UAAEJFR0lOOlZDQUxFTkRBUg0KVkVSU0lPTjoxLjANCkJFR0lOOlZFVkVOVA0KREVTQ1JJUFRJT046U3RlZXJpbmcgR3JvdXAgbWVldGluZyBpbiBQb3J0YWwNCkRUU1RBUlQ6MjAwMDA5MDZUMTAwMDAwDQpEVEVORDoyMDAwMDkwNlQxMjAwMDANCkVORDpWRVZFTlQNCkVORDpWQ0FMRU5EQVINCg==

```

# API Console (again)

Now we can test whether we managed to encode the binary message correctly by using the API Console, which you can find in the developer portal at the _Application Profile_ of your _send_ application:

![Sending a Binary Message]({{ site.url }}{{ site.ref_path }}/img/ApiConsoleExampleBin1.png)
*Sending a Binary Message using API Console* 

You only need to
* select _outboundSMSBinaryMessage_ as _messageType_
* fill in recipient address
* paste the _base64 encoded string_ we constructed (_as a single line_) into the _message_ field
* fill in the default _senderAddress_ (+358000000000)
* optionally set a meaningful _senderName_

When we click the _Send!_ button we can see that the message was successfully sent:

![Sending a Binary Message: response]({{ site.url }}{{ site.ref_path }}/img/ApiConsoleExampleBin2.png)
*Sending a Binary Message using API Console: response* 

(_Notice that you don't need to authenticate when using the API Console, it does it automatically for you._)

And if your phone supports receiving vCalendar messages you will find it on your phone:

![vCal message received]({{ site.url }}{{ site.ref_path }}/img/vcalmsg.png)

(_Note that this is an old Symbian phone - many of the current smartphones will not show you anything!_)

# Why don't we do it on the command line?

In [Getting Started with Opaali API Part 2]({{ site.baseurl }}{% post_url 2017-07-27-Getting-Started-Part2 %}) we learned how to send a MT _text_ message from the command line. After retrieving a fresh _access_token_ we can construct the send request for a binary message based on the _request_body_ displayed in the API Console output:
```
{
  "outboundMessageRequest": {
    "senderAddress": "tel:+358000000000",
    "senderName": "JPLa",
    "address": [
      "tel:+358403219113"
    ],
    "outboundSMSBinaryMessage": {
      "message": "CwADqgICBQQj9QAAbiBQb3J0YWwNCkRUU1RBUlQ6MjAwMDA5MDZUMTAwMDAwDQpEVEVORDoyMDAwMDkwNlQxMjAwMDANCkVORDpWRVZFTlQNCkVORDpWQ0FMRU5EQVINCg=="
    }
  }
}
```
We need to practice some quotation-trickery so that we can store the binary message into shell variable _bin_msg_ and have it correctly expanded:
```bash
$ echo $bin_msg
BgUEI/UAAEJFR0lOOlZDQUxFTkRBUg0KVkVSU0lPTjoxLjANCkJFR0lOOlZFVkVOVA0KREVTQ1JJUFRJT046U3RlZXJpbmcgR3JvdXAgbWVldGluZyBpbiBQb3J0YWwNCkRUU1RBUlQ6MjAwMDA5MDZUMTAwMDAwDQpEVEVORDoyMDAwMDkwNlQxMjAwMDANCkVORDpWRVZFTlQNCkVORDpWQ0FMRU5EQVINCg==

$ curl -s -k -d '{"outboundMessageRequest":{"senderAddress":"tel:+358000000000","senderName":"JPLa","address":["tel:+358403219113"],"outboundSMSBinaryMessage":{"message":"'"$bin_msg"'"}}}' "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/4d61a20a-f44d-478e-9153-9394e3f7d596"
  }
}
$

```
<br/>
# Stop! This is more than we can handle!

Those of you who have _really_ paid attention may have noticed that our vCalendar message contains more than 160 characters...<br/>
That's right! Opaali API fragments long messages, even _binary messages_, for you (_...or it may be the SMSC API below Opaali API. We don't know and shouldn't even care! It is an implementation detail beyond our control._)

