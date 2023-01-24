FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append:halium9-gsi = " \
    file://wifi-macaddr-persister.service \
    file://wifi-module-load.service \
    file://persist-wifi-mac-addr.sh \
    file://hciattach.service \
    file://hciattach.sh \
"

do_install:append:halium9-gsi() {
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/wifi-macaddr-persister.service ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/hciattach.service ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/wifi-module-load.service ${D}${systemd_unitdir}/system

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/persist-wifi-mac-addr.sh ${D}${bindir}
    install -m 0755 ${WORKDIR}/hciattach.sh ${D}${bindir}
}

SYSTEMD_SERVICE:${PN}:halium9-gsi = " \
    wifi-macaddr-persister.service \
    wifi-module-load.service \
    hciattach.service \
"
