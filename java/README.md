# libmvt-multiplatform (starter)

Pure Java library (no Android dependencies) that implements parsing & IOC matching for Android-related artifacts.

- Package base: `org.osservatorionessuno.libmvt`
- No Android Gradle plugin or androidx deps.
- Feed it raw text dumps (dumpsys, getprop, etc.) and obtain structured results + detections.

## Build & Test
```bash
./gradlew test
# or, if wrapper isn't generated yet:
gradle wrapper
./gradlew test
```

## AndroidQF API
The `AndroidQFRunner` class can parse a directory exported with the
[androidqf](https://mvt.re/) format and run all available modules.
Example:
```java
Path dir = Path.of("/path/to/androidqf");
Indicators iocs = Indicators.loadFromDirectory(Path.of("/path/to/iocs").toFile());
AndroidQFRunner runner = new AndroidQFRunner(dir);
runner.setIndicators(iocs);
Map<String, Artifact> result = runner.runAll();
```

Individual modules can be invoked via `runModule("processes")` etc.
See `AndroidQFRunner.AVAILABLE_MODULES` for the list of names.


## Next steps
- Translate more artifact parsers from Python.
- Extend Detection metadata (source file, STIX IDs, etc.).
- Optionally publish to Maven Central / local repo.

## Android feature parity

Below is a quick overview of the Android artifact support compared to the Python
version. "Parser" indicates whether a class exists to parse the artifact. The
"Detection" column reflects if the heuristic/IOC logic is on par with the
Python implementation.

| Artifact | Parser | Detection |
|----------|:------:|:---------:|
| Accessibility | ✅ | ✅ |
| ADB | ✅ | ❌ (no IOC logic) |
| Android backup | ✅ | ✅ |
| Appops | ✅ | ⚠ partial |
| Battery daily | ✅ | ✅ |
| Battery history | ✅ | ✅ |
| DB info | ✅ | ✅ |
| Package activities | ✅ | ✅ |
| Packages | ✅ | ⚠ partial |
| Platform compat | ✅ | ✅ |
| Receivers | ✅ | ✅ |
| File timestamps | N/A | N/A |
| Getprop | ✅ | ⚠ partial |
| Processes | ✅ | ⚠ partial |
| Settings | ✅ | ✅ |
| Tombstone crashes | ✅ | ⚠ partial |

Legend: ✅ implemented, ❌ missing, ⚠ simplified compared to Python.

