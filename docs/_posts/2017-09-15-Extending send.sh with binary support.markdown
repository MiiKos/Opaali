---
layout:     post
title:      "Extending send.sh with binary support"
date:       2017-09-15 09:00:00
author:     "JPLa"
header-img: "img/SendScriptBinary.png"
---
In [Sending a binary message - Part 1]({{ site.baseurl }}{% post_url 2017-08-28-Sending-a-binary-message-Part1%}) we learned how to send a MT binary SMS from command line. Based on that we can add this functionality to the <code>send.sh</code> script.

# Usage

Lets see what new command line switches we can add. Out with the old -- in with the new (_this output is from the <code>diff</code> utility_):
```diff
37c37
<     echo "Usage: $1 -s sender -r recipient [-m message] [-f filename] [-hex message in hex]" >/dev/stderr
---
>     echo "Usage: $1 -s sender -r recipient [-bin|-text] [-m message] [-smart smart port] [-udh udh in hex] [-f filename] [-hex message in hex]" >/dev/stderr

```
Documentation from the <code>send</code> script README:

|Parameter      |Value|Definition|
|---------------|-----|----------|
|[-bin \| <br/> -text]|Bin \| text|The message content is a binary or text message.|
|[-smart]|Smart messaging port number|The smart messaging destination and originator ports. Remember to use the bin parameter if the content is a binary message.|
|[-udh]|UDH in hex|The User Data Header information in hexadecimal format<br/>To find out more about the UDH parameter, see the Nokia Smart Messaging documentation. Do not use this parameter if you do not know how it affects the mobile terminal’s functions.|

