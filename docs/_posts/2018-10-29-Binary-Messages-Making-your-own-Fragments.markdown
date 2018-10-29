---
layout:     post
title:      "Tuning Binary Messages"
subtitle:   "Fragmenting sent messages on your own"
date:       2018-10-29 07:00:00
author:     "JPLa"
header-img: "img/LoremIpsum.jpg"
---
Here I present an actual customer case regarding _sending prefragmented binary messages_. Most of you readers may want to skip reading this, as this is a very specific situation only very few of you will encounter. However, if you are sending binary messages there might be something useful for you to know here.

If you do choose to read this post, you may want to refresh your memory by visiting the earlier article [Sending a binary message - Part 2]({{ site.baseurl }}{% post_url 2017-08-30-Sending-a-binary-message-Part2 %}) which shows how binary messages are fragmented.

# The Problem

One of the Opaali customers had the need to send their MT messages in 140 octet _fragments_ (because that is how their existing application functioned). With _Opaali_, it didn't work quite like they expected...

Normally you can ignore fragmenting (both for text and binary data) because _Opaali does it for you_. You can include up to 1600 characters in an outgoing message when using __OpaaliAPI__. It is magically divided into 160 character (_well, actually less than 160, packed into 140 octet_) fragments which the receiver can reassemble into a single message. You will be charged for each sent fragment (_so this is not a way to save in costs_).
  
__Warning:__ This is going to be a very long, technical and boring story, which only the nerdiest of the nerds will appreciate (_but I find it interesting..._).

----
## The Original long message

The customer provided this sample for the original long message:
```
Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,when an unknown printer took a galley of type and scrambled it to make a type specimen book. åäö@ÅÄÖ
```
The message is 252 characters long, so it needs to be fragmented into two fragments.
If you want the fragments to be automatically reassembled in the correct order at the receiving end, you will need to add a _User Data Header_ (__UDH__) with _concatenation information_ and send the message as a binary message.

The most optimal UDH would only contain UDHLen and the concatenation Information Element (__IE__) like this:

|UDHLen|IE1|
|------|---|
|05|0003A50201|
|len=5|concatenation information|

This requires 6 octets (~bytes) of the 140 available, which leaves 134 octets for the message content. If the message text contains only characters that can be converted into _GSM 7 bit Default Alphabet_, we can pack 153 7-bit characters into the remaining 134 octets.
And this is what the customer's existing software did, it produced the following two fragments (represented as _hexstrings_ here):

```
$ hexstring1="050003a502014c6f72656d20497073756d2069732073696d706c792064756d6d792074657874206f6620746865207072696e74696e6720616e64207479706573657474696e6720696e6475737472792e204c6f72656d20497073756d20686173206265656e2074686520696e6475737472792773207374616e646172642064756d6d79207465787420657665722073696e6365207468652031353030732c77"

$ hexstring2="050003a5020268656e20616e20756e6b6e6f776e207072696e74657220746f6f6b20612067616c6c6579206f66207479706520616e6420736372616d626c656420697420746f206d616b65206120747970652073706563696d656e20626f6f6b2e20e5e4f640c5c4d6"

```
To see where they split the message, let's convert the hexstrings into text (...and take another hexdump so that we can see the _unprintable characters_ too):
```
$ echo $hexstring1 | xxd -r -p | xxd
00000000: 0500 03a5 0201 4c6f 7265 6d20 4970 7375  ......Lorem Ipsu
00000010: 6d20 6973 2073 696d 706c 7920 6475 6d6d  m is simply dumm
00000020: 7920 7465 7874 206f 6620 7468 6520 7072  y text of the pr
00000030: 696e 7469 6e67 2061 6e64 2074 7970 6573  inting and types
00000040: 6574 7469 6e67 2069 6e64 7573 7472 792e  etting industry.
00000050: 204c 6f72 656d 2049 7073 756d 2068 6173   Lorem Ipsum has
00000060: 2062 6565 6e20 7468 6520 696e 6475 7374   been the indust
00000070: 7279 2773 2073 7461 6e64 6172 6420 6475  ry's standard du
00000080: 6d6d 7920 7465 7874 2065 7665 7220 7369  mmy text ever si
00000090: 6e63 6520 7468 6520 3135 3030 732c 77    nce the 1500s,w

$ echo $hexstring2 | xxd -r -p | xxd
00000000: 0500 03a5 0202 6865 6e20 616e 2075 6e6b  ......hen an unk
00000010: 6e6f 776e 2070 7269 6e74 6572 2074 6f6f  nown printer too
00000020: 6b20 6120 6761 6c6c 6579 206f 6620 7479  k a galley of ty
00000030: 7065 2061 6e64 2073 6372 616d 626c 6564  pe and scrambled
00000040: 2069 7420 746f 206d 616b 6520 6120 7479   it to make a ty
00000050: 7065 2073 7065 6369 6d65 6e20 626f 6f6b  pe specimen book
00000060: 2e20 e5e4 f640 c5c4 d6                   . ...@...

$

```
We can see the six bytes of the UDH at the beginning, followed by the text, split after the 153rd character. The letters åäö and ÅÄÖ are encoded in Latin9 which OpaaliAPI supports (_but my terminal does not_).

