var SHELL_SERVER_URL = 'http://localhost:8080';

function isServerRunning() {
    try {
        var response = http.get(SHELL_SERVER_URL + '/run?cmd=true', { 
            connectTimeout: 500,
            readTimeout: 500
        });
        return response.status === 200;
    } catch (e) {
        return false;
    }
}

function run(cmd) {
    if (!isServerRunning()) {
        return { exitCode: -1, stdout: '', stderr: 'shell-over-http server not running' };
    }
    var response = http.get(SHELL_SERVER_URL + '/run?cmd=' + encodeURIComponent(cmd));
    var result = JSON.parse(response.body);
    return result;
}

function start(cmd) {
    if (!isServerRunning()) {
        return -1;
    }
    var response = http.post(SHELL_SERVER_URL + '/start?cmd=' + encodeURIComponent(cmd), { body: '' });
    var pid = parseInt(response.body, 10);
    return pid;
}

function stop(pid) {
    if (!isServerRunning()) {
        return { exitCode: -1, stdout: '', stderr: 'shell-over-http server not running' };
    }
    var response = http.post(SHELL_SERVER_URL + '/stop?pid=' + pid, { body: '' });
    var result = JSON.parse(response.body);
    return result;
}

output.shell = {
    run: run,
    start: start,
    stop: stop
};
