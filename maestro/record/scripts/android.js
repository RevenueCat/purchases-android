function normalizeFilename(filename) {
    return filename.replace(/\.[^.]+$/, '') + '.mp4';
}

function start(filename) {
    var name = normalizeFilename(filename);

    var pid = output.shell.start('adb shell screenrecord /sdcard/' + name);

    if (!output.recordingPids) {
        output.recordingPids = {};
    }
    output.recordingPids[name] = pid;

    return pid;
}

function stop(filename, outputDir) {
    var name = normalizeFilename(filename);

    var pid = output.recordingPids ? output.recordingPids[name] : null;

    if (pid && pid !== -1) {
        output.shell.stop(pid);
        delete output.recordingPids[name];
    }

    if (outputDir) {
        var destination = outputDir + '/' + name;
        output.shell.run('adb pull /sdcard/' + name + ' ' + destination);
    }

    // Clean up the recording file from the emulator
    output.shell.run('adb shell rm -f /sdcard/' + name);
}

output.record = {
    start: start,
    stop: stop
};
