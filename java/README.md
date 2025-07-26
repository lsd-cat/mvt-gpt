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

## Next steps
- Translate more artifact parsers from Python.
- Extend Detection metadata (source file, STIX IDs, etc.).
- Optionally publish to Maven Central / local repo.
