#!/usr/bin/env python3
"""Run a command with a deadline and terminate its child process group on timeout."""

from __future__ import annotations

import os
import signal
import subprocess
import sys


def main() -> int:
    if len(sys.argv) < 4 or sys.argv[2] != "--":
        print("Usage: run-with-timeout.py SECONDS -- COMMAND [ARG ...]", file=sys.stderr)
        return 2

    try:
        timeout_seconds = int(sys.argv[1])
        if timeout_seconds < 1:
            raise ValueError
    except ValueError:
        print("Timeout must be a positive number of seconds.", file=sys.stderr)
        return 2

    process = subprocess.Popen(sys.argv[3:], start_new_session=True)
    try:
        return process.wait(timeout=timeout_seconds)
    except subprocess.TimeoutExpired:
        print(
            f"Command exceeded its {timeout_seconds}-second timeout; stopping its process group.",
            file=sys.stderr,
            flush=True,
        )
        os.killpg(process.pid, signal.SIGTERM)
        try:
            process.wait(timeout=30)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        return 124


if __name__ == "__main__":
    raise SystemExit(main())
