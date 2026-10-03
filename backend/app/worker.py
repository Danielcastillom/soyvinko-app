"""Durable in-app delivery; no automatic bank transfers or service releases."""

import logging
import signal
import time
from app.core.db import call, start_pools, stop_pools
from app.core.settings import settings

logging.basicConfig(level=logging.INFO)
running = True


def stop(*_):
    global running
    running = False


signal.signal(signal.SIGTERM, stop)
signal.signal(signal.SIGINT, stop)


def purge_deleted_files():
    """Borra del disco los archivos marcados como eliminados (p. ej. KYC vencido por retención)."""
    folder = (settings.data_dir / "files").resolve()
    for row in call("worker_files_to_purge", {"limit": 100}, role="worker"):
        path = (folder / row["object_key"]).resolve()
        if path.parent != folder:
            logging.warning("Skipping unsafe object key id=%s", row["id"])
            continue
        path.unlink(missing_ok=True)
        call("worker_mark_file_purged", {"file": row["id"]}, role="worker")


def tick():
    call("expire_requests", role="worker")
    call("worker_housekeeping", role="worker")
    try:
        purge_deleted_files()
    except Exception:
        logging.exception("File purge failed")
    rows = call("claim_notifications", {"limit": 50}, role="worker")
    for row in rows:
        try:
            call("deliver_in_app", {"outbox": row["id"], "attempt": row["attempts"]}, role="worker")
        except Exception:
            logging.exception("Notification failed id=%s", row["id"])
            try:
                finish_failed(row)
            except Exception:
                # No abandonamos el resto del lote si el registro del fallo también falla.
                logging.exception("Could not record failure id=%s", row["id"])
    return len(rows)


def finish_failed(row):
    call(
        "finish_notification",
        {
            "id": row["id"],
            "attempt": row["attempts"],
            "success": False,
            "channel": "in_app",
            "provider_id": None,
            "error": "in_app_delivery_failed",
        },
        role="worker",
    )


def main():
    start_pools()
    failures = 0
    try:
        while running:
            try:
                tick()
                failures = 0
            except Exception:
                failures += 1
                logging.exception("Worker cycle failed (consecutive=%s)", failures)
            # Espera base de 10 s; con fallos seguidos (p. ej. base de datos caída) hasta 5 min.
            for _ in range(min(10 * 2 ** min(failures, 5), 300)):
                if not running:
                    break
                time.sleep(1)
    finally:
        stop_pools()


if __name__ == "__main__":
    main()
