# forex-mtl-impl

Proxy for https://hub.docker.com/r/paidyinc/one-frame 

### Notes

* In order to work around limitation of one-frame API I created cache that is refreshed by timer. Following confg options are available:
    *  cache-expiration-time - default 5 min
    *  rates-request-interval - default 4 min  (about 360 request per day)
    *  rates-request-retry-interval - default 10 sec
* The cache is purely functional, so I had to extract it into a separate package. Access to to this cache should be considered as a side effect.

### Assumptions

* I assumed that all currency pairs that can be made by combining currencies from Currency object can be always returned by one-frame API.

* For simplicity I assumed that I can add unlimited number of "pair" parameters in 
"GET /rates?" request.

* There are a three options available to check how old rates are:

      1. time of requesting this rate from one-frame API
      2. time of receiving this rate from one-frame API
      3. time from "time_stamp" field in JSON received from one-from API 

    I selected #2.

* I assumed that it's ok to return all kinds of errors back to user using Internal Server error status.

* I assumed that all non 2xx responses from one-frame API are not taken into account during request-limit calculation, so I can retry requesting the endpoint on error response unlimited number of times. 
       
