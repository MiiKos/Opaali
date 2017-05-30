---
layout:     post
title:      "API Migration Paths from CGW to Opaali"
subtitle:   
date:       2017-05-29 12:30:00
author:     "JPLa"
header-img: "img/space4.jpg"
---
This article discusses some of the migration paths from specific **Content Gateway API**s towards the single **Opaali API**. There are dead ends to watch out -- for some of the CGW APIs there are no simple migration paths!

![Content Gateway APIs](/img/OpaaliAPI.png)

### send command line utility ###
While not strictly an _API_, some users just use the _sample applications provided with CGW Provider Server_ for their message sending needs. The command line send (or send.exe) application is simple to implement as a bash shell script using curl and other command line utilities.
(Have a look at [send.sh](https://github.com/MiiKos/Opaali/tree/master/sample_applications/send))


### HTTP API ###
Although both CGW and Opaali provide an HTTP API, they look quite different:

* CGW has a simple HTTP __GET__-request based API
* Opaali has a modern _REST API_ (both __GET__ and __POST__) with _OAuth2_ based authentication

Another difference is where in the _network topology_ the API is located:

* in the _customer's network_ (__Provider Server of CGW__)
* in the _operator's network_ (__Opaali API__)

The _good news_ is, that it is relatively easy to implement most of the CGW HTTP API on top of Opaali API, and you can place this implementation where your CGW Provider Server is located (so you might not need to make any changes at all to your current applications).

You can implement this in e.g. _Java_ (in fact there are already several internal partial implementations using this technique) or you could use a scripting language (like _node.js_) for maximum portability.

While sending an MT message using an HTTP-request in CGW was easy (a simple URL containing all the parameters) receiving MO messages was just as simple (a HTTP GET callback with predefined parameter substitutions) and getting Delivery Reports was not much harder (the actual status was in custom HTTP headers though).

Opaali API uses an HTTP POST request with parameters in a _JSON body_ for most operations, and you need to maintain an authenticated session for client initiated requests. You can configure callback notifications to be used for _MO_ messages and _MT_ delivery Reports, but you will either need to adapt to the new request format or implement translation to the CGW style in your "Provider Server emulator"...which might not be worth the trouble.

### Those were the easy ones... ###
Unfortunately migration paths from the other old CGW APIs are not so straightforward. As HTTP is universally used in modern applications, Opaali has no need for programming language specific APIs like the _C++_ or _Java_ libraries provided with CGW. Notice also, that CGW's Provider Server was a native (non-portable) implementation and only available for _Windows_ or _Linux_ in recent times. In contrast, you can use Opaali-API from _any_ platform supporting HTTP (and that practically means __any platform__ today).

#### Java and C++ libraries ####
The Java and C++ libraries are documented in detail in CGW documentation so based on that (and C++ header file/Java library bytecode) you should be able to implement the same API on top of Opaali API. As it would be easier to just rewrite your application to use HTTP this makes sense only if you cannot change your application (like when you don't have access to the original source code, that is).

As the Java library is implemented on top of _OTP_ protocol and C++ library on top of _CGW internal_ (undocumented) protocol you could continue using the existing libraries if you can get an implementation of those on top of Opaali API, but that is even less likely to happen.

#### OTP ####
The OTP protocol is actually documented in CGW documentation so it should be possible to make your own implementation (and get the Java library support as a bonus!).

#### UCP/EMI ####
UCP/EMI is a low level _SMSC API_, access to which CGW can provide through a tunnel, as CGW uses UCP/EMI underneath. Opaali uses a different SMSC API, so UCP/EMI is not available. Telia wishes to move away from providing low level SMSC specific APIs so they will not be available to Opaali users. Instead, Telia plans to provide some extensions to the standard OMA REST Messaging API so that the most common low level SMSC API use cases can be implemented (__this is still work in progress so no details are available now__). As UCP/EMI specification is publicly available, it would be possible to implement a UCP/EMI to Opaali API gateway, but you would need to have a really good reason for spending your resourses this way (why not rewrite your application instead - could be easier?)

#### SMTP ####
CGW has a built-in _SMTP server_ for implementing a _Mail-to-SMS_ (and back) Gateway. For Opaali you would need to find a third party implementation. The security requirements for SMTP servers today are much higher than they were when CGW was introduced - many networks don't allow your own server at port 25 anymore.

#### MMS ####
I am not covering MMS here. Opaali does support MMS but not necessary the same API that was used in CGW (for which finding documentation might be a bit challenging these days). _MMS support will come in a future release of Opaali_.

#### Then there is also MOBILE CHARGER... ####
_Mobile Charger_ is another topic which will probably deserve its own article.
