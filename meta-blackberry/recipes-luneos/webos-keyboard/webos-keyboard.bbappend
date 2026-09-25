# athena (BlackBerry KEY2): collapse the on-screen keyboard while the physical
# one is in use, symbols panel on Sym (with athena-extras' libathena-symkey.so),
# and tap-to-latch Shift in the athena hwkeyboard profiles.
# 0001 is useful for any hwkeyboard device and belongs in webOS-ports/webos-keyboard.
FILESEXTRAPATHS:prepend:athena := "${THISDIR}/${PN}:"
SRC_URI:append:athena = " \
    file://0001-Keyboard.qml-stay-collapsed-while-a-physical-keyboar.patch \
    file://0002-hwkeyboard-athena-Shift-latches-on-a-tap.patch \
"
