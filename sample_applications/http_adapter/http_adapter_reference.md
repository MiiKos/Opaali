# HTTP_ADAPTER REFERENCE MANUAL
####(work in progress...)

# Introduction

What does the _http\_adapter_ do? Well, basically it is a converter between a really simple HTTP GET based API and a modern, more complicated HTTP POST based REST API, in the domain of SMS Messaging. You can use it for a quick start to using Telia's Opaali API without learning the API details.

__Specifically, the _http\_adapter_ is a more or less _drop-in replacement_ for the Content Gateway Provider Server, when migrating to Opaali from CGW.__ 


### Converting between a simple GET API and a modern REST API using POST

With the _Content Gateway Provider Server_, sending an SMS message to a phone could be as easy as making the following HTTP GET Request:

http://localhost:80/send?from=1234&to=0101234567&msg=Hello

The _http\_adapter_ converts this simple __HTTP GET__ request:

```
GET /send?from=1234&to=0101234567&msg=Hello HTTP/1.1
Host: localhost
User-Agent: curl/7.47.1
Accept: */*


``` 
into a _not-so-simple_ __HTTP POST__ request with parameters in a JSON formatted body:

```
POST /production/messaging/v1/outbound/1234/requests HTTP/1.1
Host: api.opaali.telia.fi
User-Agent: curl/7.47.1
Accept: */*
Content-Type:application/json
Authorization: Bearer 155652e2-f38d-4c14-82b9-73f6cee7ddbb
Content-Length: 122

{
    "outboundMessageRequest" : {
	    "address" : ["0101234567"],
		"senderAddress" : "1234",
		"outboundSMSTextMessage" : {
		    "message": "Hello"
		}
	}
}
```

It can also work in the other direction, converting this _notification of an incoming SMS message_:

```
POST /notif HTTP/1.1
'content-type': 'application/json; charset=UTF-8',
'message-segment-count': '1',
accept: 'application/json',
authorization: 'Basic bWU6anVzdE1l',
host: '52.215.182.97:80'
'transfer-encoding': 'chunked'

"inboundMessageNotification" : {
  "inboundMessage" : {
    "destinationAddress" : "1234",
    "senderAddress" : "tel:+358401234567",
    "dateTime" : "2019-06-03T12:14:57.000+0000",
    "resourceURL": https://api.opaali.telia.fi/production/messaging/v1/inbound/registrations/61203021-95e9-4683-809f-89eacc03bd8e/messages/1877503686",
    "messageId" : "1877503686",
    "inboundSMSTextMessage" : {
      "message" : "hello world"
    }
  }
}

```

into this HTTP GET request which might be what your application server expects to get:

```
GET http://localhost/helloservice?keyword=hello&message=world&msisdn=0401234567&shortnumber=1234 HTTP/1.1
Accept: */*
Character-set: ISO-8859-15
User-Agent: CGW Provider Server 4.0 http_adapter
Host: localhost

```

# Getting Started/Quick Start

You will need to have  a Java 8 compatible runtime environment to run the _http\_adapter_.

###1. Download the Software

You can find pre-built _http\_adapter_ releases from the Release page: https://github.com/MiiKos/Opaali/releases 

###2. Basic Configuration

