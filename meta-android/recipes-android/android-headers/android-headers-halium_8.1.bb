FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

require recipes-android/android-headers/android-headers.inc

PV = "8.1+gitr${SRCPV}"
SRCREV = "c72d831550c6ced634c457eff98080b7b17d8f04"

SRC_URI = "git://github.com/herrie82/android-headers.git;branch=halium-8.1;protocol=git"
ANDROID_API = "27"
S = "${WORKDIR}"
