SUMMARY = "BlackBerry KEY2 (athena) hardware integration for LuneOS"
DESCRIPTION = "Keyboard backlight following the ambient light sensor, scrolling \
and cursor movement from the capacitive keyboard, BlackBerry-style Sym / Shift / \
Alt behaviour and auto-capitalisation for the physical keyboard, a glib pidfd \
workaround for appinstalld on the 4.19 kernel, and a power-key wake report."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "^athena$"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = " \
    file://kbdscroll.c \
    file://symkey.c \
    file://nopidfd.c \
    file://athena-kbd-backlight \
    file://athena-kbd-backlight.service \
    file://athena-kbd-scroll.service \
    file://athena-wake-report \
    file://athena-symkey.conf \
    file://appinstalld-nopidfd.conf \
    file://90-athena-extras.preset \
"

S = "${UNPACKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "athena-kbd-backlight.service athena-kbd-scroll.service"
SYSTEMD_AUTO_ENABLE = "enable"

do_compile() {
    # athena-kbd-scroll: keyboard-surface scrolling / Alt+slide cursor
    ${CC} ${CFLAGS} ${LDFLAGS} -O2 -Wall -o athena-kbd-scroll ${S}/kbdscroll.c
    # Preloaded into MaliitServer (maliit-server@.service drop-in)
    ${CC} ${CFLAGS} ${LDFLAGS} -O2 -Wall -shared -fPIC -o libathena-symkey.so ${S}/symkey.c -ldl
    # Preloaded into appinstalld (appinstalld.service drop-in)
    ${CC} ${CFLAGS} ${LDFLAGS} -O2 -Wall -shared -fPIC -o libnopidfd.so ${S}/nopidfd.c -ldl
}

do_install() {
    install -d ${D}${bindir} ${D}${libdir}/athena
    install -m 0755 athena-kbd-scroll ${D}${bindir}/
    install -m 0755 ${S}/athena-kbd-backlight ${D}${bindir}/
    install -m 0755 ${S}/athena-wake-report ${D}${bindir}/
    install -m 0755 libathena-symkey.so libnopidfd.so ${D}${libdir}/athena/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/athena-kbd-backlight.service ${S}/athena-kbd-scroll.service ${D}${systemd_system_unitdir}/
    install -d ${D}${systemd_system_unitdir}/maliit-server@.service.d
    install -m 0644 ${S}/athena-symkey.conf ${D}${systemd_system_unitdir}/maliit-server@.service.d/athena-symkey.conf
    install -d ${D}${systemd_system_unitdir}/appinstalld.service.d
    install -m 0644 ${S}/appinstalld-nopidfd.conf ${D}${systemd_system_unitdir}/appinstalld.service.d/nopidfd.conf
    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${S}/90-athena-extras.preset ${D}${systemd_unitdir}/system-preset/
}

FILES:${PN} += " \
    ${libdir}/athena \
    ${systemd_system_unitdir} \
    ${systemd_unitdir}/system-preset \
"
# The preload shims are plain shared objects, not development libraries.
INSANE_SKIP:${PN} += "dev-so"

RDEPENDS:${PN} = "luna-service2"
