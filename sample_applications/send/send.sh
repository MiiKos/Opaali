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

    echo "Usage: $1 -s sender -r recipient [-m message] [-f filename]" >/dev/stderr
    exit 1
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
        usage $0
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
                MESSAGE="$1"
                shift
                ;;
                -f)
                shift
                MESSAGE="$(<$1)"
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

        # sender and recipient cannot be empty
        if [[ -z "${SENDER}"  || -z "${RECIPIENT}" ]]; then
            usage "$0"
        fi
    fi
}

# main program
function main {
    #params: all command line parameters
    parse_arguments "$@"
}

# call main program
main "$@"

echo no actual functionality implemented yet..
