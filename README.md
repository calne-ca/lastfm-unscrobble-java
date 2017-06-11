## A simple Java Unscrobbler for Last.fm

### Usage
```java
LastFm lastFm = new LastFm();
lastFm.init(<username>,<password>);
lastFm.unscrobble(<artist>,<trackname>,<timestamp>);
```

### Logging
```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```
