require recipes-kernel/linux/linux.inc
# Options every Halium target needs; see the file for what and why.
require recipes-kernel/linux/halium-kernel.inc
# Binder nodes, veth, overlayfs and the rest of what Waydroid needs.
require recipes-kernel/linux/waydroid-kernel.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "^sm-t220$"

DESCRIPTION = "Linux kernel for the Samsung Galaxy Tab A7 Lite Wi-Fi (SM-T220, \
gta7litewifi, MT8768T), built from the gta7lite community's halium-12 branch of \
the OT8 common kernel"

#-----------------------------------------------------------------------------
# Source
#-----------------------------------------------------------------------------
# github.com/gta7lite/android_kernel_samsung_ot8 is described as "Samsung Galaxy
# OT8 Common Kernel (T220, T225, T227)". Its halium-12 branch is Samsung's
# 4.19.191 drop plus a single commit, "arm64: create halium.config", which adds
# arch/arm64/configs/halium.config (a nine-line fragment: DEVTMPFS, FHANDLE,
# SYSVIPC and the five namespaces, VT). It is the only branch of the five there
# that is Halium-specific; the others are lineage-21, t220/master and master.
#
# The defconfig is gta7litewifi_defconfig, which is what the LineageOS device
# tree for this tablet names (TARGET_KERNEL_CONFIG in
# android_device_samsung_gta7litewifi/BoardConfig.mk).
#
# Pinned to the halium-12 head as of 2 Oct 2026, commit date 23 Aug 2024.
SRCREV = "448cc3f1aa531a80c7dc10419b07a7e2681c680e"

SRC_URI = "git://github.com/gta7lite/android_kernel_samsung_ot8.git;branch=halium-12;protocol=https \
           file://luneos.cfg \
           file://0001-drvgen-use-the-checked-in-cust.dtsi-instead-of-runni.patch \
           file://0002-Bluetooth-restore-the-raw-HCI-socket-layer.patch \
           file://0003-pidfd-add-P_PIDFD-to-waitid.patch \
           "

LINUX_VERSION = "4.19"
KV = "4.19.191"
PV = "${KV}+git"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr

LIC_FILES_CHKSUM = "file://COPYING;md5=bbea815ee2795b2f4230826c0c6b8814"

#-----------------------------------------------------------------------------
# Boot image
#-----------------------------------------------------------------------------
# Taken from the LineageOS common device tree,
# android_device_samsung_mt6765-common/BoardConfigCommon.mk, which is what
# builds the boot.img this bootloader accepts. Those are mkbootimg offsets
# against a base; the header fields, and so the ANDROID_BOOTIMG_*_RAM_BASE
# variables, are base + offset:
#
#   BOARD_KERNEL_BASE        0x40078000
#   BOARD_KERNEL_OFFSET      0x00008000   ->  kernel   0x40080000
#   BOARD_RAMDISK_OFFSET     0x11a88000   ->  ramdisk  0x51b00000
#   BOARD_KERNEL_TAGS_OFFSET 0x07808000   ->  tags     0x47880000
#   BOARD_DTB_OFFSET         0x07808000   ->  dtb      0x47880000
#
# Header version 2, page size 2048, Image.gz, and the dtb inside the boot image
# (BOARD_INCLUDE_DTB_IN_BOOTIMG) with the dtbo kept in its own partition
# (BOARD_KERNEL_SEPARATED_DTBO). The second-image address is zero as on radon,
# which has no second image either.
#
# CONFIRMED against the stock boot.img of T220XXSAEYE4 (the SAMFW factory
# image, 13 May 2025): header v2, page size 2048, kernel 0x40080000, ramdisk
# 0x51b00000, tags and dtb 0x47880000, second 0 - identical to the above.
ANDROID_BOOTIMG_HEADER_VERSION = "2"
ANDROID_BOOTIMG_PAGESIZE = "2048"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x40080000"
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x51b00000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x00000000"
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x47880000"
ANDROID_BOOTIMG_DTB_RAM_BASE = "0x47880000"

# bootopt is how MediaTek's lk is told the SoC/kernel/userland word sizes;
# 64S3,32N2,64N2 is what Lineage's BOARD_KERNEL_CMDLINE passes for this
# platform, and the same string radon uses. Not ours to change.
#
# Deliberately NOT carried over from that line: androidboot.hardware=mt6765 and
# androidboot.init_fatal_reboot_target=recovery, which are for Android's own
# first-stage init; the container's init gets its properties from the
# Halium initramfs.
#
# TODO(verify): radon needs firmware_class.path=/vendor/firmware because its
# MediaTek combo drivers are built in and read their config through
# request_firmware(). This defconfig also has CONFIG_MTK_COMBO=y with
# CONSYS_6765, so Wi-Fi may need the same - add it if the journal shows
# "Direct firmware load for wifi.cfg failed with error -2".
ANDROID_BOOTIMG_CMDLINE = "bootopt=64S3,32N2,64N2 printk.devkmsg=on"

