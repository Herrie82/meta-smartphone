# A22X (Adreno 220 / HP TouchPad) freedreno patch series — clean.
#
# Replaces the experimental "minimal + cycle-engineering" bbappend (now
# mesa_%.bbappend.pre-a22x-vsc-fix). All falsified period-8 experiments
# were dropped; the old patch files are kept under files/archive/ for
# reference.
#
# The period-8 cold-start cycle is fixed KERNEL-side (mmcc-msm8660 GFX3D core
# reset on power-on); these Mesa patches are the GMEM/VSC correctness fixes plus
# the cache/state-coherency patches needed to keep CONTINUOUS rendering stable
# (without them, long runs accumulate stale GPU cache/state and lock up).
#
#   0001  is_a22x() helper for Adreno 220/225
#   0002  fix non-fast-clear color on A22X (write to PS CONST[0])
#   0003  cache flush+invalidate at batch start (A22X)
#   0004  configure the A220 hardware VSC tile-binner
#   0005  enable HW fast-clear on A22X (GMEM background clear)
#   0006  set CLIP_DISABLE during GMEM operations
#   0007  add GMEM synchronization for A22X
#   0008  invalidate L2 texture cache at every batch start  <-- textures + stale-data
#   0009  WAIT_FOR_IDLE at start of fd2_emit_restore
#   0010  aggressive cache flush+invalidate at batch start  <-- continuous-render stability
#
# NO per-draw warmup: the period-8 cold-start cycle is gone (kernel fix), so the
# ghost-draw priming is not needed; runs at full FPS.
#
# 0008-0010 restore the batch-start cache-coherency that was carried on the
# experimental a2xx-patches branch but dropped during curation. The kernel
# gpummu_unmap invalidates the MMU TLB but NOT the GPU's L2 texture cache, so
# without 0008 the GPU samples stale texels (faceted/garbage texture scenes);
# 0009+0010 drain the pipeline and flush+invalidate caches at each batch start
# so stale state can't accumulate across submits and wedge the GPU on long runs.
#
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

PACKAGECONFIG:append = " freedreno"

SRC_URI:append = " \
    file://0001-freedreno-add-is_a22x-helper-for-Adreno-220-225.patch \
    file://0002-freedreno-a2xx-fix-non-fast-clear-color-on-A22X-writ.patch \
    file://0003-freedreno-a2xx-cache-flush-invalidate-at-batch-start.patch \
    file://0004-freedreno-a2xx-configure-the-A220-hardware-VSC-tile-.patch \
    file://0005-freedreno-a2xx-enable-HW-fast-clear-on-A22X-to-fix-G.patch \
    file://0006-freedreno-a2xx-set-CLIP_DISABLE-during-GMEM-operatio.patch \
    file://0007-freedreno-a2xx-add-GMEM-synchronization-for-A22X.patch \
    file://0011-freedreno-a2xx-keep-perfmon-enabled-so-kernel-devfre.patch \
"

# 0011: keep the RBBM perfmon enabled every batch (stock freedreno emitted
# CP_PERFMON_CNTL=0 unless FD_MESA_DEBUG=perfc). The a2xx kernel devfreq
# driver reads the RBBM busy perfcounter for GPU load; without this the
# counter stays frozen, devfreq sees ~0 load and pins the GPU at its minimum
# OPP (27 MHz). Pairs with the kernel a2xx perfmon-clock enable + NRT_BUSY
# select + idle-gated DVFS clock switch.

#    file://0008-freedreno-a2xx-invalidate-L2-texture-cache-at-every-.patch
#    file://0009-freedreno-a2xx-WAIT_FOR_IDLE-at-start-of-fd2_emit_re.patch
#    file://0010-freedreno-a2xx-aggressive-cache-flush-invalidate-at-.patch
