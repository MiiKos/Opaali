---
layout:     post
title:      "How to escape"
subtitle:   ""
date:       2019-08-23 09:00:00
author:     "JPLa"
header-img: "img/escape.png"
---

According to [API_Telia_OMA_Messaging_REST](https://developer.opaali.telia.fi/resources) the Telia-OMA Messaging API supports _Latin 9 alphabet_ by default, which includes Finnish characters and the Euro symbol (€). You can use _Cyrillic characters_ if you add an sms-charset HTTP header with value as UCS-2 in the API Request. But often you need to use an _escape sequence_ to get certain characters work in the JSON or XML data that is used in an API request.

If I try to send the text "Päivää" as message text as in
```
curl -v -k -d {"outboundMessageRequest":{"address":["0401234567"],"senderAddress":"12345","outboundSMSTextMessage":{"message": "Päivää"},"senderName":"JPLa"}} https://api.sonera.fi
/production/messaging/v1/outbound/12345/requests --header Content-Type:application/json --header Authorization: Bearer feb7f38c-1f16-4ef8-8fb2-9694865e3816
```

I will probably receive this response:
```
HTTP/1.1 400 Bad Request
```

And when I check my character encoding
```
$ echo 'Päivää' | file -
/dev/stdin: UTF-8 Unicode text

$ echo 'Päivää' | xxd
00000000: 50c3 a469 76c3 a4c3 a40a                 P..iv.....

```
I can see that the character 'ä' is encoded as values __0xC3 0xa4__ in UTF-8.

The character code for 'ä' in _Latin 9_ is 0xE4, but I find no mention in the API reference document what kind of escape sequence I should use to make it work with _outboundMessageRequest_.

# The escape...

You can get a hint by observing how _nonprintable characters_ are represented in incoming messages. If you manage to send a MO message containing character codes from 0x00 upto printable characters you should get something like this (_providing you do __not__ have binary message extensions enabled_): 

```JSON
      "inboundSMSTextMessage" : {
        "message" : "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b\t\n\u000B\f\r\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u0
01A\u001B\u001C\u001D\u001E\u001F !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~¦´"
      }
```
Using unicode escape sequences starting with __"\u"__ followed by _four hexadecimal digits_ actually works. (As do the well known escapes for _tab (\t)_ etc.)

In fact, the _http\_adapter_ does the following conversions for _outboundSMSTextMessages_:

```
    /*
     * escape characters that cannot be used inside a JSON string
     */
    public static String escapeJSON(String s) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            switch (c) {
                case '\"':
                    str.append("\\\"");
                    break;
                case '\\':
                    str.append("\\\\");
                    break;
                case '/':
                    str.append("\\/");
                    break;
                case '\b':
                    str.append("\\b");
                    break;
                case '\f':
                    str.append("\\f");
                    break;
                case '\n':
                    str.append("\\n");
                    break;
                case '\r':
                    str.append("\\r");
                    break;
                case '\t':
                    str.append("\\t");
                    break;
                default:
                    if (Character.compare(c, '\u001f') <= 0) {
                        // escape control characters \u0000..\u001f
                        str.append("\\u"+String.format("%04x", (int) c));
                    }
                    else {
                        str.append(c);
                    }
                    break;
            }
        }
               return str.toString();
    }

    /*
     * for convenience escape accented characters used in Finland
     */
    public static String escapeSweFin(String s) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            switch (c) {
                case 'å':
                    str.append("\\u00E5");
                    break;
                case 'ä':
                    str.append("\\u00E4");
                    break;
                case 'ö':
                    str.append("\\u00F6");
                    break;
                case 'Å':
                    str.append("\\u00C5");
                    break;
                case 'Ä':
                    str.append("\\u00C4");
                    break;
                case 'Ö':
                    str.append("\\u00D6");
                    break;
                default:
                    str.append(c);
                    break;
            }
        }
        return str.toString();
    }
```

So this works: 
```
curl -v -k -d {"outboundMessageRequest":{"address":["0401234567"],"senderAddress":"12345","outboundSMSTextMessage":{"message": "P\u00E4iv\u00E4\u00E4"},"senderName":"JPLa"}} https:
//api.sonera.fi/production/messaging/v1/outbound/12345/requests --header Content-Type:application/json --header Authorization: Bearer 0d7595f3-08bb-4624-885b-4803ac7213fc
```

What __does not work__ is using _octal escape codes_:
```
curl -v -k -d {"outboundMessageRequest":{"address":["0401234567"],"senderAddress":"12345","outboundSMSTextMessage":{"message": "P\303\244iv\303\244\303\244"},"senderName":"JPLa"}}
https://api.sonera.fi/production/messaging/v1/outbound/12345/requests --header Content-Type:application/json --header Authorization: Bearer 0d7595f3-08bb-4624-885b-4803ac7213fc
```
_But then the documentation never promised that it would work._
