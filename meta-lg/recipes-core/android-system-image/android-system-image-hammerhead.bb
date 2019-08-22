require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "hammerhead"

PV = "20190822-1"

SRC_URI = "http://build.webos-ports.org/halium-luneos-7.1/halium-luneos-7.1-${PV}-${MACHINE}.tar.bz2"
SRC_URI[md5sum] = "d1a7c7d7327aa4ff0bd4ef60173adb0e"
SRC_URI[sha256sum] = "2d41840bfff9db5cf8d84f6a2d7b1cb3737f112e01ec379416bdf005d4762b9b"
