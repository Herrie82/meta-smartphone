require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "^rosy$"

PV = "20201220-1"

SRC_URI = "http://build.webos-ports.org/halium-luneos-9.0/halium-luneos-9.0-${PV}-${MACHINE}.tar.bz2"
SRC_URI[md5sum] = "37fa4b08c588dcfcf93686dc04035edf"
SRC_URI[sha256sum] = "3033054b77855664d10795d836a9759c0ba6d1aca763b3f375ef3a84f21d591a"
