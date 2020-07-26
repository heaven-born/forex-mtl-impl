# forex-mtl-impl

Proxy for https://hub.docker.com/r/paidyinc/one-frame 

### Notes

* In order to work around limitation of one-frame API there is a cache that is refreshed by timer. Following confg options are available:
    *  cache-expiration-time - default 5 min
    *  rates-request-interval - default 4 min  (about 360 request per day)
    *  rates-request-retry-interval - default 10 sec
* The cache is purely functional, so it had to extracted into a separate package.
       
