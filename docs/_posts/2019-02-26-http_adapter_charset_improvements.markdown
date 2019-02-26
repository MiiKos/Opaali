---
layout:     post
title:      Character Set improvements to http_adapter
subtitle:   
date:       2019-02-26 08:00:00
author:     "JPLa"
header-img: "img/http_adapter_charsets.png"
---
Many of the Content Gateway applications are _so old_ that they use __ISO-8859-1 (Latin-1)__  or __ISO-8859-15 (Latin-9)__ instead of __UTF-8__ as their _Character Set_. I've been tuning the _http_adapter_ to better serve these applications.

While testing recent service migrations using the _http_adapter_ we once again have run into problems with the _scandinavian characters_ __ÅÄÖ åäö__.
Things had been working fine for services that did __not__:
* expect other than characters A-Z or digits 0-9 in their input
* return a direct response to the handset as part of the response to the http-request (most services so far had sent their response in a separate MT message)

What was actually happening was:
* requests to the backend service were using __UTF-8__ (even though the _request headers_ claimed it was ISO-8859...)
* the _Content-Type_ header in the http response was ignored and the response was treated as __UTF-8__
* when the response was sent back through the `/send` service the Character encoding was once again ignored
 

## Fixing Character Set in http-requests to backend services 

This `TODO` note has been in the source code from the beginning:
```
    private static final String[] MOTemplate = {
            // CGW style HTTP GET request
            "GET ${TARGET_URL} HTTP/1.1",
            "Accept: */*" ,
            "Character-set: iso8859-1",    // TODO: should this be configurable
            "User-Agent: CGW Provider Server 4.0 http_adapter",
            "Host: ${TARGET_HOST}",
            ""
        };
```
I finally made it configurable. To be useful, we need to somehow get the suitable value to fill in.
For this purpose I added two new config entries to the OpaaliAPIHandler:
* defaultReplyCharset
* targetCharset

```
[opaaliqr:qr]

# defaultReplyUrl is used to specify how the MT message for a Query Reply is sent,
# this can be the "cgw" service from the top of this configuration file
# notice how the $(MSG) macro is escaped by doubling the $,
# here the macros are expanded in two passes, once when creating the queued request
# (for sender/recipient) and again when the message content is available
defaultReplyUrl=http://localhost:8081/send?to=$(M)&from=$(R)&msg=$$(MSG)

# defaultReplyCharset specifies the character encoding to be used 
# for the defaultReplyUrl request (the default is ISO-8859-15) 
defaultReplyCharset=ISO-8859-15

# targetCharset specifies the character encoding to be used 
# for requests to (external) targets (the default is ISO-8859-15) 
targetCharset=ISO-8859-15
 
```

The defaults (ISO-8859-15) are chosen so that they should match most legacy Content Gateway services or the /send service of the _http_adapter_ itself. In other words: _you normally don't need to enter these lines into the config file_.

## Using the Content-Type from the http-response

The first time a service (that we tested) returned something to be sent back to the handset, it used ISO-8859-1 encoding _and correctly reported it in the Content-Type response header_. However, the _http_adapter_ just ignored the _Content-Type header_ and assumed it was UTF-8 (a bug, I guess...). This is now fixed.


## Recap of the character set configuration entries

### cgw (send) service character set
The __cgw__ service which is typically at the URL path `/send` has a configuration entry `cgwCharset` for choosing the expected character set.
* for legacy services which send MT messages using ISO-8859-15 character set you may want to set it explicitly (although this is the default)
* if you have a new service that uses UTF-8 then set it explicitly using `cgwCharset=UTF-8`in the [send:cgw] configuration section.

### Character set for Opaali Notifications
There is a setting `opaaliCharset` for __receive__ and __qr__ services, which was intended for selecting the character set for notification messages coming from Opaali. _This is actually not implemented_ -- it is assumed that they are always using UTF-8.

### Character set for requests going to target services
There is now a `targetCharset` setting for  __receive__ and __qr__ services to set the encoding used when making a request to a _back end service_.

### Default character set for MT response messages
Another new setting is `defaultReplyCharset` which is used to set the character set for requests made to the `defaultReplyUrl` (which is the URL used to send a MT response to a query). 
This is typically the __cgw__ service at `/send` so it should match the `cgwCharset` setting.


----

This time I __did__ build a new [binary release](https://github.com/MiiKos/Opaali/releases), bumping up the version number to 0.4.0.