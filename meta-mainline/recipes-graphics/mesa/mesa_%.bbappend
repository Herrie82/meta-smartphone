# Patches for mainline kernel devices (HP TouchPad, etc.)
# Adreno 220 (Leia) specific fixes for freedreno driver
#
# Patch descriptions:
# 0001: Add is_a22x() helper to detect Adreno 220/225 vs 200/205
# 0002: Increase shader scheduler instruction limit (1024 -> 2048)
# 0003: Add shader compiler error handling for A2XX limits
# 0004: Add shader program creation error handling
# 0005: Force triple buffering in EGL DRM for smoother display
# 0006: Add blend state debug logging (FD_MESA_DEBUG=msgs)
# 0007: REMOVED - Mesa 24.0.7 already has correct A22X register values
# 0008: Add wait-for-idle after draw for A22X (fix random artifacts)
# 0009: Add draw state debug logging for artifact analysis
# 0010: Add cache flush + WFI in 3 locations for GMEM sync on A22X (fix blur)
# 0011: Initialize A220 registers (GRAS, LRZ/VSC) + keep clocks enabled (avoid underrun)
# 0012: Initialize SQ_GPR_MANAGEMENT (allocate 64 GPRs each for VS/PS, fix faceted shading)
#
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://0001-freedreno-add-is_a22x-helper-for-Adreno-220-225-dete.patch \
    file://0002-freedreno-a2xx-increase-scheduler-instruction-limit.patch \
    file://0003-freedreno-a2xx-add-shader-compiler-error-handling.patch \
    file://0004-freedreno-a2xx-add-shader-program-error-handling.patch \
    file://0005-egl-drm-force-triple-buffering-for-smoother-display.patch \
    file://0006-freedreno-a2xx-add-blend-state-debug-logging.patch \
    file://0008-freedreno-a2xx-add-wait-for-idle-after-draw-for-A22X.patch \
    file://0009-freedreno-a2xx-add-draw-state-debug-logging.patch \
    file://0010-freedreno-a2xx-add-WFI-after-gmem2mem-for-RTT-sync.patch \
    file://0011-freedreno-a2xx-initialize-A220-registers-and-keep-cl.patch \
    file://0012-freedreno-a2xx-initialize-SQ_GPR_MANAGEMENT-for-A22X.patch \
"
