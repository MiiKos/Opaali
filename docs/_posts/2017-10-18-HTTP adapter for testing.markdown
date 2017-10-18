---
layout:     post
title:      "HTTP adapter available for testing"
subtitle:   "(...with limited functionality)"
date:       2017-10-18 09:00:00
author:     "JPLa"
header-img: "img/outwiththeold.png"
---
Last summer, in article [HTTP adapter]({{ site.baseurl }}{% post_url 2017-06-16-HTTP adapter %}), I mentioned that there is source code for a _http_adapter_ in the `sample_applications` directory of this GitHub repository. Now in the [Releases section](https://github.com/MiiKos/Opaali/releases) there are the first ready-to-run Release packages.

# Limited functionality
The _http_adapter_ acts as a gateway, which you can drop in place of your existing Content Gateway _Provider Server_. It looks like the old CGW _http interface_ towards your application, but uses _Opaali_ and its _Opaali API_ to do the actual sending of the message.

![CGW vs. Opaali]({{ site.url }}{{ site.ref_path }}/img/beforeafter.png)
*CGW vs. Opaali*

Currently, it only supports sending a text __msg__, __from__ a specified number, __to__ specified recipients. That is just enough functionality for us  replacing one of our existing in-house Provider Servers without needing to make changes to the dozens of internal applications sending MT text messages through it.  

# Getting Started

The Release page should have sufficient instructions for getting started, but I'll summarise them here

1. download the __SmsServer.jar__ file (don't bother downloading the Source code packages, you don't need them now)
2. download and install __Java 1.8 runtime__, if you don't have it already
3. make the needed configuration file
4. start the __SmsServer__ from command line
5. test if it works (_if not, fix the problem..._)
  
## The Configuration file
Here is an example of a configuration file:
```
# HTTP adapter configuration file
#
# common config parameters are at the beginning
# server port
port=8081
# API host name                
#API_HOST=api.sonera.fi   
#API_HOST=api.opaali.telia.fi
            
# log file name            
log_file=OpaaliLog.txt   
# log level: 0=NONE, 1=ERROR, 2=WARNING, 3=INFO, 4=DEBUG,
log_level=2                
# always log to stderr too: 0=no, 1=yes
log_stderr=1
# append to existing log file: 0=no, 1=yes
log_append=1       

# service sections [name:type]            
[send:cgw]
# character set for CGW API side
cgwCharset=ISO-8859-1
#cgwCharset=UTF-8
# replace these with your own credentials (these won't work)      
applicationUserName=b535b0c5e5ae815cea82db6b32b5095a
applicationPassword=1%AMCC?u
            
           
# end of config"
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
- (_there is no built-in support for log rotation or other finesses_)
```
log_file=mylogfile.log
log_level=3
log_stderr=1
log_append=1
```

#### service section
- currently only one service is implemented, a _cgw-style send service_
- you can choose the service name, we use _send_ as default
- you may need to specify the character set you want to use in your requests towards the send service (_this will depend on your platform and how you are encoding special characters_)
- you also need to specify the _applicationUserName_ and _applicationPassword_ which you have configured for your application in Opaali Portal
```
[send:cgw]
cgwCharset=ISO-8859-1
applicationUserName=b535b0c5e5ae815cea82db6b32b5095a
applicationPassword=1%AMCC?u
```

## Running the Server

If you have configured the Java Runtime in your execution path, you can run the server using this command:
```bash
$ java -jar SmsServer.jar
```
(assuming you have saved the jar file as _SmsServer.jar_ in the current directory and your configuration file is in the same directory and called _config.txt_)

```bash
$ java -jar SmsServer-0.1.1-beta.jar
2017-10-16 13:51:06,494 INFO    Default CharSet:windows-1252
2017-10-16 13:51:06,600 INFO    processing configuration file "config.txt"
2017-10-16 13:51:06,600 INFO    comment:# common config parameters are at the beginning
2017-10-16 13:51:06,601 INFO    comment:# server port
2017-10-16 13:51:06,601 INFO    config :port=8877
2017-10-16 13:51:06,602 INFO    comment:
2017-10-16 13:51:06,602 INFO    comment:# log file name
2017-10-16 13:51:06,603 INFO    config :log_file=OpaaliLog.txt
2017-10-16 13:51:06,603 INFO    comment:# log level: 0=NONE, 1=ERROR, 2=WARNING, 3=INFO, 4=DEBUG,
2017-10-16 13:51:06,604 INFO    config :log_level=4
2017-10-16 13:51:06,604 INFO    comment:# always log to stderr too: 0 =no, 1=yes
2017-10-16 13:51:06,605 INFO    config :log_stderr=1
2017-10-16 13:51:06,606 INFO    comment:# append to existing log file: 0=no, 1=yes
2017-10-16 13:51:06,606 INFO    config :log_append=1
2017-10-16 13:51:06,607 INFO    comment:
2017-10-16 13:51:06,607 INFO    comment:# service sections [name:type]
2017-10-16 13:51:06,607 INFO    section:[send:cgw]
2017-10-16 13:51:06,609 INFO    config :cgwCharset=UTF-8
2017-10-16 13:51:06,609 INFO    config :applicationUserName=b535b0c5e5ae815cea82db6b3b25095a
2017-10-16 13:51:06,611 INFO    config :applicationPassword=********
2017-10-16 13:51:06,613 INFO    comment:
2017-10-16 13:51:06,613 INFO    comment:# end of config
2017-10-16 13:51:06,614 INFO    comment:
2017-10-16 13:51:06,614 INFO    end of configuration data reached
2017-10-16 13:51:06,951 INFO    CGW HTTP API started

```
(_you can terminate the server by hitting <code>Ctrl-C</code> in the command window_)
 

## Calling the _http_adapter_

You can send a text message just like with CGW by making a HTTP GET -request to the server:

```bash
$ curl -v 'http://localhost:8877/send?from=$JPLa&to=%2B358408551080&msg=Hello+World!'
* timeout on name lookup is not supported
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 8877 (#0)
> GET /send?from=$JPLa&to=%2B358408551080&msg=Hello+World! HTTP/1.1
> Host: localhost:8877
> User-Agent: curl/7.47.1
> Accept: */*
>
< HTTP/1.1 200 OK
< Date: Mon, 16 Oct 2017 11:21:14 GMT
< Content-type: text/html
< Content-length: 131
<
<html><head><title>Send SMS result</title></head><body><h2>Delivery result</h2>Success: tel:+358408551080: OK<br><br></body></html>* Connection #0 to host local
host left intact

$
```

Due to the architecture of the _http-adapter_ (_it has no internal queue, so your request will wait until Opaali API has completed the send operation_) and because emphasis was put on the clarity of code instead of performance (_this is_ sample code, _after all_) it is unlikely you would hit the _10TPS limit_ (_2TPS was reached in tests_). But you have the source code, you can make it better yourself: [http_adapter source code in GitHub](https://github.com/MiiKos/Opaali/tree/master/sample_applications/http_adapter)

