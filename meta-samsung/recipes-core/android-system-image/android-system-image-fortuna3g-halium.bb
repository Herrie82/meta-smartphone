require recipes-core/android-system-image/android-system-image-legacy-gsi.inc

# The same 32-bit GSI as a3-2015-halium, whose vendor is of the same kind (LineageOS 18.1, MSM8916,
# VNDK 30); android-system-image-a3-2015-halium.bb says why this build and not the .inc's default.
HALIUM_LUNEOS_GSI16_PV = "20261108-1"
HALIUM_LUNEOS_GSI16_SHA256 = "563c43e5b22e3ff76e0f17f8c12bca1d69ccb76c2e72f6f271473d30c30e3065"

COMPATIBLE_MACHINE = "^fortuna3g-halium$"

# No vendor image is published for this phone yet, so it is derived at build time from the device
# tarball below (HALIUM_LEGACY_VENDOR_SHA256 empty). Once UNPACKDIR/vendor.img is published as
# fortuna3g-halium-vendor.img next to the GSI, set its sha256 and HALIUM_LEGACY_VENDOR_URL here, as
# a3-2015-halium does.
HALIUM_LEGACY_VENDOR_SHA256 = ""

# The vendor comes from the LineageOS 18.1 trees for the Galaxy Grand Prime 3G, the fortuna-for-smoothie
# forks of Gabboxl's and Galaxy-MSM8916's (github.com/fortuna-for-smoothie: android_device_samsung_fortuna3g,
# android_device_samsung_gprimelte-common, android_device_samsung_msm8916-common and
# proprietary_vendor_samsung, branch lineage-18.1, the blobs lineage-18.1exp).
#
# That build is Android 11 (VNDK 30) and has no vendor partition: it puts the vendor under
# /system/vendor, so it goes through the same conversion as a3-2015-halium and sm-t520
# (halium-legacy-vendor), with no init scripts to move: the Android 11 vendor ships its init files in
# /vendor/etc/init already. The GSI carries com.android.vndk.v30 for 32-bit ARM.
HALIUM_LEGACY_VNDK = "30"

# The "device" tarball is the system.img of that build, made as a3-2015-halium's is (lunch
# lineage_fortuna3g-userdebug in the Halium 11 tree, "m -k systemimage
# android.hardware.camera.provider@2.4-service", then "m snod"), with /system/lib completed the same way
# (the Android 11 ICU, libnativehelper, libstatspull and libstatssocket in place of the links into /apex,
# and the HIDL interface libraries of /system/system_ext/lib). Scripts: build-system.sh and
# assemble-device.sh, kept with the port's notes. Not published yet: point this at a local copy, or put
# the file in DL_DIR, until it is.
# 20261110-1: built 10 Nov 2026 from device/samsung/fortuna3g e398a504ca27, gprimelte-common 0acc4f4e842d,
# msm8916-common b2f44254c4ff, proprietary_vendor_samsung a743d6117423.
FORTUNA3G_HALIUM_DEVICE_TARBALL ?= "file:///home/herrie/claude-scratch/fortuna3g/out/halium-luneos-11.0-20261110-1-fortuna3g-halium.tar.bz2"
HALIUM_LEGACY_DEVICE_URL = "${FORTUNA3G_HALIUM_DEVICE_TARBALL}"
HALIUM_LEGACY_DEVICE_SHA256 = "bfc65fdf313c6b34a0b433cf32ad94a6bef09bbe6d0fe5f724dc26b6b89264f1"

# No device init scripts to move (see above).
HALIUM_LEGACY_INIT_FILES = ""

# The plain GSI: nothing of the old image goes into it. The vendor's fstab (fstab.qcom) mounts apnhlos
# on /firmware, modem on /firmware-modem and efs on /efs, root directories the old image had
# (BOARD_ROOT_EXTRA_FOLDERS in msm8916-common: firmware firmware-modem persist efs), as on
# a3-2015-halium.
HALIUM_LEGACY_GSI_AUTODETECT = "0"