So we can specify whether it is a __text__ or __binary__ message. But lets first agree that using _-smart_ or _-udh_ switches implies that this is a binary message. And if both _-smart_ and _-udh_ switches are present the last one will override the earlier. (_...in other words: __Don't__ use both of them!_)

----
_Yes -- I'm slightly changing the semantics to something that makes more sense in Opaali API context. I'd better remember to update the README..._

----

# Parsing the arguments

Lets add a new global variable __MODE__ to choose between _TEXT_ and _BIN_ (and _FLASH_ which we will probably also add, but later, not now).

```bash
        # message sending mode: TEXT|BIN|FLASH (TEXT is default)
        MODE="TEXT"
```

We will also rewrite the _-hex_ switch parsing which we added last time. Lets introduce another global variable __HEX__ where we will store the hex string if it was given (_and we'll move decoding the hex string somewhere later in the script_).

```bash
                -hex)
                shift
                if [[ -n "$1" ]]; then
                    # HEX will contain MESSAGE as hex encoded
                    HEX="$1"
                else
                    error_exit "$0" "missing hex message" "$1"
                fi
                shift
                ;;
```

We will put the UDH (_User Data Header_) into global variable __UDH__ and also set MODE to binary:
```bash
                -udh)
                shift
                if [[ -n "$1" ]]; then
                    # User Data Header
                    UDH="$1"
                    # UDH implies MODE BIN
                    MODE="BIN"
                else
                    error_exit "$0" "missing UDH" "$1"
                fi
                shift
                ;;

```


I believe the _CGW version_ of <code>send</code> only supported decimal values for _smart messaging port number_, so we won't bother with _hex values_, either. We'll construct a __UDH__ containing just the port addressing IE and overwrite any possibly existing UDH value (_because if you can construct a UDH then what would you need the_ -smart _parameter for?_)

If the port number is less than 256 we can fit it into 8 bits, otherwise we'll use 16-bit addressing:

```bash
                -smart)
                shift
                declare -i SMART
                if [[ -n "$1" ]]; then
                    # smart messaging port number (in decimal)
                    SMART="$1"
                    # SMART implies MODE BIN
                    MODE="BIN"
                    # SMART is implemented as UDH (overwrites possibly existing UDH)
                    if [[  $1 -lt 256 ]]; then
                        # 8-bit port number
                        UDH=$(printf '040402%02x%02x' $SMART $SMART)
                    else
                        # 16-bit port number
                        UDH=$(printf '060504%04x%04x' $SMART $SMART)
                    fi
                else
                    error_exit "$0" "missing port in -smart" "$1"
                fi
                shift
                ;;

```

----

(_Like last time you can check Wikipedia for more details: [Wikipedia: User Data Header](https://en.wikipedia.org/wiki/User_Data_Header)._)

----

And if the mode is specified we will store it into variable __MODE__:
```bash
                -bin)
                MODE="BIN"
                shift
                ;;
                -text)
                MODE="TEXT"
                shift
                ;;
``` 
We will also make minor adjustments to the _argument post processing_ and add a call to a new function that will build the text or binary message:
```diff
145c184
<         if [ -z "${MESSAGE}" ]; then
---
>         if [[ -z "${MESSAGE}" && -z "${HEX}" ]]; then
164a204,205
>         # build final MESSAGE (including UDH if needed)
>         buildMessage "${MESSAGE}" "${HEX}" "${MODE}" "${UDH}"

```

# Building a Text or Binary Message

Before that last _buildMessage_ function call in _parseArguments_ we have
* __MESSAGE__ in text form from file, command line or read from stdin
* alternatively __HEX__ containing a text _or_ binary message as _hex encoded_
* for a binary message __UDH__ as given on command line or generated from _-smart_ parameter
* we also have __MODE__ but we could guess that from UDH already 

If we have
1. a HEX encoded TEXT MODE message we need to decode it into MESSAGE
2. a UDH we need to append to it the MESSAGE _base64 encoded_ (or HEX which we first _decode into binary data_)
 
So the function _buildMessage_ would look like this:
```bash
# build a message and store it in global variable MESSAGE
function buildMessage {
    #param 1: message in TEXT format
    #param 2: message in HEX format
    #param 3: MODE
    #param 4: UDH or (null)
    
    if [[ -n "$4" || "$3" == BIN ]]; then
        # UDH implies binary message
        UDH="$4"
        if [[ -z "$2" ]]; then
            # hex encode text
            HEX=$(echo -n "$1" | xxd -p | tr -d "\n")
        else
            HEX="$2"
        fi
        MESSAGE=$(echo -n "$UDH$HEX" | xxd -r -p | base64 | tr -d "\n")
    else
        # text message
        if [[ -z "$1" && -n "$2" ]]; then
            # decode hex message into text
            MESSAGE=$(echo -n "$2" | xxd -r -p)
        else
            MESSAGE="$1"
        fi
    fi
    return 0
}
``` 
And when we return from the function call the global variable __MESSAGE__ will contain either the _text to be sent_ or _a UDH followed by the binary data_.

# outboundMessageRequest

It turns out that the only change we need to make to our existing _outboundMessageRequest_ function is choosing the JSON parameter name between "__outboundSMSTextMessage__" and "__outboundSMSBinaryMessage__".
 
So we will add a third parameter __mode__ to our existing _outboundMessageRequest_ function:

```bash
# make an outboundMessageRequest
function outboundMessageRequest {
    #param 1: recipientAddress
    #param 2: message
    #param 3: mode - BIN or TEXT
    #global: senderAddress - sender address string
    #global: senderNameString - sender name string with comma or empty string
    #global: access_token - access token string
    #global: deli - resource URL to be used when querying status

    # urlencode + and :
    local sender=$(echo -n "$senderAddress" | sed -e s/\+/%2B/g -e s/\:/%3A/g)

    local outboundSMStype=""
    # choose a text or binary message
    if [[ $3 == "BIN" ]]; then
        outboundSMStype="outboundSMSBinaryMessage"
    else
        outboundSMStype="outboundSMSTextMessage"
    fi
    
    
    # call Opaali API and capture the interesting parts from the output"
    local output=$(curl -k -s -d "{\"outboundMessageRequest\":{\"address\":[\"$1\"],\"senderAddress\":\"$senderAddress\",\"$outboundSMStype\":{\"message\":\"$2\"}$senderNameString}}" --header 'Content-Type:application/json' --header "Authorization: Bearer $access_token" https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests | grep -E 'resourceURL|error')

# the rest of the function is not shown here as there will be no changes to it!
# ...
```
And finally we will add the new _mode_ parameter to our function call:
```diff
284c363
<     outboundMessageRequest "${RECIPIENT}" "${MESSAGE}"
---
>     outboundMessageRequest "${RECIPIENT}" "${MESSAGE}" "${MODE}"

```

# Examples

Lets use our old vCalendar example and set _vcal_udh_ and _vcal_msg_:
```bash
$ vcal_udh=06050423f50000

$ vcal_msg=$(cat example.vcal | xxd -p | tr -d "\n")

$ ./send.sh  -s '$JPLa' -r '+358401234567' -udh $vcal_udh -hex $vcal_msg
SENT: tel:+358401234567 DeliveredToNetwork

$

```
The data received at the other end looked similar to that which we got the last time when sending from command line using <code>curl</code>.

Because _-udh_ implies _-bin_ we don't necessarily need to give the vCalendar data (which is text anyway) hex encoded:

```bash
$ ./send.sh  -s '$JPLa' -r '+358401234567' -udh $vcal_udh -m "$(cat example.vcal)"
SENT: tel:+358401234567 DeliveredToNetwork

$
```
The data received is almost identical to the first example, but as <code>bash</code> is a _unix-style command shell_ the line endings were just __LF__ instead of __CR+LF__ (_depending on the receiving terminal this_ may _or_ may not _be significant_). 

Or we can use the _-smart_ parameter and avoid constructing a UDH:
```bash
$ ./send.sh  -s '$JPLa' -r '+358401234567' -smart 9205 -m "$(cat example.vcal)"
SENT: tel:+358401234567 DeliveredToNetwork

$ ./send.sh  -s '$JPLa' -r '+358401234567' -bin -smart 9205 -m "$(cat example.vcal)"
SENT: tel:+358401234567 DeliveredToNetwork

$
```

We can even abuse the syntax a bit by leaving out the _-udh_ parameter, adding the _-bin_ parameter and giving UDH+data together as a single hex string:
```bash
$ ./send.sh  -s '$JPLa' -r '+358401234567' -bin -hex 06050423f50000424547494e3a5643414c454e4441520d0a56455253494f4e3a312e300d0a424547494e3a564556454e540d0a4445534352495054494f4e3a5374656572696e672047726f7570206d656574696e6720696e20506f7274616c0d0a445453544152543a3230303030393036543130303030300d0a4454454e443a3230303030393036543132303030300d0a454e443a564556454e540d0a454e443a5643414c454e4441520d0a
SENT: tel:+358401234567 DeliveredToNetwork

$
```
This works with Opaali API because it is assumed that the __binary message always starts with a UDH__ so the _UDH present_ flag is always set for binary messages.

----
This BTW __does not work__ with CGW where UDH is separately provided to the API. You can send the message, but the UDH flag is not set, which will confuse the receiving end. Yes, I ran the test on the original <code>send.exe</code> of CGW:
```bash
$ /c/Program\ Files\ \(x86\)/CGW/send.exe  -s '$JPLa' -r '+358401234567' -bin -hex 06050423f50000424547494e3a5643414c454e4441520d0a56455253494f4e3a312e300d0a424547494e3a564556454e540d0a4445534352495054494f4e3a5374656572696e672047726f7570206d656574696e6720696e20506f7274616c0d0a445453544152543a3230303030393036543130303030300d0a4454454e443a3230303030393036543132303030300d0a454e443a564556454e540d0a454e443a5643414c454e4441520d0a
SENT: 00358401234567 Delivered to the SMSC

```
Also, the following syntax doesn't seem to work on CGW (_although our send.sh script accepts it_):
```bash
$ /c/Program\ Files\ \(x86\)/CGW/send.exe  -s '$JPLa' -r '+358401234567' -bin -smart 9205 -m "$(cat example.vcal)"
FAIL: 00358401234567 No message to send

```
Looks like you shouldn't give the message in text format with the -bin parameter. Giving it as a hex string works:
```bash
$ /c/Program\ Files\ \(x86\)/CGW/send.exe  -s '$JPLa' -r '+358401234567' -bin -smart 9205 -hex $vcal_msg
SENT: 00358401234567 Delivered to the SMSC

```
The rest of the examples worked on CGW also:
```bash
$ /c/Program\ Files\ \(x86\)/CGW/send.exe  -s '$JPLa' -r '+358401234567' -smart 9205 -m "$(cat example.vcal)"
SENT: 00358401234567 Delivered to the SMSC

$ /c/Program\ Files\ \(x86\)/CGW/send.exe  -s '$JPLa' -r '+358401234567' -udh $vcal_udh -m "$(cat example.vcal)" 
SENT: 00358401234567 Delivered to the SMSC

$ /c/Program\ Files\ \(x86\)/CGW/send.exe  -s '$JPLa' -r '+358401234567' -udh $vcal_udh -hex $vcal_msg
SENT: 00358401234567 Delivered to the SMSC

```

----

_See [Extending send.sh with -hex switch]({{ site.baseurl }}{% post_url 2017-08-17-Extending send.sh with -hex switch %}) for the previous related episode._
