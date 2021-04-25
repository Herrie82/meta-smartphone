require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "cheryl"

PV = "20210424-1"

SRC_URI = "http://build.webos-ports.org/halium-luneos-9.0/halium-luneos-9.0-${PV}-${MACHINE}.tar.bz2"
SRC_URI[md5sum] = "44887135c1aa5109a0a3d2469bfcdf89"
SRC_URI[sha256sum] = "44ecc08c6e37f85ed345e7fa591189f98e38324028183b921723493e46352a19"

ANDROID_SYSTEM_IMAGE_DESTNAME = "android-rootfs.img"
