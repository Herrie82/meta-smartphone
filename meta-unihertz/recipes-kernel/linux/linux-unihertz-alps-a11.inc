# The Android-11 MT6771 ALPS drop: Titan Pocket and Titan Slim.
#
# One tree, one branch, one revision and one dtb serve both devices - the two
# only diverge in the defconfig. Everything in this file is therefore shared,
# and the per-device recipes are three lines each.
#
# See the layer README, "Can one image serve the Pocket and the Slim?", for how
# close the two defconfigs are and what would have to be settled on hardware to
# collapse them into a single boot image.

require recipes-kernel/linux/linux-unihertz-mt6771.inc

SRC_URI = "git://github.com/ubuntu-touch-unihertz-titan/kernel-alps-mt6771.git;branch=halium-11.0;protocol=https \
           file://luneos.cfg \
           file://TitanPocket-mt6771.dtb \
           file://0001-kbuild-demote-benign-newer-gcc-warnings.patch \
           file://0002-block-drop-bogus-inline-on-exported-flock-helpers.patch \
           file://0003-vt-selection-remove-extraneous-set_selection.patch \
           file://0004-lcm-R63308_CASCADEMDZ40-lcm_init-prototype.patch \
           file://0005-eccci-dpmaif-cast-pointers-through-uintptr_t.patch \
           file://0006-mediatek-give-K-R-definitions-a-void-prototype.patch \
           file://0007-cmdq-return-NULL-not-ENOMEM-from-a-pointer-function.patch \
           file://0008-mediatek-disp_pwm-invert-the-backlight-level-on-the-Titan-Pocket.patch \
           file://0009-input-fingerprint-add-the-Sunwave-TEE-driver-for-the-Titan-Pocket.patch \
           file://0010-imgsensor-add-the-Titan-Pocket-ov16880-and-ov8865-sensor-drivers.patch \
"

# The patches above are what it takes to build this tree with OE's GCC instead
# of the clang r383902 it was written against. Split the way athena's notes
# recommend: 0001 is build plumbing, 0002 is a real bug that GCC found and
# clang did not. They belong on a shr-distribution/linux branch once there is
# one (mindphone's 4.14 work lives on dnim/4.14.186); they are layer patches
# only while the port is being brought up.

# halium-11.0 @ "aw9523: rework the keyboard driver based on the orig kernel
# binary" (10 Sep 2025), the branch head. This branch carries the drivers
# TheKit recovered for both devices: GT1151QM and CHSC5432 touchscreens, the
# R63308_CASCADEMDZ40 and R63308_ZMK42 panels, the is31fl backlight and the
# AW9523 keyboard matrix.
#
# NOTE: unlike the Titan's halium-10.0 branch, nothing here has been shown to
# boot - the Ubuntu Touch port for these two devices still packs the *stock*
# prebuilt kernel and builds only loadable modules out of this tree. We are
# ahead of upstream in asking it to be a whole kernel.
SRCREV = "ba3ad72c6c2d545d85a499788ec54cbdb80b5543"

LINUX_VERSION = "4.14"
KV = "4.14.186"

# The dtb section of a v2 header must be an AOSP dt_table blob (magic
# 0xd7b7ab1e), not a bare FDT: MediaTek lk only parses the table format.
#
# This is the stock blob out of the UBports port repo, and the Pocket and the
# Slim ship the BYTE-IDENTICAL file - md5 1c0ec5e5c084e9bbb559ea99651a2e56,
# 114,158 bytes, one entry of 114,094 bytes at offset 64, page_size 2048,
# platform id 0. Hence one copy here rather than two, and hence the dtb is not
# a reason the two devices need separate images.
ANDROID_BOOTIMG_DTB = "${UNPACKDIR}/TitanPocket-mt6771.dtb"

# 11.0.0, security patch 2022-03, packed as (os_version << 11) | patch_level
# the way the v3/v4-style header field wants it. Matches the stock boot.img
# these replace, so MediaTek lk's rollback check stays happy. Never lower it
# below what is already flashed.
ANDROID_BOOTIMG_OS_VERSION = "0x16000163"
