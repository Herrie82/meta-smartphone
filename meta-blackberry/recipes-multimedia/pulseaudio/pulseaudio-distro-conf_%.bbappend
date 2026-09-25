# athena: system sounds crackled. The HAL's FAST primary output runs 192-frame
# periods in a 384-frame (8 ms) buffer and underruns whenever the UI is busy
# (boot sound, volume keys). deep_buffer runs 1920-frame periods / 80 ms.
do_install:append:athena() {
    sed -i 's|^load-module module-remap-sink sink_name=pcm_output master=sink.primary_output remix=no|load-module module-remap-sink sink_name=pcm_output master=sink.deep_buffer remix=no|' \
        ${D}${sysconfdir}/pulse/webos-system.pa
    grep -q "master=sink.deep_buffer" ${D}${sysconfdir}/pulse/webos-system.pa
}
