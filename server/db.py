import sqlite3
from datetime import datetime, timedelta

DB_NAME = "users.db"

# initializing database
def init_db():
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE,
        pass_hash TEXT,
        public_key TEXT,
        nickname TEXT,
        email TEXT UNIQUE,
        email_confirmed INTEGER DEFAULT 0
    )
    """)
    cur.execute("""
    CREATE TABLE IF NOT EXISTS email_codes (
        user_id INTEGER,
        code TEXT,
        expires_at DATETIME
    )
    """)
    cur.execute("""
    CREATE TABLE IF NOT EXISTS sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        access_token TEXT,
        refresh_token TEXT,
        created_at DATETIME,
        expires_at DATETIME,
        FOREIGN KEY(user_id) REFERENCES users(id)
    )
    """)
    conn.commit()
    conn.close()

# adding user
def add_user(username, pass_hash, nickname, email):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO users (username, pass_hash, nickname, email) VALUES (?, ?, ?, ?)",
        (username, pass_hash, nickname, email)
    )
    user_id = cur.lastrowid
    conn.commit()
    conn.close()
    return user_id

def add_public_to_user(user_id: int, public_key: str) -> bool:
    try:
        conn = sqlite3.connect(DB_NAME)
        cur = conn.cursor()
        cur.execute("UPDATE users SET public_key = ? WHERE id = ?", (public_key, user_id))
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        print("Error adding public key:", e)
        return False


# get user by using username
def get_user_by_username(username):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("SELECT id, pass_hash, email_confirmed, email FROM users WHERE username = ?", (username,))
    row = cur.fetchone()
    conn.close()
    return row  # (id, pass_hash, email_confirmed, email)

# has the username/email already been taken?
def exists_username(username):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("SELECT id FROM users WHERE username = ?", (username,))
    result = cur.fetchone() is not None
    conn.close()
    return result

def exists_email(email):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("SELECT id FROM users WHERE email = ?", (email,))
    result = cur.fetchone() is not None
    conn.close()
    return result

# confirming email
def confirm_email(user_id):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("UPDATE users SET email_confirmed=1 WHERE id=?", (user_id,))
    conn.commit()
    conn.close()

# code handlers (2FA / confirming email)
def add_code(user_id, code, minutes_valid=5):
    expires_at = datetime.now() + timedelta(minutes=minutes_valid)
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO email_codes (user_id, code, expires_at) VALUES (?, ?, ?)",
        (user_id, code, expires_at)
    )
    conn.commit()
    conn.close()

def get_latest_code(user_id):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("SELECT code, expires_at FROM email_codes WHERE user_id = ? ORDER BY expires_at DESC LIMIT 1", (user_id,))
    row = cur.fetchone()
    conn.close()
    return row  # (code, expires_at)

# session managing
def create_session(user_id, access_token, refresh_token, minutes_valid=30):
    created_at = datetime.now()
    expires_at = created_at + timedelta(minutes=minutes_valid)

    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("""
    INSERT INTO sessions (user_id, access_token, refresh_token, created_at, expires_at)
    VALUES (?, ?, ?, ?, ?)
    """, (user_id, access_token, refresh_token, created_at, expires_at))

    session_id = cur.lastrowid
    conn.commit()
    conn.close()

    return session_id

def get_session_by_access(access_token):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("""
    SELECT id, user_id, access_token, refresh_token, created_at, expires_at
    FROM sessions
    WHERE access_token = ?
    """, (access_token,))
    row = cur.fetchone()
    conn.close()
    return row

def get_session_by_refresh(refresh_token):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("""
    SELECT id, user_id, access_token, refresh_token, created_at, expires_at
    FROM sessions
    WHERE refresh_token = ?
    """, (refresh_token,))
    row = cur.fetchone()
    conn.close()
    return row

def update_session_tokens(session_id, new_access, new_refresh):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("""
        UPDATE sessions
        SET access_token=?, refresh_token=?, created_at=?
        WHERE id=?
    """, (new_access, new_refresh, datetime.now(), session_id))
    conn.commit()
    conn.close()

def delete_sessions_for_user(user_id):
    conn = sqlite3.connect(DB_NAME)
    cur = conn.cursor()
    cur.execute("DELETE FROM sessions WHERE user_id = ?", (user_id,))
    conn.commit()
    conn.close()
