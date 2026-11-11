require recipes-kernel/linux/linux.inc
# Options every Halium target needs; see the file for what and why.
require recipes-kernel/linux/halium-kernel.inc
# The build fixes of the 3.4 Halium kernels apply to this 3.10 tree as well, as for a3-2015-halium:
# the C standard and no-PIE flags for the target compiler, -std=gnu17 for the host tools and the
# mach-types banner rewrite. Its renameat2 patch is only for machines that set HALIUM_LEGACY_GSI,
# which this one does not: this kernel has renameat2 already (__NR_renameat2 in unistd.h).
require recipes-kernel/linux/halium-kernel-3.4.inc

SECTION = "kernel"

# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "^fortuna3g-halium$"

DESCRIPTION = "Linux kernel for the Samsung Galaxy Grand Prime 3G (SM-G530H, fortuna3g, MSM8916) \
on Halium, built from Gabboxl's LineageOS kernel"

#-----------------------------------------------------------------------------
# Source
#-----------------------------------------------------------------------------
# github.com/Gabboxl/android_kernel_samsung_msm8916, branch lineage-19.1 (head 8e443930f193 of
# 1 Apr 2022): Samsung's MSM8916 kernel as the Galaxy-MSM8916 team carried it for the Grand Prime
# family (fortuna3g, fortunave3g, gprimelte, ...), maintained by Gabboxl for the fortuna3g LineageOS
# builds. lineage-19.1 is lineage-18.1 (what the LineageOS 18.1 build and the SailfishOS port of
# fortuna-for-smoothie use) plus nine commits: the dtc yylloc fix a current host compiler needs, memory
# cgroups and PSI, and no Android low-memory killer. It is a 32-bit ARM kernel (arch/arm, zImage),
# 3.10.108, with binder (binder, hwbinder, vndbinder), ashmem, seccomp, getrandom, memfd_create and
# renameat2, so Android 16's bionic needs nothing backported.
#
# Built from fortuna3g/3.10/lineage-19.1 in shr-distribution/linux: that head with LuneOS' patches
# committed on top, as for a3-2015/3.10/lineage-18.1. The patches live there as commits, not as .patch
# files here. Checked against the kernels the GSI already runs on (a3-2015, bullhead, sm-t520, flo,
# hammerhead, tenderloin), each fix either taken or found already in this 3.10 tree:
#  - from a3-2015/3.10/lineage-18.1 (cherry-picked with -x): COMMAND_LINE_SIZE 2048, ipc_router's
#    net_raw check, MemAvailable, the L2CAP shutdown series, the v4l2 bits for v4l2loopback, the rt5033
#    log flood, the put_user and section-directive build fixes; not its lk2nd bring-up commits;
#  - renameat2 for flags == 0 and clock_gettime64/clock_getres_time64, as sm-t520 has them: this tree
#    only reserved slot 382 (the uapi number commented out) and had no time64 calls;
#  - Halium's SECURITY_ANDROID_GID_CAPABILITIES, as on bullhead (ANDROID_PARANOID_NETWORK is off);
#  - the linux/err.h fix to cpufreq_relaxed.c from the SailfishOS port (fortuna-for-smoothie), which
#    GCC 15 needs once PID_NS is on.
# Already right in 3.10, so not taken: skb_set_peeked, pidns_install, the cgroup grace periods and
# release flag, the snd_pcm_info ioctl, PR_SET_VMA, per-netns ifindex, O_PATH fchdir/fstatfs, the
# android.c "ffs" alias. Not needed on armv7: execveat (glibc here keeps its /proc fallback). The in-tree
# Bluetooth stack has mgmt 1.3 like the A3's and bullhead's, so no backports stack (flo, mako and
# tenderloin need it for their mgmt 0.1).
SRCREV = "488e03d47ac4788ff19800a6c533e674844dea5b"

SRC_URI = "git://github.com/shr-distribution/linux.git;branch=fortuna3g/3.10/lineage-19.1;protocol=https \
           file://luneos.cfg \
           git://github.com/LineageOS/android_system_tools_dtbtool.git;branch=lineage-18.1;protocol=https;name=dtbtool;destsuffix=dtbtool \
           "
SRCREV_FORMAT = "default_dtbtool"
SRCREV_dtbtool = "ffd4bcb0e07a135e9bb65b07e33f6c8f5f1c47b9"

LINUX_VERSION = "3.10.108"
PV = "${LINUX_VERSION}+git"
# for bumping PR bump MACHINE_KERNEL_PR in the machine config
inherit machine_kernel_pr

