# minimal Mesa + 0017 (VSC pipe init) + 0040 (WFI) + 0046 (cache flush) + 0070 (SQ-slot scrub)
#
# 0017 (kept): zero all 8 VSC_PIPE registers + VSC_BIN_SIZE + LRZ_VSC_CONTROL
# in fd2_emit_restore. Falsified as the period-8 cycle source (deployed,
# tested 5/10/2026, 8-cycle persists), but kept as defensive init - matches
# what KGSL does at every context restore.
#
# 0070 (NEW): software emulation of KGSL's PM4_LOAD_CONSTANT_CONTEXT type=4
# shadow=1 broadcast (hw shadow path is unsafe on A2XX without a working
# GDSC governor). 8 dummy POINT draws with SQ_PROGRAM_CNTL forcing max VS
# GPR allocation, so each DRAW_INDX consumes a fresh SQ wavefront slot.
# After 8 draws all 8 internal SRAM slots have been touched with safe
# (zero) state from solid_prog. Inserted in fd2_clear after clear_state()
# emits solid_prog. Reference: Gemini AI update 16-17.
#
# Active set:
#   0001  is_a22x() helper
#   0002  scheduler instruction limit
#   0017  initialize VSC registers for A22X (defensive, falsified as fix)
#   0040  CP_WAIT_FOR_IDLE at start of fd2_emit_restore
#   0045  clear color via PS CONST[0]
#   0046  CACHE_FLUSH_AND_INV_EVENT at start of fd2_emit_restore (A22X)
#   0070  SQ wavefront-slot scrub via 8x dummy POINT draws (NEW)
#
# Expected outcome of 0070:
#   - 100/100 same hash 5adc3160 -> SQ slot theory confirmed, bug fixed.
#   - Still 8-cycle -> SQ slot model is wrong, investigate other 8-element
#     SRAM arrays. Update Gemini.
#   - GPU hang -> high-VS_REGS override unsafe; fall back to default
#     SQ_PROGRAM_CNTL or use a known-good user shader.
#
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

PACKAGECONFIG:append = " freedreno"

SRC_URI:append = " \
    file://0001-freedreno-add-is_a22x-helper-for-Adreno-220-225.patch \
    file://0002-freedreno-a2xx-increase-scheduler-instruction-limit.patch \
    file://0017-freedreno-a2xx-initialize-VSC-registers-for-A22X.patch \
    file://0040-freedreno-a2xx-WAIT_FOR_IDLE-at-start-of-fd2_emit_re.patch \
    file://0045-freedreno-a2xx-fix-non-fast-clear-color-on-A22X-writ.patch \
    file://0046-freedreno-a2xx-aggressive-cache-flush-invalidate-at-.patch \
    file://0080-freedreno-a2xx-reorder-SQ-inst-store-partition-seque.patch \
"
