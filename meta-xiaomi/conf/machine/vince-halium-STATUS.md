# Xiaomi Redmi 5 Plus (vince) — LuneOS Halium port status

Machine `vince-halium`, written 31 Oct 2026. Qualcomm MSM8953, 1080x2160. Approach: the 4.9.251 kernel of
xiaomi-msm8953-devs/temp_android_kernel_xiaomi_vince (lineage-18.1, 0fd7a835) and the arm64 16.0 GSI over a
LineageOS 18.1 `m vendorimage` vendor, VNDK 30. Nothing is derived or converted, unlike tissot and mido.

State: **built, never booted.** `luneos-dev-image` builds; no vince has been available, so every statement
below comes from the trees, the blobs or the built image, none from a phone.

## Checked on the build output

- boot.img: kernel 13,053,987 B, inside the 16,744,448 B before the ramdisk address; ramdisk 4,115,310 B;
  17,174,528 B in all against a 67,108,864 B boot partition. Addresses and cmdline read back from the header.
- wlan.ko (prima), wireguard.ko and v4l2loopback.ko are in the rootfs, signed, vermagic 4.9.251.
- vendor.img in the rootfs is 456,933,376 B, `ro.vndk.version=30` (in /vendor/default.prop), raw ext4.
- Symbol closure against VNDK 30 + LLNDK + bionic: nothing unresolved in the vendor's own objects for composer,
  allocator, audio, sensors, camera, fingerprint, BT, GNSS, Wi-Fi, qcrild, vibrator, IR, health, power, OMX.
  That is link level only.
- `/usr/share/luneos/adaptations/vince/deviceinfo` is installed.

## To look at on the first boot

- ofono: 40-ofono-binder derives the slots through lshal. The vendor manifest has @1.4::IRadio/slot1 and slot2.
  If lshal fails as it did on sargo, ofono is left with the shipped socket-based config: set
  `deviceinfo_binder_slots` and `deviceinfo_binder_radio_interface` in the adaptation.
- Audio: pulseaudio needs the vendor's VNDK (set_sched_policy). `deviceinfo_hybris_prefer_vndk` is set for that.
- Fingerprint: the vendor has the goodix and fpc HALs, not `gf_fingerprint.default.so`, so the tissot block of
  fingerprint-hal-fixup is skipped and only its uinput fix runs.
- Key devices, pixel ratio and the 401 PPI are not confirmed. The device tree's panel dimensions cannot give it.
- The phone's firmware partitions must suit LineageOS 18.1.

## Install

`bitbake luneos-dev-package` gives `luneos-dev-package-vince-halium-*.zip`, flashed from a custom recovery
that can mount `/data`. It writes `/dev/block/bootdevice/by-name/boot` and unpacks to `/data/luneos`;
`vendor.img` and the GSI come with it, so `system` and `cust` are not touched. Never tried on a phone.

## Open

- `vince-halium-vendor.img` is not published. Until it is in the `halium-luneos-20261005` release of
  webOS-ports/halium-images, `android-system-image-vince-halium` only builds with the file seeded in DL_DIR as
  `vince-halium-vendor-20261017-1.img`.
- The kernel comes from a repository named `temp_`. It should be mirrored to shr-distribution/linux, where the
  recipe patches would become commits.
- Kernel build: 110 compiler warnings under GCC 15, all in the third-party tree, none in lines the patches add.
  Clang is not tried. The kernel is arm64 only, so armv7 and x86-64 do not apply.
- `CONFIG_CC_WERROR` in luneos.cfg is a symbol this tree does not have; the line is a no-op.
