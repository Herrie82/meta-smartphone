FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH:halium9-gsi   = "${MACHINE_ARCH}"

SRC_URI:append:halium9-gsi   = " file://70-halium9-gsi.rules"


do_install:append:halium9-gsi() {
    install -m 0644 ${WORKDIR}/70-halium9-gsi.rules ${D}${sysconfdir}/udev/rules.d/70-halium9-gsi.rules
}
