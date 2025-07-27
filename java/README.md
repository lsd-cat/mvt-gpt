# libmvt-multiplatform

[![Java CI](https://github.com/mvt-project/mvt-gpt/actions/workflows/java-tests.yml/badge.svg)](https://github.com/mvt-project/mvt-gpt/actions/workflows/java-tests.yml)

Pure Java library (no Android dependencies) that parses Android artifacts and performs IOC matching.

## Build & Test
```bash
# run once if the wrapper is missing
gradle wrapper
./gradlew test
```

## Usage
### Updating indicators
```java
// downloads the latest indicators and loads them
Indicators iocs = Indicators.updateAndLoad();
```

### Loading local indicators
```java
Indicators iocs = Indicators.loadDefault();
// or use a custom folder
// Indicators iocs = Indicators.loadFromDirectory(Path.of("/path/to/iocs").toFile());
```

### Running AndroidQF modules
```java
Path dir = Path.of("/path/to/androidqf");
AndroidQFRunner runner = new AndroidQFRunner(dir);
runner.setIndicators(iocs);
Map<String, Artifact> result = runner.runAll();
```

Individual modules can be invoked via `runModule("processes")` etc. See `AndroidQFRunner.AVAILABLE_MODULES` for the list of names.
