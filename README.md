# Caching proxy

## How it works

Caching proxy accepts HTTP requests (see `CachingProxyController`), puts them to a queue and responds immediately.
The queue is `LinkedBlockingQueue`. Another option could be `ConcurrentLinkedQueue`. `ConcurrentLinkedQueue` is non-blocking,
but consumers have to perform polling constantly.

The queued requests are concurrently consumed (see `BackendService`) and sent to the Backend in `backend.maxConcurrentRequests` 
(default `10`) threads. If request is failed, then it goes back to the queue. There is configurable `RetryPolicy`.
Default is `ExponentialBackoffRetry`: delays between attempts of the same request are increased exponentially starting
from `backend.initialDelayMillis` (default 1 second), max number of attempts is `backend.numOfRetries` (default `8`).

If `backend.sequentialFailuresThreshold` (default `7`) sequential requests are failed, then we assume that the Backend
is down. In such a mode we send one request per `backend.downDelayMillis` (default 1 second) until the Backend is up.   

## Standalone build & run

Java 11 or newer is required.

`gradlew clean build`

Above includes autotests.

`java -Dbackend.addr=http://backend:8080 -jar build\libs\caching-proxy-1.0-SNAPSHOT.jar`

## Docker build & run

`docker build -t rogaikopyta/caching-proxy .`

`docker run -t --rm -p 8080:8080 --env backend.addr=http://backend:8080 rogaikopyta/caching-proxy`