# The stock header's os_version field is 0x18000195: Android 12.0.0, patch
# level 2025-05, packed as (os_version << 11) | os_patch_level with
#   os_version     = (12<<14) | (0<<7) | 0   = 0x30000
#   os_patch_level = ((2025-2000)<<4) | 5    = 0x195
# It says 12 although the firmware's vendor is Android 14 (the AP file name ends
# in OS14): the boot image is built separately from the rest. Same value as
# stock so a bootloader rollback check sees nothing go backwards; never write a
# lower one than what is already flashed.
ANDROID_BOOTIMG_OS_VERSION = "0x18000195"

# The base dtb. Samsung's defconfig sets CONFIG_BUILD_ARM64_APPENDED_DTB_IMAGE
# with APPENDED_DTB_IMAGE_NAMES="mediatek/mt6765" and builds the board-specific
# part as an overlay: DTB_OVERLAY_IMAGE_NAMES="mediatek/mt8768t_gta7litewifi_eur_open_00",
# a /plugin/ tree over ot8.dts whose compatible is "Samsung,Tab-A7-Lite WiFi EUR
# OPEN 00" / "Mediatek,MT8768T".
#
# Like radon, this port takes the base tree into the boot image and leaves the
# dtbo partition as flashed: it changes no board wiring, so the stock overlay
# is the right one. KERNEL_DEVICETREE is what makes kernel.bbclass compile it.
KERNEL_DEVICETREE = "mediatek/mt6765.dtb"

# The stock boot image's dtb section is NOT a bare FDT. Unpacked from
# T220XXSAEYE4 it is a dt_table (magic 0xd7b7ab1e, 407377 bytes): a 32-byte
# header, one 32-byte entry (id 0, rev 0, no custom words) and the FDT at
# offset 64. This lk only parses that format, as on mindphone, so wrap the
# kernel's own mt6765.dtb in it instead of shipping Samsung's blob.
#
# Layout, all big-endian u32: header = magic, total_size, header_size (32),
# dt_entry_size (32), dt_entry_count (1), dt_entries_offset (32), page_size
# (2048), version (0); entry = dt_size, dt_offset (64), id, rev, custom[4].
# Rebuilding the stock table from its own FDT with this layout reproduces
# Samsung's file byte for byte.
ANDROID_BOOTIMG_DTB = "${B}/dt-table.img"

python do_make_dt_table() {
    import os, struct
    fdt_path = os.path.join(d.getVar("B"), d.getVar("KERNEL_OUTPUT_DIR"), "dts",
                            d.getVar("KERNEL_DEVICETREE"))
    with open(fdt_path, "rb") as f:
        fdt = f.read()
    hdr = struct.pack(">8I", 0xd7b7ab1e, 64 + len(fdt), 32, 32, 1, 32, 2048, 0)
    ent = struct.pack(">8I", len(fdt), 64, 0, 0, 0, 0, 0, 0)
    with open(os.path.join(d.getVar("B"), "dt-table.img"), "wb") as f:
        f.write(hdr + ent + fdt)
}
addtask make_dt_table after do_compile before do_deploy

#-----------------------------------------------------------------------------
# Toolchain: Google's prebuilt Clang, as radon, sunfish, q25 and mp01 do
#-----------------------------------------------------------------------------
# The defconfig header records the compiler Samsung built with:
#
#   Android (7284624, based on r416183b) clang version 12.0.5
#
# which is the r416183b release radon, q25, mp01, bramble and sunfish already
# pin, so GKI_CLANG_DIR:sm-t220 can point at the same checkout. do_check_toolchain
# fails early and legibly rather than letting a GCC build get halfway.
#
# Modelled on linux-furilabs-radon_git.bb. Differences, each read from this
# tree's defconfig: no CONFIG_COMPAT_VDSO (so no CROSS_COMPILE_COMPAT), and
# CONFIG_LTO_NONE=y (so LTO is not a concern here).
GKI_CLANG_VERSION ?= "r416183b"
GKI_CLANG_DIR ?= ""

python do_check_toolchain() {
    clang_dir = d.getVar("GKI_CLANG_DIR")
    if not clang_dir:
        bb.fatal("GKI_CLANG_DIR is not set. This kernel must be built with "
                 "Google's Clang %s - see the comment in %s."
                 % (d.getVar("GKI_CLANG_VERSION"), d.getVar("FILE")))
    if not os.path.exists(os.path.join(clang_dir, "bin", "clang")):
        bb.fatal("GKI_CLANG_DIR %s has no bin/clang" % clang_dir)
}
addtask check_toolchain before do_configure

# Prepended for every task rather than exported in each one: do_configure,
# do_compile and do_compile_kernelmodules all invoke the compiler.
PATH =. "${GKI_CLANG_DIR}/bin:"

