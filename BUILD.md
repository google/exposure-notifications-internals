# Building

This source in this project does not create a usable product, but is provided as a reference that
can be used with the following specifications:

- [Bluetooth Specification](https://blog.google/documents/70/Exposure_Notification_-_Bluetooth_Specification_v1.2.2.pdf)
- [Cryptography Specification](https://blog.google/documents/69/Exposure_Notification_-_Cryptography_Specification_v1.2.1.pdf)

It is nonetheless possible to compile this code in order to run the various test cases provided.

## Dependencies

This project depends on a few third-party, open source libraries:

* [BoringSSL](https://boringssl.googlesource.com/boringssl/)
* [NaoPB](https://jpa.kapsi.fi/nanopb/download/)

It is helpful to place both of these dependencies in a common directory. For convenience, shell
scripts which download and build these third-party libraries are provided which require Docker to
be installed to run:

* [Docker Engine](https://docs.docker.com/engine/install/)

## Building BoringSSL

### Using Docker

To build cross-compile BoringSSL for Android using Docker, run the following bash script:
```bash
bash ./third_party/boringssl/build-android.sh
```

### Using Native Toolchain

To build BoringSSL, clone the repo and build it according to the steps described in the included
`BUILDING.md` file.

NOTE: This project is set to build for `arm64-v8a` and `x86`, in order to support testing on
modern Android devices, as well as emulators. As such, it expects the output from building
BoringSSL to be in a directory named `build-${ANDROID_ABI}`. This means BoringSSL must be built
twice. Once with the setting `-DANDROID_ABI=x86`, for Android emulators, and a second time with
 `-DANDROID_ABI=arm64-v8a` for physical devices.

Depending on where you place the resulting build artifacts, you may need to change the
`boringSslRoot` variable in the `build.gradle` file.

## Downloading NanoPB

The `build.gradle` file expects a binary distribution of NanoPB to be present under the folder
`./third_party/nanopb/${platform}`. You can run the provided bash script to download it:
```bash
bash ./third_party/nanopb/download.sh
```

Alternatively, you can manually download a binary distribution of NanoPB from the provided link
above. Extract it under the folder described above, or configure `build.gradle` accordingly.


At this point the project should be able to build and the tests should run.
