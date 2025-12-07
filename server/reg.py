from fastapi import APIRouter, FastAPI, HTTPException, Form
from pydantic import BaseModel, EmailStr
from passlib.hash import bcrypt_sha256
from datetime import datetime
from db import *
from handlers.email_handler import send_email, generate_code

import jwt
from __init__ import SERVER_PRIVATE_KEY, SERVER_PUBLIC_KEY_PEM
from handlers.token_utils import generate_access_token, generate_refresh_token

router = APIRouter()

# registration data model
class RegisterData(BaseModel):
    username: str
    password: str
    nickname: str
    email: EmailStr


# register
@router.post("/register")
def register(data: RegisterData):
    if exists_username(data.username):
        raise HTTPException(400, "This username has already been taken")
    if exists_email(data.email):
        raise HTTPException(400, "This email has already been taken")

    pass_hash = bcrypt_sha256.hash(data.password)
    user_id = add_user(data.username, pass_hash, data.nickname, data.email)

    access_token = generate_access_token()
    refresh_token = generate_refresh_token()

    create_session(user_id, access_token, refresh_token)

    return {
        "status": "ok",
        "message": "Registration successful! Check your email to confirm it!",
        "access_token": access_token,
        "refresh_token": refresh_token
    }


# confirming email
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

# login
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

    access_token = generate_access_token()
    refresh_token = generate_refresh_token()

    create_session(user_id, access_token, refresh_token)

    return {
        "status": "ok",
        "message": "You've logged in!",
        "access_token": access_token,
        "refresh_token": refresh_token,
        "username": username
    }

# loging in by using session token
@router.post("/login_by_token")
def login_by_token(access_token: str = Form(...), refresh_token: str = Form(...)):
    session = get_session_by_access(access_token)

    if session:
        user_id, username = session[1], session[2]
        return {
            "status": "ok",
            "message": "Logged in with access token",
            "access_token": access_token,
            "refresh_token": refresh_token,
            "username": username
        }

    session = get_session_by_refresh(refresh_token)

    if session:
        session_id, user_id, username = session[0], session[1], session[2]

        new_access = generate_access_token()
        new_refresh = generate_refresh_token()

        update_session_tokens(session_id, new_access, new_refresh)

        return {
            "status": "ok",
            "message": "Tokens refreshed",
            "access_token": new_access,
            "refresh_token": new_refresh,
            "username": username
        }

    raise HTTPException(401, "Invalid tokens. Log in again please")

# add public key to db
@router.post("/add_public_key")
def add_public_key(username: str = Form(...), public_key: str = Form(...)):
    user = get_user_by_username(username)
    if not user:
        raise HTTPException(400, "User not found")

    user_id = user[0]
    success = add_public_to_user(user_id, public_key)
    if not success:
        raise HTTPException(500, "Failed to save public key")

    return {"status": "ok", "message": "Public key saved successfully"}


# verify 2FA + JWT (in development)
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
