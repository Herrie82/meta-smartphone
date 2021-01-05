require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "^sagit$"

DESCRIPTION = "Linux kernel for the Xiaomi Mi 6 (sagit) device based on the offical \
source from Xiaomi"

DEPENDS += "openssl-native"
ANDROID_BOOTIMG_CMDLINE = "console=ttyMSM0,115200,n8 androidboot.console=ttyMSM0 earlycon=msm_serial_dm,0xc1b0000 androidboot.hardware=qcom user_debug=31 msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 sched_enable_hmp=1 sched_enable_power_aware=1 service_locator.enable=1 swiotlb=2048 androidboot.configfs=true androidboot.usbcontroller=a800000.dwc3 androidboot.selinux=permissive --"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x00008000" 
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x01000000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x00f00000" 
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x00000100"

inherit kernel_android

SRC_URI = "git://github.com/herrie82/android_kernel_xiaomi_msm8998-1.git;branch=hybris-14.1"
S = "${WORKDIR}/git"

do_configure_prepend() {
    cp -v -f ${S}/arch/arm64/configs/sagit_user_defconfig ${WORKDIR}/defconfig
}

SRCREV = "3244d66148b658ef7d403142982df7141cdfcbed"

KV = "4.4.21"
PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr

do_install_append () {
    rm -rf ${D}/usr/src/usr
}