# kernel-arch.bbclass builds these out of ${HOST_PREFIX}gcc. Keep everything it
# puts in them - the -ffile-prefix-map pair is what keeps build paths out of the
# -src package and the buildpaths QA check green - and swap the tools.
# DEBUG_PREFIX_MAP is filtered rather than used as-is: OE puts
# -fcanon-prefix-map in it, which is a GCC 12+ option this Clang does not know.
KERNEL_CC = "clang ${HOST_CC_KERNEL_ARCH} \
 ${@' '.join(f for f in (d.getVar('DEBUG_PREFIX_MAP') or '').split() if f != '-fcanon-prefix-map')} \
 -ffile-prefix-map=${STAGING_KERNEL_DIR}=${KERNEL_SRC_PATH} \
 -ffile-prefix-map=${STAGING_KERNEL_BUILDDIR}=${KERNEL_SRC_PATH} \
"
KERNEL_LD = "ld.lld"
KERNEL_OBJCOPY = "llvm-objcopy"
KERNEL_STRIP = "llvm-strip"

# CLANG_TRIPLE: 4.19 errors out with "Clang with Android --target detected. Did
# you specify CLANG_TRIPLE?" without it. LLVM_IAS=1 uses Clang's integrated
# assembler; the llvm-* tools replace the $(CROSS_COMPILE) binutils.
EXTRA_OEMAKE += "CLANG_TRIPLE=aarch64-linux-gnu- LLVM_IAS=1 \
    AR=llvm-ar NM=llvm-nm OBJDUMP=llvm-objdump READELF=llvm-readelf"

# Samsung's tree stubs out the raw HCI socket layer (hci_sock_create() is just
# "return 0", most of hci_sock.c is commented out). Android never opens an HCI
# socket; Halium's bluebinder does, hits BUG_ON(!sk) in bt_sock_reclassify_lock()
# and MediaTek's panic handler resets the device ~57 s into boot. Patch 0002
# restores net/bluetooth/hci_sock.c to v4.19.191, the only file in
# net/bluetooth/ that differs from upstream. Found from ramoops (see the patch).
#
# Patch 0003 is the 5.4 waitid(P_PIDFD) half of the pidfd API. Samsung's tree has
# Android's backport of pidfd_open() but not that, so GLib's pidfd-based child
# watch (it commits to pidfds as soon as pidfd_open() works) fails every reap with
# EINVAL: umediaserver's pipelines all "exit with status -1", and nothing played
# through the webOS media server makes a sound. Same backport athena and mindphone
# take from shr-distribution/linux; their CLONE_PIDFD follow-up is not needed here.

inherit kernel_android pkgconfig

# kernel.bbclass sets S = "${STAGING_KERNEL_DIR}", and do_symlink_kernsrc only
# moves the unpacked tree there when the recipe points S somewhere else.
S = "${UNPACKDIR}/${BP}"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

DEPENDS += "dtc-native openssl-native"

# gta7litewifi_defconfig is the full config (5,886 lines) that Lineage's device
# tree selects for this tablet. halium.config is the branch's own fragment and
# luneos.cfg is the LuneOS delta on top; later lines win over earlier ones when
# kconfig reads the concatenation.
do_configure:prepend() {
    cat ${S}/arch/arm64/configs/gta7litewifi_defconfig \
        ${S}/arch/arm64/configs/halium.config \
        ${UNPACKDIR}/luneos.cfg > ${WORKDIR}/defconfig
}

# Kbuild writes the absolute path of the tool or input into the banner of a few
# generated files, and linux.inc ships the build tree as the -src package, so
# wrynose rejects it with "contains reference to TMPDIR [buildpaths]". Rewrite
# the banners to name the files the way an in-tree build would; they are
# comments, and the generated data below them is unchanged. Same treatment as
# radon and athena.
do_compile:append() {
    for f in drivers/tty/vt/consolemap_deftbl.c \
             drivers/video/logo/logo_linux_clut224.c \
             lib/oid_registry_data.c; do
        if [ -f ${B}/$f ]; then
            sed -i -e "s|${STAGING_KERNEL_DIR}/||g" -e "s|${B}/||g" ${B}/$f
        fi
    done
}

do_install:append () {
    # make headers_install leaves kbuild's ..install.cmd bookkeeping behind, and
    # linux.inc ships everything under ${exec_prefix}/src/linux* as
    # kernel-headers. Those files record absolute command lines, which wrynose
    # rejects as "contains reference to TMPDIR [buildpaths]".
    find ${D}${exec_prefix}/src -name '..install.cmd' -delete 2>/dev/null || true
    rm -rf ${D}/usr/src/usr
}

# scripts/unifdef.c declares "static bool constexpr;" and assigns to it. C23
# made constexpr a keyword and the host GCC here defaults to -std=gnu23, so
# building the kernel host tools fails. Only the host tools are affected, and
# kernel.bbclass passes HOSTCFLAGS="${BUILD_CFLAGS}". Same fix as radon.
BUILD_CFLAGS:append = " -std=gnu17"
