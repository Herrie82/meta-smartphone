FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append:tenderloin = "\
    file://S01-mount-boot.sh \
    file://K99-move-boot.sh \
    file://leia_pm4_470.fw \
    file://leia_pfp_470.fw \
    file://ath6k/AR6003/hw2.1.1/athwlan.bin \
    file://ath6k/AR6003/hw2.1.1/bdata.SD32.bin \
    file://ath6k/AR6003/hw2.1.1/bdata.SD32_3G.bin \
    file://ath6k/AR6003/hw2.1.1/otp.bin \
    file://ath6k/AR6003/hw2.1.1/data.patch.bin \
"

COMPATIBLE_MACHINE:tenderloin = "tenderloin"

do_install:append:tenderloin() {
    install -d ${D}/scripts/local-premount
    install -d ${D}/scripts/local-bottom
    install -m 0755 ${WORKDIR}/S01-mount-boot.sh ${D}/scripts/local-premount/S01-mount-boot.sh
    install -m 0755 ${WORKDIR}/K99-move-boot.sh ${D}/scripts/local-bottom/K99-move-boot.sh
    echo ". /scripts/local-premount/S01-mount-boot.sh" >> ${D}/scripts/local-premount/ORDER
    echo ". /scripts/local-bottom/K99-move-boot.sh" >> ${D}/scripts/local-bottom/ORDER

    # Install Adreno 200 (Z180) GPU firmware for early boot
    # Must be /lib/firmware (not /usr/lib) for kernel firmware loader
    install -d ${D}/lib/firmware
    install -m 0644 ${WORKDIR}/leia_pm4_470.fw ${D}/lib/firmware/leia_pm4_470.fw
    install -m 0644 ${WORKDIR}/leia_pfp_470.fw ${D}/lib/firmware/leia_pfp_470.fw

    # WiFi firmware for AR6003 - must be in initramfs so it's available at probe time
    # (rootfs not mounted until ~49s after boot; BMI times out at ~45s)
    install -d ${D}/lib/firmware/ath6k/AR6003/hw2.1.1
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/athwlan.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/athwlan.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/bdata.SD32.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/bdata.SD32.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/bdata.SD32_3G.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/bdata.SD32_3G.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/otp.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/otp.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/data.patch.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/data.patch.bin
}

FILES:${PN} += "/scripts/local-premount /scripts/local-bottom /lib/firmware"

# Skip usrmerge check - initramfs needs /lib/firmware for kernel firmware loader
INSANE_SKIP:${PN}:append:tenderloin = " usrmerge"
