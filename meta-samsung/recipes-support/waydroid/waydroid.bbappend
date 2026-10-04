# Same treatment waydroid.bb gives halium-arm64, halium-arm and mindphone.
#
# waydroid-data freezes a system/vendor image pair at build time, and the vendor
# half only exists for the Halium levels the recipe knows (WAYDROID_HALIUM_LEVEL
# is set for tissot-halium and mido-halium only). On any other machine it is
# empty, and the vendor URL comes out as
#   .../lineage-16.0-20250809--waydroid_arm64-vendor.zip
# which does not exist, so do_fetch fails and takes the whole image with it.
#
# This tablet's vendor is Android 12 (ro.vndk.version=31, read from
# T220XXSAEYE4), which none of the frozen pairs is for, so the pair has to be
# resolved at runtime over the OTA channel, as on the GSI machines.
WAYDROID_IMAGE_RDEPENDS:sm-t220 = ""

# The package differs from the one tissot-halium and mido-halium build for the
# same aarch64-halium arch (they keep the waydroid-data dependency), so it must
# not share their feed entry. waydroid.bb explains the collision at length where
# it does the same for halium-arm64.
PACKAGE_ARCH:sm-t220 = "${MACHINE_ARCH}"
