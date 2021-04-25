FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH_cheryl   = "${MACHINE_ARCH}"

SRC_URI_append_${MACHINE}   = " file://70-${MACHINE}.rules"

do_install_append_${MACHINE}() {
    install -m 0644 ${WORKDIR}/70-${MACHINE}.rules ${D}${sysconfdir}/udev/rules.d/70-${MACHINE}.rules
}
