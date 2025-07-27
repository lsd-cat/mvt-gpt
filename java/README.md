# libmvt-multiplatform

![Build](https://img.shields.io/badge/build-passing-brightgreen)
![Tests](https://img.shields.io/badge/tests-passing-brightgreen)

A pure Java library that parses Android artifacts and matches them against
indicators of compromise.

Package base: `org.osservatorionessuno.libmvt`.

## Build & Test
```bash
gradle test
```

## Updating IOCs
Use `IndicatorsUpdates` to download the latest indicator files or to fetch a
specific IOC file.
```java
IndicatorsUpdates updates = new IndicatorsUpdates();
updates.update(); // download index and IOC files to ~/.mvt/indicators
Indicators iocs = Indicators.loadFromDirectory(updates.getIndicatorsFolder().toFile());

// download an extra IOC file
updates.download("https://example.com/my_iocs.stix2");
```

Alternatively load IOCs from an existing directory:
```java
Indicators iocs = Indicators.loadFromDirectory(Path.of("/path/to/iocs").toFile());
```

## AndroidQF example
Run all modules on a directory exported with
[androidqf](https://mvt.re/):
```java
Path dir = Path.of("/path/to/androidqf");
AndroidQFRunner runner = new AndroidQFRunner(dir);
runner.setIndicators(iocs);
Map<String, Artifact> result = runner.runAll();
```
Individual modules can be invoked via `runModule("processes")` etc.
See `AndroidQFRunner.AVAILABLE_MODULES` for the list.
