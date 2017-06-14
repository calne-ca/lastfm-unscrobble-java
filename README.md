## A simple Java Unscrobbler for Last.fm

### Usage
```java
Unscrobbler unscrobbler = new Unscrobbler();

try {
    unscrobbler.login("username","password");
} catch(AuthenticationFailedException | CsrfTokenFetchFailedException e){
    System.err.println("Failed to initialize unscrobbler! " + e.getMessage());
}

unscrobbler.unscrobble("LIQ","[un]INSOMNIA","1497478667");
```

### Enable debug logging
```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```
