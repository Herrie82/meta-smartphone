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
    file://0040-freedreno-a2xx-WAIT_FOR_IDLE-at-start-of-fd2_emit_re.patch \
    file://0045-freedreno-a2xx-fix-non-fast-clear-color-on-A22X-writ.patch \
    file://0046-freedreno-a2xx-aggressive-cache-flush-invalidate-at-.patch \
    file://0090-freedreno-a2xx-write-per-tile-VGT_CURRENT_BIN_ID-for.patch \
    file://0093-freedreno-a2xx-Fork-C-minimum-viable-A22X-hw-binning.patch \
    file://0094-freedreno-a2xx-VSC-pipe-BO-dump-diagnostic.patch \
"
# 0093 (CURRENT): Fork A/B full A22X hw binning integration.
# Filename kept as "Fork-C-minimum-viable" for historical continuity
# (avoid build-cache churn) but the content has evolved:
#   Fork C v1 (broken): CP_REG underflow for VSC regs
#   Fork C v2: OUT_PKT0 fix, prelude works, cycle still period-8
#   Fork D: + SQ_GPR_MANAGEMENT pulse, cycle shifts but persists
#   Fork D + disengage: + LRZ_VSC_CONTROL=0 at prelude start
#   Fork A/B (CURRENT): use_hw_binning() returns true for A22X,
#     prelude gated on use_hw_binning, A20X-specific shader patching
#     wrapped in is_a20x guards, batch->binning IB now runs for A22X,
#     binner disengaged after IB completes, per-tile renderprep
#     fires CP_SET_DRAW_INIT_FLAGS for A22X via use_hw_binning path.
# Known follow-up: ir2_nir.c extra_position_exports still emits 8
# A20X memexport CFs in the binning shader; for A22X these write to
# undefined memory (we skip the A20X memexport address constants).
# References:
#   - leia_configure_binning_pass decomp (webos libGLESv2 @ 0x124a50)
#   - reports/a22x-hw-binning-port-analysis.md
#   - reports/a22x-hw-binning-phase2-impl-plan.md
#
# 0094 (NEW): diagnostic VSC pipe BO content dump (env FD2_VSC_DUMP=1).
# Per-hash debugfs proved cycle source not in MMIO. This dumps the
# actual visibility-stream bytes the binner wrote per pipe, so we can
# tell if the binner is cycling (different content per phase) or
# downstream of binner (same content, different downstream interpretation).
# No effect when env var unset. Used with gl-cap-and-regdump-mainline
# double-render mode. Helps verify the Fork A/B hw binner actually
# produces non-zero visibility data per pipe.
#
# 0092 (DROPPED - hangs GPU): LRZ_VSC_CONTROL=0x03 partial-binning experiment.
# Result 2026-05-11: cycle DID collapse (13/13 same hash) but GPU hangs on
# every submit, returning all-zero (alpha=0) framebuffer. Confirms the
# register is the right lever - 0x03 activates binner data flow - but
# without full state setup (VSC_PIPE buffers, binning shader EXPORTs,
# GMEM binning config) the binner has nowhere to write and hangs.
# Phase 0 informative: validates full hw-binning port path.
# Patch file kept on disk under files/ for reference.
#
# 0091 absorbed into 0090 (combined patch, single hunk-set). Dropped to avoid double-patch.
# 0017 DROPPED (this build): tests theory that the zero-initialization of VSC_PIPE
# registers (which patch 0017 did at every batch start) puts the A22X binner into
# auto-cycle mode. By dropping 0017 the binner sees no VSC writes from us at all
# and may stabilize on whatever state it had.
#
# 0091 (NEW): skip BIN_ID=0 write in fd2_emit_tile_init for A22X. Follow-up to
# 0090 - the initial 0-write at batch start may put binner in auto-cycle mode
# before the per-tile non-zero writes from 0090 can take effect. Skipping the
# 0-write entirely lets the per-tile writes be the only ones.
#
# 0090 (HYPOTHESIS test): write per-tile VGT_CURRENT_BIN_ID for A22X non-binning.
# Visual analysis 2026-05-11 confirmed period-8 cycle is tile-coverage failure,
# not interpolation. Theory: A22X hw binner is active even though Mesa's
# use_hw_binning() returns false; with VGT_CURRENT_BIN_ID_MIN=MAX=0 the binner
# uses internal state to choose tile coverage, producing 8 patterns. Writing
# per-tile bin_id with the a20x ((row+1)<<3 | col+1) encoding (which is never 0)
# should collapse cycle to 1 hash if hypothesis correct. If null, the BIN_ID
# register doesn't drive the binner's relevant state machine.
#
# 0081 (REMOVED): bulk-zero all 4 shader-constant banks (ALU/TEX/BOOL/LOOP) in
# fd2_emit_restore. Re-applied as a 3/8 alignment test - the original commits
# bd9b43960f0 + 9e370652a6a celebrated 3/8 hash convergence which a later report
# judged as "lucky phase alignment from extra PM4 dwords". Re-test 5/11/2026
# on kernel 3c78981d produced byte-identical 1/8 baseline (same 8 hashes,
# same frequencies, same run order) - falsifies the phase-alignment claim AND
# rules out constant-bank zeroing as a viable cycle fix on the current kernel
# (whose sanitizer preamble already zeroes user ALU 32-511 + BOOL + LOOP).
# Patch file kept on disk but excluded from SRC_URI.
#
# 0080 (REMOVED): reorder SQ inst-store partition + INVALIDATE_STATE + SET_SHADER_BASES
# sequence in fd2_emit_restore to match legacy KGSL build_shader_save_restore_cmds
# restore-path order. Tested 5/11/2026 with kernel 3c78981d (force_collapse=N) and
# 100-cap test: produced byte-identical 8-cycle (87108faf 73bb37bb 5adc3160 259d419d
# acb14db9 9dbee617 0ad64bdc 070bdc57) with 12.5% 5adc3160 - zero change vs baseline.
# Falsifies the "CP_INVALIDATE_STATE clobbers SQ_INST_STORE_MANAGMENT" hypothesis.
# Patch file kept on disk but excluded from SRC_URI.
