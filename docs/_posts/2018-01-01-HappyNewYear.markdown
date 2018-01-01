---
layout:     post
title:      "A New Year..."
subtitle:   
date:       2018-01-01 00:10:00
author:     "JPLa"
header-img: "img/newyear2018.png"
---
Some of you may have noticed that we didn't reach our target of _migrating all CGW users to Opaali by the end of year **2017**._ Looks like I will continue writing this blog some time into year **2018**...

I see that I haven't written here anything since more than _two months_ ago. Most of that time I have been busy with the User Acceptance Tests (__UAT__) for the _enhanced binary sms support_, which went live on 2017-12-08 (__R4SP2__). I'm hoping to write more about that _once we have the final updated API documentation available_.

After the UAT I have been trying to get the [http_adapter sample application](https://github.com/MiiKos/Opaali/tree/master/sample_applications/http_adapter) finalized for _production use_. It was really difficult to _find volunteers to test_ it in production use, __almost everybody wanted to wait until someone else has found all the bugs in the code__. And when it finally was taken to production use by _some brave people_ -- of course __loads of bugs__ were discovered (_and fixed_) during the first couple of days. As far as I know, _http_adapter_ is currently used in production for an internal service, so _it should be stable enough_ for others, too. _If there were more users testing the http_adapter, I'm sure the bugs would be found and fixed much faster..._

The latest release of the __http_adapter__ has some small enhacements, _of which I'm hoping to write an article about in the near future._
(_It currently supports only sending messages, but there have been requests to implement receive functionality -- I'll continue with that when I have time..._)

I should also write about __session handling__. Our vendor suspects that some of the recent _performance issues_ may have been related to some applications requesting a new **access_token** much too frequently. I know that it is easy to write bad code that continuously loops requesting new tokens (_been there; done that_) but lets try to get a new token only when the previous one has expired or is about to expire.

That's all for now. In my next post, I might also write about _what I did during my **Xmas Holiday**_...

----
