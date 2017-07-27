---
layout:     post
title:      "Getting started with Opaali API - Part 1"
subtitle:   "Authentication & Authorization - making your first API request"
date:       2017-07-26 13:00:00
author:     "JPLa"
header-img: "img/ApiConsoleOAUTHBanner.png"
---
Opaali uses an _authorization framework_ based on IETF OAuth 2.0. To use the API you need to first get an _access token_ to be added in the header of _all subsequent API calls_ from the application to the platform for authentication and authorization.

This is described in the __API_Telia_OMA_OAuth_REST.pdf__ document, which you can find in the [Resources section](https://developer.opaali.telia.fi/resources) of the Opaali Portal. It may not be the easiest document to understand, though...

The __R4__ (or _Rel4_) version of _Opaali platform software_ introduced the __API Console__ which should make getting started with the Opaali API easier. I haven't used the API Console earlier and I haven't read much documentation about it, but lets see how easy it is to use. 

----
# API Console
The dialog for making a token request to the OAUTHService seems simple enough -- there is actually nothing you need to input (because it knows your _Application Credentials_ and gets them automatically):

<!--![Opaali API Console]({{ site.url }}/Opaali/img/ApiConsoleOAUTHPost.png)-->
![Opaali API Console]({{ site.url }}{{ site.ref_path }}/img/ApiConsoleOAUTHPost.png)
*Opaali API Console*

After clicking the _Send!_ button we get a response:

<!--![Opaali API Console response]({{ site.url }}/Opaali/img/ApiConsoleOAUTHResp.png)-->
![Opaali API Console response]({{ site.url }}{{ site.ref_path }}/img/ApiConsoleOAUTHResp.png)
*Opaali API Console response*

From here we can see what the __Response body__ looks like. 

```
{ 
 "access_token" : "fa2e13c2-338b-4b33-8375-0df44573079c", 
 "token_type" : "bearer", 
 "expires_in" : 599 
} 
```
You will need the _access_token_ value _"fa2e13c2-338b-4b33-8375-0df44573079c"_ in subsequent API calls. 
We can also see that the token will expire in __599 seconds__ (about 10 minutes) so you will need to get a new one before that.
We can also see what URL we should use in our request and which HTTP headers should be included.

Seems easy enough, almost _too easy_, as it turns out...

# What the API Reference says...
This is an excerpt from the _OMA Authorization REST API Guide_:

![Request Access Token]({{ site.url }}{{ site.ref_path }}/img/APIReferenceAuth.png)

OK -- using the _API Console_ did not teach us how to build the _Authorization header_ value, in fact -- the whole Authorization header was missing from the API Console response. 
Also, we see that the request body should actually contain _grant_type=client_credentials_ (_maybe there is a bug in the API Console?_). Despite its limitations, the __API Console__ is an improvement that helps us in writing/testing our code.

# Command line to the rescue
When I first started learning how to use the Opaali API I found it was easiest for me to use the <code>curl</code> tool from a Unix-like command line shell. In fact, you can get surprisingly far using a few common tools from any Unix-like environment.

### The Tools
* you need access to a Unix command line (bash shell) or equivalent (e.g. mingw or Git Bash on windows)
* <code>curl</code> is used for making http requests
* <code>base64</code> is used for base64 encoding/decoding
* <code>grep</code>, <code>cat</code>, <code>tr</code>, <code>sed</code>, <code>cut</code> and other common unix tools will be used here to filter output (without much explaining their use in detail)

(_Almost all of my tests are run from a Git Bash command line on Windows 7...that's just my preference. Google around to locate versions of these tools for your own development environment._)

### Building the Authorization header
To quote the __API Reference__: _"Use the base64 (user name and password) method to generate Authentication
and Authorization headers. These credentials are partner-specific and application specific
and appear in the Manage Endpoints page for a partner of the Opaali Portal."_

What this means in practice:
You will first need to concatenate _username_ and _password_ separated with ':' inbetween them. 
You can use the <code>base64</code> command line tool for encoding, but you need to be careful not to add any extra characters (such as newline) to the characters to be encoded. You should also use single quotes in case the password contains characters which have special meaning to the shell. The -n option of echo will suppress the newline: (__important:__ Please replace with your own service's username:password) 
```bash
$ echo -n 'b535b0c5e5ae815cea82db6b3b25059a:1%AMvv?w' | base64 
YjUzNWIwYzVlNWFlODE1Y2VhODJkYjZiM2IyNTA1OWE6MSVBTXZ2P3c= 
$ 
```

You can now request the Access Token by making a _HTTP POST request_ using _Basic Authentication_. Use the application specific base64 encoded credentials as shown before. For simplicity, we use <code>curl</code> command with the -k option to turn off certificate verification: 
```bash
$ curl -v -k -d grant_type=client_credentials "https://api.opaali.telia.fi/autho4api/v1/token" --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic YjUzNWIwYzVlNWFlODE1Y2VhODJkYjZiM2IyNTA1OWE6MSVBTXZ2P3c=" 
```

Running the command you will get something like this (the '-v' verbose option lets you even see the request and response lines): 
```bash
$ curl -v -k -d grant_type=client_credentials "https://api.opaali.telia.fi/autho4api/v1/token" --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic YjUzNWIwYzVlNWFlODE1Y2VhODJkYjZiM2IyNTA1OWE6MSVBTXZ2P3c="
* timeout on name lookup is not supported
*   Trying 79.98.236.12...
* Connected to api.opaali.telia.fi (79.98.236.12) port 443 (#0)
* ALPN, offering http/1.1
* Cipher selection: ALL:!EXPORT:!EXPORT40:!EXPORT56:!aNULL:!LOW:!RC4:@STRENGTH
* successfully set certificate verify locations:
*   CAfile: C:/Program Files/Git/mingw64/ssl/certs/ca-bundle.crt
  CApath: none
* TLSv1.2 (OUT), TLS header, Certificate Status (22):
* TLSv1.2 (OUT), TLS handshake, Client hello (1):
* TLSv1.2 (IN), TLS handshake, Server hello (2):
* TLSv1.2 (IN), TLS handshake, Certificate (11):
* TLSv1.2 (IN), TLS handshake, Server key exchange (12):
* TLSv1.2 (IN), TLS handshake, Server finished (14):
* TLSv1.2 (OUT), TLS handshake, Client key exchange (16):
* TLSv1.2 (OUT), TLS change cipher, Client hello (1):
* TLSv1.2 (OUT), TLS handshake, Finished (20):
* TLSv1.2 (IN), TLS change cipher, Client hello (1):
* TLSv1.2 (IN), TLS handshake, Finished (20):
* SSL connection using TLSv1.2 / DHE-RSA-AES256-GCM-SHA384
* ALPN, server did not agree to a protocol
* Server certificate:
*        subject: C=FI; L=Helsinki; O=TeliaSonera Finland Oyj; CN=api.opaali.telia.fi
*        start date: Jun 27 12:27:28 2017 GMT
*        expire date: Jun 26 12:27:28 2020 GMT
*        issuer: C=FI; O=TeliaSonera; CN=TeliaSonera Server CA v2
*        SSL certificate verify ok.
> POST /autho4api/v1/token HTTP/1.1
> Host: api.opaali.telia.fi
> User-Agent: curl/7.47.1
> Accept: */*
> Content-Type:application/x-www-form-urlencoded
> Authorization: Basic YjUzNWIwYzVlNWFlODE1Y2VhODJkYjZiM2IyNTA1OWE6MSVBTXZ2P3c=
> Content-Length: 29
>
* upload completely sent off: 29 out of 29 bytes
< HTTP/1.1 200 OK
< accept: application/json; charset=UTF-8
< cache-control: no-store
< content-type: application/json; charset=UTF-8
< date: Wed, 26 Jul 2017 14:21:27 GMT
< pragma: no-cache
< server: Operator Service Platform
< Content-Length: 110
<
{
  "access_token" : "c213cdfc-c1f1-4862-8810-199b8ea7ce7a",
  "token_type" : "bearer",
  "expires_in" : 599
}* Connection #0 to host api.opaali.telia.fi left intact

$
```
Using the magic of _pipelines_ and _shell scripting_ the access_token value can be extracted from the response.
To make further use easier, you should capture the access token into a shell variable, in this way: 
```bash
$ access_token=`curl -k -d grant_type=client_credentials "https://api.sonera.fi/autho4api/v1/token" --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic YjUzNWIwYzVlNWFlODE1Y2VhODJkYjZiM2IyNTA1OWE6MSVBTXZ2P3c=" | grep access_token | cut -d: -f2 | tr -d "\","` 
```
This only works if everything goes right - you should display the content of the access_token variable to check it looks ok: 
```bash
$ echo $access_token 
9152c00c-c605-4c0d-a8d2-9182981e1aa5 
```
_(actually you may want to store your Basic Auth string into a variable also, because you will need it again when your token expires)_ 

_(and sorry for my old-school notation of using_ backticks _to extract the output of a command - I'll try to switch to the newer $() notation)_

Now that you have an access token you can start using the other API calls for sending or receiving messages. Notice that the access token is _service specific_ and you may need a different token for sending and receiving. 
The token will eventually _expire_ and things will stop working, usually you will get someting like this as a response: 
```bash
{ 
 "error" : "invalid_token" 
} 
```
You will then need to authenticate again. To avoid this error you should make sure you will get a new access_token before the old one expires, but __don't do this more often than is needed__ (if the token expires in 10 minutes wait at least 9 before getting a new token). There is a limit to how frequently you can make API Calls, exceeding your _TPS_ limit will make your following API Calls fail until you have paused for a long enough (_it isn't really long_) time. 

# Onwards
There is more to _access token_ and _session handling_ than this and we'll get to that later. Now we have learned enough about _authentication_ to make our first useful API Call. _More about that in the next episode..._

