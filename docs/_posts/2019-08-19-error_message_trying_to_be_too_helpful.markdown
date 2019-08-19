---
layout:     post
title:      "When an error message is trying to be too helpful"
subtitle:   ""
date:       2019-08-19 09:00:00
author:     "JPLa"
header-img: "img/UnrecoverableError.png"
---

Sometimes an error message tries to be helpful, but ends up obfuscating the real problem. When the _http_adapter_ encounters an unrecoverable error while trying to authenticate towards the _Opaali API_, it will stop trying (_after all, the situation seems to be unrecoverable_) and returns an error message for all future requests:

```
2019-08-19 09:21:19,857 ERROR   unrecoverable error, returning 401 Unauthorized to request
2019-08-19 09:21:19,858 DEBUG   CGWHandler response: HTTP 401: <b>Access is denied due to probably invalid credentials in http_adapter configuration</b>

```

# Recoverable vs Unrecoverable

The most typical case where _authentication towards __Opaali API__ fails_ is the use of __wrong credentials__. Maybe you didn't edit the _http_adapter_ configuration file before starting it and the default values were used. Or maybe you are using valid credentials but they belong to some other Opaali application. In any case, the situation will not be corrected until you change your configuration settings.

The _http_adapter_ tries to spare Opaali from unnecessary load caused by these authentication requests that are destined to always fail, so it enters into a _failure_mode_ where any subsequent requests will fail until the configuration is corrected and the _http_adapter_ restarted.

## Recoverable error?

A _recoverable failure_ when authenticating might happen when your application exceeds its _TPS limit_ (i.e. tries to make API requests in too fast succession). In these cases the _http_adapter_ will pause and resume operation a little bit later.

## Recovering from an unrecoverable error?

In the above cases it is assumed that the Opaali API itself is functioning normally. But what if there is a break in the service or network connectivity? The _http_adapter_ sees this as an unrecoverable error and enters the _failure_mode_. It does not exit the failure mode automatically when Opaali API resumes its operation - it needs to be shut down and restarted.

In this case this error message you will see is misleading:
__Access is denied due to probably invalid credentials in http_adapter configuration__

This time you _actually would not need to make configuration changes_, you just need to _restart the http_adapter_.

----

(_maybe the_ http\_adapter _should be more intelligent and recover from this situation automatically...maybe by exiting the_ failure\_mode _after 15 minutes or something?_)

#### Perhaps something like this might work: 

```
$ diff http_adapter/src/smsServer/ServerConfig.java http_adapter_recoverable/src/smsServer/ServerConfig.java
55a56
>     public static final String CONFIG_FAILUREMODETIMEOUT = "failureModeTimeout";
```

```
$ diff http_adapter/src/smsServer/CgwHttpApiHandler.java http_adapter_recoverable/src/smsServer/CgwHttpApiHandler.java
64a65,69
>         failureModeTimeout = sc.getConfigEntryInt(ServerConfig.CONFIG_FAILUREMODETIMEOUT);
>         if (failureModeTimeout > 0) {
>               // convert from minutes to milliseconds
>               failureModeTimeout *= 60000;
>         }
76a82,83
>     long failureModeStartTime = 0;
>     long failureModeTimeout = -1;
81a89,97
>
>         // reset failureMode if failureModeTimeout reached
>         if (failureMode && failureModeTimeout > 0) {
>             if (startTime - failureModeStartTime > failureModeTimeout) {
>               failureMode = false;
>               failureModeStartTime = 0;
>               Log.logError("resetting unrecoverable error after reaching failureModeTimeout of "+failureModeTimeout/60000);
>             }
>         }
273a290
>                                 failureModeStartTime = RequestLogger.getTimeNow();

```

and then adding a new configuration entry in the ```[send:cgw]``` section:
```
[send:cgw]
# failureModeTimeout in minutes
failureModeTimeout=15
```
