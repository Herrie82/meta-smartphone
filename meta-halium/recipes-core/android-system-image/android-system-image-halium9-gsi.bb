require recipes-core/android-system-image/android-system-image.inc

COMPATIBLE_MACHINE = "halium9-gsi"

PV = "20230124-1"

SRC_URI = "https://github.com/webOS-ports/halium-images/releases/download/halium-luneos-9.0-${PV}-halium_arm64.tar.bz2/halium-luneos-9.0-${PV}-halium_arm64.tar.bz2"
SRC_URI[sha256sum] = "688ba74f6a075939a66cf2dcf9a2450b776047fbc697ab96e9ed67222ffd4ce7"

ANDROID_SYSTEM_IMAGE_DESTNAME = "android-rootfs.img"
