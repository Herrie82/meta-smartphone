require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "hammerhead"

DESCRIPTION = "Linux kernel for the LG Hammerhead (Nexus 5) device based on the offical \
source from Google/LG"

ANDROID_BOOTIMG_CMDLINE = "androidboot.hardware=hammerhead user_debug=31 maxcpus=2 msm_watchdog_v2.enable=1"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x00008000"
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x02900000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x00f00000"
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x02700000"

inherit kernel_android

SRC_URI = " \
  git://github.com/Herrie82/android_kernel_lge_hammerhead.git;branch=halium-7.1 \
"
S = "${WORKDIR}/git"

do_configure_prepend() {
    cp -v -f ${S}/arch/arm/configs/lineageos_hammerhead_defconfig ${WORKDIR}/defconfig
}

SRCREV = "b09105c0d9e0223ec6304219859aaa7564e3d69f"

KV = "3.4.0"
PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr
