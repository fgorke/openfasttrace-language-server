#!/usr/bin/env bash

# Drives one LSP session against a server over stdio and checks that the server answers.
#
# Usage: native_smoke_test.sh <workspace directory> <server command...>
#
# The session opens the workspace, waits for the trace diagnostics, asks for all workspace
# symbols, then shuts the server down. It fails when any answer is missing or the server does
# not exit. Meant for the native binary, where a missing reflection registration only shows
# at run time, but works for `java -jar` as well.

set -o errexit
set -o nounset
set -o pipefail

if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <workspace directory> <server command...>" >&2
    exit 2
fi

workspace="$1"
shift

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) root_uri="file:///$(cd "$workspace" && pwd -W)" ;;
    *) root_uri="file://$(cd "$workspace" && pwd -P)" ;;
esac

message() {
    local body="$1"
    # The bodies are ASCII, so the character count equals the byte count.
    printf 'Content-Length: %d\r\n\r\n%s' "${#body}" "$body"
}

session() {
    message '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"processId":null,"rootUri":"'"$root_uri"'","capabilities":{},"workspaceFolders":[{"uri":"'"$root_uri"'","name":"workspace"}]}}'
    message '{"jsonrpc":"2.0","method":"initialized","params":{}}'
    sleep 5
    message '{"jsonrpc":"2.0","id":2,"method":"workspace/symbol","params":{"query":""}}'
    sleep 2
    message '{"jsonrpc":"2.0","id":3,"method":"shutdown"}'
    sleep 1
    message '{"jsonrpc":"2.0","method":"exit"}'
}

stdout_file="$(mktemp)"
stderr_file="$(mktemp)"
trap 'rm -f "$stdout_file" "$stderr_file"' EXIT

echo "Starting: $*"
echo "Workspace: $root_uri"
session | "$@" >"$stdout_file" 2>"$stderr_file" &
server_pid=$!

for _ in $(seq 1 60); do
    if ! kill -0 "$server_pid" 2>/dev/null; then
        break
    fi
    sleep 1
done

if kill -0 "$server_pid" 2>/dev/null; then
    echo "Server did not exit within 60 seconds" >&2
    kill "$server_pid" 2>/dev/null || true
    cat "$stderr_file" >&2
    exit 1
fi

exit_code=0
wait "$server_pid" || exit_code=$?

failed=0
check() {
    local description="$1" pattern="$2"
    if grep -q -- "$pattern" "$stdout_file"; then
        echo "ok   $description"
    else
        echo "FAIL $description (no match for '$pattern')" >&2
        failed=1
    fi
}

check "initialize answered with capabilities" '"id":1,"result":{"capabilities"'
check "trace diagnostics published for the workspace" '"method":"textDocument/publishDiagnostics"'
check "workspace symbols returned" '"id":2,"result":\[{'
check "shutdown answered" '"id":3,"result":null'

if [[ $exit_code -ne 0 ]]; then
    echo "FAIL server exited with code $exit_code" >&2
    failed=1
fi

if [[ $failed -ne 0 ]]; then
    echo "--- server stderr:" >&2
    cat "$stderr_file" >&2
    echo "--- server stdout (first 2000 bytes):" >&2
    head -c 2000 "$stdout_file" >&2
    echo >&2
    exit 1
fi

echo "Smoke test passed"
