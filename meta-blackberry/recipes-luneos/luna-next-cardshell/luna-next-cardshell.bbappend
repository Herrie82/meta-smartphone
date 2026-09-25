# The gesture-area back swipe (and the other SwipeArea gestures) were dropped
# when the finger paused before lifting. Generic; belongs in
# webOS-ports/luna-next-cardshell, gated to athena until it lands there.
FILESEXTRAPATHS:prepend:athena := "${THISDIR}/${PN}:"
SRC_URI:append:athena = " file://0001-SwipeArea-count-a-swipe-that-pauses-before-lifting.patch"
