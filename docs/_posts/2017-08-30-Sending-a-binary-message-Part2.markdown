---
layout:     post
title:      "Sending a binary message - Part 2"
subtitle:   "(...diving into protocol dumps)"
date:       2017-08-30 10:10:00
author:     "JPLa"
header-img: "img/BinaryMessage.png"
---
In article [Sending a binary message - Part 1]({{ site.baseurl }}{% post_url 2017-08-28-Sending-a-binary-message-Part1 %}) we learned how to send a MT _binary_ message and also discovered that _something_ fragmented the data without asking our permission. Lets dive deeper into the magical world of protocol dumps and bits and bytes within...

----
__WARNING!__ _Most of you probably don't need to understand what is going on here, so you can safely stop reading this article here. Those of you, who want to know, you know who you are._

----

# How do I know it works?

As the vCalendar event we sent as a binary message arrived at our handset and was displayed as a calendar event by the phone we know that everything worked. _But what if nothing had appeared in the phone? This actually happened more often than not while I was writing this artice._

When there is no sign of a message at the receiving end, you tend to assume that the message never left the sending system. But checking the delivery status might show that the message was successfully delivered, it was just not shown, as the phone could not make sense of the data.

In such a situation you would want to get hold of _the protocol data that was actually received_. For me, the easiest way to do this is to send the message again, but to a GSM or 3G modem connected to a computer running sms software that writes everything from the AT command interface to a log file. (_My test hardware is from the early part of this Millennium..._)
![My Test Setup]({{ site.url }}{{ site.ref_path }}/img/TestSetup.png)
*My test setup* 

#### Sample data from the AT Interface:
```
AT2[/dev/ttyUSB0]: <-- OK
AT2[/dev/ttyUSB0]: <-- +CMT: ,45
AT2[/dev/ttyUSB0]: <-- 07915348500202904407D04A28330C0004718092616571211C0B0003AB0202050423F500000A454E443A5643414C454E4441520D0A
AT2[/dev/ttyUSB0]: --> AT+CNMA^M
```
<br/>

#### This was our original User Data

|UDHLen|IE1|Message content|
|------|---|---------------|
|06|050423F50000|424547494E3A5643...|
|len=6|port addressing|BEGIN:VC...|


#### This is what we received

|UDHLen|IE1|IE2|IE3|Message content|
|------|---|---|---|---------------|
|0E|050423F50000|070103|0003C70201|424547494E3A5643...|
|len=14|port addressing|UDH Source Indicator (the following part is created by SMSC)|concatenation information|BEGIN:VC...|

This looks pretty good. Our original port addressing info and the actual data are the same. The SMSC added concatenation information when it fragmented our message, marked it as added by SMSC by adding the UDH Source Indicator and adjusted the length of the UDH accordingly.

We also received a second fragment with a similar UDH, where just the fragment number was 2 instead of 1. And when sent to a phone the fragments were succesfully combined and the vCalendar event could be seen.

This is what the "Concatenated short messages, 8-bit reference number" IE contains:


|offset|value(hex)|Meaning|
|------|----------|-------|
| 0    | 00       | IEI: Concatenated short messages, 8-bit reference number|
| 1    | 03       | IE Length |
| 2    | C7       | concatenated short message reference number |
| 3    | 02       | maximum number of fragments in the message |
| 4    | 01       | sequence number of the current fragment |

When the receiving terminal has received all the fragments (having the same reference number and all sequence numbers up to maximum) it will reassemble the message and decode it (and usually displays it to the user). If one or more fragments are lost in transit, the received fragments will eventually expire and no message is seen. Also, if the reassembled message cannot be decoded no message is seen (or it tells you that there is a new message but it cannot be viewed).  



# Can we fragment by ourselves if we want to?

You may have an application where you don't want anything mangling your data, like when you are sending firmware updates to a remote device. Can we do the fragmentation by ourselves? Lets try it.

Lets split the vCalendar event text roughly into half and construct UDHs with concatenation and port addressing IEs for both:
```bash
$ vcal_msg1=$(echo -n $vcal_msg | cut -b -160)

$ vcal_msg2=$(echo -n $vcal_msg | cut -b 161-)

$ echo $vcal_msg1 | xxd -r -p
BEGIN:VCALENDAR
VERSION:1.0
BEGIN:VEVENT
DESCRIPTION:Steering Group meeting i
$ echo $vcal_msg2 | xxd -r -p
n Portal
DTSTART:20000906T100000
DTEND:20000906T120000
END:VEVENT
END:VCALENDAR

$ vcal_udh1=0B0003aa0201050423f50000

$ vcal_udh2=0B0003aa0202050423f50000

$ bin_msg1=$(echo -n $vcal_udh1$vcal_msg1 | xxd -r -p | base64 | tr -d "\n")

$ bin_msg2=$(echo -n $vcal_udh2$vcal_msg2 | xxd -r -p | base64 | tr -d "\n")

$
```

## It works?

When we send these two fragments as binary messages to a compatible phone, the calendar event is displayed just like when we let the system fragment it.
If we then send these two binary messages to our test target, we get two fragments with the following UDHs:

|UDHLen|IE1|IE2|Message content|
|------|---|---|---------------|
|0B|0003AA0201|050423F50000|424547494E3A5643...|
|len=11|concatenation information|port addressing|BEGIN:VC...|

|UDHLen|IE1|IE2|Message content|
|------|---|---|---------------|
|0B|0003AA0202|050423F50000|6E20506F7274616C...|
|len=11|concatenation information|port addressing|n Portal...|

Which is exactly what we were sending!

What will happen if we increase the size of the first fragment and decrease the size of the second one? At some point the data and the UDH will be too big to fit in the 140 octets that is reserved for a single message. In my test, when the text in the first fragment was 130 characters long, only the second fragment arrived. No sight of the first one, which was probably fragmented by the system as it was too big to fit in a single message. 

# Can I count on it?

These were results that I got on a particular day and on a particular version of the Opaali platform. The API visible to users does not go into details how messages are fragmented, so there is no promise of any particular behaviour. The detailed behaviour might change in the next software version. _A bit like when you use an undocumented Java library function - there is no guarantee that it would work in a future Java version._

I happen to know that the current version of Opaali API is implemented on top of _SMPP (SMSC) API_. SMPP has support for fragmenting long messages built in. If sometime in the future the underlying SMSC API would change, there is no guarantee that fragmentation would work exactly the same.

If you absolutely must fragment your messages, then test if it works by yourself, but be prepared that what works today might not work tomorrow.

---- 
(_Some changes to the binary message support in Opaali API are expected later this year. The aim is to improve the functionality and I'll return to this topic when that functionality is available and I have had a chance to test it._)

----


