---
layout:     post
title:      "HTTP adapter"
subtitle:   
date:       2017-06-16 12:00:00
author:     "JPLa"
header-img: "img/Telia_pebble_abacus.jpg"
---
One last day before __my summer vacation begins__...I had been hoping to have had time to _write about actual code_ here, but I have been busy _writing actual code_ instead.

A very early version of this code is uploaded to the `sample_applications` directory in this repository. 

It is an __http adapter__ which is supposed to eventually replicate most of the _Content Gateway HTTP interface_ so that some of you users can drop it in place of your current _CGW Provider Server_ and avoid the hassle of updating your own application. That is the intention -- it is too early to tell how well this would work. If you can avoid coding work, there'll be plenty of _installation_ and _configuration_ tasks anyway.

## What does it Do? Not much now...

I barely got sending simple text messages through it to work. There has been virtually no actual testing yet. If you are reading this blog, you are hopefully a _programmer_. Then you might be able to get the code to _compile_ and _run_. Not sure if it is worth your while for anyone else at this point. Maybe you can learn something from the code about how to use the __Opaali API__. The code worked for me, at least.

At this point the http adapter has pretty much the same functionality as the send.sh script I released earlier in this repository. It is my intention to add functionality to both, as soon as I can, but don't hold your breath - _I'll be away for a month or so_, first.

## What does it look like?

I initially started to write this as an example of how you can use the Opaali API from your Java code. So the emphasis is on _code clarity_ and __not__ on _performance_. (_Although, already there was talk about using this in production, in-house..._) I chose to use a _templating system_ where the template looks like the _http request examples_ in the official Opaali API documentation.  

Here is an example from the Messaging API documentation:
![API example](/img/APIReference.png)

And this is the template from my code:
````


        String[] MTTemplate = {
            // API request for sending MT messages
            "POST https://${API_HOST}/production/messaging/v1/outbound/${SENDERADDRESS_ENCODED}/requests HTTP/1.1",
            "Host: ${API_HOST}",
            "Content-type: application/json",
            "Accept: application/json",
            "Authorization: Bearer ${ACCESS_TOKEN}",
            "",
            "{",
            "    \"outboundMessageRequest\":",
            "        {\"address\":[${RECIPIENTLIST}],",
            "         \"senderAddress\":\"${SENDERADDRESS}\",",
            "         ${MESSAGE}",
            "         ${SENDERNAMESTRING}${CHARGINGINFO}${DLRREQUESTSTRING}${CLIENTCORRELATOR}",
            "    }",
            "}"
        };
        
````

The idea was that you can modify/extend the code easily based on the _examples in the API reference documentation_. And maybe it will be easier to understand how the code works.

----

Incidentally, there already was _Java example code_ in the _Resources section_ of the [Opaali Portal]({{site.opaali_portal}}). That code is much more professionally written than mine and it has a build script. It beautifully implements _information hiding_ so that you cannot easily tell how the Opaali API  calls are made at the low level. Which kind of makes it rather useless as an example of _how to call Opaali API from your own code_...

----

## If it works today, will it work next week?

If you already have an Opaali account, you should have received a message about the __service break next week (19.6.-20.6.2017)__. The Opaali platform software is to be upgraded from _rel3_ to _rel4_. While there aren't any _API changes_ (other than new functionality being added) there are some major changes in the _implementation underneath_ to improve _performance_ and _reliability_. My code has been tested to work on rel3, but I haven't tested it on rel4 yet. Should something in _your code_ break after this upgrade, if you cannot resolve it by yourself, you can always contact [{{site.opaali_tuki}}](mailto:{{site.opaali_tuki}}) for help.
 