#-----------------------------------------------------------------------------
# Boot image
#-----------------------------------------------------------------------------
# From msm8916-common's BoardConfigCommon.mk (fortuna-for-smoothie, lineage-18.1): base 0x80000000,
# page size 2048, ramdisk offset 0x02000000, tags offset 0x01E00000, and the usual kernel (0x8000)
# and second (0x00f00000) offsets; header version 0. The command line is the LineageOS one
# (BOARD_KERNEL_CMDLINE), with the loop.max_part=7 Halium needs. The boot partition is 13 MiB.
ANDROID_BOOTIMG_CMDLINE = "console=null androidboot.hardware=qcom user_debug=23 msm_rtb.filter=0x3F ehci-hcd.park=3 androidboot.bootdevice=7824900.sdhci androidboot.selinux=permissive loop.max_part=7"
ANDROID_BOOTIMG_KERNEL_RAM_BASE = "0x80008000"
ANDROID_BOOTIMG_RAMDISK_RAM_BASE = "0x82000000"
ANDROID_BOOTIMG_SECOND_RAM_BASE = "0x80f00000"
ANDROID_BOOTIMG_TAGS_RAM_BASE = "0x81e00000"

inherit kernel_android

# kernel.bbclass sets S = "${STAGING_KERNEL_DIR}", and do_symlink_kernsrc only
# moves the unpacked tree there when the recipe points S somewhere else.
S = "${UNPACKDIR}/${BP}"

FILESEXTRAPATHS:prepend := "${THISDIR}/linux-samsung-fortuna3g-halium:"

# What the LineageOS build configures (TARGET_KERNEL_CONFIG, TARGET_KERNEL_VARIANT_CONFIG and
# TARGET_KERNEL_SELINUX_CONFIG of the device trees; Samsung's kconfig concatenates the three, the
# variant and SELinux parts after the base), then luneos.cfg, the LuneOS/Halium delta: later lines win
# over earlier ones when kconfig reads the concatenation. (TARGET_KERNEL_SELINUX_LOG_CONFIG names
# selinux_log_defconfig, which this tree does not have; the LineageOS build skips it as well.)
#
# msm8916_sec_defconfig sets CONFIG_MODULES but no option to m. The LineageOS build resolves it with
# "make <defconfig>", which keeps the "USB Gadget Drivers" choice at y; Yocto's olddefconfig over the same
# file with CONFIG_MODULES already on puts that tristate choice in m mode instead, where the bool
# USB_G_ANDROID cannot be selected, and drops it (no adb, no RNDIS). So, as on a3-2015-halium, resolve the
# configuration without modules first (every option the build has is =y), then turn on module loading
# only and run olddefconfig again, which keeps every =y: what v4l2loopback needs and nothing else changes.
do_configure:prepend() {
    cat ${S}/arch/arm/configs/msm8916_sec_defconfig \
        ${S}/arch/arm/configs/msm8916_sec_fortuna3g_eur_defconfig \
        ${S}/arch/arm/configs/selinux_defconfig \
        ${UNPACKDIR}/luneos.cfg | sed '/^CONFIG_MODULES=/d; /^CONFIG_MODULE_UNLOAD=/d' > ${WORKDIR}/defconfig
    echo "# CONFIG_MODULES is not set" >> ${WORKDIR}/defconfig
}

do_configure:append() {
    sed -i '/CONFIG_MODULES[ =]/d' ${B}/.config
    echo "CONFIG_MODULES=y"       >> ${B}/.config
    echo "CONFIG_MODULE_UNLOAD=y" >> ${B}/.config
    yes '' | oe_runmake_call -C ${S} O=${B} olddefconfig > /dev/null
    grep -q '^CONFIG_MODULES=y' ${B}/.config || bbfatal "fortuna3g: CONFIG_MODULES did not stick"
    grep -q '^CONFIG_MACH_FORTUNA3G_EUR=y' ${B}/.config || bbfatal "fortuna3g: the board variant was lost"
    grep -q '^CONFIG_USB_G_ANDROID=y' ${B}/.config || bbfatal "fortuna3g: the gadget choice was lost"
}

do_install:append() {
    # make headers_install leaves kbuild's ..install.cmd bookkeeping behind, and
    # linux.inc ships everything under ${exec_prefix}/src/linux* as kernel-headers.
    # Those files record absolute command lines, which wrynose rejects as
    # "contains reference to TMPDIR [buildpaths]".
    find ${D}${exec_prefix}/src -name '..install.cmd' -delete 2>/dev/null || true
    # This 3.10 tree's headers_install also writes the staging ION uapi header under
    # /usr/src/usr, which no package ships; a3-2015-halium drops it the same way.
    rm -rf ${D}${exec_prefix}/src/usr
}

