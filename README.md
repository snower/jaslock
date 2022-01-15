# jaslock

High-performance distributed sync service and atomic DB. Provides good multi-core support through lock queues, high-performance asynchronous binary network protocols. Can be used for spikes, synchronization, event notification, concurrency control. https://github.com/snower/slock

# Install

```xml
<dependency>
    <groupId>io.github.snower</groupId>
    <artifactId>jaslock</artifactId>
    <version>1.0.4</version>
</dependency>
```

# Lock

```java
package main;

import io.github.snower.jaslock.SlockClient;
import io.github.snower.jaslock.Event;
import io.github.snower.jaslock.Lock;
import io.github.snower.jaslock.SlockReplsetClient;
import io.github.snower.jaslock.SlockClient;
import io.github.snower.jaslock.exceptions.SlockException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        SlockClient client = new SlockClient("172.27.214.150", 5658);
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
    }
}
```

```java
package main;

import io.github.snower.jaslock.SlockClient;
import io.github.snower.jaslock.Event;
import io.github.snower.jaslock.Lock;
import io.github.snower.jaslock.SlockReplsetClient;
import io.github.snower.jaslock.SlockReplsetClient;
import io.github.snower.jaslock.exceptions.SlockException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        SlockReplsetClient replsetClient = new SlockReplsetClient(new String[]{"172.27.214.150:5658"});
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
    }
}
```

# Event

```java
package main;

import io.github.snower.jaslock.SlockClient;
import io.github.snower.jaslock.Event;
import io.github.snower.jaslock.Lock;
import io.github.snower.jaslock.SlockReplsetClient;
import io.github.snower.jaslock.SlockReplsetClient;
import io.github.snower.jaslock.exceptions.SlockException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        SlockReplsetClient replsetClient = new SlockReplsetClient(new String[]{"172.27.214.150:5658"});
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
    }
}
```

# License

slock uses the MIT license, see LICENSE file for the details.