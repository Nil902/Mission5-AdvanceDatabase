from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse, FileResponse
from contextlib import asynccontextmanager
import psycopg2
import psycopg2.extras
import redis
import json
import os
import subprocess
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ─── Config from environment ──────────────────────────────
PG_HOST     = os.getenv("PG_HOST", "postgres-db")
PG_PORT     = int(os.getenv("PG_PORT", "5432"))
PG_DB       = os.getenv("PG_DB", "eventdb")
PG_USER     = os.getenv("PG_USER", "eventuser")
PG_PASS     = os.getenv("PG_PASS", "eventpass")

REDIS_HOST  = os.getenv("REDIS_HOST", "redis-cache")
REDIS_PORT  = int(os.getenv("REDIS_PORT", "6379"))
REDIS_TTL   = int(os.getenv("REDIS_TTL", "3600"))  # 1 hour

EVENTS_KEY     = "events:all"
CATEGORIES_KEY = "categories:all"

# ─── App ──────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Cache Manager starting up")
    yield
    logger.info("Cache Manager shutting down")

app = FastAPI(title="Cache Manager", version="1.0", lifespan=lifespan)


def get_pg_connection():
    return psycopg2.connect(
        host=PG_HOST, port=PG_PORT, dbname=PG_DB,
        user=PG_USER, password=PG_PASS,
        cursor_factory=psycopg2.extras.RealDictCursor
    )


def get_redis_client():
    return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)


def datetime_serializer(obj):
    if isinstance(obj, datetime):
        return obj.strftime("%Y-%m-%d %H:%M")
    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")


# ─── Routes ───────────────────────────────────────────────

@app.get("/health")
def health():
    """Health check — Server 01 polls this to show status."""
    try:
        conn = get_pg_connection()
        conn.close()
        r = get_redis_client()
        r.ping()
        return {"status": "ok", "postgres": "up", "redis": "up"}
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))


@app.post("/refresh")
def refresh_cache():
    """
    Main endpoint — called by Spring Boot after any admin data change.
    1. Fetch fresh events + categories from PostgreSQL
    2. Push JSON into Redis with TTL
    """
    logger.info("Cache refresh triggered")

    try:
        # ── Fetch from PostgreSQL ──────────────────────────
        conn = get_pg_connection()
        cur = conn.cursor()

        # Events with category name (uses idx_events_active_date)
        cur.execute("""
            SELECT
                e.id,
                e.title,
                e.description,
                e.location,
                e.event_date   AS "eventDate",
                e.category_id  AS "categoryId",
                c.name         AS "categoryName",
                e.image_url    AS "imageUrl",
                e.is_active    AS "isActive",
                e.created_at   AS "createdAt"
            FROM events e
            JOIN categories c ON c.id = e.category_id
            WHERE e.is_active = TRUE
            ORDER BY e.event_date ASC
        """)
        events = [dict(row) for row in cur.fetchall()]

        # Categories
        cur.execute("SELECT id, name, description FROM categories ORDER BY name")
        categories = [dict(row) for row in cur.fetchall()]

        conn.close()

        # ── Push to Redis ──────────────────────────────────
        r = get_redis_client()

        events_json     = json.dumps(events, default=datetime_serializer)
        categories_json = json.dumps(categories, default=datetime_serializer)

        r.setex(EVENTS_KEY,     REDIS_TTL, events_json)
        r.setex(CATEGORIES_KEY, REDIS_TTL, categories_json)

        logger.info("Cached %d events, %d categories (TTL=%ds)",
                    len(events), len(categories), REDIS_TTL)

        return {
            "status": "ok",
            "events_cached": len(events),
            "categories_cached": len(categories),
            "ttl_seconds": REDIS_TTL
        }

    except Exception as e:
        logger.error("Cache refresh failed: %s", str(e))
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/invalidate")
def invalidate_cache():
    """Clear all cached data from Redis."""
    try:
        r = get_redis_client()
        deleted = r.delete(EVENTS_KEY, CATEGORIES_KEY)
        logger.info("Redis cache cleared (%d keys deleted)", deleted)
        return {"status": "ok", "deleted_keys": deleted}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/status")
def cache_status():
    """Show what's currently in Redis — useful for debugging."""
    try:
        r = get_redis_client()
        events_ttl = r.ttl(EVENTS_KEY)
        cats_ttl   = r.ttl(CATEGORIES_KEY)

        events_raw = r.get(EVENTS_KEY)
        cats_raw   = r.get(CATEGORIES_KEY)

        return {
            "events_key":         EVENTS_KEY,
            "events_ttl":         events_ttl,
            "events_count":       len(json.loads(events_raw)) if events_raw else 0,
            "categories_key":     CATEGORIES_KEY,
            "categories_ttl":     cats_ttl,
            "categories_count":   len(json.loads(cats_raw)) if cats_raw else 0,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ─── pgBadger Report ─────────────────────────────────────

REPORT_PATH = "/reports/report.html"
PG_LOG_DIR = "/var/log/postgresql"


@app.post("/report/generate")
def generate_pgbadger_report():
    """Run pgBadger against PostgreSQL logs and generate an HTML report."""
    import glob
    log_files = glob.glob(f"{PG_LOG_DIR}/postgresql-*.log")
    if not log_files:
        raise HTTPException(status_code=404, detail="No PostgreSQL log files found")

    cmd = [
        "pgbadger",
        "--outfile", REPORT_PATH,
        "--format", "stderr",
        "--jobs", "2",
    ] + log_files

    logger.info("Running pgBadger: %s", " ".join(cmd))
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        logger.error("pgBadger failed: %s", result.stderr)
        raise HTTPException(status_code=500, detail=f"pgBadger error: {result.stderr}")

    logger.info("pgBadger report generated at %s", REPORT_PATH)
    return {"status": "ok", "report_url": "/report/view"}


@app.get("/report/view")
def view_pgbadger_report():
    """Serve the generated pgBadger HTML report."""
    if not os.path.exists(REPORT_PATH):
        raise HTTPException(status_code=404, detail="No report generated yet. Call POST /report/generate first.")
    return FileResponse(REPORT_PATH, media_type="text/html")