require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "dumpling"

DESCRIPTION = "Linux kernel for the OnePlus dumpling (OnePlus 5T) device based on the official \
source from OnePlus"

SRC_URI = " \
    git://github.com/herrie82/android_kernel_oneplus_msm8998.git;branch=cm-14.1-sultan \
"

S = "${WORKDIR}/git"

CMDLINE = "${ANDROID_BOOTIMG_CMDLINE}"

do_configure_prepend() {
    cp -v -f ${S}/arch/arm64/configs/dumpling_defconfig ${WORKDIR}/defconfig
}

SRCREV = "025fa1daa7f6f86bf3c5d118c33d817e01aa9583"

KV = "4.4.63"

PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr
