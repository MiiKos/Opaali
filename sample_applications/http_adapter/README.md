# HTTP adapter

This is an early version of an __HTTP adapter__ which will implement (as much as possible) the _Content Gateway HTTP interface_ for sending and receiving SMS messages.

The source code (written in _Java version 8_) will be published in several steps and the first version will have very limited functionality. The code will also be virtually untested at first (read: __very buggy__) so if you are not an adventurous mind you may be better off not trying to run the latest code by yourself.

This is work in progress. Eventually we hope this will result in a useful application that you can run in place of your current Content Gateway Provider Server. Or you can use it as an example while writing your own Java code.

And remember: _there will be __no__ official support for this code by Telia_!

## Building

Sorry, there is no build script yet. You'll need to manage compiling the source code by yourself.
You will need an external JSON implementation which can be found here: https://github.com/stleary/JSON-java

## Configuring

You will need a configuration file which should be specified as command line parameter when starting the software.
An example configuration file can be found in config directory.

## Running

Here is an example of running the software (this was done on Windows):
```
c:\Users\lasanenj\workspace\SmsServer\bin>java -cp .;..\..\JSON-java-master\bin smsServer.SmsServer ..\config.txt
2017-06-16 13:55:47,444 INFO Default CharSet:windows-1252
2017-06-16 13:55:47,524 INFO comment:# common config parameters are at the beginning
2017-06-16 13:55:47,525 INFO comment:# server port
2017-06-16 13:55:47,525 INFO config line:port=8877
2017-06-16 13:55:47,526 INFO comment:# API host name
2017-06-16 13:55:47,526 INFO comment:#API_HOST=api.sonera.fi
2017-06-16 13:55:47,527 INFO comment:#API_HOST=api.opaali.telia.fi
2017-06-16 13:55:47,527 INFO comment:
2017-06-16 13:55:47,528 INFO comment:# log file name
2017-06-16 13:55:47,528 INFO config line:log_file=OpaaliLog.txt
2017-06-16 13:55:47,529 INFO comment:# log level: 0=NONE, 1=ERROR, 2=WARNING, 3=INFO, 4=DEBUG,
2017-06-16 13:55:47,529 INFO config line:log_level=2
2017-06-16 13:55:47,530 INFO comment:
2017-06-16 13:55:47,530 INFO comment:# service sections [name:type]
2017-06-16 13:55:47,531 INFO comment:# character set for CGW API side
2017-06-16 13:55:47,531 INFO config line:cgwCharset=ISO-8859-1
2017-06-16 13:55:47,532 INFO config line:applicationUserName=b535b0c5e5ae815cea82db6b3b25059a
2017-06-16 13:55:47,532 INFO config line:applicationPassword=1%AMvv?w
2017-06-16 13:55:47,533 INFO comment:
2017-06-16 13:55:47,534 INFO comment:
2017-06-16 13:55:47,534 INFO comment:
2017-06-16 13:55:47,535 INFO comment:# end of config"
2017-06-16 13:55:47,535 INFO comment:
2017-06-16 13:55:47,963 DEBUG CGW HTTP API started
```

## Using

You have probably used Content Gateway before so you should know how to send a message using its HTTP interface.
Here is an example, anyway:
![sending an sms](screenshots/sending_an_sms.png)

