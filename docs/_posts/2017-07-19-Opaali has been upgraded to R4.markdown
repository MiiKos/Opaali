---
layout:     post
title:      "Opaali updated to Rel4 (with Telia branding)"
subtitle:   
date:       2017-07-19 12:00:00
author:     "JPLa"
header-img: "img/NewPortal.png"
---
As was predicted in the previous post, there was a major upgrade to Opaali platform on 2017-06-20 (to version __R4__ a.k.a. __Rel4__).
The most notable change is the new __Telia branding__, seen in the visuals but also in the new host names for the portal [https://developer.opaali.telia.fi](https://developer.opaali.telia.fi) and the API [https://api.opaali.telia.fi](https://api.opaali.telia.fi). (The old addresses _developer.sonera.fi_ and _api.sonera.fi_ still work -- I'll update the new addresses to my sample  source code later.)

If you are a registered _Opaali user_ you should have received the e-mail containing a Release note listing the changes, so I won't repeat those here.

One thing __R4__ introduced is something called __API Console__, which you can use to make Opaali API requests _without having to write an application first_. I have only briefly tried out the API Console, the documentation for it seems quite minimal (and the UI may not be the most intuitive) but _if you already know how to use the API_ you can use this to check the request and response details while writing your own application. To make it easy to use, it prefetches some of the needed parameters, such as your application credentials. The downside of this is that the requests don't look _exactly_ like the ones you need to make in your own code. I'll continue using __curl__ for my examples, but the API Console is a handy tool for checking the exact format of things like response bodies.

<!--![API Console](/img/ApiConsoleExample.png)-->
![API Console]({{ site.url }}/Opaali/img/ApiConsoleExample.png)
*Api Console*