# msm8916-common's TARGET_LD_SHIM_LIBS (BoardConfigCommon.mk), which the Android 16 linker does not
# implement, so each shim becomes a dependency of its library. All seven libraries are in the vendor image.
# libboringssl-compat.so is the one shim the build puts in /system/lib; HALIUM_LEGACY_EXTRA_LIBS below brings
# it into the vendor image (camera.msm8916.so also names it in DT_NEEDED).
HALIUM_LEGACY_LD_SHIMS = " \
    libmmjpeg_interface.so|libboringssl-compat.so \
    libsec-ril.so|libshim_secril.so \
    libsec-ril-dsds.so|libshim_secril.so \
    camera.vendor.msm8916.so|libcamera_shim.so \
    libizat_core.so|libshim_gps.so \
    libqomx_jpegenc.so|libboringssl-compat.so \
    libgeofence.so|liblocadapterbase_shim.so \
"

# The composer HAL (hwcomposer.msm8916.so) links libhwui.so and libmedia.so directly, as the A3's does,
# so it needs the same two settings as a3-2015-halium: a private libft2.so (the container's vendor linker
# does not hand the LLNDK one to this vendor) and halium-legacy-shim for the framework copies.
HALIUM_LEGACY_EXTRA_LIBS = "libft2.so libboringssl-compat.so"
HALIUM_LEGACY_SHIM_TARGETS = "libgui.so libmedia.so libmediautils.so libsensor.so libandroid_runtime.so libmedia_codeclist.so"

# The camera provider is declared hwbinder in msm8916-common's manifest.xml already, and its service is in
# the device tarball, so unlike a3-2015-halium no HALIUM_LEGACY_BINDERIZED_HALS. None of the 385 ELF files of
# this vendor has text relocations (readelf -d, 10 Nov 2026), so the A3's camera target-SDK rc is not
# needed either.

# Bluetooth: the HAL reads its address from ro.bt.bdaddr_path (/efs/bluetooth/bt_addr), set with the other
# Bluetooth properties in the old /system/build.prop, which the GSI replaces; carry them into the vendor
# build.prop, as a3-2015-halium does. Unlike the A3, fstab.qcom mounts /efs itself and init.qcom.rc makes
# bt_addr radio:bluetooth 0640, which the HAL (group bluetooth) can read, so no fstab or rc of our own.
HALIUM_LEGACY_SYSTEM_PROP_PREFIXES += "ro.bt. ro.bluetooth. ro.qualcomm.bt. vendor.bluetooth. vendor.qcom.bluetooth."

# More of msm8916-common's system_prop.mk that the vendor side reads and the converter's default prefixes
# miss (a diff of the old /system/build.prop against the derived vendor build.prop, 10 Nov 2026):
# ro.use_data_netmgrd=false, with which init.qcom.rc stops netmgrd (Samsung's RIL does data itself, and the
# blobs lack the libqmi_client_helper.so netmgrd links, so it would crash-loop), and persist.data.*; the audio
# HAL's vendor.audio.*, vendor.voice.*, ro.vendor.audio.* and audio.offload.*; ro.opengles.version; and the FM
# and GPS settings.
HALIUM_LEGACY_SYSTEM_PROP_PREFIXES += "ro.use_data_netmgrd persist.data. vendor.audio. vendor.voice. ro.vendor.audio. audio.offload. ro.opengles. ro.fm. ro.gps."

# wcnss_service (Wi-Fi NV data and MAC for the WCNSS firmware) is in the old /system/bin, which the GSI
# replaces, and init.qcom.rc starts it from there; the converter only rewrites such paths in the init
# scripts it moves, and this Android 11 vendor's init.qcom.rc is already in /vendor/etc/init/hw. Copy the
# daemon into the vendor image and point the service at it.
HALIUM_LEGACY_COPY_FILES += "/system/bin/wcnss_service:/bin/wcnss_service"
SRC_URI += "file://zz-fortuna3g-wcnss.rc"
HALIUM_LEGACY_EXTRA_FILES += "zz-fortuna3g-wcnss.rc:/etc/init/zz-fortuna3g-wcnss.rc:644"
