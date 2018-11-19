---
layout:     post
title:      "HTTP adapter configuration tips"
subtitle:   (finally some documentation available)
date:       2018-11-19 08:00:00
author:     "JPLa"
header-img: "img/http_adapter_config.png"
---
The last time I wrote about the _http\_adapter_ (article [HTTP adapter available for testing]({{ site.baseurl }}{% post_url 2017-10-18-HTTP adapter for testing%})) was a little over a year ago. Maybe it's time to start documenting how to configure the new functionality that has been added since then...

# Less and less limited in functionality

At that time the _http\_adapter_ only supported MT messages. During last spring I kept adding (_and adding_) functionality so that we could as much as possible avoid making changes to existing internal applications that were implemented using _CGW HTTP APIs_.

So I added support for:
* __RECEIVE_ONLY__ type of services
* __QUERY/REPLY__ type of services
* even support for __asynchronous handling__ of the above type of services (_it turned out that Opaali notifications time out much sooner than how it was in CGW -- many existing services could not handle these shorter timeouts_)

----

# But a little bit about Security first...

Now that the _http\_adapter_ has not only MT (_Mobile Terminated_) message support but also supports MO (_Mobile Originated_) messages, you should consider where to install it.
The _http\_adapter_ uses _https_ to make requests to OpaaliAPI, so the content of messages being sent is encrypted and not easily visible to external parties. But for incoming traffic the _http\_adapter_ implements a http (not __https__!) server, which means that incoming message requests are not encrypted. This may not be a big issue if the incoming traffic only contains a keyword, possibly followed by some non-personal data, but as [GDPR](https://en.wikipedia.org/wiki/General_Data_Protection_Regulation) has become effective since the http_adapter was first introduced, you probably really should use encryption for incoming messages also.

# Security in Content Gateway

![Security in CGW]({{ site.url }}{{ site.ref_path }}/img/securityincgw.png)
*CGW by default*

Content Gateway uses its own _proprietary encrypted tunnel_ between the _Operator Server_ and the _Provider Server_ - the messages between them are not visible to outsiders. It is assumed that the the messages between the Provider Server and an Application Server are protected by the customer's firewall.

# Limited Security with http_adapter

![Security in http_adapter]({{ site.url }}{{ site.ref_path }}/img/securityinhttp_adapter.png)
*http_adapter by default*

* http_adapter uses __https__ to make requests to OpaaliAPI so the content of messages being sent is encrypted and not easily visible to external parties.
* http_adapter implements a __http server__ for incoming notifications, which means that _incoming message requests are not encrypted_.

There are at least a couple of approaches that could be used to implement https support for incoming notifications.

# Implementing _https server_ inside http_adapter

![Security with https server implemented]({{ site.url }}{{ site.ref_path }}/img/securityinhttp_adapter1.png)
*http_adapter if https server were implemented*

The Java _http server_ used by _http\_adapter_ contains _a https version of the server_, so you can modify the existing code _by yourself_ to use this. 
(_The main reason why I haven't done this by myself is that I have been so busy with other things. And then there would also have been the trouble of obtaining certificates and finding the place where to put them..._)

# Putting http_adapter behind a Reverse Proxy

![Security with Reverse Proxy]({{ site.url }}{{ site.ref_path }}/img/securityinhttp_adapter2.png)
*http_adapter behind a HTTPS Reverse Proxy*

It might be easier to put the _http\_adapter_ behind a [_reverse proxy_](https://en.wikipedia.org/wiki/Reverse_proxy) which implements https support (you could use e.g. the [Apache HTTP Server](http://httpd.apache.org/docs/2.0/mod/mod_proxy.html#forwardreverse)).

# ...and then a little more about Security

This may be a little far fetched, but as the _http\_adapter does not check where the Incoming Message Notifications come from_, it is possible for evil hackers to _feed bogus incoming messages to you system_, especially if the traffic is not encrypted. I'll leave it to your imagination to think if this can be used to harm you... You may want to _create a Firewall rule_ that allows incoming notifications only from the IP-address where Opaali sends them from.

And I have probably said this before: _don't let your Opaali Application __username__ and __password__ leak to outsiders._ That is all the information that is needed to send SMS messages from your account.


----


# http_adapter block diagram


![http_adapter block diagram]({{ site.url }}{{ site.ref_path }}/img/http_adapter_block_diag.png)
*http_adapter block diagram*

The _diagram above_ shows the _message traffic_ through the _http\_adapter_  inserted between _OpaaliAPI_ and _your backend service_.
It also shows some of the _internal modules_ of the _http\_adapter_ (_of which only the CgwHttpApiHandler existed in the original version last year_). 
The rest of this article shows how to _configure_ the various services.

## The common part of the configuration file

Some of the following configuration entries were already covered in the previous article about the _http_adapter_, but there are a couple of new enhancements:
- simple log rotation
- multiple threads

----

```
# HTTP adapter configuration file
#
# common config parameters are at the beginning
# server port
port=8081
            
# log file name            
log_file=OpaaliLog.txt   
# log level: 0=NONE, 1=ERROR, 2=WARNING, 3=INFO, 4=DEBUG,
log_level=3
# always log to stderr too: 0=no, 1=yes
log_stderr=1
# append to existing log file: 0=no, 1=yes
log_append=1
# log_rotate - insert current date before last dot in filename: 0=no 1=yes
#log_rotate=1


# by default the http server is single threaded
# which may lead to lockups especially if you
# have more than one service configured
#
# you probably want to configure multiple threads
# using a thread pool
#
# set the threadpool size for http-server (optional, default is 1)
threadPoolSize=10

```

#### port
- you need to specify the server port where this will run on your local machine
```
port=8081
```

#### log configuration
- you probably want to configure a _log file_ and the _level of logging_
- you can choose to output log to _standard error_ output of the _console window_
- you can also choose whether to clear existing log at start or append to it
- there now is simple built-in support for log rotation: _date will be inserted before the last dot in log filename, a new log file is started for the first incoming request after midnight_
```
log_file=mylogfile.log
log_level=3
log_stderr=1
log_append=1
```

#### threadpool size
- you may need to tune the amount of _parallel threads_ that the http-server is allowed to use
- by default the http-server is single threaded, which may be sufficient for light _send-only_ use, but if you have _receiving_ enabled you will need to have several simultaneous threads to avoid deadlocks or other performance problems
```
threadPoolSize=10
```

## Configuring services
You will need at least one _service section_ in the configuration file.
Service sections start with a label containing a _name_ and the _service type_ separated by a colon.

```
# service sections [name:type]            
#
# service will run at the /name URL path
# if name is empty the service will run at root path
# but notice that all incoming requests will then match
# this service and you cannot have any other services
```


## How to configure send service
The _send service_ is almost always needed and it may be the only service you configure.
This service sets up a http service that accepts MT SMS messages in the same format as the http send interface in CGW.

The outgoing message is processed by _CgwHttpApiHandler_ which will call _OpaaliAPI_ for actually delivering MT messages.

![http_adapter send service]({{ site.url }}{{ site.ref_path }}/img/http_adapter_send.png)
*http_adapter send functionality*

The type of the send service is __cgw__ (_referring to Content Gateway http send API_) and typically its name will be __send__ (_but you can use other names instead, if you like_).

##### A configuration file example:
```
# service type "cgw" is for sending MT messages using
# Content Gateway type of http request
[send:cgw]
# character set for CGW API side
cgwCharset=ISO-8859-1
#cgwCharset=UTF-8
# replace these with your own credentials (these won't work)      
applicationUserName=b535b0c5e5ae815cea82db6b32b5095a
applicationPassword=1%AMCC?u

# mask specified request parameters with '*' in log at log_level info
# -format: (key, position, length)
# -replaces key value (from left to right) starting from given position with '*' up to given length or end of value
# -if position is negative, applies mask right to left from end of value
# -length is optional, if omitted applies mask until the end/start of value
#log_mask=(to,-2,4),(msg,15)
```
#### cgwCharset
- you may explicitly set the character encoding used by the http server
- you only need to explicitly set this if the default does not match what your calling application expects (depending on your platform, you may also need to change the _Java Virtual Machine_ (JVM) default character encoding to get the desired result) 

#### applicationUserName
#### applicationPassword
- these are the credentials of your MT send application which you will find in the _Manage Endpoints_ section of your _Application Profile_ in the _Developer Portal_

#### log_mask 
- the log_mask can be used to hide personal details from URLs in the log file
- a mask is defined inside parentheses, you can have a list of masks
- a mask starts with a URL parameter name
- followed by the starting position of data to be hidden (starting from left, unless when negative starting from right)
- followed by optional length, if missing mask is applied to the end/start of the value

----
##### an example:

The following log_mask
```
log_mask=(to,-2,4),(msg,15)
```
could produce this kind of a log entry
```
2017-12-16 15:23:24,014 INFO    http request(port 8877): /send?to=04017****9&from=15609&msg=Täma+on+testiä* (response in 2582ms:200 OK)
```

----            

## How to configure receiveonly service

For incoming messages there is the ___receive___ service, which receives _callback notifications_ from Opaali and forwards them as _HTTP GET requests_ in the same format as Content Gateway does.
There is support for _keyword detection_ and URL _variables_.
Currently only _text messages_ are supported; there is no specific support for _binary_ or _MMS messages_.

![http_adapter receive service]({{ site.url }}{{ site.ref_path }}/img/http_adapter_ro.png)
*http_adapter receive functionality*

##### A configuration file example:
```
# service type "receive" is for receiving callback notifications
# from Opaali for received MO messages
# (this service is available since v0.2.0)
[opaalinotif:receive]
##opaaliCharset=UTF-8
#
# see above for log_mask usage
log_mask=(msisdn,-2,4),(msg,15)

# incoming MO message notifications will be sent
# to a configurable target service using
# Content Gateway style HTTP GET requests
# using template URLs with macros filled in
# (notice that NOT all of the CGW macros are supported)
#
# a default URL is called when no there is no
# other matching configuration
defaultUrl=http://localhost:80/?msisdn=$(M)&shortnumber=$(R)&keyword=$(1)&msg=$(*)
#
# a separate mapping file can be used for choosing the target
# based on keyword and/or short code
mappingFile=mappings.txt
#
#
```
#### opaaliCharset
- you may explicitly set the character encoding used by the http server
- you only need to explicitly set this if the default does not match what Opaali notifications expect

#### log_mask
- _log_mask_ acts similarly to what whas described above for the _send_ service

#### defaultUrl
- this is the URL that is called to pass the received message on to a backend service (unless a keyword specific target is specified in a separate mapping file)
- it supports many of the same _variables_ as Content Gateway does and replaces them with _values_ taken from the notification coming from Opaali

#### mappingFile
- if you want to choose _the backend service URL to be called_ based on the _short code_ and _keyword_ of the _MO message_ you need to specify a separate _mapping file_ where these are configured

---

### Mapping file

The _mapping file_ is a separate file where you can specify _short code_ specific mappings to backend URLs and optionally _keyword_ specific mappings to URLs.
The purpose of these mappings is to provide similar functionality that you could configure using the _Provider Admin UI_ in Content Gateway. 
- the file contains ___sections___ starting with a _short code_ inside brackets
- all the following configuration lines are specific to this short code
- if there are more than one consecutive _section lines_ before an actual URL mapping then these are interpreted as a _list of short codes_ for which the mappings will apply
- there is a _global section_ before the first explicit _short code section_ which is applied to _any short code_ if none of the _more specific_ rules fits
- a mapping is of the format _keyword_=_URL template_, where __@__ matches to any keyword   

##### An example of a mapping file:
```
# This is a mapping configuration file from
# - service_address and keyword to a target URL template
# - keyword to a target URL template
#
# The mappings are specified as lines containing
# SERVICE_KEYWORD=APPLICATION_URI
# 
# a default mapping is specified using special keyword @
# (you can only have one default mapping for a set of service addresses)
# @=APPLICATION_URI
#
# mapping lines at the beginning of this file are applied to any service address
#
# to specify service address specific mappings you insert any number of service address lines
# in front of the mapping lines, like this:
# [SERVICE_ADDRESS]
# SERVICE_KEYWORD=APPLICATION_URI
#
# mapping lines are applied to the preceding service address line or lines until the next 
# batch of service address lines
#
# service address specific mappings take precedence over service address independent mappings
#
# the following mappings are applied to all service addresses 
testi=http://localhost:8080/$(msisdn)?msg=$(msg)


# all the rest of the mappings are tied to one or more service addresses
[12345]
INFO=http://localhost:28077/tfolsms/sms.do?sender=$(M)&recp=$(R)&msg=$(MSG)
TILAA=http://localhost:28077/tfolsms/sms.do?sender=$(M)&recp=$(R)&msg=$(MSG)&tilaa=yes

[1234]
[54321]
@=http://localhost/testi/$(R)/$(M)

```
----
## How to configure asynchronous receiveonly service

It turns out that _Opaali_ has _a much shorter timeout_ for getting a _http response from a backend service_ than what _Content Gateway_ had. 
Many services which used _http_adapter_ as a gateway for incoming MO messages were unable to process the request quickly enough (_because they performed lengthy database operations which was ok previously_).

Symptoms of this were the sender of a MO message first getting a response that the _service is unavailable_ and shortly afterwards receiving _the actual response anyway!_ (_Unless it was a service that does not send any response back...in this case the user thinks the action was not performed although it actually was..._)
   
To support these services _http_adapter_ can process received notifications _asynchronously_: the incoming _http request_ is acknowledged as successful right away, without waiting for the backend to process it. Such requests are entered into an _internal queue_ where requests are made to the backend service synchronously.  

![http_adapter async receive service]({{ site.url }}{{ site.ref_path }}/img/http_adapter_async.png)
*http_adapter receive functionality (asynchronous processing)*

### Configuring internal queueing service

The internal queue is automatically enabled, but you may want to configure its _size_ and especially _log_mask_ manually (_to avoid exposing user details in the log file_). The service type is _queue_, its name is not important as only one queue is used.

```
# an internal queue is automatically generated so it is not necessary to list here,
# unless you want to configure a log_mask or need to change the queue size
# even if more than one queue is configured in this file only one will be created
# (...and you have to guess which one of them)
#[internalq:queue]
#queueSize=20
#log_mask=(to,-2,4),(msg,15)
```
#### queueSize
- you can manually set the queue size if the default value is too small
- if the queue happens to become full (due to messages arriving much faster than they can be processed) the processing will revert to synchronous mode _which may lead to incoming requests timing out_

#### log_mask
- use the same mask as you used for receive service

### Enabling asynchronous mode for a service
You need to explicitly specify which receiving services have _asynchronous mode_ enabled.
```
# allow caller to request actual processing to be queued for later processing: 0=no, 1=yes
# this is done by appending /nowait to the request URL
# this may be needed if the backend service cannot process requests fast enough
# if the queue is full, we fall back to normal processing and take the risk of timing out
# notice that if the queued processing fails or http server crashes the data may be lost
#nowait=0
```
#### nowait
- adding _nowait_ configuration entry you can turn asynchronous mode on (1) or off (0)


### Using asynchronous mode

There is a final piece of information you need to actually use asynchronous processing: _requests are processed asynchronously __only__ if they append __/nowait__ to the URL they call_. 


| synchronous processing | asynchronous processing |
|-------|--------|
| http://server:port/receive | http://server:port/receive/nowait |
{:.table-bordered}



## How to configure Query/Reply service

Query/Reply service is like _receive service_, but the backend service can return a reply to be sent back in the _HTTP request response body_. The service type is ___qr___. The reply is sent through the queue service and requires configuring a URL to a service that can be used to send a MT message in Content Gateway request format (_usually the_ send service _of the http_adapter, but it could be an external service as well._)

![http_adapter query/reply service]({{ site.url }}{{ site.ref_path }}/img/http_adapter_qr.png)
*http_adapter query/reply functionality*

```
# service type "qr" is a variant of "receive" service where the response body (if there is one) 
# is returned to the caller in a separate, queued MT request to mimic the functionality of
# CGW QR services
# (this service is available since v0.3.0)
#[opaaliqr:qr]
# see above for log_mask usage
log_mask=(msisdn,-2,4),(msg,15)
#
# see above for defaultUrl and mappingFile usage
defaultUrl=http://localhost:80/?msisdn=$(M)&shortnumber=$(R)&keyword=$(1)&msg=$(*)
mappingFile=mappings.txt
#
# see above for nowait usage
# this affects only the MO notification processing, the MT response is always queued as it requires
# a separate Opaali API call
nowait=0
#
# defaultReplyUrl is used to specify how the MT message for a Query Reply is sent,
# this can be the "cgw" service from the top of this configuration file
# notice how the $(MSG) macro is escaped by doubling the $,
# here the macros are expanded in two passes, once when creating the queued request
# (for sender/recipient) and again when the message content is available
defaultReplyUrl=http://localhost:8081/send?to=$(M)&from=$(R)&msg=$$(MSG)

```

#### log_mask
#### defaultUrl
#### mappingFile
#### nowait
- see _receive service_ for details of the above configuration entries

#### defaultReplyUrl
- this is the URL used for sending a MT message as a reply to a query
- this is similar to the format of _defaultUrl_ but with an extra twist: the _sender_ and _recipient_ values are taken from the incoming request, but the _reply message macro_ contains _two $-characters_ so that it is first expanded into "$(MSG)" which is ultimately replaced with the reply text returned in the _response body_ by the backend service. (_This will not work if you only have $(MSG) as the value will be extracted from the __request__, not from the __response___.) 

----
# Enough already!
The _real reason_ why I had all the trouble writing this down is that _I myself couldn't remember how these services were supposed to be configured_. (_I have a feeling I should have stopped adding new functionality long ago. Or at least have written the documentation while the code was written..._) 


