---
layout:     post
title:      "Getting started with Opaali API - Part 2"
subtitle:   "Sending a message"
date:       2017-07-27 13:00:00
author:     "JPLa"
header-img: "img/BashScript1.png"
---
Last time we learned how to get the _access_token_. This time we will use it while trying to send an SMS message.

```bash
$ access_token=$(curl -s -k -d grant_type=client_credentials "https://api.sonera.fi/autho4api/v1/token" --header "Content-Type:application/x-www-form-urlencod
ed" --header "Authorization: Basic $basic_auth_string" | grep access_token | cut -d: -f2 | tr -d "\",")

$ echo $access_token
41282571-a525-439a-8462-79db44ad2af1

```
(_notice how I this time use the_ $(curl...) _syntax instead of backtick notation_ \`curl...\`)

# API Reference
You should always first check what the API Reference tells you to do:

<!--![Outbound Message Request]({{ site.url }}/Opaali/img/ApiReferenceMTSMS.png)-->
![Outbound Message Request]({{ site.url }}{{ site.ref_path }}/img/ApiReferenceMTSMS.png)
*API Reference for Outbound Message Requests*

For some reason it shows you how to send a _binary_ message ... when most likely you would want to start with sending a _text_ message. (_the binary message is unlikely to be displayed by your phone ... which will probably leave you confused about whether your API Call worked or not!_)

There is a _Request Parameters_ chapter later in the document, based on which one can construct a simple  request with _outboundSMSTextMessage_:

```bash
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:+358403219113"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"},"senderName":"JPLa"}}' "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" 
```

There are a couple of "tricks" you may want to make note of:
* we use the dummy address __+358000000000__ as senderAddress as we haven't been assigned a short code yet (_you don't need a short code for sending BULK MT messages_)
* numbers in international (+358...) format need to have a "tel:" prefix (_no prefix for short codes_)
* the _senderAddress_ value needs to be repeated in the _API URL_ (and it needs to be URL-encoded when used as part of a URL!)
* the optional _senderName_ field will overwrite the given senderAddress  
* also note the use of single quotes (') around the message content to avoid needing to escape nested quotes(") (_you could write "{\\"outboundMessageRequest\\":{\\"... if that is your preference..._) 

Lets run this command and see what we get!
```bash
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:+358403219113"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"},"senderName":"JPLa"}}' "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"

$
```
...we got _absolutely nothing_, which is not what we expected. To do a little bit of debugging lets run it in _verbose mode_ and use some _shell script trickery_ to filter out everything except the request and response lines (_by directing stderr output to stdout and grepping only lines starting with < or > (if you really wanted to know)_)
```bash
$ curl -v -k -d '{"outboundMessageRequest":{"address":["tel:+358403219113"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"},"senderName":"JPLa"}}' "https://api.opaali.telia.fi/production/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" 2>&1 | grep -E "<|>"
> POST /production/messaging/v1/outbound/tel%3A%2B358000000000/requests HTTP/1.1
> Host: api.opaali.telia.fi
> User-Agent: curl/7.47.1
> Accept: */*
> Content-Type:application/json
> Authorization: Bearer  753fc2e3-d69b-452d-a5a8-1230dbeb6583
> Content-Length: 167
>
< HTTP/1.1 403 Forbidden
< date: Thu, 27 Jul 2017 11:03:37 GMT
< server: Operator Service Platform
< Content-Length: 0
<

$
```
<br/>
# What is 'Sandbox mode'?

When you create a new application, it will initially be in __'Sandbox mode'__. While in _sandbox mode_ you can test your application outside of the real telephone network and avoid making costly mistakes. You don't need to decide all the details of the service yet and you won't be billed for messages until you request _'Promote to Production'_ to enter _production mode_.
Production mode and sandbox mode use different API URLs, so we need to change 'production' into 'sandbox': 
```bash
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:+358403219113"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"},"senderName":"JPLa"}}' "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token"
{
  "resourceReference" : {
    "resourceURL" : "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests/1485bfbb-5d03-4535-b5a7-2221f0326471"
  }
}
$
```
This time everything seemed to go well and we even got a _resourceURL_ back which we can later use to check the delivery status of the message.

# Where did it disappear?

After a while you may start wondering _why hasn't the sent message arrived at your phone_ even though the send request seemed to be successful. Before you write to [{{site.opaali_tuki}}](mailto:{{site.opaali_tuki}}) to ask why it didn't work, go back to Chapter 2 of the _Opaali Portal Quick Guide_ and have a look at this picture:

![Sandbox Use Case]({{ site.url }}{{ site.ref_path }}/img/Sandboxusecase.png)
*Sandbox Use Case*

While in _sandbox_, you are disconnected from the actual telephone network, so there is no way the message you sent from sandbox could end up in your phone! Read the document __API Telia OMA Sandbox REST__ in the [Resources section](https://developer.opaali.telia.fi/resources) of Opaali Portal. It turns out that you are supposed to use specified predefined phone numbers to get canned responses. This way you can easily simulate many of the common error cases, too. _Chapter 3.2_ lists the scenarios with expected responses, lets try number 6 where we should get a policy error:
 
```bash
$ curl -s -k -d '{"outboundMessageRequest":{"address":["tel:+35812345005"],"senderAddress":"tel:+358000000000","outboundSMSTextMessage":{"message":"Hello World"},"senderName":"JPLa"}}' "https://api.opaali.telia.fi/sandbox/messaging/v1/outbound/tel%3A%2B358000000000/requests" --header "Content-Type:application/json" --header "Authorization: Bearer $access_token" 
{
  "requestError" : {
    "policyException" : {
      "messageId" : "POL3003",
      "text" : "The following policy error occurred: %1. Error code is %2.",
      "variables" : [ "Maximum Transactions per Interval Exceeded.", "3003" ]
    }
  }
}
$
```
Yep - you can get this policy error in _production mode_, too, if you make _too many API requests in a short period of time_. To avoid this, try to slow down making your requests so that you don't exceed the _Transactions Per Second (TPS)_ limit.

When you think your application is ready, you can request _'Promote to Production'_ and move to _production mode_. The request and response will look the same as in our successful sandbox example earlier in this article (just replace _sandbox_ with _production_ in the URL this time) and if all goes well the message should reach your phone.

----
_Next time we will see how to use the_ ResourceURL _we got in the response to check the delivery status of the sent message._
