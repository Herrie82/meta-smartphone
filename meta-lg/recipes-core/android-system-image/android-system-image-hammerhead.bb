require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "hammerhead"

PV = "20190828-1"

SRC_URI = "http://build.webos-ports.org/halium-luneos-8.1/halium-luneos-8.1-${PV}-${MACHINE}.tar.bz2"
SRC_URI[md5sum] = "15052b0308c5d13d82f13308a66dc3a4"
SRC_URI[sha256sum] = "36fbe00fac23ffb91e40770d933b33c371f3ca6368829915a9fddaf0950c5d2a"
