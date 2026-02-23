FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append:tenderloin = "\
    file://S01-mount-boot.sh \
    file://K99-move-boot.sh \
    file://qcom/leia_pm4_470.fw \
    file://qcom/leia_pfp_470.fw \
    file://ath6k/AR6003/hw2.1.1/athwlan.bin \
    file://ath6k/AR6003/hw2.1.1/bdata.bin \
    file://ath6k/AR6003/hw2.1.1/bdata.SD31.bin \
    file://ath6k/AR6003/hw2.1.1/bdata.SD32.bin \
    file://ath6k/AR6003/hw2.1.1/bdata.WB31.bin \
    file://ath6k/AR6003/hw2.1.1/data.patch.bin \
    file://ath6k/AR6003/hw2.1.1/endpointping.bin \
    file://ath6k/AR6003/hw2.1.1/fw-2.bin \
    file://ath6k/AR6003/hw2.1.1/fw-3.bin \
    file://ath6k/AR6003/hw2.1.1/otp.bin \
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
    # Must be /lib/firmware/qcom (not /usr/lib) for kernel firmware loader
    install -d ${D}/lib/firmware/qcom
    install -m 0644 ${WORKDIR}/qcom/leia_pm4_470.fw ${D}/lib/firmware/qcom/leia_pm4_470.fw
    install -m 0644 ${WORKDIR}/qcom/leia_pfp_470.fw ${D}/lib/firmware/qcom/leia_pfp_470.fw

    # Install AR6003 wifi firmware files for early boot
    install -d ${D}/lib/firmware/ath6k/AR6003/hw2.1.1
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/athwlan.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/athwlan.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/bdata.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/bdata.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/bdata.SD31.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/bdata.SD31.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/bdata.SD32.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/bdata.SD32.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/bdata.WB31.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/bdata.WB31.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/data.patch.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/data.patch.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/endpointping.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/endpointping.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/fw-2.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/fw-2.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/fw-3.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/fw-3.bin
    install -m 0644 ${WORKDIR}/ath6k/AR6003/hw2.1.1/otp.bin ${D}/lib/firmware/ath6k/AR6003/hw2.1.1/otp.bin
}

FILES:${PN} += "/scripts/local-premount /scripts/local-bottom /lib/firmware/qcom /lib/firmware/ath6k/AR6003/hw2.1.1/"

# Skip usrmerge check - initramfs needs /lib/firmware for kernel firmware loader
INSANE_SKIP:${PN}:append:tenderloin = " usrmerge"
