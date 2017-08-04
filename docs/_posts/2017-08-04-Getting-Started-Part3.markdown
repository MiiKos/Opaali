---
layout:     post
title:      "Getting started with Opaali API - Part 3"
subtitle:   "Checking sent message delivery status"
date:       2017-08-04 10:00:00
author:     "JPLa"
header-img: "img/BashScript2.PNG"
---
When you send an MT message using outboundMessageRequest, you get back a _resourceURL_ which you can use to check the delivery status of the sent message.
```bash
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/3eef0382-0014-45ea-9ee5-1ebce10e829d"
  }
}
```


# API Reference
Based on what the API Reference says, this seems easy:
![API Reference for Read Delivery Status]({{ site.url }}{{ site.ref_path }}/img/APIReferenceDLR.png)
*API Reference for Read Delivery Status*

We just need to make a simple HTTP GET operation with the _resourceURL_ appended with "/deliveryInfos":
```bash
$ curl -v -k "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/3eef0382-0014-45ea-9ee5-1ebce10e829d/deliveryInfos"
--header "Accept: application/json" --header "Authorization: Bearer $access_token"
```
And when we do that, we get:
```bash
$ curl -v -k "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/3eef0382-0014-45ea-9ee5-1ebce10e829d/deliveryInfos"
--header "Accept: application/json" --header "Authorization: Bearer $access_token"
> GET /production/messaging/v1/outbound/tel%3A%2B358000000000/requests/3eef0382-0014-45ea-9ee5-1ebce10e829d/deliveryInfos HTTP/1.1
> Host: api.opaali.telia.fi
> User-Agent: curl/7.47.1
> Accept: application/json
> Authorization: Bearer ed07956e-084c-4c9b-9402-649dca141a0a
>
< HTTP/1.1 200 OK
< accept: application/json
< content-type: application/json
< date: Mon, 31 Jul 2017 14:28:00 GMT
< server: Operator Service Platform
< Content-Length: 316
<
{
  "deliveryInfoList" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/3eef0382-0014-45ea-9ee5-1ebce10e829d/deliveryInfos",
    "deliveryInfo" : [ {
      "address" : "tel:+358403219113",
      "deliveryStatus" : "DeliveredToNetwork"
    } ]
  }
}* Connection #0 to host api.opaali.telia.fi left intact

```
There is a limitation to this kind of _polling for the delivery status_: you can only get one of these statuses:
* DeliveredToNetwork
* DeliveryImpossible

(This is probably to keep the load on the system lower - delivery status is monitored from the SMSC only when __specifically requested__ by adding the notifyRequest.)

# Polling vs. Push Notifications

There is also another way to get delivery status for MT messages: requesting a _Push Notification_ to a _notifyURL_ (also known as a _callbackURL_), which is the way you would probably want to do it when your application is in production use.

You can do this by adding a _receiptRequest_ element to your outboundMessageRequest (include a unique, message specific URL as your notifyURL so you can match the callback to the original message):
```JSON
"receiptRequest":{"notifyURL":"http://52.212.225.145:8080/mynotif1234", "notificationFormat":"JSON"}
```

Sending an MT message with a _receiptRequest_:
```bash
$  curl -v -k -d '{"outboundMessageRequest":{"address":["tel:+358403219113"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello
rld"},"senderName":"JPLa","receiptRequest":{"notifyURL":"http://52.212.225.145:8080/mynotif1234", "notificationFormat":"JSON"}}}' "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
> POST /production/messaging/v1/outbound/tel%3A%2B358000000000/requests HTTP/1.1
> Host: api.opaali.telia.fi
> User-Agent: curl/7.47.1
> Accept: */*
> Content-Type:application/json
> Authorization: Bearer 164f135c-4813-4438-9792-13a3e0e5cff6
> Content-Length: 268
>
< HTTP/1.1 201 Created
< accept: */*
< content-type: application/json
< date: Tue, 01 Aug 2017 12:11:27 GMT
< location: https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/5c77bd67-c27f-4cf8-81ee-ef3038f0256c
< server: Operator Service Platform
< Content-Length: 184
<
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/5c77bd67-c27f-4cf8-81ee-ef3038f0256c"
  }
}

```

You will need to have set up your own server software to handle the callbacks. When the delivery status of the message changes, you will receive a __HTTP POST request__ from Opaali:

```bash
POST /mynotif1234 HTTP/1.1
Content-Type: application/json; charset=UTF-8
Accept: application/json
Host: 52.212.225.145:8080
Transfer-Encoding: chunked

173
{
  "deliveryInfoNotification" : {
    "deliveryInfo" : [ {
      "address" : "tel:+358403219113",
      "deliveryStatus" : "DeliveredToTerminal"
    } ],
    "link" : [ {
      "rel" : "OutboundMessageRequest",
      "href" : "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests/5c77bd67-c27f-4cf8-81ee-ef3038f0256c"
    } ]
  }
}
0
```
This requires that:
1. your own firewall has open access to requests from Opaali (Opaali outbound traffic comes from IP __79.98.236.21__)
2. if your server that handles the callbacks is located at a _"nonstandard port"_ (i.e. something other that __80,8080,443,8443__) you will need to request a firewall opening at the Opaali end from [{{site.opaali_tuki}}](mailto:{{site.opaali_tuki}})

As a bonus also the status __DeliveredToTerminal__ is now                                                                                                                                                                                                                                                                                                                                              available when you have specifically expressed your desire to follow the delivery status (by adding that _receiptRequest_ element). 

# Subscriptions?

Opaali __R4__ added "_Outbound and Inbound subscriptions via Messaging API to setup partner notifications_". I haven't tried these myself yet, so I'll return to this topic later. _Looks like the documentation could be better, however..._

