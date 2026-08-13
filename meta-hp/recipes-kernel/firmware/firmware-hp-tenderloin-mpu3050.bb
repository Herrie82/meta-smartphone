DESCRIPTION = "MPU-3050 DMP firmware blob for HP TouchPad (tenderloin)"
SUMMARY = "InvenSense MPU-3050 Digital Motion Processor firmware extracted \
from the legacy webOS userspace driver (libusd_drv_mpu3050.so) shipped \
with HP webOS Doctor v3.0.5 for the TouchPad. The mainline iio/gyro/ \
mpu3050 driver requests this blob via request_firmware() at probe time \
to upload it into the chip's 1 KiB DMP RAM, which is then used by the \
gyro FIFO read path to bypass a stale-byte hazard on the apq8060 i2c \
controller."

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Proprietary;md5=0557f9d92cf58f2ccdd50f62f8ac0b28"

PACKAGE_ARCH = "${MACHINE_ARCH}"

COMPATIBLE_MACHINE = "^tenderloin$"

PV = "20111221"

SRC_URI = " \
    file://mpu3050_dmp.bin \
"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware/invensense
    install -m 0644 ${UNPACKDIR}/mpu3050_dmp.bin ${D}${nonarch_base_libdir}/firmware/invensense/mpu3050_dmp.bin
}

FILES:${PN} = "${nonarch_base_libdir}/firmware"
