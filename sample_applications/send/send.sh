#!/bin/bash
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

# print usage instructions and exit
function usage {
    #param 1: commandName

    echo "Usage: $1 -s sender -r recipient [-bin|-text] [-m message] [-smart smart port] [-udh udh in hex] [-f filename] [-hex message in hex]" >/dev/stderr
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

# parse arguments
function parse_arguments {
    #params: all command line parameters
    if [ "$#" -lt 2 ]; then
        usage "$0"
    else
        # parse arguments
        
        # message sending mode: TEXT|BIN|FLASH (TEXT is default)
        MODE="TEXT"
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
                -hex)
                shift
                if [[ -n "$1" ]]; then
                    # HEX will contain MESSAGE as hex encoded
                    HEX="$1"
                else
                    error_exit "$0" "missing hex message" "$1"
                fi
                shift
                ;;
                -udh)
                shift
                if [[ -n "$1" ]]; then
                    # User Data Header
                    UDH="$1"
                    # UDH implies MODE BIN
                    MODE="BIN"
                else
                    error_exit "$0" "missing UDH" "$1"
                fi
                shift
                ;;
                -smart)
                shift
                declare -i SMART
                if [[ -n "$1" ]]; then
                    # smart messaging port number (in decimal)
                    SMART="$1"
                    # SMART implies MODE BIN
                    MODE="BIN"
                    # SMART is implemented as UDH (overwrites possibly existing UDH)
                    if [[  $1 -lt 256 ]]; then
                        # 8-bit port number
                        UDH=$(printf '040402%02x%02x' $SMART $SMART)
                    else
                        # 16-bit port number
                        UDH=$(printf '060504%04x%04x' $SMART $SMART)
                    fi
                else
                    error_exit "$0" "missing port in -smart" "$1"
                fi
                shift
                ;;
                -bin)
                MODE="BIN"
                shift
                ;;
                -text)
                MODE="TEXT"
                shift
                ;;
                -t|\
                -h|\
                -nrq|\
                -vp|\
                -ddt|\
                -c)
                not_implemented "$0" "$1"
                ;;
                *)
                usage "$0"
                ;;
            esac
        done

        # post processing arguments
        
        if [[ -z "${SENDER}" ]]; then
            error_exit "$0" "sender missing"
        fi
        if [[ -z "${RECIPIENT}" ]]; then
            error_exit "$0" "recipient missing"
        fi

        # add "tel:" prefix to sender and/or recipient if they are not short numbers (i.e. contain '+')
        if [[ "${SENDER:0:1}" == '+' ]]; then
            SENDER="tel:${SENDER}"
        fi
        if [[ "${RECIPIENT:0:1}" == '+' ]]; then
            RECIPIENT="tel:${RECIPIENT}"
        fi

        # if message is empty read it from stdin
        if [[ -z "${MESSAGE}" && -z "${HEX}" ]]; then
            readMsg MESSAGE
        fi

        # implement CGW style of specifying alphanumeric sender name
        if [[ ${SENDER:0:1} == '$' ]]; then
            senderNameString=",\"senderName\":\"${SENDER:1}\""
            senderAddress="tel:+358000000000"
        else
            senderNameString=""
            senderAddress="${SENDER}"
        fi
        
        # escape (some) JSON reserved chars
        MESSAGE=$(echo -n "$MESSAGE" | sed 's/\"/\\\"/g; s/\\/\\\\/g; s/\//\\\//g')

        # convert scandinavian chars to unicode
        MESSAGE=$(echo -n "$MESSAGE" | sed 's/å/\\u00e5/g; s/ä/\\u00e4/g; s/ö/\\u00f6/g; s/Å/\\u00c5/g; s/Ä/\\u00c4/g; s/Ö/\\u00d6/g')

        # build final MESSAGE (including UDH if needed) 
        buildMessage "${MESSAGE}" "${HEX}" "${MODE}" "${UDH}"
    fi
}

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
    local output=$(curl -k -s -d grant_type=client_credentials https://api.opaali.telia.fi/autho4api/v1/token --header "Content-Type:application/x-www-form-urlencoded" --header "Authorization: Basic $basic_auth" | grep -E 'access_token|error')
    
    # post processing: check for success or failure
    # we could test the return value, but choose to check the output only
    
    # try grabbing access_token from the output
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

# build a message and store it in global variable MESSAGE
function buildMessage {
    #param 1: message in TEXT format
    #param 2: message in HEX format
    #param 3: MODE
    #param 4: UDH or (null)
    
    if [[ -n "$4" || "$3" == BIN ]]; then
        # UDH implies binary message
        UDH="$4"
        if [[ -z "$2" ]]; then
            # hex encode text
            HEX=$(echo -n "$1" | xxd -p | tr -d "\n")
        else
            HEX="$2"
        fi
        MESSAGE=$(echo -n "$UDH$HEX" | xxd -r -p | base64 | tr -d "\n")
    else
        # text message
        if [[ -z "$1" && -n "$2" ]]; then
            # decode hex message into text
            MESSAGE=$(echo -n "$2" | xxd -r -p)
        else
            MESSAGE="$1"
        fi
    fi
    return 0
}


# make an outboundMessageRequest
function outboundMessageRequest {
    #param 1: recipientAddress
    #param 2: message
    #param 3: mode - BIN or TEXT
    #global: senderAddress - sender address string
    #global: senderNameString - sender name string with comma or empty string
    #global: access_token - access token string
    #global: deli - resource URL to be used when querying status

    # urlencode + and :
    local sender=$(echo -n "$senderAddress" | sed -e s/\+/%2B/g -e s/\:/%3A/g)

    local outboundSMStype=""
    # choose a text or binary message
    if [[ $3 == "BIN" ]]; then
        outboundSMStype="outboundSMSBinaryMessage"
    else
        outboundSMStype="outboundSMSTextMessage"
    fi
    
    
    # call Opaali API and capture the interesting parts from the output"
    local output=$(curl -k -s -d "{\"outboundMessageRequest\":{\"address\":[\"$1\"],\"senderAddress\":\"$senderAddress\",\"$outboundSMStype\":{\"message\":\"$2\"}$senderNameString}}" --header 'Content-Type:application/json' --header "Authorization: Bearer $access_token" https://api.opaali.telia.fi/production/messaging/v1/outbound/$sender/requests | grep -E 'resourceURL|error')
 
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
    outboundMessageRequest "${RECIPIENT}" "${MESSAGE}" "${MODE}"

    if [[ "$?" -ne 0 ]]; then
        error_exit "$0" "$emsg"
    fi    

    deliveryInfo "$deli"
    echo "SENT: ${RECIPIENT} ${deliveryStatus}"
}

# call main program
main "$@"

# end of script