Download this [sample config file](https://github.com/MiiKos/Opaali/blob/master/sample_applications/http_adapter/config/config.sample) to the same directory and edit your own credentials to it. Then rename the file to ```config.txt```.

###3. Run!
Start the SmsServer from a command line (check the actual file name of your downloaded jar file):
```
   $ java -jar SmsServer.jar

```

You can terminate the running SmsServer with ```ctrl-C``` in its command window.

## Sending Messages to a Mobile Terminal 

If you have an existing HTTP application, point it to the _http\_adapter_ instead of the _CGW Provider Server_.

In the HTTP application, type a request of the following format in the URL field:
```
http://<server-address>:<port>/<service-name>?<parameters>
```

__AN EXAMPLE__
http://localhost:80/send?from=1234&to=0101234567&msg=Hello

You can also test this using a web browser. See the __Reference Manual__ for details of the parameters.


#Reference Manual

## Configuration file

The configuration file is needed to tell the _http\_adapter_ its common configuration settings as well as service specific settings. 
By default, the _http\_adapter_ is looking for a file called ```config.txt``` in the current working directory, but you can specify another file as a command line parameter.


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



## Common configuration settings

The common configuration settings are at the beginning of the configuration file before any service specific sections. 

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

Mandatory Parameters are shown in __bold__.

|Parameter |Value |Definition|
|----------|------|----------|
|__port__  | integer |port number for the http server | 
|API_HOST  | Opaali API host name |_api.opaali.telia.fi_ is the default|
|API_MODE  |_sandbox_ or _production_ | chooses the operating mode| 
|log_file| file name |file name for log file| 
|log_level| 0-4 | chosen log level <br/> 0=NONE<br/> 1=ERROR<br/> 2=WARNING<br/> 3=INFO<br/> 4=DEBUG|
|log_stderr| 0 or 1 | also write log to stderr <br/>0=no<br/>1=yes|
|log_append| 0 or 1 | append (instead of overwrite) to log file <br/>0=no<br/>1=yes|
|threadPoolSize| small integer | max number of simultaneous threads <br/>1 = single threaded operation (_not recommended_)|

## Service settings common to most services

### Character Conversions
```
# character set for CGW API side
cgwCharset=ISO-8859-1
#cgwCharset=UTF-8
```

### Log file configuration
```
# mask specified request parameters with '*' in log at log_level info
# -format: (key, position, length)
# -replaces key value (from left to right) starting from given position with '*' up to given length or end of value
# -if position is negative, applies mask right to left from end of value
# -length is optional, if omitted applies mask until the end/start of value
#log_mask=(to,-2,4),(msg,15)
```


## send service specific settings

```
# replace these with your own credentials (these won't work)      
applicationUserName=b535b0c5e5ae815cea82db6b32b5095a
applicationPassword=1%AMCC?u
```

Mandatory Parameters are shown in __bold__.

|Parameter |Value |Definition|
|----------|------|----------|
|cgwCharset| character set name | sets the character encoding used by the http server |
|__applicationUserName__| string | username from the _Manage Endpoints_ section of your _Application Profile_ in the _Developer Portal_|
|__applicationPassword__| string | password from the _Manage Endpoints_ section of your _Application Profile_ in the _Developer Portal_|
|log_mask| 
- the log_mask can be used to hide personal details from URLs in the log file
- a mask is defined inside parentheses, you can have a list of masks
- a mask starts with a URL parameter name
- followed by the starting position of data to be hidden (starting from left, unless when negative starting from right)
- followed by optional length, if missing mask is applied to the end/start of the value


## receive service specific settings

|Parameter |Value |Definition|
|----------|------|----------|
|opaaliCharset| character set name | sets the character encoding used by the http server |
|log_mask  |
|defaultUrl| URL | the default URL to be called to pass on the received message to a backend service|
|mappingFile| filename | a separate file for more detailed backend service configuration |
|nowait     | 0,1 | 0=asynchronous mode <br/> 1=syncronous mode (default) |
- adding _nowait_ configuration entry you can turn asynchronous mode on (1) or off (0)




## qr service specific settings

## internal queue specific settings

|Parameter |Value |Definition|
|----------|------|----------|
|queueSize | small integer | how many requests can be queued simultaneously|
|log_mask  |



## A Mapping file for configuring the handling of inbound services

(_...you can use a mapping file...details to be added..._)

### The CGW Parameters supported by the _send_ service

The following table shows the _original CGW_ parameters you can use in the request, and which of them are supported by the _http_adapter_. The parameters in brackets are optional.


|Parameter |Value |Definition|
|----------|------|----------|
|From |Sender number |Your Short Number or other phone number|
|To |Recipient number |The recipient’s mobile terminal number. You can send the message to multiple recipients by adding each recipient’s mobile terminal number as a to parameter.|
|Msg |Message text/Binary message in hex text |The message|
|[nrqurl] |Any URL address |__not implemented__|
|[ddt] |DDMMYYYYhhmm |__not implemented__|
|[vp] |Minutes |__not implemented__|
|[bin] |Any |The message is a binary message|
|[udh] |UDH in hex text |User Data Header information. Check that the UDH header multiplied by 8/7 and rounded up to the nearest integer value plus the length of text message do not exceed 160 characters. If the message is a binary message, the size of the UDH header and the size of the message must not exceed 140 bytes. Do not use the UDH parameter with the smart parameter.|
|[mcl] |Class number |The message class. Defines where the received message is stored in the mobile terminal. The classes are as follows: <br/>0 = Displays the message (flash) on the display immediately, but does not store the message. <br/> __Any other values are ignored.__|
|[smart] |Port number |__not implemented__|
|[charge] |Price in cents |__not implemented__|
|[info] |Billing information reference|__not implemented__|
|[validateonly] | <br/> |Only validates the syntax of the parameters without actually performing the request. <br/>_This parameter is specific to the http_adapter._|
 

### The CGW Parameters supported by receive and qr services

|Variable |Value|
|---------|-----|
|$(M) or $(MSISDN)|The sender’s number|
|$(R) or $(RECIPIENT)|The recipient number. In practice, a short number.|
|$(number) |The n’th word from the message body. $(1) is first word, $(2) the second, etc. <br/>If the message is a binary message, the first word is the message in the hexadecimal text format.|
|$(*) |The words in the message separated with the + character.|
|$(*c) |__not implemented__|
|$[] |The next word in the message. When this occurs for the first time in the URL, it is replaced with the first word after the keyword. The next occurrence is replaced with the second word after the keyword and so on.|
|$[DEF:text] |The next word in the message. When this occurs for the first time in the URL, it is replaced with the first word after the keyword. The next occurrence is replaced with the second word after the keyword and so on. If there is no word to insert, “text” after DEF: is inserted.|
|$[*] |The words in the message excluding the keyword separated with the + character.|
|$[*c] |__not implemented__|
|$(MSG) |The full message content. Non-safe characters in the URL are replaced with %<two-digit-hex-code>. For example, the string “AB ÄA” is converted to “AB%20%C4A”.|
|$(POSITION- LATITUDE) |__not implemented__|
|$(POSITIONLONGITUDE) |__not implemented__|
|$(POSITION- ESTIMATE) |__not implemented__|
|$$ | Escaped version of $. _This is specific to the http_adapter._|
___________________________________________________________________________

## Suggested Installation Directory Layout

One example:

* _bin_ for http_adapter jar file and startup scripts
* _etc/config_ for configuration files
* _var/log_ for log files

