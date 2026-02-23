# Patches for mainline kernel devices (HP TouchPad, etc.)
# Adreno 220 (Leia) specific fixes for freedreno driver
#
# Rebased for Mesa 26.1.0-devel
#
# Patch descriptions:
# 0001: Add is_a22x() helper to detect Adreno 220/225 vs 200/205
# 0002: Increase shader scheduler instruction limit (1024 -> 2048)
# 0003: Add shader compiler error handling for A2XX limits
# 0004: Add shader program creation error handling
# 0005: Add blend state debug logging (FD_MESA_DEBUG=msgs)
# 0006: Add wait-for-idle after draw for A22X
# 0007: Add shader/GPU state debug logging (includes SQ_GPR_MANAGEMENT fix)
# 0008: Set SQ_INTERPOLATOR_CNTL after SQ_PROGRAM_CNTL (fixes faceted shading)
# 0009: Add draw state debug logging for artifact analysis
# 0011: Comprehensive interpolation debugging (VB hash, varying details)
# 0012: Add WFI around shader program setup (timing workaround)
# 0013: Enhanced A22X debug logging (always-on, more details)
# 0014: Add WFI after constant emission (fixes uniform-based shaders)
# 0015: Add WFI after vertex buffer setup (vertex fetch timing)
# 0016: Add VGT DMA wait on A22X before draw (replaces simple WFI with DMA poll)
# 0017: Initialize VSC (Visibility Stream Cache) registers for A22X
# 0018: Use synchronous cache flush (CACHE_FLUSH_AND_INV_EVENT) for A22X
#
# NOTE: TP0_CHICKEN must stay at 0x02 - setting to 0x00 hangs GPU!
# KGSL only sets it to 0 temporarily during GMEM ops, not globally.
#
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Enable freedreno driver for Adreno 220/225
PACKAGECONFIG:append = " freedreno"

SRC_URI:append = " \
    file://0001-freedreno-add-is_a22x-helper-for-Adreno-220-225.patch \
    file://0002-freedreno-a2xx-increase-scheduler-instruction-limit.patch \
    file://0003-freedreno-a2xx-add-shader-compiler-error-handling.patch \
    file://0004-freedreno-a2xx-add-shader-program-error-handling.patch \
    file://0005-freedreno-a2xx-add-blend-state-debug-logging.patch \
    file://0006-freedreno-a2xx-add-wait-for-idle-after-draw-for-A22X.patch \
    file://0007-freedreno-a2xx-add-shader-and-GPU-state-debug-loggin.patch \
    file://0008-freedreno-a2xx-set-SQ_INTERPOLATOR_CNTL-after-SQ_PRO.patch \
    file://0009-freedreno-a2xx-add-draw-state-debug-logging.patch \
    file://0011-freedreno-a2xx-add-comprehensive-interpolation-debug.patch \
    file://0012-freedreno-a2xx-add-WFI-around-shader-program-setup.patch \
    file://0013-freedreno-a2xx-enhance-A22X-debug-logging-for-interp.patch \
    file://0014-freedreno-a2xx-add-WFI-after-constant-emission-on-A2.patch \
    file://0015-freedreno-a2xx-add-WFI-after-vertex-buffer-setup-on-.patch \
    file://0016-freedreno-a2xx-add-VGT-DMA-wait-on-A22X-before-draw.patch \
    file://0017-freedreno-a2xx-initialize-VSC-registers-for-A22X.patch \
    file://0018-freedreno-a2xx-use-synchronous-cache-flush-for-A22X.patch \
"
