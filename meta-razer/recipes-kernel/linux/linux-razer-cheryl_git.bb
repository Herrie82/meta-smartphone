require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "^cheryl$"

DESCRIPTION = "Linux kernel for the Razer Phone 1 (cheryl) device based on the offical \
source from Razer"

#DEPENDS += "openssl-native"

ANDROID_BOOTIMG_CMDLINE = "androidboot.console=ttyMSM0 androidboot.hardware=qcom user_debug=31 msm_rtb.filter=0x37 ehci-hcd.park=3 service_locator.enable=1 swiotlb=2048 firmware_class.path=/vendor/firmware_mnt/image --"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x00008000"
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x01000000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x00f00000"
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x00000100"

inherit kernel_android

SRC_URI = "git://github.com/herrie82/android_kernel_razer_msm8998.git;branch=halium-9.0-LuneOS"
S = "${WORKDIR}/git"

do_configure_prepend() {
    cp -v -f ${S}/arch/arm64/configs/lineageos_cheryl_defconfig ${WORKDIR}/defconfig
}

do_configure_append() {
  kernel_conf_variable_fixup() {
      sed -i "/CONFIG_$1[ =]/d" ${B}/.config
      kernel_conf_variable $1 $2 ${B}/.config
  }

# fixup some options which get changes from Y to M in oldconfig :/
  kernel_conf_variable_fixup USB_LIBCOMPOSITE y
  kernel_conf_variable_fixup USB_F_ACM y
  kernel_conf_variable_fixup USB_U_SERIAL y
  kernel_conf_variable_fixup USB_U_ETHER y
  kernel_conf_variable_fixup USB_F_SERIAL y
  kernel_conf_variable_fixup USB_F_RNDIS n
  kernel_conf_variable_fixup USB_F_MASS_STORAGE y
  kernel_conf_variable_fixup USB_F_FS y
  kernel_conf_variable_fixup USB_F_MIDI y
  kernel_conf_variable_fixup USB_F_HID y
  kernel_conf_variable_fixup USB_F_MTP y
  kernel_conf_variable_fixup USB_F_PTP y
  kernel_conf_variable_fixup USB_F_AUDIO_SRC y
  kernel_conf_variable_fixup USB_F_ACC y
  kernel_conf_variable_fixup USB_CONFIGFS y
  kernel_conf_variable_fixup SDCARD_FS y
  oe_runmake oldnoconfig
}


SRCREV = "45c3ac999094e8b6dd1f1fdd6ba05777adfe1a7a"

KV = "4.4.185"
PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr

do_install_append () {
    rm -rf ${D}/usr/src/usr
}
