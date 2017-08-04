---
layout:     post
title:      "My curl(s) are better than yours"
subtitle:   "(or: why don't my curl examples work for you?)"
date:       2017-08-02 10:00:00
author:     "JPLa"
header-img: "img/CurlFail.png"
---
Depending on your environment and the version of the <code>curl</code> application you have, my curl examples might fail for you.

So far the most common problem has been this:
```bash
$ curl -k -v -d grant_type=client_credentials https://api.opaali.telia.fi/autho4api/v1/token --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic NJF4NzFjNjdmYjY1MzY3Y2Y5M2VkMDVkNzY3MDZhMDk6YWNIMStNX3I=" 
* About to connect() to api.opaali.telia.fi port 443 (#0)
*   Trying 79.98.236.12...
* Connected to api.opaali.telia.fi (79.98.236.12) port 443 (#0)
* Initializing NSS with certpath: sql:/etc/pki/nssdb
* NSS error -5961 (PR_CONNECT_RESET_ERROR)
* TCP connection reset by peer
* Closing connection 0
curl: (35) TCP connection reset by peer

$
```
In such situations the first thing I usually do is feed the error message _(NSS error -5961 (PR_CONNECT_RESET_ERROR))_ to __google__ and see what comes out (_...in this case it took quite a lot of more googling until I hit a useful answer - which I cannot find again right now..._)

Or you could google for _curl exit code 35_ - one of the resulting pages says this:
> A TLS/SSL connect error. The SSL handshake failed. The SSL handshake can fail due to numerous different reasons so the error message may offer some additional clues. Maybe the parties couldn't agree to a SSL/TLS version, an agreeable cipher suite or similar.

While this points you to the right direction, we still don't know how to fix the problem...

## Get to the point - give me my fix!

To cut a long story short (_especially as I have already forgotten where I found the solution_) this should help:
_add command line parameter __--tlsv1.2__ to the curl command_:
```bash
$ curl --tlsv1.2 -k -v -d grant_type=client_credentials https://api.opaali.telia.fi/autho4api/v1/token --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic NJF4NzFjNjdmYjY1MzY3Y2Y5M2VkMDVkNzY3MDZhMDk6YWNIMStNX3I=" 
```
----
--tlsv1 _may also work (or __not__ -- Opaali FAQ v5.0 says that only TLSv1.1 and TLSv1.2 are supported). If you are wondering why the parameter_ -k _is sometimes present, but not always: I originally had to use it to disable certification verification because the self signed certificates couldn't be verified at that time. I guess it now works also without this._
