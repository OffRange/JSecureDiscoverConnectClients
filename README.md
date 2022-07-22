# JSecureDiscoverConnectClients [![](https://jitpack.io/v/OffRange/JSecureDiscoverConnectClients.svg)](https://jitpack.io/#OffRange/JSecureDiscoverConnectClients)

A java project containing two clients - a TCP client and a UDP client. The TCP client performs a handshake securing the connection.
The connection is secured by an AES key generated on the client side.
The AES key is encrypted by a server-side generated RSA public key and sent to the server.

#### NOTE: the server is not yet included in this project or on my github. If you want to use this client library, you'll need to code your own server side for now. I'm working on the server-side sometime later.

## How to implement the libary
### For gradle
Add the JitPack repository to your build file
```
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
```

Then add the dependency

<pre>
dependencies {
    implementation 'com.github.OffRange:JSecureDiscoverConnectClients:<i>version</i>'
}
</pre>

### For maven
Add the JitPack repository to your build file
```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency

```
<dependency>
    <groupId>com.github.OffRange</groupId>
    <artifactId>JSecureDiscoverConnectClients</artifactId>
    <version>Tag</version>
</dependency>
```
