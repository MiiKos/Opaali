---
layout:     post
title:      "Writing a send application as a shell script"
subtitle:   "send.sh explained"
date:       2017-08-07 10:10:00
author:     "JPLa"
header-img: "img/SendScript.png"
---
Now that we know how to send an SMS message using the Opaali API and shell tools (see _Getting started with Opaali API - Parts 1-3_), we can write a (bash) shell script that acts like the __send__ command that came with Content Gateway (CGW).

You may have already noticed _send.sh_ in the sample applications of this GitHub repository. I'll go through all the code (of the version that is the latest while I'm writing this) and try to explain how it works. (_As part of my job, I don't actually write shell scripts that often, so as a bash script it may not be the most elegant one, but at least it seems to work._)

# send
If you are migrating from __Content Gateway__ then you may already be familiar with the original <code>send</code>-command, whose documentation you can find in your CGW documentation. The initial version of this script supports only a tiny fraction of the command line parameters, but there is a plan to add more functionality later.  
```bash
$ ./send.sh
Usage: ./send.sh -s sender -r recipient [-m message] [-f filename]
```
As such this may be useful to non-CGW users alike, as a simple way to send a basic MT text message. You can specify the
* sender
* recipient
* message content in three _mutually exclusive_ ways:
    * on command line by using the __-m__ switch
    * from a file by using the __-f__ switch
    * from stdin (if neither of those flags is present)

I'll present the whole script here (cut into pieces with intervening comments) so that you can easily compare it to the full script. (_And if you are reading this later, when I have added new functionality, the code in the repository will not match this anyway..._)

# Boring...
Like all shell scripts it starts by specifying the command interpreter to run...
```bash
#!/bin/bash
```
...and a comment section stating the purpose of the script:
```bash
#
# send.sh - an application for sending a text message using Opaali API
#
# This is an example of using Opaali API from a shell script
# It is modeled after the send command line utility that has been
# distributed with Content Gateway Provider Server
#
# This is not intended for production use as such
# but might satisfy modest needs just for sending a message
#
# Only a subset of options in the CGW version are supported here
#
# Author: jlasanen
#

```
## Your credentials
To use the API you need to authenticate and for that you need your credentials. It is a bad idea to store them in the script itself (_but go ahead if you like living dangerously_) so you should save them in a separate file, which outsiders cannot read (I'll leave it to you find the best way to do that. Here that external file is just read and executed as part of this script by using the <code>source</code> command).
```bash
# file where your credentials are stored
CREDENTIALS_FILE=.opaalicredentials

# read service specific credentials from a file
# containing the following two entries
# (uncomment and replace with your own credentials,
#  try to keep the file in a safe place so that your
#  credentials won't leak for others to use)
#applicationUserName="b535b0c5e5ae815cea82db6b3b25095a"
#applicationPassword='1%AMCC?w'
function read_credentials {
    #param 1: filename

    source "$1"
}
```
# Usage and error messages
There are the usual _shell functions_ for displaying _usage instructions_ and _error messages_. There is also a function for displaying a message if you try to use a command line option which has not been implemented (yet).
```bash

# print usage instructions and exit
function usage {
    #param 1: commandName

    echo "Usage: $1 -s sender -r recipient [-m message] [-f filename]" >/dev/stderr
    exit 1
}

# print error message and exit
function error_exit {
    #param 1: commandName
    #param 2: msg
    #param 3: param

    echo "$1: $2 $3" >/dev/stderr
    usage "$1"
}

# print functionality not implemented -error and exit
function not_implemented {
    #param 1: commandName
    #param 2: functionality

    echo "$1: Sorry, $2 is not implemented." >/dev/stderr
    exit 2
}
```
# Command line processing
This function processes the command line arguments. I guess you would normally use _getopt(1)_ for parsing the options, but I wanted to replicate exactly the behaviour that is in the old CGW send command (_...either that, or I was just too lazy to learn how to do this with getopt_). 
```bash
# parse arguments
function parse_arguments {
    #params: all command line parameters
    if [ "$#" -lt 2 ]; then
        usage "$0"
    else
        # parse arguments
        while [ "$#" -gt 0 ]
        do
            case "$1" in
                -s)
                shift
                SENDER="$1"
                shift
                ;;
                -r)
                shift
                RECIPIENT="$1"
                shift
                ;;
                -m)
                shift
                if [[ -n "$1" ]]; then
                    MESSAGE="$1"
                else
                    error_exit "$0" "message missing"
                fi
                shift
                ;;
                -f)
                shift
                if [[ -f "$1" ]]; then
                    MESSAGE="$(<$1)"
                else
                    error_exit "$0" "can't read input file" "$1"
                fi
                shift
                ;;
                -t|\
                -bin|\
                -text|\
                -h|\
                -nrq|\
                -vp|\
                -ddt|\
                -smart|\
                -udh|\
                -c|\
                -hex)
                not_implemented "$0" "$1"
                ;;
                *)
                usage "$0"
                ;;
            esac
        done

        #post processing arguments
        
        if [[ -z "${SENDER}" ]]; then
            error_exit "$0" "sender missing"
        fi
        if [[ -z "${RECIPIENT}" ]]; then
            error_exit "$0" "recipient missing"
        fi

        # add "tel:" prefix to sender and/or recipient if they are not short numbers (i.e. contain '+')
        if [[ "${SENDER:0:1}" == '+' ]]
        then
            SENDER="tel:${SENDER}"
        fi
        if [[ "${RECIPIENT:0:1}" == '+' ]]
        then
            RECIPIENT="tel:${RECIPIENT}"
        fi

        # if message is empty read it from stdin
        if [ -z "${MESSAGE}" ]; then
            readMsg MESSAGE
        fi

        # implement CGW style of specifying alphanumeric sender name
        if [[ ${SENDER:0:1} == '$' ]]
        then
            senderNameString=",\"senderName\":\"${SENDER:1}\""
            senderAddress="tel:+358000000000"
        else
            senderNameString=""
            senderAddress="${SENDER}"
        fi

    fi
}
```
I am not going into the details of argument parsing, instead I'll write something about the postprocessing of options.

Opaali API requires that __long MSISDN numbers__ must be given as a URL with "tel:" prefix (_ex. tel:+358401234567_). Notice that __short codes__ are given as is, without any prefix. 
You don't have to be aware of this when using the send.sh script: if a number starts with '_+_' the prefix is automatically added. (_If you give a long number in local format without '+' and country code, it is treated as a short code_ __and it just seems to work!__)

If a message is not read from file or given as command line parameter, function _readMsg_ is called to read it from standard input.

__CGW__ lets you specify an _alphanumeric sendername_ by _prefixing it with '$'_. This script supports this notation and sets the Opaali API parameters _senderName_ and _senderAddress_ based on that. Notice that you can use the _dummy sender address_ __+358000000000__ if you don't have a real one.

# Reading the message from stdin
This function just reads lines from standard input and returns them as one string (with embedded newlines). It is called if you didn't specify any other source for the message content.
```bash
# read message from stdin into the specified variable
function readMsg {
    #param 1: variableName
    local separator=""
    local temp=""
    local line
    # read multiple lines into a single variable
    while read line
    do 
        temp="${temp}${separator}${line}"
        separator="\n"
    done
    # copy read message into specified variable
    eval "$1"=\${temp}
}
```
# Authentication
This function gets the _access_token_ like we did in [Getting started with Opaali API - Part 1]({{ site.baseurl }}{% post_url 2017-07-26-Getting-Started-Part1 %})
, only with a slightly more thorough error checking.
```bash
# authenticate and get access_token
function authenticate {
    #param 1: Application User Name
    #param 2: Application Password
    #global: access_token 
    #global: emsg
    
    # construct basic_auth string by combining username and password separated
    # with a colon and base64-encoding it all
    basic_auth=$(echo -n "$1:$2" |base64)

    # call Opaali API and capture the interesting parts from the output"
    local output=$(curl -k -s -d grant_type=client_credentials https://api.sonera.fi/autho4api/v1/token --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic $basic_auth" | grep -E 'access_token|error')
    
    # post processing: check for success or failure
    # we could test the return value, but choose to check the output only
    
    # try grabbibg access_token from the output
    access_token=$(echo "$output" | grep access_token | cut -d\: -f2 | tr -d "\", ")
    if [[ -z "$access_token" ]]; then
        # access_token is empty so something went wrong
        local error=$(echo "$output" | grep 'error' )
        if [[ -n "$error" ]]; then
            # we got error message from Opaali API
            emsg=$(echo "$error" | cut -d\: -f2)
        else
            # something went wrong with curl (now testing return value would have beeen useful...)
            emsg="unknown error"
        fi
        return 1
    fi
    return 0
}
```
# Sending the Message
And in [Getting started with Opaali API - Part 2]({{ site.baseurl }}{% post_url 2017-07-27-Getting-Started-Part2 %}) we learned how to send a message. Here, instead of surrounding double quotes (") with single quotes ('), we use escaped quotes (\\") inside double quotes (") so that shell variables inside strings are expanded and replaced with their values.
```bash
# make an outboundMessageRequest
function outboundMessageRequest {
    #param 1: recipientAddress
    #param 2: message
    #global: senderAddress - sender address string
    #global: senderNameString - sender name string with comma or empty string
    #global: access_token - access token string
    #global: deli - resource URL to be used when querying status

    # urlencode + and :
    local sender=$(echo -n "$senderAddress" | sed -e s/\+/%2B/g -e s/\:/%3A/g)

    # call Opaali API and capture the interesting parts from the output"
    local output=$(curl -k -s -d "{\"outboundMessageRequest\":{\"address\":[\"$1\"],\"senderAddress\":\"$senderAddress\",\"outboundSMSTextMessage\":{\"message\":\"$2\"}$senderNameString}}" --header 'Content-Type:application/json' --header "Authorization: Bearer $access_token" https://api.sonera.fi/production/messaging/v1/outbound/$sender/requests | grep -E 'resourceURL|error')
    
    
       # try grabbing deliveryURL from output
    deli=$(echo "$output" | grep resourceURL | cut -d\: -f2- | tr -d "\" ")

    # post processing: check for success or failure
    # we could have tested the return value, but choose to check the output only
    if [[ -z "$deli" ]]; then
        # deli is empty so something went wrong
        local error=$(echo "$output" | grep 'error' )
        if [[ -n "$error" ]]; then
            # we got error message from Opaali API
            emsg=$(echo "$error" | cut -d\: -f2)
        else
            # something went wrong with curl (now testing return value would have beeen useful...)
            emsg="unknown error"
        fi
        return 1
    fi
    return 0

}
```
# Getting Delivery Status
Like in [Getting started with Opaali API - Part 3]({{ site.baseurl }}{% post_url 2017-08-04-Getting-Started-Part3 %}) we check the delivery status by polling. (_This is not very useful here, because we will not get the status __DeliveredToTerminal__ by polling, and if sending failed we would normally have noticed it already._) 
```bash
# get delivery status
function deliveryInfo {
    #param 1: resourceURL
    #global: deliveryStatus
    
    # call Opaali API and capture the interesting parts from the output"
    local output=$(curl -k -s --header 'Accept: application/json' --header "Authorization: Bearer $access_token" "$1/deliveryInfos")
    deliveryStatus=$(echo "$output" | grep deliveryStatus | cut -d\: -f2- | tr -d "\" ")
    
    if [[ -z "$deliveryStatus" ]]; then
        defiveryStatus="unknown status"
    fi
}
```
# Main Program
This is finally the main program. We
1. parse the command line parameters
2. read application specific credentials from a configuration file
3. (for simplicity) always authenticate, even if an existing _access_token_ was still valid (if this fails we will exit the script)
4. make an outboundMessageRequest to send the message
5. and finally waste our time explicitly checking the delivery status
 
 
```bash
# main program
function main {
    #params: all command line parameters
    parse_arguments "$@"
    
    read_credentials "${CREDENTIALS_FILE}"
    
    emsg=""
    authenticate "$applicationUserName" "$applicationPassword"
    
    if [[ "$?" -ne 0 ]]; then
        error_exit "$0" "$emsg"
    fi
    
    emsg=""
    outboundMessageRequest "${RECIPIENT}" "${MESSAGE}"

    if [[ "$?" -ne 0 ]]; then
        error_exit "$0" "$emsg"
    fi    

    deliveryInfo "$deli"
    echo "SENT: ${RECIPIENT} ${deliveryStatus}"
}

# call main program
main "$@"

# end of script
```
# Using send.sh
When <code>send.sh</code> is run without any parameters, the usage syntax is shown:

```bash
$ ./send.sh
Usage: ./send.sh -s sender -r recipient [-m message] [-f filename]
```

Here are a couple of usage examples:
* send with message content on command line:
<br/><code>send –s 0401235 –r 0401234 –m Hello!</code>
* from file:
<br/><code>send –s 0401235 –r 0401234 –f msg.txt</code>
* from stdin:
<br/><code>date | send –s 0401235 –r 0401234</code> 
* using an alhanumeric sender name (you need to escape the '$'-character to guard it from shell expansion)
<br/><code>send –s '$Opaali' –r 0401234 –m Hello!</code>



