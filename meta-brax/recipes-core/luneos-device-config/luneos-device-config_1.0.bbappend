# Ship the BraX3 Tier 1 adaptation. Installed unconditionally like every other
# adaptation - it only takes effect when the running device's codename matches -
# so no COMPATIBLE_MACHINE guard.
#
# luneos-device-config reads the codename from ro.product.vendor.device first.
# BaseOS sets PRODUCT_DEVICE := brax3 (baseos_brax3.mk), so "brax3".
# UNVERIFIED on a running device: the UBports port calls it "prithvi"; if
# getprop ro.product.vendor.device says that, add a symlink rather than renaming.
FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://brax3-deviceinfo"

do_install:append() {
    install -d ${D}${datadir}/luneos/adaptations/brax3
    install -m 0644 ${UNPACKDIR}/brax3-deviceinfo \
        ${D}${datadir}/luneos/adaptations/brax3/deviceinfo
}
