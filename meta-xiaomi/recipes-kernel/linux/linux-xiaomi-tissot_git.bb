require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "^tissot$"

DESCRIPTION = "Linux kernel for the Xiaomi A1 (tissot) device based on the offical \
source from Xiaomi"

DEPENDS += "openssl-native"

ANDROID_BOOTIMG_CMDLINE = "androidboot.console=ttyHSL0 androidboot.hardware=qcom msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 androidboot.bootdevice=7824900.sdhci earlycon=msm_hsl_uart,0x78af000 firmware_class.path=/vendor/firmware_mnt/image androidboot.usbconfigfs=true androidboot.selinux=permissive androidboot.keymaster=1 androidboot.usbconfigfs=true --"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x80008000"
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x81000000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x00f00000"
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x80000100"

inherit kernel_android

SRC_URI = "git://github.com/Tofee/tissot.git;branch=tissot/4.9/halium-9.0"
S = "${WORKDIR}/git"

do_configure_prepend() {
    cp -v -f ${S}/arch/arm64/configs/tissot_defconfig ${WORKDIR}/defconfig
}

SRCREV = "2023941ad6a241699f12daa0a29eceefd1f54111"

KV = "4.9.188"
PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr

do_install_append () {
    rm -rf ${D}/usr/src/usr
}