#-----------------------------------------------------------------------------
# Device tree: a Qualcomm QCDT image in the boot image
#-----------------------------------------------------------------------------
# The bootloader takes the device tree from a QCDT table placed after the kernel, ramdisk and second
# stage of a version 0 boot image, with its size in the header word that later versions use for
# header_version (offset 40); see a3-2015-halium, whose a3_qcdt_bootimg this copies. The A3 takes a
# prebuilt table from its blobs; fortuna3g's blobs have none, and the LineageOS build makes it from
# the kernel's own device trees (BOARD_KERNEL_SEPARATED_DT): "make dtbs" builds the eight
# msm8916-sec-fortuna-r*.dtb that CONFIG_MACH_FORTUNA3G_EUR selects (board ids 0xCE08FF01 rev 0 to
# 11), and dtbToolLineage packs them, as vendor/lineage/build/tasks/dt_image.mk runs it:
#   dtbToolLineage -2 -o dt.img -s 2048 -p <kernel out>/scripts/dtc/ <kernel out>/arch/arm/boot/
# (-2 from BOARD_DTBTOOL_ARGS, -s from BOARD_KERNEL_PAGESIZE). The tool is LineageOS'
# system/tools/dtbtool (BSD-3-Clause, The Linux Foundation), built here for the build host only.
do_compile:append() {
    oe_runmake -C ${S} O=${B} dtbs
    ${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS} -o ${B}/dtbToolLineage ${UNPACKDIR}/dtbtool/dtbtool.c
    ${B}/dtbToolLineage -2 -o ${B}/dt.img -s 2048 -p ${B}/scripts/dtc/ ${B}/arch/arm/boot/
    [ "$(head -c 4 ${B}/dt.img)" = "QCDT" ] || bbfatal "dtbToolLineage made no QCDT table"
}

do_deploy[postfuncs] += "fortuna3g_qcdt_bootimg"

python fortuna3g_qcdt_bootimg() {
    import hashlib, os, struct

    qcdt = open(os.path.join(d.getVar("B"), "dt.img"), "rb").read()
    if qcdt[:4] != b"QCDT":
        bb.fatal("dt.img is not a QCDT table")

    deploydir = d.getVar("DEPLOYDIR")
    for t in d.getVar("KERNEL_IMAGETYPES").split():
        path = os.path.join(deploydir, "%s-%s.fastboot" % (t, d.getVar("KERNEL_IMAGE_NAME")))
        img = bytearray(open(path, "rb").read())
        if img[:8] != b"ANDROID!":
            bb.fatal("%s is not an Android boot image" % path)
        ksize, _, rsize, _, ssize, _, _, page, dtsize = struct.unpack_from("<9I", img, 8)
        if dtsize:
            bb.fatal("%s already has a header version or dt size (%d) at offset 40" % (path, dtsize))
        pages = lambda n: (n + page - 1) // page * page
        end = page + pages(ksize) + pages(rsize) + pages(ssize)
        sections = [bytes(img[page:page + ksize]),
                    bytes(img[page + pages(ksize):page + pages(ksize) + rsize]),
                    bytes(img[page + pages(ksize) + pages(rsize):page + pages(ksize) + pages(rsize) + ssize]),
                    qcdt]
        # The image id is the sha1 over each section and its size, the dt included, as
        # LineageOS' mkbootimg computes it.
        sha = hashlib.sha1()
        for s in sections:
            sha.update(s)
            sha.update(struct.pack("<I", len(s)))
        struct.pack_into("<I", img, 40, len(qcdt))
        img[576:608] = sha.digest() + b"\0" * 12
        out = bytes(img[:end]) + qcdt + b"\0" * (-len(qcdt) % page)
        # The LineageOS build appends this marker to every boot image (hardware/samsung's
        # mkbootimg.mk); without it the bootloader warns that the kernel is not SEAndroid enforcing.
        out += b"SEANDROIDENFORCE"
        limit = 13631488
        if len(out) > limit:
            bb.fatal("%s is %d bytes, the boot partition holds %d" % (path, len(out), limit))
        with open(path, "wb") as f:
            f.write(out)
        bb.note("Added the %d byte QCDT table to %s (%d bytes)" % (len(qcdt), path, len(out)))
}
