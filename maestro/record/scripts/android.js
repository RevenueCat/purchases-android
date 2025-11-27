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
        // Send SIGINT to the host-side adb process (propagates properly to device screenrecord)
        output.shell.run('kill -SIGINT ' + pid);
        
        // Wait for the process to exit gracefully (lets screenrecord write the moov atom)
        output.shell.run('while kill -0 ' + pid + ' 2>/dev/null; do sleep 0.5; done');
        
        // Extra wait for file system sync
        output.shell.run('sleep 2');
        
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
