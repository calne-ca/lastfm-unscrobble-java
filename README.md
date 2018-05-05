## A simple Java Unscrobbler for Last.fm

### Usage
```java
Unscrobbler unscrobbler = new Unscrobbler();

try {
    unscrobbler.login("username","password");
} catch(UnscrobblerAuthenticationException e){
    System.err.println("Failed to login to Last.fm! " + e.getMessage());
}

unscrobbler.unscrobble("LIQ","[un]INSOMNIA","1497478667");
```

### Maven Dependency
```xml
<dependency>
    <groupId>net.beardbot</groupId>
    <artifactId>lastfm-unscrobble</artifactId>
    <version>0.2</version>
</dependency>
```

### Enable debug logging
```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```
