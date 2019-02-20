---
layout:     post
title:      Using http_adapter in sandbox mode
subtitle:   
date:       2019-02-20 08:00:00
author:     "JPLa"
header-img: "img/API_MODE.png"
---
While testing some modifications to the [http_adapter](https://github.com/MiiKos/Opaali/tree/master/sample_applications/http_adapter) I had the need to test sending MT messages _without actually letting them reach the phone_. Opaali API has the _sandbox mode_ for such a need.

There already was the __API_HOST__ configuration parameter which was useful when the API endpoints were moved to their current ```opaali.telia.fi``` domain. Just after the hostname in _Opaali API URLs_ comes either ```/production``` or ```/sandbox```, which up to now was hardcoded as _/production_ in the _http_adapter_ source code.

(_But notice that the_ authentication endpoint _is common to both_ sandbox _and_ production _APIs so I couldn't just rewrite all the URLs on the fly._)

I only needed to add one line to ```Config.java```:
```diff
--- a/sample_applications/http_adapter/src/OpaaliAPI/Config.java
+++ b/sample_applications/http_adapter/src/OpaaliAPI/Config.java
@@ -44,6 +44,7 @@ public class Config {
         // set default config
         configSettings = new HashMap <String, String>();
         configSettings.put("API_HOST", "api.opaali.telia.fi");
+        configSettings.put("API_MODE", "production");
         serviceSettings = new HashMap <String, HashMap<String, String>>();
     }
```
and change one line in ```MessagingAPI.java```:
```diff
--- a/sample_applications/http_adapter/src/OpaaliAPI/MessagingAPI.java
+++ b/sample_applications/http_adapter/src/OpaaliAPI/MessagingAPI.java
@@ -139,7 +139,7 @@ public class MessagingAPI {

         String[] MTTemplate = {
             // API request for sending MT messages
-            "POST https://${API_HOST}/production/messaging/v1/outbound/${SENDERADDRESS_ENCODED}/requests HTTP/1.1",
+            "POST https://${API_HOST}/${API_MODE}/messaging/v1/outbound/${SENDERADDRESS_ENCODED}/requests HTTP/1.1",
             "Host: ${API_HOST}",
             "Content-type: application/json",
             "Accept: application/json",
```    
and now _adding one line to config.txt_ switches the http_adapter to _call the sandbox API_ instead:
```
# server port
port=8878
# API host name                
API_HOST=api.opaali.telia.fi
# sandbox or production (default)
API_MODE=sandbox
```
(_...if there were more than just the Messaging API implemented in the http_adapter I would need to change the other __URL templates__ too._)

----

I did __not__ build a new [binary release](https://github.com/MiiKos/Opaali/releases) yet, because there will be more updates to the _http_adapter_ after the current tests are finished - _mostly fixes related to_ character sets _in_ http-requests.