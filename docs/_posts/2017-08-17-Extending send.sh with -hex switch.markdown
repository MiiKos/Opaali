---
layout:     post
title:      "Extending send.sh with -hex switch"
subtitle:   "(and åäö support too)"
date:       2017-08-17 10:10:00
author:     "JPLa"
header-img: "img/SendScript2.png"
---
I promised to add more functionality to the send.sh script, so I thought that adding the <code>-hex</code> switch would be simple enough. Not quite, as the hex string I tested with happened to be text containing the scandinavian characters __ö__ and __ä__ _which lead to the dreaded __Bad Request__ error_ from Opaali API. 

```
HTTP/1.1 400 Bad Request
content-type: text/xml
Content-Length: 11

```

Opaali API seems to want special characters as _unicode escape sequences_:

```
'å' -> "\\u00E5"
'ä' -> "\\u00E4"
'ö' -> "\\u00F6"
'Å' -> "\\u00C5"
'Ä' -> "\\u00C4"
'Ö' -> "\\u00D6"
```
There are more, but these are the most common ones used in texts written in _Finnish_ or _Swedish_.

# Those _@#!$_ åäö characters

These conversions can be applied to the content of the MESSAGE variable by adding this single line:
```bash
MESSAGE=$(echo -n "$MESSAGE" | sed 's/å/\\u00e5/g; s/ä/\\u00e4/g; s/ö/\\u00f6/g; s/Å/\\u00c5/g; s/Ä/\\u00c4/g; s/Ö/\\u00d6/g')
```
_(I'm counting on writing those special characters as is actually working in the shell script. If not, you may need to replace them with their character codes, the actual values of which may depend on your platform.)_

# There's more!

While we are at it, we might as well escape characters that have special meaning in JSON. (Although you might as well escape them on the send.sh command line already. And I left out the backspace escape as I couldn't get it to work nicely. _Who would even want to use a backspace in a text message?_)
 
```bash
MESSAGE=$(echo -n "$MESSAGE" | sed 's/\"/\\\"/g; s/\\/\\\\/g; s/\//\\\//g')
```
You will want to insert this line before the previous one so that you won't break the unicode escapes by rewriting their backslashes.
(_Don't worry -- even I am not sure if I fully understand what happens there..._)

# Oh yes -- the hex bit

I almost forgot what I originally started to do! We need to add the new command line switch <code>-hex</code> and convert the hex data into text. (_Hex input data is actually more useful for sending a binary message, but we'll return to that in some later episode of this story..._)

Converting the hex data into text can be done using the <code>xxd</code> command:
```bash
MESSAGE=$(echo -n "$MESSAGE" | xxd -r -p)
```
(_You can learn how <code>xxd</code> works by running it with the --help parameter: <code>xxd --help</code>_)


# Changes

After adding the <code>-hex</code> switch processing the <code>diff</code> between the original and new script is this:
```
$ diff -b send.sh.orig send.sh
37c37
<     echo "Usage: $1 -s sender -r recipient [-m message] [-f filename]" >/dev/stderr
---
>     echo "Usage: $1 -s sender -r recipient [-m message] [-f filename] [-hex message in hex]" >/dev/stderr
97a98,106
>                 -hex)
>                 shift
>                 if [[ -n "$1" ]]; then
>                     MESSAGE=$(echo -n "$1" | xxd -r -p)
>                 else
>                     error_exit "$0" "error decoding hex message" "$1"
>                 fi
>                 shift
>                 ;;
107,108c116
<                 -c|\
<                 -hex)
---
>                 -c)
150a159,164
>         # escape (some) JSON reserved chars
>         MESSAGE=$(echo -n "$MESSAGE" | sed 's/\"/\\\"/g; s/\\/\\\\/g; s/\//\\\//g')
>
>         # convert scandinavian chars to unicode
>         MESSAGE=$(echo -n "$MESSAGE" | sed 's/å/\\u00e5/g; s/ä/\\u00e4/g; s/ö/\\u00f6/g; s/Å/\\u00c5/g; s/Ä/\\u00c4/g; s/Ö/\\u00d6/g')
>
```
I'll update these changes to the repository where you can read the script in full.
<br/>
_(...did anybody notice that I'm not really testing if <code>xxd</code> fails? 
)_

# Examples
Lets now convert some text into hex data and try sending it:
```bash
$ echo "Soittopyyntö äänipostiin" | xxd -p
536f6974746f7079796e74c3b620c3a4c3a46e69706f737469696e0a

$ ./send.sh -s '$jpla' -r 0401234567 -hex 536f6974746f7079796e74c3b620c3a4c3a46e69706f737469696e0a
SENT: 0401234567 DeliveredToNetwork

$
```
Looks like it worked!

----

![Soittopyyntö]({{ site.url }}{{ site.ref_path }}/img/soittopyynto.png)

----

_See [Writing a send application as a shell script]({{ site.baseurl }}{% post_url 2017-08-07-Writing a send application as a shell script %}) for the previous episode._
