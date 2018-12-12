# packto7bits

This is a quick and dirty tool for packing and unpacking 7-bit text as specified in 3GPP TS 23.038 V5.0.0

$ ./packto7bits.sh
Usage: ./packto7bits.sh [-r] hexstring
      pack 7-bit characters into 8-bit bytes and output as hexstring
      hexstring = text encoded as a string of two character hexadecimal values
      -r = unpack 8-bit bytes into 7-bit characters

Example:
- you can give it a hexstring to be packed
$ echo "Hello World!" | xxd -p
48656c6c6f20576f726c64210a

$ ./packto7bits.sh 48656c6c6f20576f726c64210a
c8329bfd065ddf723639a400

- and you can unpack the hexstring to get the original content
$ ./packto7bits.sh -r c8329bfd065ddf723639a400 | xxd -r -p
Hello World!

