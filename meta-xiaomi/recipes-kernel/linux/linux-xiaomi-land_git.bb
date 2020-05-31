require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "^land$"
 
DESCRIPTION = "Linux kernel for the Xiaomi Land (Redmi 3, Snapdragon) device based on the offical \
source from Xiaomi"
 
ANDROID_BOOTIMG_CMDLINE = "androidboot.hardware=qcom msm_rtb.filter=0x237 ehci-hcd.park=3 lpm_levels.sleep_disabled=1 androidboot.bootdevice=7824900.sdhci earlycon=msm_hsl_uart,0x78af000"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x80008000"
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x81000000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x00f00000"
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x80000100"
 
inherit kernel_android
 
SRC_URI = " \
  git://github.com/kskarthik/android_kernel_xiaomi_msm8937.git;branch=halium-7.1 \
"
S = "${WORKDIR}/git"
 
do_configure_prepend() {
    cp -v -f ${S}/arch/arm64/configs/lineageos_land_defconfig  ${WORKDIR}/defconfig
}
 
SRCREV = "143a0336369523e89aaf16d2684b3f168dd0b8a9"
 
KV = "3.18.31"
PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr
 
do_install_append () {
    rm -rf ${D}/usr/src/usr
}
