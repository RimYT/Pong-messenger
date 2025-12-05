from fastapi import APIRouter, FastAPI, HTTPException, Form
from pydantic import BaseModel, EmailStr
from passlib.hash import bcrypt_sha256
from datetime import datetime
from db import init_db, add_user, get_user_by_username, exists_username, exists_email, confirm_email, add_code, get_latest_code
from handlers.email_handler import send_email, generate_code

import jwt
from __init__ import SERVER_PRIVATE_KEY, SERVER_PUBLIC_KEY_PEM

router = APIRouter()
init_db()

# ------------------------------
# Registration data model
# ------------------------------
class RegisterData(BaseModel):
    username: str
    password: str
    nickname: str
    email: EmailStr


# ------------------------------
# Register endpoint with JWT + public key
# ------------------------------
@router.post("/register")
def register(data: RegisterData):
    if exists_username(data.username):
        raise HTTPException(400, "This username has already been taken")
    if exists_email(data.email):
        raise HTTPException(400, "This email has already been taken")

    pass_hash = bcrypt_sha256.hash(data.password)
    user_id = add_user(data.username, pass_hash, data.nickname, data.email)

    # Create signed JWT token
    token_payload = {
        "user_id": user_id,
        "username": data.username,
        "iat": datetime.utcnow().timestamp()
    }
    token = jwt.encode(token_payload, SERVER_PRIVATE_KEY, algorithm="RS256")

    return {
        "status": "ok",
        "message": "Registration successful! Check your email to confirm it!",
        "token": token,
        "public_key": SERVER_PUBLIC_KEY_PEM
    }


# ------------------------------
# Confirming email
# ------------------------------
@router.post("/confirm_email")
def confirm_email_endpoint(username: str = Form(...), code: str = Form(...)):
    user = get_user_by_username(username)
    if not user:
        raise HTTPException(400, "User not found")
    user_id = user[0]

    row = get_latest_code(user_id)
    if not row:
        raise HTTPException(400, "Confirming code not found")
    db_code, expires_at = row
    expires_at = datetime.strptime(expires_at, "%Y-%m-%d %H:%M:%S.%f")
    if datetime.now() > expires_at:
        raise HTTPException(400, "Confirming code has expired")
    if code != db_code:
        raise HTTPException(400, "Invalid code")

    confirm_email(user_id)
    return {"status": "ok", "message": "Email has been confirmed!"}


# ------------------------------
# Login + JWT
# ------------------------------
@router.post("/login")
def login(username: str = Form(...), password: str = Form(...)):
    user = get_user_by_username(username)
    if not user:
        raise HTTPException(400, "User not found")
    user_id, pass_hash, email_confirmed, email = user

    if not bcrypt_sha256.verify(password, pass_hash):
        raise HTTPException(400, "Invalid password")
    # if not email_confirmed:
    #    raise HTTPException(400, "Email is not confirmed")

    token_payload = {
        "user_id": user_id,
        "username": username,
        "iat": datetime.utcnow().timestamp()
    }
    token = jwt.encode(token_payload, SERVER_PRIVATE_KEY, algorithm="RS256")

    return {
        "status": "ok",
        "message": "You have been logged in",
        "token": token,
        "public_key": SERVER_PUBLIC_KEY_PEM
    }


# ------------------------------
# Verify 2FA + JWT
# ------------------------------
@router.post("/verify_2fa")
def verify_2fa(username: str = Form(...), code: str = Form(...)):
    user = get_user_by_username(username)
    if not user:
        raise HTTPException(400, "User not found")
    user_id = user[0]

    row = get_latest_code(user_id)
    if not row:
        raise HTTPException(400, "2FA code not found")
    db_code, expires_at = row
    expires_at = datetime.strptime(expires_at, "%Y-%m-%d %H:%M:%S.%f")
    if datetime.now() > expires_at:
        raise HTTPException(400, "2FA code has been expired")
    if code != db_code:
        raise HTTPException(400, "Invalid 2FA code")

    token_payload = {
        "user_id": user_id,
        "username": username,
        "iat": datetime.utcnow().timestamp()
    }
    token = jwt.encode(token_payload, SERVER_PRIVATE_KEY, algorithm="RS256")

    return {
        "status": "ok",
        "message": "You have been logged in",
        "token": token,
        "public_key": SERVER_PUBLIC_KEY_PEM
    }
