---
layout:     post
title:      "Absolute Beginners"
subtitle:   
date:       2017-07-24 12:00:00
author:     "JPLa"
header-img: "img/PortalQuickGuideBanner.png"
---
Lets assume you have obtained an Opaali account, have absolutely no previous experience with Opaali, and want to send a simple text message to test the Opaali API.

If you haven't already, go to the [Resources section](https://developer.opaali.telia.fi/resources) of the Opaali Portal and download and read the document called _"Opaali Portal Quick Guide"_.

<!--![Opaali Portal Quick Guide]({{ site.url }}/Opaali/img/PortalQuickGuide.png)-->
![Opaali Portal Quick Guide]({{ site.url }}{{ site.ref_path }}/img/PortalQuickGuide.png)
*Opaali Portal Quick Guide*

----
__Chapter 1__ of the _Opaali Portal Quick Guide_ tells you how to set up your _Partner Profile_. You need to do that before you can start creating _applications_, so we'll assume you've already done that.

## Creating Your First Test Application

__Chapter 2__ lists the most common _use cases_. Your first test would probably be sending a _Mobile Terminated_ __(MT)__ message to your phone. Typically this would be a __BULK MT__ message:

<!--![BULK MT SMS Example]({{ site.url }}/Opaali/img/BULKMTusecase.png)-->
![BULK MT SMS Example]({{ site.url }}{{ site.ref_path }}/img/BULKMTusecase.png)
*BULK MT SMS Example*

----
#### __WARNING__
__Once you create an _application_ in Opaali you cannot delete or rename it by yourself! So at least choose the name for your application carefully!__
_If you do need to have an application deleted please turn to the administration at_ [{{site.opaali_tuki}}](mailto:{{site.opaali_tuki}})

----
__Chapter 3__ guides you in _creating an application_. Follow through up to the end of _Chapter 3.3_. Now you should have and application which can send BULK MT SMS messages and you can see your _Application Credentials_ on the page:

<!--![BULK MT Send Endpoints]({{ site.url }}/Opaali/img/SendEndpoints.png)-->
![BULK MT Send Endpoints]({{ site.url }}{{ site.ref_path }}/img/SendEndpoints.png)
*BULK MT Send Endpoints*

----
#### __ANOTHER WARNING__
__Please do not publicly reveal your application credentials to anyone else, because then they can send messages on your account! You are responsible for all activity that is tied to the use of these credentials.__

_(And Please do not use the credentials you might see in these examples, either.)_

----

At this point we should be ready for actually trying out using the _Opaali API_. As you can see from the diagram earlier in this article, to send a message you will need to make two correctly formatted __HTTP requests__:
1. one for authentication (to get an _access token_)
2. another for actually sending the message
3. (an optional third one if you want to check the _delivery status_ of the sent message)

_We will go further into the details in future Opaali Blog posts..._




 


