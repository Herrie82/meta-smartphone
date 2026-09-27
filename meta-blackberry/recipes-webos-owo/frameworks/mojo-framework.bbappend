# Mojo on current WebAppMgr/Chromium - generic, found while getting
# SimpleChat to run on athena. These belong in webOS-ports/mojo-framework.
# mojo-framework is allarch, so they cannot be gated to one machine here.
FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI:append = " \
    file://0001-palmInitFramework506-remove-the-alert-dialog-s-raw-f.patch \
    file://0002-mojo.js-make-lightweight-stages-work-and-install-the.patch \
    file://0003-mojo-compat-touch-scrolling-file-cookies-IME-key-eve.patch \
"

do_install:append() {
    # Web apps may only load file:// URLs under their own and the framework
    # directories; the compat file picker shows thumbnails of /media/internal
    # through this link (as on webOS, where Mojo apps read it directly).
    ln -sfn /media/internal ${D}${webos_frameworksdir}/mojo/media-internal
}
