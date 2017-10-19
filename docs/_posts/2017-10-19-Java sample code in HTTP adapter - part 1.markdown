---
layout:     post
title:      "Java sample code in HTTP adapter - part 1"
subtitle:   (template for an API call)
date:       2017-10-19 10:00:00
author:     "JPLa"
header-img: "img/JavaSampleCode.png"
---

Opaali-support used to get a lot of questions like "_Why does OpaaliAPI not work? We always get 400 Bad Request_" and occasionally these questions came with _logs of the HTTP traffic_.

Although we did have access to the Opaali platform vendor's _technical support_, the _Opaali migration team_ tried verifying and solving these problems by ourselves first. So I wrote tools where I could feed in the HTTP request data obtained from our customers to see what went wrong. And in our responses to said customers, we often needed to attach sample code to show how to successfully use the _Opaali REST API_. Out of these eventually emerged the Java library code that is used by the [HTTP adapter sample application](https://github.com/MiiKos/Opaali/tree/master/sample_applications/http_adapter).

# A template based approach

To make it easy to write calls to the _Opaali REST API_ based on the examples in the official _API documentation_ (or _logs_ sent to us by customers), the _OpaaliAPI Java package_ uses text string based templates of the actual HTTP request/response conversation between the application and the API.


## Implementing an API Call function

So the templates are based on the examples in the API documentation by replacing values that change with template variables.
As an example, outboundMessageRequest is documented like this:
![API example](/img/APIReference.png)

The code in _OpaaliAPI Java package_ follows this pattern:
* all variables are passed as method arguments
* there is a request specific template string with variables to be expanded with given values
* a new template is created (from the template string) along with a HashMap for variable names and their values
* finally an API call is made with the template and variables as arguments

Code from <code>MessagingAPI</code> class for _outboundMessageRequest_ method looks something like this:

```java

   /*    
     * outboundMessageRequest
     *  - access_token
     *  - config - request specific config variables or null for default config
     *  - address/addressList
     *  - senderAddress
     *  - senderName
     *  - receiptRequest
     *  - clientCorrelator
     *  - chargingInformation
     *  - outboundSMSMessage
     */
    public static String outboundMessageRequest(AccessToken access_token,
                                                final HashMap <String, String> config,
                                                String[] addressList,
                                                String senderAddress, 
                                                String senderName,
                                                ApiCall.ReceiptRequest receiptRequest,
                                                String clientCorrelator,
                                                ApiCall.ChargingInfo chargingInfo,
                                                Message message) {

        String[] MTTemplate = {
            // API request for sending MT messages
            "POST https://${API_HOST}/production/messaging/v1/outbound/${SENDERADDRESS_ENCODED}/requests HTTP/1.1",
            "Host: ${API_HOST}",
            "Content-type: application/json",
            "Accept: application/json",
            "Authorization: Bearer ${ACCESS_TOKEN}",
            "",
            "{",
            "    \"outboundMessageRequest\":",
            "        {\"address\":[${RECIPIENTLIST}],",
            "         \"senderAddress\":\"${SENDERADDRESS}\",",
            "         ${MESSAGE}",
            "         ${SENDERNAMESTRING}${CHARGINGINFO}${DLRREQUESTSTRING}${CLIENTCORRELATOR}",
            "    }",
            "}"
        };
        
        senderAddress = (senderAddress != null ? senderAddress : DEFAULT_SENDER);
        
        Template tmpl = new Template(MTTemplate);
        
        
        HashMap <String, String> vars = (config != null ? (HashMap<String, String>)config.clone() : Config.getConfig());
        vars.put("ACCESS_TOKEN", access_token.renew());
        vars.put("SENDERADDRESS", senderAddress);
        vars.put("RECIPIENTLIST", makeList(addressList));
        vars.put("MESSAGE", message.toString());
        vars.put("SENDERNAMESTRING", (senderName != null ? ",\"senderName\":\""+senderName+"\"" : ""));
        vars.put("CHARGINGINFO", (chargingInfo != null ? chargingInfo.toString() : ""));
        vars.put("DLRREQUESTSTRING", (receiptRequest != null ? receiptRequest.toString() : ""));
        vars.put("CLIENTCORRELATOR", (clientCorrelator != null ? ", \"clientCorrelator\":\""+ApiCall.escapeJSON(clientCorrelator)+"\"" : ""));
        
        try {
            vars.put("SENDERADDRESS_ENCODED", URLEncoder.encode(senderAddress, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            vars.put("SENDERADDRESS_ENCODED", URLEncoder.encode(senderAddress));
        }
        
        
        return ApiCall.makeRequest(access_token, tmpl, vars);
        
    }
    
```

There are actually three variants of each API request:
1. one with built in session (access_token)
2. a static one with no configuration data
3. a static one with configuration data supplied

The one with no configuration data was the original one which I used in testing customers' examples. When I made the http_adapter on top of this I needed a way to store session and configuration data.

The <code>ApiCall</code> class and its <code>makeRequest</code> method make the HTTP Request to Opaali but hide details such as retrying when session has expired and conversions for characters that are not accepted in JSON syntax.

Class <code>HttpRequest</code> also has a <code>makeRequest</code> which makes the actual HTTP requests. (_Due to the template approach this code is a bit more complicated than making a HTTP Request in Java normally would need to be. Lets not think about that now, though._) A <code>HttpResponse</code> class stores the _return value_, _response headers_ and _response body_.

Although there is also an implementation for the <code>AuthAPI</code>, session handling with its retries is actually handled by the <code>AccessToken</code> class. Getting it to work took some trial&error, so maybe it is better not to try explain how I think it works...

## Log writing...
Although not visible in the example above, I guess I need to say something about the <code>LogWriter</code> interface and the <code>Log</code> class. These implement a pluggable logging system with a default implementation, so that when you call one of the AuthAPI or MessagingAPI methods from your code, it will by default write log information to _stderr_ for you to see. You can provide your own _LogWriter_ implementation for your application so that you can write into a log file instead. 
This works well enough when you have only one copy of the library running in the same JVM. Afterwards, when I got the idea that I could run several processes of the _http_adapter_ in different ports I ran into trouble with this logging code. Lets just say that it might be a better idea to run several JVM instances instead. (_Or, as you have the source code, you can rewrite a multiprocessing friendly version by yourself._)

# Your turn

Not all of the _Opaali API_ is implemented in this library, only what we have needed so far. But you can easily expand it yourself following the model of existing functions.

And even if you don't want to use this code (_because it is inefficient, ugly, and the author clearly doesn't seem to know how to write Java code..._), you may want to use it as _a reference implementation_ that actually works, while writing a better implementation by yourself. _If your API requests fail, compare them to this code, before contacting [Opaali support](mailto:{{site.opaali_tuki}})._ 

