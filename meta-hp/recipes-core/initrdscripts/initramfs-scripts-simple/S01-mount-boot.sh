#!/bin/sh

info "[initramfs] S01-mount-boot: starting"

# Check if mmcblk0p13 exists
if [ ! -b /dev/mmcblk0p13 ]; then
    info "[initramfs] Waiting for mmcblk0p13..."
    sleep 2
    if [ ! -b /dev/mmcblk0p13 ]; then
        info "[initramfs] Available block devices:"
        ls -la /dev/mmcblk* 2>/dev/null
        fail "/dev/mmcblk0p13 not found"
    fi
fi

# mount boot partition (may already be mounted rw for debug logging)
mkdir -p /mnt/boot
if mount | grep -q "/mnt/boot"; then
    info "[initramfs] /mnt/boot already mounted (debug logging), remounting ro..."
    # Sync any pending writes before remounting read-only
    sync
    mount -o remount,ro /mnt/boot
else
    mount -o ro /dev/mmcblk0p13 /mnt/boot
    if [ $? -ne 0 ]; then
        fail "Failed to mount /dev/mmcblk0p13 on /mnt/boot"
    fi
fi

info "[initramfs] mmcblk0p13 mounted."

# use lvm.static binary file to initialize /dev/store
[ ! -d /var/lock ] && mkdir -p /var/lock
[ ! -d /run/lock ] && mkdir -p /run/lock

if [ -e /mnt/boot/usr/sbin/lvm.static ]; then
    info "[initramfs] Activating LVM..."
    export LVM_SYSTEM_DIR=/mnt/boot/etc/lvm
    info "LVM_SYSTEM_DIR: $LVM_SYSTEM_DIR"
    # The PV (mmcblk0p14) may not be fully ready the instant we get here
    # (device-node creation / first eMMC access settling after the boot
    # partition remount), so a single vgchange can race ahead of readiness
    # and silently activate nothing. Retry until /dev/store appears rather
    # than relying on a fixed sleep. (vgchange -ay returns 1 on the
    # "already active" re-run, so gate on /dev/store, not on rc.)
    n=0
    while [ "$n" -lt 15 ]; do
        /mnt/boot/usr/sbin/lvm.static vgchange -ay >/dev/null 2>&1
        if [ -d /dev/store ]; then
            break
        fi
        n=$((n + 1))
        sleep 1
    done
    if [ -d /dev/store ]; then
        info "[initramfs] LVM activated (/dev/store present after $n retr$([ "$n" = 1 ] && echo y || echo ies))"
    else
        info "[initramfs] LVM vgchange: /dev/store still absent after $n retries"
        /mnt/boot/usr/sbin/lvm.static vgchange -ay 2>&1
    fi
else
    info "/mnt/boot/usr/sbin/lvm.static not found: skipping LVM2 volume group activation!"
fi

if [ -d /dev/store ]; then
    info "[initramfs] LVM volumes found in /dev/store:"
    for g in $(ls /dev/store); do
        info "  /dev/store/$g"
    done

    # --- fabric/ICC clock diagnostic vs webOS reference ------------------
    # webOS (measured live over novacom): dfab_clk/dfab_sdc1_clk = 64 MHz
    # CONSTANT; ebi1/afab scale 125-314 MHz. Our large eMMC write still
    # wedges ~5x slower than webOS even with the DFAB vote at 64 MHz, so
    # dump our actual fabric rates + ICC votes to find the second starved
    # clock (suspect: the ADM EBI/SFAB memory-read path). Idle + during a
    # safe sustained read.
    mount -t debugfs none /sys/kernel/debug 2>/dev/null
    dump_fabric() {
        grep -iE "fab|ebi|sdc|adm|axi|dma" /sys/kernel/debug/clk/clk_summary \
            2>/dev/null | while read l; do info "  CLK $l"; done
        if [ -r /sys/kernel/debug/interconnect/interconnect_summary ]; then
            while read l; do info "  ICC $l"; done \
                < /sys/kernel/debug/interconnect/interconnect_summary
        fi
    }
    info "[fabric] === IDLE (after LVM) ==="
    dump_fabric
    # NOTE: the 48 MB sustained-read test was REMOVED — a large eMMC read
    # wedges ADM ch2 and dd then hangs in close()/writeback forever,
    # blocking the boot in initramfs. The real workload (rootfs mount +
    # journald flush) exercises the eMMC; we don't need a synthetic read
    # here. Idle fabric/ICC dump above is kept (harmless).
    # --------------------------------------------------------------------
else
    info "[initramfs] /dev/store does not exist after LVM activation"
    info "[initramfs] Available /dev/mapper entries:"
    ls -la /dev/mapper/ 2>/dev/null
    info "[initramfs] Available /dev/dm-* entries:"
    ls -la /dev/dm-* 2>/dev/null
    fail "Failed to start LVM - /dev/store not found"
fi

