# http_proxy_server
NDSU CSCI 848 Assignment 1

Eclipse, Java, gradlew project. 

Implement a multi-threaded HTTP proxy server that passes requests and data between web clients and web servers.

## Running
I'm using jdk17 and gradle 7.6, but you should be able to use whatever jdk/gradle version as long as the two are compatible.

`./gradlew run`

## Request against server
Get request `curl -x http://localhost:80 http://www.neverssl.com/online/`  

Test non-get request `curl -x http://localhost:80 -X POST http://www.neverssl.com/online/`
