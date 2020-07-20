# Building

This source in this project does not create a usable product, but is provided as a reference that
can be used with the following specifications:

- [Bluetooth Specification](https://blog.google/documents/70/Exposure_Notification_-_Bluetooth_Specification_v1.2.2.pdf)
- [Cryptography Specification](https://blog.google/documents/69/Exposure_Notification_-_Cryptography_Specification_v1.2.1.pdf)

It is nonetheless possible to compile this code in order to run the various test cases provided.

## Dependencies

This project depends on a few 3rd party, open source libraries:

* [BoringSSL](https://boringssl.googlesource.com/boringssl/)
* [NaoPB](https://jpa.kapsi.fi/nanopb/download/)

It is helpful to place both of these dependencies in a common directory.

## Building BoringSSL

To build BoringSSL, clone the repo and build it according to the steps described in the included `BUILDING.md` file.

NOTE: This project is set to build for `arm64-v8a` and `x86`, in order to support testing on
modern Android devices, as well as emulators. As such, it expects the output from building
BoringSSL to be in a directory named `build-${ANDROID_ABI}`. This means BoringSSL must be built
twice. Once with the setting `-DANDROID_ABI=x86`, for Android emulators, and a second time with
 `-DANDROID_ABI=arm64-v8a` for physical devices.

If you'd like to change this structure, see the `TODO(BoringSSL)` in `CMakeList.txt`.

## Building NanoPB

Simply download a binary distribution of NanoPB from the provided link above. Extract it and
continue on to configure `build.gradle`.

## Configuring build.gradle

After downloading the libraries and following their build steps, the `build.gradle` file in the
"matching" module should be updated to point at their common root:

```groovy
def depsRoot = "${System.properties['user.home']}/Projects"
def boringSslRoot = "${depsRoot}/boringssl"
def nanopbRoot = "${depsRoot}/nanopb-0.4.2-macosx-x86"
```

Update the value of `boringSslRoot` and `nanopbRoot` to point at the root folder of the two
libraries. The `depsRoot` is only used for convenience, and can be removed if the libraries are not
in a common root.

At this point the project should be able to build and the tests should run.

