---
layout:     post
title:      "Sending a binary message - Part 3"
subtitle:   "(What? No inboundSMSBinaryMessage!!!)"
date:       2017-08-31 09:10:00
author:     "JPLa"
header-img: "img/InboundBinaryMessage.png"
---
While reading the API Reference (__API_Telia_OMA_OAuth_REST.pdf__, which you can find in the [Resources section]({{site.opaali_portal}}/resources) of the [Opaali Portal]({{site.opaali_portal}})), did you notice that there is no inboundSMSBinaryMessage? Does this mean we cannot _receive_ binary messages?

# Yes and No

The _Opaali API Messaging REST interface_ is implemented based on the __OMA__ (_Open Mobile Alliance_) specification __OMA-TS-REST_NetAPI_Messaging-V1_0-20130709-C__. Looks like they didn't see any use case for receiving MO binary messages.

The current Opaali version does not make a difference between incoming _text_ and _binary_ messages. It tries to show binary content as text so the results depend on the binary content.

With luck you get something that you can decode the original binary message from:
```JSON
      "inboundSMSTextMessage" : {
        "message" : "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000B\f\r\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u0
01A\u001B\u001C\u001D\u001E\u001F !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~¦´"
      }
```
But you might as well get total garbage:
```JSON
      "inboundSMSTextMessage" : {
        "message" : "´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´
+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+
¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢´+¢"
      }
```

# Things don't look hopeless forever

Existing CGW customers do have valid use cases for incoming binary messages (and they are currently in production), so Telia has requested the Opaali Platform vendor _to add extended support for binary messaging that should cover the most common needs_. The API itself cannot be changed because that would break the _OMA compliance_. The plan is that service providers can _for each service_ select between the current functionality and the extended binary message support.

There should be a new inboundMessage type called __inboundSMSBase64Message__ (_the name was chosen assuming that it would not clash with anything OMA would come up with if they ever extend the current standard..._) which would contain the UDH and data in similar format as the current _outboundSMSBinaryMessage_.  

We are expecting to get this extension by the end of this year, but as I have not seen even a prototype implementation yet, I shouldn't go too deep into the details. Things might change, but we are expecting support for
* accessing DCS (Data Coding Scheme)
* accessing PID (Protocol IDentifier)
* getting the segment count of the original message

Some of these will apply to both incoming and outgoing messages, some both for text and binary messages. They will probably be implemented as additional HTTP headers and not inside the JSON body. We should even get support for _message specific_ Validity Period (currently there is support for setting an _application specific_ Validity Period).

----
__NOTICE:__ We don't have an implementation for these extensions yet, so it remains to be seen which of them make to the release. There could be technical reasons making it not possible to get everything as we would like.

----

# But you can't please them all

To be honest, the binary message support in CGW was not that great either. A number of service providers used CGW's ability to tunnel an UCP/EMI low level SMSC interface which gave them more control over all the low level details. Opaali does not offer access to the underlying SMSC API (and the currently used SMPP does not quite provide access to every bit and piece).

So there will be some use cases where there is no migration path from CGW to Opaali. But most of the common binary messaging use cases _should_ be possible, such as:
* smart messaging/sending and receiving vCard and vCalendar messages
* sending a message to be processed by an application on the SIM Card
* (_I'm sure there are more, but my mind is blank at the moment..._)


---- 
(_Some changes to the binary message support in Opaali API are expected later this year. The aim is to improve the functionality and I'll return to this topic when that functionality is available and I have had a chance to test it._)

----


