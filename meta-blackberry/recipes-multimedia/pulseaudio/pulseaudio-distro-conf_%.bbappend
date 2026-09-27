# athena: system sounds crackled. The HAL's FAST primary output runs 192-frame
# periods in a 384-frame (8 ms) buffer and underruns whenever the UI is busy
# (boot sound, volume keys). deep_buffer runs 1920-frame periods / 80 ms.
#
# Moving only pcm_output is not enough: the compositor's own sounds (boot,
# battery, shutdown, banners - QtMultimedia MediaPlayer) do not go through
# audiod but play on the default sink, sink.primary_output, at a fixed 100%.
# With pcm_output on deep_buffer and the boot sound alone on the FAST output
# before audiod is up, the boot sound was silent. Make pcm_output the default
# sink so every system sound takes the same path and follows the volume.
do_install:append:athena() {
    sed -i 's|^load-module module-remap-sink sink_name=pcm_output master=sink.primary_output remix=no|load-module module-remap-sink sink_name=pcm_output master=sink.deep_buffer remix=no|' \
        ${D}${sysconfdir}/pulse/webos-system.pa
    grep -q "master=sink.deep_buffer" ${D}${sysconfdir}/pulse/webos-system.pa
    printf '\nset-default-sink pcm_output\n' >> ${D}${sysconfdir}/pulse/webos-system.pa
}
