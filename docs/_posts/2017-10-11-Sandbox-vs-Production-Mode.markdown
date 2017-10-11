---
layout:     post
title:      "Sandbox vs. Production Mode"
subtitle:   "(is one of them useless?)"
date:       2017-10-11 12:00:00
author:     "JPLa"
header-img: "img/sand_lotsofsand.png"
---
Now and then customers attempt _performance testing in sandbox mode_ and wonder why __sandbox is so slow__ and __why they get policy error about exceeding their TPS limit__.

----
# SANDBOX IS FOR TESTING THE CORRECTNESS OF YOUR APPLICATION -- NOT FOR TESTING PERFORMANCE

----

Sandbox is a __simulator__ for OpaaliAPI, it is especially useful for _testing error cases_ which are hard to generate in _Production Mode_. Such as _"Subscriber barred"_ or _"Prepaid Subscriber with insufficient balance"_ -- see the document __API_Telia_OMA_Sandbox_REST.pdf__ in the [Resources section]({{ opaali_portal }}/resources) of the Opaali Portal.

As a simulator its performance is not comparable to production so _running any kind of performance tests in Sandbox is useless_. And because it is a simulator, there is _no connection to the actual telephone network_.

----
# NO, YOU CANNOT RECEIVE A MESSAGE SENT FROM SANDBOX INTO YOUR OWN PHONE

----
I'll repeat: _you cannot receive a message sent from sandbox into your own phone_.

## What good is it, then?

Once you have made it past the _Opaali API equivalent of "Hello World"_ -- your first application on a new platform -- you should use the Sandbox to simulate how your application behaves in common error cases.

----
(_what is the Opaali API equivalent to "Hello World" application? -- it is of course the_ __"400 Bad Request"__ _application, which was also my first attempt at using the Opaali REST API_ :-) ) 
```bash
$ curl -i -d '{"outboundMessageRequest":{"address":["tel:+35812345001"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"}
,"senderName":"JPLa"}' "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
HTTP/1.1 400 Bad Request
content-type: text/xml
Content-Length: 11

Bad Request
```

----

__API_Telia_OMA_Sandbox_REST.pdf__ has various chapters listing _Canned Responses for Predefined Parameter Values_ -- lets try out some of them.
You will get different responses depending on the _address of the recipient_.
 
#### Successful response (tel:+35812345001)
```bash
$ curl -k -d '{"outboundMessageRequest":{"address":["tel:+35812345001"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World!"
},"senderName":"JPLa"}}' "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests/548a1e26-326a-4c4e-a7af-8354fa6ffb24"
  }
}
```
Sending a message to number +35812345001 simulates a successful send where you are returned a _resourceURL_.

#### POL3006 Address not in destination whitelist (tel:+35812345004)
```bash
$ curl -k -d '{"outboundMessageRequest":{"address":["tel:+35812345004"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"}
,"senderName":"JPLa"}}' "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "requestError" : {
    "policyException" : {
      "messageId" : "POL3006",
      "text" : "The following policy error occurred: %1. Error code is %2.",
      "variables" : [ "Destination Whitelist.", "3006" ]
    }
  }
}
```

#### POL3003 Maximum transactions per interval exceeded (tel:+35812345005) 
```bash
$ curl -k -d '{"outboundMessageRequest":{"address":["tel:+35812345005"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"}
,"senderName":"JPLa"}}' "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "requestError" : {
    "policyException" : {
      "messageId" : "POL3003",
      "text" : "The following policy error occurred: %1. Error code is %2.",
      "variables" : [ "Maximum Transactions per Interval Exceeded.", "3003" ]
    }
  }
}
```

----
## Sandbox Performance (there is none...)

One _argument against doing performance testing in Sandbox Mode_ is that it has tighter _TPS limits_ than Production. The benefit of this is that you'll notice if your application behaves badly, because you'll get the __"Policy exception: POL3003 Maximum transactions per interval exceeded."__ sooner than in _Production Mode_.

Sandbox also has _a limit of 1000 transactions per day_. Getting the access_token takes one transaction, but as the next time you need to do this is after the token expires _after 10 minutes_, this will not be a problem _unless your code is broken and gets a new access_token once every (milli)second_...

## ...and it is _slow_
You may have problems in Sandbox Mode if you try to send messages to multiple recipients in the same submission.

Sandbox is designed to test the functionality of the call flow. It is really only required to test with one or, if required, two numbers to check if the API request is well formed and valid, and to test responses back. 

So when many numbers are added you may experience _a delay in getting response_ from the sandbox container due to internal checks and validations.  As a result of the delay experienced, on client side the application may see the error _"The underlying connection was closed: An unexpected error occurred on a receive."_ because _the request is taking longer_. 


## What then?

As soon as you have gained confidence in your own code you would want to move to _Production Mode_ and continue testing in _a real, live, environment_.

_Sandbox_ does protect you from your initial mistakes creating huge bills when you accidentally send hundreds of text messages, but you will only get the correct feeling how your application interacts with Opaali when you test in _Production Mode_.
 
----
_You can test in sandbox mode which allows you to simulate different responses using predefined parameters. In sandbox mode you cannot send messages to real phones. If you want to do that then you need to promote your applications to production mode and for that you need to have a service agreement with Telia first._

----

