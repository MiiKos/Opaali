---
layout:     post
title:      "Migration to Opaali is almost done"
subtitle:   
date:       2018-10-19 06:00:00
author:     "JPLa"
header-img: "img/default.png"
---

Time flies...my [previous update to this blog]({{ site.baseurl }}{% post_url 2018-01-19-HowToContactUsNow %}) seems to have been _exactly nine months ago today_.
Most of that time I have been _busy writing code_ related to migrating some internal systems. In fact, _one of them was finally migrated to Opaali earlier this week_. There is only a couple more left, and I believe all the external customers were migrated from __Content Gateway__ to __Opaali__ _already before the summer_.


Although this blog has not been actively updated anymore, there are still a couple of things that _need to be documented better_ - I'll try to do that while I still can. 


# Not using international number format may cost more!

You may have noticed that sending messages to local numbers (_040... and so on, you know_) works without the __"tel:"__ prefix followed by the number in __international format__ (_+35840... you knew that, too_). This is so convenient that even I have been doing this.

But it turns out that Opaali thinks the message is going outside of Telia's own network -- it will be delivered, but _it will cost more!_

Because of this, some _Opaali Service Providers_ have this week received email with the following message: 

```
We have noticed that Your application <application name> use Opaali wrong way on case of MT sms sending. Number format must be “tel:+358”- started when sending to Finnish numbers and on case of other locations target country code must be use. We can see sending on format “0401234567” which is not supported  on Opaali and might cause extra expenses as number don’t be recognized as Telia subscription. Please repair sending side as soon as possible.

Documentation where number format is clearly shown: https://developer.opaali.telia.fi/resources

```
So you are urged to __fix this__ (_I don't know how long they will wait -- I expect them to be reasonable with this, but if you keep ignoring them they may eventually cut your service off!_)


# Restrictions to access_token fetching

I have earlier shown code examples where the _access_token_ is retrieved every time before sending the actual message. This is OK, if you send messages seldom enough. My [send.sh](https://github.com/MiiKos/Opaali/tree/master/sample_applications/send) script does it this way.

But if too many applications keep doing this, it has _a negative effect on the performance of Opaali_ as a whole. To improve the performance, the platform maintainers have started enforcing the rule that __you should not renew your token before it is about to expire__. Initially those who fetch the token too frequently have been contacted and _asked to change their implementation_. If needed, there will be _a hard limit_ enforced and if the overall Opaali system performance keeps degrading the limit will become tighter.

_I should probably update my send.sh script and write about it in a future blog post._  

# Using http_adapter in migrating old services

Along with writing code for that internal system, that was migrated earlier this week, most of my spring was spent on _enhancing the [http_adapter](https://github.com/MiiKos/Opaali/tree/master/sample_applications/http_adapter)_ so that it could be used in migrating some of the _other_ internal systems. I have already almost forgotten how to use this new functionality, so I at least need to _write another blog post_ documenting them.
  

# Binary messaging extensions

Some time last winter (_...I can't even remember when, exactly...oh, looks like it was in [R4SP2](https://developer.opaali.telia.fi/f/files/resources/Opaali_ReleaseNotes_R4SP2)_) Opaali got an update to _binary message handling_. The [OMA Standard](http://www.openmobilealliance.org/release/MessagingREST/V1_0-20130709-C/OMA-TS-REST_NetAPI_Messaging-V1_0-20130709-C.pdf), which OpaaliAPI is based on, does not define how incoming binary messages should be handled. Originally they were _treated as text_ in Opaali, but as _Content Gateway_ already supported receiving binary messages, _extended binary message support_ was implemented in OpaaliAPI. At the same time, the sending side received a couple of new extensions, too. _I'll write more about this in a separate blog post to come_. 

# Finally

As the _migration from Content Gateway to Opaali_ has been getting closer to its completion, my role in this has also been becoming smaller. I am not part of the official _[Opaali support channel]({{ site.url}}{{ site.ref_path }}/contact)_, but I have been consulting them in various support cases. As I said, I'm intending to write at least a couple more posts here about details needing more documentation.     
