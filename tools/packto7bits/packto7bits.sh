#!/bin/bash
#
# packto7bits.sh - an application for packing/unpacking 7-bit characters into 8-bit bytes
#
# This is a test tool I have used for packing/unpacking of 7-bit characters as specified in 
# 3GPP TS 23.038 V5.0.0, mainly for checking SMS PDUs containing packed 7-bit text
#
# This is not intended for production use as such
# and has not been thoroughly tested, but I think 
# I have fixed those bugs I have encountered so far...
#
# Author: jlasanen
#


# print usage instructions and exit
function usage {
    #param 1: commandName

    echo "Usage: $1 [-r] hexstring" >/dev/stderr
    echo "      pack 7-bit characters into 8-bit bytes and output as hexstring" >/dev/stderr
    echo "      hexstring = text encoded as a string of two character hexadecimal values" >/dev/stderr
    echo "      -r = unpack 8-bit bytes into 7-bit characters" >/dev/stderr
    exit 1
}



# parse hexstring into an int array hxs
function parse_hexstring {
    #param 1: hexstring
    
    local hs="$1"
    if ((${#hs} % 2)); then
       echo "$0: invalid input: $hs" >/dev/stderr
       exit 2
    fi

    for (( i=0; i<${#hs}; i=i+2 )); do
        h=${hs:$i:2}
        if [ ${#h} -eq 2 ]; then
            hxs[$i/2]=$((0x$h))
        fi
    done

    #echo "${hxs[@]}"
}

# pack from 8-bit to 7-bit into int array t
function packbits {
    #params: integer array of 7-bit values
    
    declare -a hs=($@)
    
    tpos=0
    for (( pos=0; pos<${#hs[@]}; pos++ )); do
        x=$(( $pos % 8 ))
        case $x in
        0)
          t[$tpos]="${hs[$pos]}"
          tpos=$(($tpos+1))
          ;;
        [1-6])
          mask=$(( 0x3f>>(6-x) ))
          t[tpos-1]="$(( t[tpos-1] | (hs[pos] & mask ) << (8-x) ))"
          t[tpos]="$(( hs[pos] >> x ))"
          tpos=$(($tpos+1))
          ;;
        7)
          t[tpos-1]="$(( t[tpos-1] | (hs[pos] & 0x7f ) << 1 ))"
          ;;
        esac
    done
    
    #echo "t:len=${#t}:${t[@]}"
    
}

# unpack from 7-bit to 8-bit into int array t
function unpackbits {
    #params: integer array of 8-bit values

    declare -a hs=($@)
    
    tmax=$(( (${#hs[@]}/7)*8 + (${#hs[@]}%7) ))
    tpos=0
    for (( pos=0; pos<${#hs[@]}; pos++ )); do
        x=$(( $pos % 7 ))
        case $x in
        0)
          mask=0x7f
          t[tpos]="$(( (hs[pos] & mask ) ))"
          tpos=$(($tpos+1))
          if [ $tpos -lt $tmax ]; then 
              t[tpos]="$(( (hs[pos] & (mask ^ 0xff) ) >> 7 ))"
          fi
          ;;
        [1-5])  
          mask=$(( 0x7f>>x ))
          t[tpos]="$(( t[tpos] | (hs[pos] & mask ) << x ))"
          tpos=$(($tpos+1))
          if [ $tpos -lt $tmax ]; then 
              t[tpos]=$(( (hs[pos] & (mask ^ 0xff) ) >> (7-x) ))
          fi
          ;;
        6)
          t[tpos]=$(( t[tpos] | (hs[pos] & 0x01 ) << 6 ))
          tpos=$(($tpos+1))
          if [ $tpos -lt $tmax ]; then 
              t[tpos]=$(( (hs[pos] & 0xfe ) >> 1 ))
              tpos=$(($tpos+1))
          fi
          ;;
        esac
    done
    
    #echo "t:len=${#t}:${t[@]}"
}


# output an int array as hexstring
function print_as_hexstring {
    #params: int array
    
    declare -a h=($@)
    #echo "${h[@]}"

    for (( i=0; i<${#h[@]}; i++ )); do
        #if [ ${h[i]} -eq 32 ]; then
        if [ ${h[i]} -lt 16 ]; then
            printf "0%-1x" "$(( ${h[i]} ))"
        else
            printf "%-2x" "$(( ${h[i]} ))"
        fi
    done

} 


# main program

if [ -z "$1" ]; then
    usage "$0"
fi

if [ "$1" = "-r" ]; then
    # unpack
    if [ "$#" -ne 2 ]; then
        usage "$0"
    else
        parse_hexstring "$2"
        unpackbits "${hxs[@]}"
    fi
    
else
    # pack
    if [ "$#" -ne 1 ]; then
        usage "$0"
    else
        parse_hexstring "$1"
        packbits "${hxs[@]}"
    fi
fi    

print_as_hexstring "${t[@]}"

exit 0
