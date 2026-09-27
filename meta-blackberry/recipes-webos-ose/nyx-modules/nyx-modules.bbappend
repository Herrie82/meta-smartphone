# Generic bug, found on athena: read_input_event() read every input node into
# the same buffer, so a second node with pending input overwrote the power
# key's events on resume ("the power button often does nothing").
# Belongs in webOS-ports/nyx-modules; gated to athena until it lands there.
FILESEXTRAPATHS:prepend:athena := "${THISDIR}/${PN}:"
SRC_URI:append:athena = " file://0001-keys-read-each-input-node-after-what-the-previous-on.patch"
