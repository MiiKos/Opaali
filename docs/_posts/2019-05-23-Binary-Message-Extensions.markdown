---
layout:     post
title:      "Binary Message Extensions"
subtitle:   
date:       2019-05-23 07:00:00
author:     "JPLa"
header-img: "img/ZerosAndOnes.png"
---
This is a summary of the enhanced binary message features which were introduced in the release R4SP2 of Opaali. These are proprietary enhancements to the _OMA Messaging_ specification whose binary message support offers much less than what was possible with Content Gateway (CGW).

# OMA view of Binary Messaging

The OMA standard documentation [OMA-TS-REST_NetAPI_Messaging-V1_0-20130709-C](http://www.openmobilealliance.org/release/MessagingREST/V1_0-20130709-C/OMA-TS-REST_NetAPI_Messaging-V1_0-20130709-C.pdf) states the following: 
```
[...] outboundSMSBinaryMessage is supported in order to facilitate legacy applications that may send SMS in binary format [...]
```
So this standard only supports sending - _not receiving_ - binary messages. Also, details of how the binary data is internally organized is left for the implementation do decide.

As discussed earlier in [Sending a binary message - Part 1]({{ site.baseurl }}{% post_url 2017-08-28-Sending-a-binary-message-Part1 %}) Opaali expects the binary data to contain (as described in another standard __3GPP TS 23.040 V5.3.0__)

* UDH (User Data Header) as binary 
* possible fill bits
* followed by the message data as binary

and the resulting binary data is then _base64 encoded_ into a textual representation.

This was possible to do with Content Gateway (CGW), with the addition of being able to specify the message class too (so you could send a binary message as a _flash message_).

Content Gateway also supported receiving binary messages with the data encoded as a hex string.

# Opaali extensions to Binary Messaging

Based on feedback from some of the customers migrating from Content Gateway to Opaali, a number of extensions to the _OMA Messaging_ standard were implemented in the Opaali Release _R4SP2_. There were some implementation limitations due to the SMPP SMSC-interface used by Opaali, so the binary data format is not exactly identical in sending and receiving. 

To avoid any possible clashing with the OMA standard (should it ever have support for inbound binary messages added), a new message type called  _InboundSMSBase64Message_ was chosen instead of the more obvious InboundSMSBinaryMessage. 

---- 
 
## There are new HTTP Headers...

### ....for Outbound Messages...

* __SMS-DCS__ for setting the _Data Coding Scheme_ value
* __SMS-PID__ for setting the _Protocol IDentifier_ value

You don't need to care about _DCS_ or _PID_ in typical use cases, but using specific values for them you can send messages that
* are processed on the SIM Card instead of being shown on the display
* are OTA (_Over The Air_) configuration settings
* use a specific data encoding

Opaali does not process these _DCS_ and _PID_ values itself, it just passes their values to the underlying SMPP layer. This may lead to unexpected results: if you send a pre-fragmented text message using _OutboundSMSBinaryMessage_ and set SMS-DCS to 0x00 to let the receiving end know that the content is _GSM 7 bit default alphabet_, you should __not__ perform _7-bit packing_ to the content as the SMPP layer will do it!

----

(_This is one of the unfortunate cases where the higher level API cannot hide the details of the lower level implementation._) 

----

(_Also note that you should not add the __SMS-Charset__ HTTP-header to binary messages as it makes no sense in this context and has sometimes led to incorrect billing due to a bug in the system..._)

----

### ...and for Inbound Messages, too!

* __Message-Segment-Count__ will tell you how many fragments were in the original message

This applies to both text and binary messages.
Why would you want to know this? It appears there are applications where use is billed based on the amount of sent and received short message segments. 

There is no feedback through the API how many segments were generated from a particular long message that was sent (_you can see the total of sent segments from_ Partner Reports), but you can make a pretty accurate guess.

For inbound messages guessing is more difficult, because you don't know if there was a _UDH_ attached (which takes space from the actual message) and how long the _UDH_ was. So this HTTP header will tell you how many segments were in the original message when it was received.  



# Inbound Binary Messages

As noted, the OMA Messaging standard does not mention inbound binary messages. By default, Opaali tries to convert received MO messages into text based on the _DCS_ value:
* DCS = 0 -> default alphabet
* DCS = 1 -> IA5/ASCII
* DCS = 3 -> ISO8859
* DCS = 4 -> UTF8
* DCS = 8 -> Unicode
* any other DCS -> UTF8

For binary messages, this works pretty well as long as the content contains only byte values that can be displayed as text or certain control characters like _Carriage Return_ or _Line Feed_, as can be seen from this MO vCalendar message _which I tested shortly after joining the Migration Team_:

#### Inbound vCalendar message as inboundSMSTextMessage

```

  "inboundMessageNotification" : {
    "inboundMessage" : {
      "destinationAddress" : "15593",
      "senderAddress" : "tel:+358402516993",
      "dateTime" : "2016-06-01T12:17:48.000+0000",
      "resourceURL" : "https://api.sonera.fi/production/messaging/v1/messaging/v1/inbound/registrations/ec933cd5-dfa5-407f-a91b-bf7573cbfe8d/messages/516290",
      "messageId" : "516290",
      "inboundSMSTextMessage" : {
        "message" : "BEGIN:VCALENDAR\r\nVERSION:1.0\r\nTZ:+02\r\nDAYLIGHT:TRUE;+03;20160327T030000;20161030T040000;;\r\nDAYLIGHT:TRUE;+03;20170326T030000;20171029T040000;;\r\nDAYLIGHT:TRUE;+03;20180325T030000;20181028T040000;;\r\nDAYLIGHT:TRUE;+03;20190331T030000;20191027T040000;;\r\nDAYLIGHT:TRUE;+03;20200329T030000;20201025T040000;;\r\nBEGIN:VEVENT\r\nUID:040000008200E00074C5B7101A82E00800000000C0432EFF925ECC0100000000000000001000000004285BE1CD74F7439FCBBD1D28AC36AF\r\nSUMMARY:tuntiraportti\r\nDESCRIPTION;ENCODING=QUOTED-PRINTABLE:=0D=0A=0D=0A\r\nDTSTART:20111202T151500Z\r\nDTEND:20111202T151500Z\r\nX-EPOCAGENDAENTRYTYPE:APPOINTMENT\r\nCLASS:PUBLIC\r\nX-SYMBIAN-DTSTAMP:20111202T075253Z\r\nSEQUENCE:0\r\nX-METHOD:NONE\r\nATTENDEE;ROLE=ORGANIZER;STATUS=NEEDS ACTION;RSVP=NO;EXPECT=FYI;X-CN=Lasanen, Jukka-Pekka;ENCODING=QUOTED-PRINTABLE:=\r\njukka-pekka.lasanen=40logica.com\r\nRRULE:W1 FR #0\r\nLAST-MODIFIED:20111202T075228Z\r\nPRIORITY:0\r\nX-SYMBIAN-LUID:194\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
      }
    }
  }

```

Based on the message content you can guess that this is a vCalendar appointment, although the UDH which would help in content detection is missing.

With some other binary content - what you get might be total garbage (see [Sending a binary message - Part 3]({{ site.baseurl }}{% post_url 2017-08-31-Sending-a-binary-message-Part3 %}) ).

----

## Deliver as Binary Policy

To enable receiving _InboundSMSBase64Message_ you need to have _"Deliver as Binary Policy"_ enabled for your application. You can select this when creating a new application or ask _Opaali Support_ to enable it for your existing application.

If the _DCS_ value of the inbound message is 0, 1 or 3 the message is still delivered as _inboundSMSTextMessage_, but if it is something else it will be delivered as _inboundSMSBase64Message_. 

## InboundSMSBase64Message

_InboundSMSBase64Message_ contains four elements:

* __datacoding__ is the _data_coding_ value copied from the received _SMPP PDU_
* __sourcePort__ is the _source_port_ value from the received _SMPP PDU_  
* __destinationPort__  is the _destination_port_ value from the received _SMPP PDU_
* __message__ is the _message_payload_ from the _SMPP PDU_ as base64 encoded

You might expect the _UDH_ to be at the beginning of the _message_ field, but it is not available from the _SMPP layer_. Some of the information that would normally be in the UDH (like the _source_ and _destination ports_) is available as separate parameters. 

Here is a vCalendar appointment received when _Deliver as Binary Policy_ is enabled: 

#### Inbound vCalendar message as SMS Binary

```JSON
"inboundMessageNotification" : {
    "inboundMessage" : {
      "destinationAddress" : "91589",
      "senderAddress" : "tel:+358402565797",
      "dateTime" : "2019-05-21T13:12:38.000+0000",
      "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/inbound/registrations/bf021072-7583-4478-a9f1-25989bcbc6d1/messages/1829148492",
      "messageId" : "1829148492",
      "inboundSMSBase64Message" : {
        "dataCoding" : 4,
        "sourcePort" : 0,
        "destinationPort" : 9205,
        "message" : "QkVHSU46VkNBTEVOREFSDQpWRVJTSU9OOjEuMA0KVFo6KzAyDQpEQVlMSUdIVDpUUlVFOyswMzsyMDE5MDMzMVQwMzAwMDA7MjAxOTEwMjdUMDQwMDAwOzsNCkRBWUxJR0hUOlRSVUU7KzAzOzIwMjAwMzI5VDA
zMDAwMDsyMDIwMTAyNVQwNDAwMDA7Ow0KREFZTElHSFQ6VFJVRTsrMDM7MjAyMTAzMjhUMDMwMDAwOzIwMjExMDMxVDA0MDAwMDs7DQpEQVlMSUdIVDpUUlVFOyswMzsyMDIyMDMyN1QwMzAwMDA7MjAyMjEwMzBUMDQwMDAwOzsNCkRBWUx
JR0hUOlRSVUU7KzAzOzIwMjMwMzI2VDAzMDAwMDsyMDIzMTAyOVQwNDAwMDA7Ow0KQkVHSU46VkVWRU5UDQpVSUQ6MDQwMDAwMDA4MjAwRTAwMDc0QzVCNzEwMUE4MkUwMDgwMDAwMDAwMEMwNDMyRUZGOTI1RUNDMDEwMDAwMDAwMDAwMDA
wMDAwMTAwMDAwMDAwNDI4NUJFMUNENzRGNzQzOUZDQkJEMUQyOEFDMzZBRg0KU1VNTUFSWTp0dW50aXJhcG9ydHRpDQpERVNDUklQVElPTjtFTkNPRElORz1RVU9URUQtUFJJTlRBQkxFOj0wRD0wQT0wRD0wQQ0KRFRTVEFSVDoyMDExMTI
wMlQxNTE1MDBaDQpEVEVORDoyMDExMTIwMlQxNTE1MDBaDQpYLUVQT0NBR0VOREFFTlRSWVRZUEU6QVBQT0lOVE1FTlQNCkNMQVNTOlBVQkxJQw0KWC1TWU1CSUFOLURUU1RBTVA6MjAxMTEyMDJUMDc1MjUzWg0KU0VRVUVOQ0U6MA0KWC1
NRVRIT0Q6Tk9ORQ0KQVRURU5ERUU7Uk9MRT1PUkdBTklaRVI7U1RBVFVTPU5FRURTIEFDVElPTjtSU1ZQPU5PO0VYUEVDVD1GWUk7WC1DTj1MYXNhbmVuLCBKdWtrYS1QZWtrYTtFTkNPRElORz1RVU9URUQtUFJJTlRBQkxFOj0NCmp1a2t
hLXBla2thLmxhc2FuZW49NDBsb2dpY2EuY29tDQpSUlVMRTpXMSBGUiAjMA0KTEFTVC1NT0RJRklFRDoyMDExMTIwMlQwNzUyMjhaDQpQUklPUklUWTowDQpYLVNZTUJJQU4tTFVJRDoxOTQNCkVORDpWRVZFTlQNCkVORDpWQ0FMRU5EQVI
NCg=="
      }
    }
  }
```

The destinationPort value _9205_ tells that this is a calendar appointment.

If we _base64 decode_ the message field content we get the vCalendar event as text:

```vCal
BEGIN:VCALENDAR
VERSION:1.0
TZ:+02
DAYLIGHT:TRUE;+03;20190331T030000;20191027T040000;;
DAYLIGHT:TRUE;+03;20200329T030000;20201025T040000;;
DAYLIGHT:TRUE;+03;20210328T030000;20211031T040000;;
DAYLIGHT:TRUE;+03;20220327T030000;20221030T040000;;
DAYLIGHT:TRUE;+03;20230326T030000;20231029T040000;;
BEGIN:VEVENT
UID:040000008200E00074C5B7101A82E00800000000C0432EFF925ECC0100000000000000001000000004285BE1CD74F7439FCBBD1D28AC36AF
SUMMARY:tuntiraportti
DESCRIPTION;ENCODING=QUOTED-PRINTABLE:=0D=0A=0D=0A
DTSTART:20111202T151500Z
DTEND:20111202T151500Z
X-EPOCAGENDAENTRYTYPE:APPOINTMENT
CLASS:PUBLIC
X-SYMBIAN-DTSTAMP:20111202T075253Z
SEQUENCE:0
X-METHOD:NONE
ATTENDEE;ROLE=ORGANIZER;STATUS=NEEDS ACTION;RSVP=NO;EXPECT=FYI;X-CN=Lasanen, Jukka-Pekka;ENCODING=QUOTED-PRINTABLE:=
jukka-pekka.lasanen=40logica.com
RRULE:W1 FR #0
LAST-MODIFIED:20111202T075228Z
PRIORITY:0
X-SYMBIAN-LUID:194
END:VEVENT
END:VCALENDAR

```

### Inbound Message HTTP Headers

When the binary message above was received by an application the following HTTP headers were included:

```JSON
'content-type': 'application/json; charset=UTF-8'
accept: 'application/json',
'message-segment-count': '8'
authorization: 'Basic bWU6anVzdE1l'
host: '52.210.250.36:8080'
'transfer-encoding': 'chunked' 
```

We can see that the original message (_sent from an old Symbian phone_) was delivered using 8 segments.
The _authorization_ header, when _base64 decoded_ contained the credentials I had configured for the Push Notification in Opaali:

```
$ echo bWU6anVzdE1l | base64 -d
me:justMe
```

----
I think now we have binary SMS messages pretty much covered.
