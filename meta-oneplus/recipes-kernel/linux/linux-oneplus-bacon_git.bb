require recipes-kernel/linux/linux.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "bacon"

DESCRIPTION = "Linux kernel for the OnePlus Bacon (OnePlus One) device based on the official \
source from OnePlus"

SRC_URI = " \
    git://github.com/herrie82/thunder_kernel_oneplus_bacon.git;branch=thunder-oss \
"

S = "${WORKDIR}/git"

CMDLINE = "${ANDROID_BOOTIMG_CMDLINE}"

do_configure_prepend() {
    cp -v -f ${S}/arch/arm/configs/thunder_bacon_defconfig ${WORKDIR}/defconfig
}

SRCREV = "6948aed1e253fc281ff29a49b30de05380e96642"

KV = "3.4.113"

PV = "${KV}+gitr${SRCPV}"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr
