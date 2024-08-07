# jaslock

[![Tests](https://img.shields.io/github/actions/workflow/status/snower/jaslock/build-test.yml?label=tests)](https://github.com/snower/jaslock/actions/workflows/build-test.yml)
[![GitHub Repo stars](https://img.shields.io/github/stars/snower/jaslock?style=social)](https://github.com/snower/jaslock/stargazers)

High-performance distributed sync service and atomic DB. Provides good multi-core support through lock queues, high-performance asynchronous binary network protocols. Can be used for spikes, synchronization, event notification, concurrency control. https://github.com/snower/slock

# Install

```xml
<dependency>
    <groupId>io.github.snower</groupId>
    <artifactId>jaslock</artifactId>
    <version>1.1.1</version>
</dependency>
```

# Client Lock

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
        SlockClient client = new SlockClient("127.0.0.1", 5658);
        try {
            client.open();
            Lock lock = client.newLock("test", 5, 5);
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

# Replset Client Lock

```java
package main;

import io.github.snower.jaslock.SlockClient;
import io.github.snower.jaslock.Event;
import io.github.snower.jaslock.Lock;
import io.github.snower.jaslock.SlockReplsetClient;
import io.github.snower.jaslock.exceptions.SlockException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        SlockReplsetClient replsetClient = new SlockReplsetClient(new String[]{"127.0.0.1:5658"});
        try {
            replsetClient.open();
            Lock lock = replsetClient.newLock("test", 5, 5);
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

# Async Callback Lock

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
        SlockClient client = new SlockClient("127.0.0.1", 5658);
        client.enableAsyncCallback();
        try {
            client.open();
            Lock lock = client.newLock("test", 5, 5);
            lock.acquire(callbackFuture -> {
                try {
                    callbackFuture.getResult();
                    lock.release(callbackFuture1 -> {
                        try {
                            callbackFuture1.getResult();
                            System.out.println("succed");
                        } catch (IOException | SlockException e) {
                            e.printStackTrace();
                        } finally {
                            client.close();
                        }
                    });
                } catch (IOException | SlockException e) {
                    e.printStackTrace();
                    client.close();
                }
            });
        } catch (IOException | SlockException e) {
            e.printStackTrace();
            client.close();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored1) {}
    }
}
```

# Async Future Lock

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
import java.util.concurrent.ExecutionException;

public class App {
    public static void main(String[] args) {
        SlockReplsetClient replsetClient = new SlockReplsetClient(new String[]{"127.0.0.1:5658"});
        replsetClient.enableAsyncCallback();
        try {
            replsetClient.open();
            Lock lock = replsetClient.newLock("test", 5, 5);
            CallbackFuture<Boolean> callbackFuture = lock.acquire(null);
            callbackFuture.get();
            callbackFuture = lock.release(null);
            callbackFuture.get();
        } catch (IOException | SlockException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            client.close();
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
        SlockReplsetClient replsetClient = new SlockReplsetClient(new String[]{"127.0.0.1:5658"});
        try {
            replsetClient.open();
            Event event1 = replsetClient.newEvent("test", 5, 5, true);
            event1.clear();

            Event event2 = replsetClient.newEvent("test", 5, 5, true);
            event2.set("{\"value\": 10}");

            event1.wait(10);
            System.out.println(event1.getCurrentLockDataAsString());
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