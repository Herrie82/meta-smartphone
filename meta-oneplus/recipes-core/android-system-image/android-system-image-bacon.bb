require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "bacon"

PV = "20190221-1"

SRC_URI = "http://build.webos-ports.org/halium-luneos-7.1/halium-luneos-7.1-${PV}-${MACHINE}.tar.bz2"
SRC_URI[md5sum] = "8111625f21096cc309c5def3eccf8c0c"
SRC_URI[sha256sum] = "8692bb54d1b15891c66c6f17902dd3b170e02f9e7ecfa0b310f2a6f5987e2eee"