----

### What could possibly go wrong?
So let's try to send these two fragments as binary messages. First we need to convert the hexstrings into binary data which we then base64-encode (_as that is how OpaaliAPI wants to have it_):

```bash
$ payload1=$(echo -n $hexstring1 |xxd -r -p| base64 | tr -d '\n')

$ echo $payload1
BQADpQIBTG9yZW0gSXBzdW0gaXMgc2ltcGx5IGR1bW15IHRleHQgb2YgdGhlIHByaW50aW5nIGFuZCB0eXBlc2V0dGluZyBpbmR1c3RyeS4gTG9yZW0gSXBzdW0gaGFzIGJlZW4gdGhlIGluZHVzdHJ5J3Mgc3RhbmRhcmQgZHVtbXkgdGV4dCBldmVyIHNpbmNlIHRoZSAxNTAwcyx3

$ payload2=$(echo -n $hexstring2 |xxd -r -p| base64 | tr -d '\n')

$ echo $payload2
BQADpQICaGVuIGFuIHVua25vd24gcHJpbnRlciB0b29rIGEgZ2FsbGV5IG9mIHR5cGUgYW5kIHNjcmFtYmxlZCBpdCB0byBtYWtlIGEgdHlwZSBzcGVjaW1lbiBib29rLiDl5PZAxcTW

$

```
And let's send them as _outboundSMSBinaryMessages_:
```
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload1"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/15a56bfa-39c0-4841-9e54-d105145c93ea"
  }
}

$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload2"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/7eaac0aa-a1b5-4fd0-bb9e-7092e318943a"
  }
}

$
```
Looking good -- both fragments were successfully sent. If I request delivery status using the _resourceURL_ that was returned it will return "DeliveredToNetwork" for both. So what did I get at the receiving end:
```
2018-10-24 17:30:52 [17372] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,121
2018-10-24 17:30:52 [17372] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804405811985F900048101427103152169050003A5020268656E20616E20756E6B6E6F776E207072696E74657220746F6F6B20612067616C6C6579206F66207479706520616E6420736372616D626C656420697420746F206D616B65206120747970652073706563696D656E20626F6F6B2E20E5E4F640C5C4D6
2018-10-24 17:30:52 [17372] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-24 17:30:52 [17372] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-24 17:30:52 [17372] [6] DEBUG: AT2[/dev/com5]: User data length read as (105)
2018-10-24 17:30:52 [17372] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=99 udhi=1 udhlen=5 udh=''
```
Only the second fragment???? No trace of the first fragment!!!!
If I had sent the message to my phone, nothing would be displayed (_as it will wait for the missing fragment, which doesn't seem to ever come_).

----
### So, what _did_ go wrong?

We can see that the fragment we did get was much shorter than 140 octets, so there was no need to _refragment_ it. But where did the first fragment disappear? If I had explicitly requested delivery report notifications by adding a _receiptRequest_ element to the requests, I would get "DeliveryImpossible" for the first fragment and "DeliveredToTerminal" for the fragment that was...actually _delivered to the terminal_. (_So if you really need to know if the message was delivered you should explicitly request for a delivery report. By default the status_ after delivering to the network _is not requested from SMSC._)

If you look at the hexdump of _hexstring1_ earlier in this article you can see that it contains 159 bytes (6 for UDH and 153 for the actual text). 
Now this would fit into 140 octets, _but only if the text is packed as 7-bit characters_! Binary messages are always 8-bit data, so there is actually 19 bytes _too much to fit_ -- the fragment would need to be _(re)fragmented into two fragments_!

Normally _Opaali_ (_or SMPP layer, I don't know which_) will fragment also binary messages for you. Maybe it does it here also, as we initially get a _DeliveredToNetwork_ but eventually it will be updated to _DeliveryImpossible_. This would hint that the message was submitted to SMPP but it was rejected by some of the layers below.

It would be possible to refragment this fragment, but there already being a _concatenation IE_ makes it a bit tricky. The _concatenation IE_ contains the count of all fragments, so this would need to be updated when the count of fragments increases. But we already have a fragment number 2 of 2 (_it was the one that was actually delivered_) so the fragment number and count would need to be updated for _all of the fragments_ that belong together. This would mean that the lower layer would need to buffer a potentially large number of fragments (_or otherwise rewrite the UDHs on the fly_) and I don't think they would bother to do this. So too long fragments will be dropped and the short enough ones will go through.

I can also see from the Developer Portal _Partner Reports_ that __I was charged for 3 messages even though only one of them reached the phone and none of them were actually displayed on the phone!__

![Partner Report]({{ site.url }}{{ site.ref_path }}/img/BinaryDeliveryReport.png)
*Partner Report (CDR Count = count of charged fragments)* 

----
## Doing it the "standard" way

At this point we might check how Opaali fragments this text, if it is sent as text (_because that's what it is..._).


Because OpaaliAPI accepts long messages which it will fragment by itself, we can send this as a normal outbound text message (_we only need to replace some of the special characters with their equivalent unicode escape codes so that we will not get a "BAD REQUEST" response_).

```
$ originalmessage="Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry\u0027s standard dummy text eve
r since the 1500s,when an unknown printer took a galley of type and scrambled it to make a type specimen book. \u00E5\u00E4\u00F6\u0040\u00C5\u00C4\u00D6"

$ printf "$originalmessage"
Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,when an unknown printer took a galley of type and scrambled it to make a type specimen book. åäö@ÅÄÖ

$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSTextMessage":{"message": "'"$originalmessage"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/974c0aee-48d8-4132-bc59-996eb3b45d75"
  }
}

$
```

What we get at the receiving end is this:
```
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,154
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804005811985F90000810142514544219D080701030003C4020180F99697DBA0247C5E6F83D273D03CDD86B3F32072BDDDCE83E8653C1DF43683E8E832082E4FBBE969F71914769341F43CBC3C2FD3E969F719947693EB73BA3CEF0231DFF2721B9484CFEB6D103A3C0789CB6537888E2E83D26E727D4E97E74F73D09C1E7693C37232885C6FB7F3207A194F0795ED6539689E768FCB203ABA0C02
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: User data length read as (157)
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=148 udhi=1 udhlen=8 udh='
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,119
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804405811985F900008101425145442175080701030003C40202205683C1E6AC3BBAEC0685DDA0BA7BED7EDFDD20B83CEDA697E520FAFBBD068541E7309B5DCE83DE66103D0F2F83C26E32683E9687DB6276990C4AD341F437A81D5E974161103D0F2F83E6F0F238DD2EBB41E2F77BED023DF67C8063CB05
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: User data length read as (117)
2018-10-24 15:54:45 [17372] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=108 udhi=1 udhlen=8 udh='

```
The message has been split into two fragments, the user data being:

```
$ received1="080701030003C4020180F99697DBA0247C5E6F83D273D03CDD86B3F32072BDDDCE83E8653C1DF43683E8E832082E4FBBE969F71914769341F43CBC3C2FD3E969F719947693EB73BA3CEF0231DFF2721B9484CFEB6D103A3C0789CB6537888E2E83D26E727D4E97E74F73D09C1E7693C37232885C6FB7F3207A194F0795ED6539689E768FCB203ABA0C02"

$ received2="080701030003C40202205683C1E6AC3BBAEC0685DDA0BA7BED7EDFDD20B83CEDA697E520FAFBBD068541E7309B5DCE83DE66103D0F2F83C26E32683E9687DB6276990C4AD341F437A81D5E974161103D0F2F83E6F0F238DD2EBB41E2F77BED023DF67C8063CB05"

$
```
If we try to display this hex data as text, we get garbage:

```
$ echo -n $received1 | xxd -r -p | xxd
00000000: 0807 0103 0003 c402 0180 f996 97db a024  ...............$
00000010: 7c5e 6f83 d273 d03c dd86 b3f3 2072 bddd  |^o..s.<.... r..
00000020: ce83 e865 3c1d f436 83e8 e832 082e 4fbb  ...e<..6...2..O.
00000030: e969 f719 1476 9341 f43c bc3c 2fd3 e969  .i...v.A.<.</..i
00000040: f719 9476 93eb 73ba 3cef 0231 dff2 721b  ...v..s.<..1..r.
00000050: 9484 cfeb 6d10 3a3c 0789 cb65 3788 8e2e  ....m.:<...e7...
00000060: 83d2 6e72 7d4e 97e7 4f73 d09c 1e76 93c3  ..nr}N..Os...v..
00000070: 7232 885c 6fb7 f320 7a19 4f07 95ed 6539  r2.\o.. z.O...e9
00000080: 689e 768f cb20 3aba 0c02                 h.v.. :...

$ echo -n $received2 | xxd -r -p | xxd
00000000: 0807 0103 0003 c402 0220 5683 c1e6 ac3b  ......... V....;
00000010: baec 0685 dda0 ba7b ed7e dfdd 20b8 3ced  .......{.~.. .<.
00000020: a697 e520 fafb bd06 8541 e730 9b5d ce83  ... .....A.0.]..
00000030: de66 103d 0f2f 83c2 6e32 683e 9687 db62  .f.=./..n2h>...b
00000040: 7699 0c4a d341 f437 a81d 5e97 4161 103d  v..J.A.7..^.Aa.=
00000050: 0f2f 83e6 f0f2 38dd 2ebb 41e2 f77b ed02  ./....8...A..{..
00000060: 3df6 7c80 63cb 05                        =.|.c..

$

```

That is because the text part has been 7-bit packed. You can see it from the DCS value which is 0x00 (which means "GSM 7 bit default alphabet" which implies it is 7-bit packed text ) instead of 0x04 for binary (_see  3GPP TS 23.038 V5.0.0_). 

This is in the PDU data preceding the User Data:

|...|TP-OA|TP-PID|TP-DCS|TP-SCTS|...|
|---|-----|------|------|-------|---|
|   |05811985F9|00|00|81014251454421|   |


So, we need to reverse the 7-bit packing:

```
$ ./packto7bits.sh -r $(echo -n $received1) | xxd -r -p | xxd
00000000: 080e 0418 0060 0062 0202 004c 6f72 656d  .....`.b...Lorem
00000010: 2049 7073 756d 2069 7320 7369 6d70 6c79   Ipsum is simply
00000020: 2064 756d 6d79 2074 6578 7420 6f66 2074   dummy text of t
00000030: 6865 2070 7269 6e74 696e 6720 616e 6420  he printing and
00000040: 7479 7065 7365 7474 696e 6720 696e 6475  typesetting indu
00000050: 7374 7279 2e20 4c6f 7265 6d20 4970 7375  stry. Lorem Ipsu
00000060: 6d20 6861 7320 6265 656e 2074 6865 2069  m has been the i
00000070: 6e64 7573 7472 7927 7320 7374 616e 6461  ndustry's standa
00000080: 7264 2064 756d 6d79 2074 6578 7420 6576  rd dummy text ev
00000090: 6572 2073 696e 6365 2074 6865 20         er since the

$ ./packto7bits.sh -r $(echo -n $received2) | xxd -r -p | xxd
00000000: 080e 0418 0060 0062 0204 0031 3530 3073  .....`.b...1500s
00000010: 2c77 6865 6e20 616e 2075 6e6b 6e6f 776e  ,when an unknown
00000020: 2070 7269 6e74 6572 2074 6f6f 6b20 6120   printer took a
00000030: 6761 6c6c 6579 206f 6620 7479 7065 2061  galley of type a
00000040: 6e64 2073 6372 616d 626c 6564 2069 7420  nd scrambled it
00000050: 746f 206d 616b 6520 6120 7479 7065 2073  to make a type s
00000060: 7065 6369 6d65 6e20 626f 6f6b 2e20 0f7b  pecimen book. .{
00000070: 7c00 0e5b 5c                             |..[\

$

```

_(Yes -- I wrote a script called packto7bits.sh to do the packing/unpacking. More about it some other time, perhaps.)_

The message has been split into two fragments, at a point where the latter fragment starts with:
```
1500s,when an unknown printer took a galley of type and scrambled i...
```
And for anyone interested, the UDH is __080701030003C40202__:

|UDHLen|IE1|IE2|Message content|
|------|---|---|---------------|
|08|070103|0003C40202|205683C1E6AC3BBAEC0685DDA0BA7BED7EDFDD...|
|len=8|UDH Source Indicator (the following part is created by SMSC)|concatenation information|1500s,when an unknown...|

You may also notice that the characters åäö@ÅÄÖ have been converted to their character codes in _GSM 7 bit Default Alphabet_.

You can see that this is not the optimal UDH we discussed earlier. It is 3 bytes longer, because elements added by the system are preceded by a _UDH Source Indicator_. Due to this the text is split already after the 146th character.





  
----
### How it _should_ work (_no, this doesn't work either_)

If the _application_ would do the _7-bit packing_ before submitting the message to _OpaaliAPI_ the fragment length would be 140 octets and there should be no problem to deliver it. 

![7-bit packing]({{ site.url }}{{ site.ref_path }}/img/bitpacking.png)
*Packing 8 7-bit characters into 7 bytes* 

The _diagram above_ shows the idea of 7-bit packing. (_Implementing this into a program is a bit more complicated, but I'll spare you from the details this time._)


To send a fragment containing 7-bit packed text we need to place the 8-bit UDH at the beginning followed by the packed data. 
The standard __3GPP TS 23.040 V5.3.0__ says that the 7-bit data needs to start at a _septet boundary_ and _fill bits_ may need to be inserted between the 8-bit data of the UDH and the 7-bit data of the (packed) text. (_BTW: packing is not the same as compression, which is also mentioned in the standard._)

One way to do this is to insert enough dummy nul characters (as 00 hex) in front of the text to be packed, so that when you pack the whole hexstring there is just enough zero space at the beginning that the UDH bytes will fit in (_maybe it is easier to explain by example..._).

* the _UDH_ we want has 6 bytes
* from our _packing diagram_ we see that 6 bytes (+1 fill bit) is the result of packing 7 characters
* so we will _cut off_ the 6 byte UDH from our _hexstring_ and replace it with a hexstring representing 7 nuls: __00000000000000__

----

##### extract udh1 from hexstring1

```
$ udh1=$(echo $hexstring1 | cut -b-12)

$ echo $udh1
050003a50201

$
```



##### extract the text part from hexstring1

```
$ hextext1=$(echo $hexstring1 | cut -b13-)

$ echo $hextext1
4c6f72656d20497073756d2069732073696d706c792064756d6d792074657874206f6620746865207072696e74696e6720616e64207479706573657474696e6720696e6475737472792e204c6f72656d20497073756d20686173206265656e2074686520696e6475737472792773207374616e646172642064756d6d79207465787420657665722073696e6365207468652031353030732c77

$
```

##### add a dummy header in front of hextext and 7-bit pack the whole string

```
$ hextext1packed=$(./packto7bits.sh $(echo 00000000000000$hextext1))

$ echo $hextext1packed
000000000000986f79b90d4ac2e7f536283d07cdd36d383b0f22d7dbed3c885ec6d3416f33888e2e83e0f2b49b9e769f4161371944cfc3cbf3329d9e769f416937b93ea7cbf32e10f32d2fb74149f8bcde06a1c37390b85c7683e8e83228ed26d7e77479fe3407cde96137392c2783c8f5763b0fa297f17450d95e9683e669f7b80ca2a3cba0580d069bb3ee

$
```

##### combine the UDH and packed hextext (with its fill bits included)

```
$ hexstring1packed=$(echo "$udh1"$(echo $hextext1packed | cut -b13-))

$ echo $hexstring1packed
050003a50201986f79b90d4ac2e7f536283d07cdd36d383b0f22d7dbed3c885ec6d3416f33888e2e83e0f2b49b9e769f4161371944cfc3cbf3329d9e769f416937b93ea7cbf32e10f32d2fb74149f8bcde06a1c37390b85c7683e8e83228ed26d7e77479fe3407cde96137392c2783c8f5763b0fa297f17450d95e9683e669f7b80ca2a3cba0580d069bb3ee

$
```
##### verify that the result can be unpacked into the original text _(the UDH is replaced with garbage when trying to unpack)_

```
$ ./packto7bits.sh -r $(echo $hexstring1packed) | xxd -r -p | xxd
00000000: 0500 0c28 2a20 004c 6f72 656d 2049 7073  ...(* .Lorem Ips
00000010: 756d 2069 7320 7369 6d70 6c79 2064 756d  um is simply dum
00000020: 6d79 2074 6578 7420 6f66 2074 6865 2070  my text of the p
00000030: 7269 6e74 696e 6720 616e 6420 7479 7065  rinting and type
00000040: 7365 7474 696e 6720 696e 6475 7374 7279  setting industry
00000050: 2e20 4c6f 7265 6d20 4970 7375 6d20 6861  . Lorem Ipsum ha
00000060: 7320 6265 656e 2074 6865 2069 6e64 7573  s been the indus
00000070: 7472 7927 7320 7374 616e 6461 7264 2064  try's standard d
00000080: 756d 6d79 2074 6578 7420 6576 6572 2073  ummy text ever s
00000090: 696e 6365 2074 6865 2031 3530 3073 2c77  ince the 1500s,w

$
```

Do the same for _hexstring2_.

----
#### Sending it as binary messages the usual way

```
$ payload1packed=$(echo -n $hexstring1packed | xxd -r -p | base64 | tr -d '\n')

$ echo $payload1packed
BQADpQIBmG95uQ1Kwuf1Nig9B83TbTg7DyLX2+08iF7G00FvM4iOLoPg8rSbnnafQWE3GUTPw8vzMp2edp9BaTe5PqfL8y4Q8y0vt0FJ+LzeBqHDc5C4XHaD6OgyKO0m1+d0ef40B83pYTc5LCeDyPV2Ow+il/F0
UNleloPmafe4DKKjy6BYDQabs+4=

$ payload2packed=$(echo -n $hexstring2packed | xxd -r -p | base64 | tr -d '\n')

$ echo $payload2packed
BQADpQIC0GU3KOwG1d1r9/vuBsHlaTe9LAfR3+81KAw6h9nsch70NoPoeXgZFHaTQfOxPNwWs8tkUJoOor9B7fC6DAqD6Hl4GTSHl8fpdtkNEr/faxeoXL4Hi8Rr

$
```
----
```
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload1packed"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/11e7de74-84c0-4d4f-90b3-298c755696f5"
  }
}

$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload2packed"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/3713efe1-ae95-4598-9e21-3763a56337d8"
  }
}

$
```

The _CDR Count_ matches the _Success Count_ _(which counts the OpaaliAPI requests, not the fragments. CDR Count counts the billed fragments)._

![Partner Report]({{ site.url }}{{ site.ref_path }}/img/BinaryDeliveryReport2.png)
*Another Partner Report (CDR Count = count of charged fragments)* 

----

This time we receive this:

```
2018-10-26 14:57:30 [5296] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,156
2018-10-26 14:57:30 [5296] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202904405811985F90004810162417592218C050003A50201986F79B90D4AC2E7F536283D07CDD36D383B0F22D7DBED3C885EC6D3416F33888E2E83E0F2B49B9E769F4161371944CFC3CBF3329D9E769F416937B93EA7CBF32E10F32D2FB74149F8BCDE06A1C37390B85C7683E8E83228ED26D7E77479FE3407CDE96137392C2783C8F5763B0FA297F17450D95E9683E669F7B80CA2A3CBA0580D069BB3EE
2018-10-26 14:57:30 [5296] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202009
2018-10-26 14:57:30 [5296] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-26 14:57:30 [5296] [6] DEBUG: AT2[/dev/com5]: User data length read as (140)
2018-10-26 14:57:30 [5296] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=134 udhi=1 udhlen=5 udh=''
2018-10-26 14:57:44 [5296] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,109
2018-10-26 14:57:44 [5296] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202904405811985F90004810162417534215D050003A50202D0653728EC06D5DD6BF7FBEE06C1E56937BD2C07D1DFEF35280C3A87D9EC721EF43683E879781914769341F3B13CDC16B3CB64509A0EA2BF41EDF0BA0C0A83E8797819348797C7E976D90D12BFDF6B17A85CBE078BC46B
2018-10-26 14:57:44 [5296] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202009
2018-10-26 14:57:44 [5296] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-26 14:57:44 [5296] [6] DEBUG: AT2[/dev/com5]: User data length read as (93)
2018-10-26 14:57:44 [5296] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=87 udhi=1 udhlen=5 udh=''


```

The UDHs __050003A50201__ and __050003A50202__ are exactly as we specified, nothing added.

```
$ received3="050003A50201986F79B90D4AC2E7F536283D07CDD36D383B0F22D7DBED3C885EC6D3416F33888E2E83E0F2B49B9E769F4161371944CFC3CBF3329D9E769F416937B93EA7CBF32E10F32D2FB74149F8BCDE06A1C37390B85C7683E8E83228ED26D7E77479FE3407CDE96137392C2783C8F5763B0FA297F17450D95E9683E669F7B80CA2A3CBA0580D069BB3EE"

$ received4="050003A50202D0653728EC06D5DD6BF7FBEE06C1E56937BD2C07D1DFEF35280C3A87D9EC721EF43683E879781914769341F3B13CDC16B3CB64509A0EA2BF41EDF0BA0C0A83E8797819348797C7E976D90D12BFDF6B17A85CBE078BC46B"

$ ./packto7bits.sh -r $(echo -n $received3) | xxd -r -p | xxd
00000000: 0500 0c28 2a20 004c 6f72 656d 2049 7073  ...(* .Lorem Ips
00000010: 756d 2069 7320 7369 6d70 6c79 2064 756d  um is simply dum
00000020: 6d79 2074 6578 7420 6f66 2074 6865 2070  my text of the p
00000030: 7269 6e74 696e 6720 616e 6420 7479 7065  rinting and type
00000040: 7365 7474 696e 6720 696e 6475 7374 7279  setting industry
00000050: 2e20 4c6f 7265 6d20 4970 7375 6d20 6861  . Lorem Ipsum ha
00000060: 7320 6265 656e 2074 6865 2069 6e64 7573  s been the indus
00000070: 7472 7927 7320 7374 616e 6461 7264 2064  try's standard d
00000080: 756d 6d79 2074 6578 7420 6576 6572 2073  ummy text ever s
00000090: 696e 6365 2074 6865 2031 3530 3073 2c77  ince the 1500s,w

$ ./packto7bits.sh -r $(echo -n $received4) | xxd -r -p | xxd
00000000: 0500 0c28 2a40 0068 656e 2061 6e20 756e  ...(*@.hen an un
00000010: 6b6e 6f77 6e20 7072 696e 7465 7220 746f  known printer to
00000020: 6f6b 2061 2067 616c 6c65 7920 6f66 2074  ok a galley of t
00000030: 7970 6520 616e 6420 7363 7261 6d62 6c65  ype and scramble
00000040: 6420 6974 2074 6f20 6d61 6b65 2061 2074  d it to make a t
00000050: 7970 6520 7370 6563 696d 656e 2062 6f6f  ype specimen boo
00000060: 6b2e 2065 6577 4145 4457                 k. eewAEDW

$
```

The text is exactly what we wanted, apart from the end åäö@ÅÖÄ which _I forgot to convert into GSM alphabet character codes_. _(My script for bitpacking just drops the highest bit before packing, so there is no way to unpack back to 8-bit values with correct highest bit.)_

But when I send this to my phone I still get nothing!
That is because the DCS now has value 0x04 and my phone does not know what to do with binary messages.

This is in the PDU data preceding the User Data:

|...|TP-OA|TP-PID|TP-DCS|TP-SCTS|...|
|---|-----|------|------|-------|---|
|   |05811985F9|00|04|81016241759221|   |

So if that one field had value 0x00 instead of 0x04 _(and I had correctly used GSM 7-bit alphabet)_ this should work.

### Luckily we now have the binary message extensions in OpaaliAPI

With _Opaali R4SP2_ we got some new _HTTP-headers_, one of which -- __SMS-DCS__ we should be able to use to set the DCS value.

This is what _API_TELIA_OMA_Messaging_REST_ documentation says about it:

![SMS-DCS documentation]({{ site.url }}{{ site.ref_path }}/img/SMS-DCS-documentation.png)
*SMS-DCS documentation* 
   
I guess they forgot to mention here that the value should be given as a _hexadecimal_ number.

Let's add a header "SMS-DCS: 0x00" and try it out:

```
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload1packed"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" --header "SMS-DCS: 0x00"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/f546a109-11ab-483a-a9a0-1e968622e052"
  }
}

$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload2packed"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" --header "SMS-DCS: 0x00"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/5140ea8c-1c80-42bd-84a9-26c0bb4d1254"
  }
}

$
```
And we receive:
```
2018-10-26 16:50:22 [5296] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,143
2018-10-26 16:50:22 [5296] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804405811985F900008101626105122191050003A50201C0EF7CACA10C8EC136540F9C7CB6713BB0085C4DF3301B0AE7197CCF3060D7E48F0083C17670307C031393C1E25B0606DBC1C1F42DE6FB16DF2EF0BBF57205930C30180C0CCEC1E0CDCB3E211064A8B4093CA6E7C13470B210BEE558A749116CDF81C7E03E1D5ADD50C0934E1A0CDE28C6812218DB00836769
2018-10-26 16:50:22 [5296] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-26 16:50:22 [5296] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-26 16:50:22 [5296] [6] DEBUG: AT2[/dev/com5]: User data length read as (145)
2018-10-26 16:50:22 [5296] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=139 udhi=1 udhlen=5 udh=''
2018-10-26 16:50:40 [5296] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,101
2018-10-26 16:50:40 [5296] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804405811985F900008101626105932161050003A50202C0E51BEA007E66D7E07A1A1C7CA46F6016B8EB49D7501B85EE523DC8C160DB8490C783C17670F00DE679C1B322190A068FC1C134F8BD512826843C1E4CBB8013057BB501067BD660D2E6050683B76B
2018-10-26 16:50:40 [5296] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-26 16:50:40 [5296] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-26 16:50:40 [5296] [6] DEBUG: AT2[/dev/com5]: User data length read as (97)
2018-10-26 16:50:40 [5296] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=91 udhi=1 udhlen=5 udh=''

```

...this looks suspicious, as the length of the data decreased when the only thing we did was changing the DCS value _(which BTW seems to be correct at the receiving end)._ _(Although the "User data length read as" has grown there are less actual bytes in the result. This is because for 7-bit data the length is actually the count of_ septets _not_ octets _.)_

If we try to restore the data into text, all we get is garbage...

----
### I know what happened!

If the data has shrunk, Opaali must have packed the already packed data again! It probably does it as stupidly as my own script and throws all the high-bits away -- which means there is no way to recover the data.

Let's see what happens if we use our original approach from the beginning but  add the "SMS-DCS: 0x00" header.

```
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload1"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" --header "SMS-DCS: 0x00"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/6bce2697-ce2f-4b6f-ba32-f0dbd019da22"
  }
}

$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:'"$recipient"'"],"senderAddress":"'"$sender"'","outboundSMSBinaryMessage":{"message": "'"$payload2"'"}}}' https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" --header "SMS-DCS: 0x00"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/91589/requests/a6b99b5d-6f93-4936-a639-b0c9c5ba1c3d"
  }
}

$

```

```
2018-10-26 17:08:17 [5296] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,156
2018-10-26 17:08:17 [5296] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804405811985F9000081016271806121A0050003A50201986F79B90D4AC2E7F536283D07CDD36D383B0F22D7DBED3C885EC6D3416F33888E2E83E0F2B49B9E769F4161371944CFC3CBF3329D9E769F416937B93EA7CBF32E10F32D2FB74149F8BCDE06A1C37390B85C7683E8E83228ED26D7E77479FE3407CDE96137392C2783C8F5763B0FA297F17450D95E9683E669F7B80CA2A3CBA0580D069BB3EE
2018-10-26 17:08:17 [5296] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-26 17:08:17 [5296] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-26 17:08:17 [5296] [6] DEBUG: AT2[/dev/com5]: User data length read as (160)
2018-10-26 17:08:17 [5296] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=154 udhi=1 udhlen=5 udh=''
2018-10-26 17:08:33 [5296] [6] DEBUG: AT2[/dev/com5]: <-- +CMT: ,109
2018-10-26 17:08:33 [5296] [6] DEBUG: AT2[/dev/com5]: <-- 07915348500202804405811985F90000810162718023216A050003A50202D0653728EC06D5DD6BF7FBEE06C1E56937BD2C07D1DFEF35280C3A87D9EC721EF43683E879781914769341F3B13CDC16B3CB64509A0EA2BF41EDF0BA0C0A83E8797819348797C7E976D90D12BFDF6B17E8B1E7031C5B2E
2018-10-26 17:08:33 [5296] [6] DEBUG: AT2[/dev/com5]: received message from SMSC: +358405202008
2018-10-26 17:08:33 [5296] [6] DEBUG: AT2[/dev/com5]: Numeric sender  <91589>
2018-10-26 17:08:33 [5296] [6] DEBUG: AT2[/dev/com5]: User data length read as (106)
2018-10-26 17:08:33 [5296] [6] DEBUG: AT2[/dev/com5]: Udh decoding done len=100 udhi=1 udhlen=5 udh=''
```
OK, the DCS is 0x00 and it looks like we finally received what we wanted:

```
$ received7="050003A50201986F79B90D4AC2E7F536283D07CDD36D383B0F22D7DBED3C885EC6D3416F33888E2E83E0F2B49B9E769F4161371944CFC3CBF3329D9E769F416937B93EA7CBF32E10F32D2FB74149F8BCDE06A1C37390B85C7683E8E83228ED26D7E77479FE3407CDE96137392C2783C8F5763B0FA297F17450D95E9683E669F7B80CA2A3CBA0580D069BB3EE"

$ received8="050003A50202D0653728EC06D5DD6BF7FBEE06C1E56937BD2C07D1DFEF35280C3A87D9EC721EF43683E879781914769341F3B13CDC16B3CB64509A0EA2BF41EDF0BA0C0A83E8797819348797C7E976D90D12BFDF6B17E8B1E7031C5B2E"

$ ./packto7bits.sh -r $(echo -n $received7) | xxd -r -p | xxd
00000000: 0500 0c28 2a20 004c 6f72 656d 2049 7073  ...(* .Lorem Ips
00000010: 756d 2069 7320 7369 6d70 6c79 2064 756d  um is simply dum
00000020: 6d79 2074 6578 7420 6f66 2074 6865 2070  my text of the p
00000030: 7269 6e74 696e 6720 616e 6420 7479 7065  rinting and type
00000040: 7365 7474 696e 6720 696e 6475 7374 7279  setting industry
00000050: 2e20 4c6f 7265 6d20 4970 7375 6d20 6861  . Lorem Ipsum ha
00000060: 7320 6265 656e 2074 6865 2069 6e64 7573  s been the indus
00000070: 7472 7927 7320 7374 616e 6461 7264 2064  try's standard d
00000080: 756d 6d79 2074 6578 7420 6576 6572 2073  ummy text ever s
00000090: 696e 6365 2074 6865 2031 3530 3073 2c77  ince the 1500s,w

$ ./packto7bits.sh -r $(echo -n $received8) | xxd -r -p | xxd
00000000: 0500 0c28 2a40 0068 656e 2061 6e20 756e  ...(*@.hen an un
00000010: 6b6e 6f77 6e20 7072 696e 7465 7220 746f  known printer to
00000020: 6f6b 2061 2067 616c 6c65 7920 6f66 2074  ok a galley of t
00000030: 7970 6520 616e 6420 7363 7261 6d62 6c65  ype and scramble
00000040: 6420 6974 2074 6f20 6d61 6b65 2061 2074  d it to make a t
00000050: 7970 6520 7370 6563 696d 656e 2062 6f6f  ype specimen boo
00000060: 6b2e 200f 7b7c 000e 5b5c                 k. .{|..[\

$
```
And to be sure it works, I also sent it to my phone _(well, actually I had to modify the_ __message reference number__ _in the_ __concatenation information IE__ _before it worked, because my phone still had some of the earlier fragments with the same reference number buffered):_

![LoremIpsumonPhone]({{ site.url }}{{ site.ref_path }}/img/LoremIpsumonPhone.jpg)
*Finally the message shown on the Phone* 


----

# Conclusion

So, we found a way to send the fragments we were given, without further fragmenting by _Opaali_ and the message was correctly displayed, and we were charged for exactly the amount of fragments we sent.

### But I'm not happy...

I would expect, that when I send a _binary message_ the intermediate components would not tamper with my data and it would be received unmodified.

By flagging our data as text by specifying DCS value 0x00 Opaali treats the binary data as text and makes character encoding conversion into GSM character encoding and then performs 7-bit packing by throwing away the high-bits. To me this looks like a lot of messing with my content, which I sent as a _binary_ message.

Granted, in this customer case it is actually what the customer wanted. However, I do think that the _sending application_ should do the conversion and packing, not the system delivering binary messages.  

There could be a use case, where the data needs to be flagged as text using DCS=0x00 so that the receiving device can recognize it correctly, but the character conversion may not be desired and the data ends up corrupted.

### It's a BUG!

I think this is a BUG and OpaaliAPI should not work like this!

But on the other hand -- this bug makes it easier to send prefragmented _text_ messages, at least in this customer case.

If the bug is some day fixed, sending text as binary this way _will no longer work_!

Will it be fixed? I don't know. Not soon -- I guess. Now that you know how to take advantage of this -- the Opaali Support will at least need to inform you in good time to change your implementation. Ideally, we could have our cake and eat it -- i.e. choose which behaviour we want, case by case.

But I don't know what _(if anything)_ will happen regarding this "bug".
What I do know is this: The API documentation does not say how this actually works, it is one of those _"implementation specific details"._ There is no guarantee that it works one way or another.

_(OK -- enough with scaring you: this is how it works now, it may change in the future, but it works now and you will probably be notified before it is  changed.)_
 

----



