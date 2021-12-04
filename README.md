# jaslock

High-performance distributed sync service and atomic DB. Provides good multi-core support through lock queues, high-performance asynchronous binary network protocols. Can be used for spikes, synchronization, event notification, concurrency control. https://github.com/snower/slock

# Lock

```
Client client = new Client("172.27.214.150", 5658);
try {
    client.open();
    Lock lock = client.newLock("test".getBytes(StandardCharsets.UTF_8), 5, 5);
    lock.acquire();
    lock.release();
} catch (IOException | SlockException e) {
    e.printStackTrace();
} finally {
    client.close();
}
```
```
ReplsetClient replsetClient = new ReplsetClient(new String[]{"172.27.214.150:5658"});
try {
    replsetClient.open();
    Lock lock = replsetClient.newLock("test".getBytes(StandardCharsets.UTF_8), 5, 5);
    lock.acquire();
    lock.release();
} catch (SlockException e) {
    e.printStackTrace();
} finally {
    replsetClient.close();
}
```

# Event

```
ReplsetClient replsetClient = new ReplsetClient(new String[]{"172.27.214.150:5658"});
try {
    replsetClient.open();
    Event event1 = replsetClient.newEvent("test".getBytes(StandardCharsets.UTF_8), 5, 5, true);
    event1.clear();

    Event event2 = replsetClient.newEvent("test".getBytes(StandardCharsets.UTF_8), 5, 5, true);
    event2.set();
    
    event1.wait(10);
} catch (SlockException e) {
    e.printStackTrace();
} finally {
    replsetClient.close();
}
```

# License

slock uses the MIT license, see LICENSE file for the details.