require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "sagit"

PV = "20210104-1"

SRC_URI = "http://build.webos-ports.org/halium-luneos-7.1/halium-luneos-7.1-${PV}-${MACHINE}.tar.bz2"

SRC_URI[md5sum] = "8726dcc7f8d742c69fb77484bc4dffd0"
SRC_URI[sha256sum] = "c8d7a72f8b25a8a0a3da4263484623cfd38c7ebc5f7645aa8d53c47f54006600"
