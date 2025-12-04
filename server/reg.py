from fastapi import FastAPI, HTTPException, Form
from pydantic import BaseModel, EmailStr
from passlib.hash import bcrypt_sha256
from datetime import datetime
from db import init_db, add_user, get_user_by_username, exists_username, exists_email, confirm_email, add_code, get_latest_code

from handlers.email_handler import send_email, generate_code

app = FastAPI()
init_db()

# ------------------------------
# Registration data nodel
# ------------------------------
class RegisterData(BaseModel):
    username: str
    password: str
    nickname: str
    email: EmailStr

# ------------------------------
# Register
# ------------------------------
@app.post("/register")
def register(data: RegisterData):
    if exists_username(data.username):
        raise HTTPException(400, "This username has already been taken")
    # if exists_email(data.email):
    #    raise HTTPException(400, "This email has already been taken")

    pass_hash = bcrypt_sha256.hash(data.password)
    user_id = add_user(data.username, pass_hash, data.nickname, data.email)

    # email operations are in development now
    # code = generate_code()
    # add_code(user_id, code, minutes_valid=10)
    # send_email(data.email, "Pong! Email confirming", f"Ping!\nHere's your email confirming code: {code}\nPong!")

    return {"status": "ok", "message": "Registration successful! Check your email to confirm it!"}

# ------------------------------
# Confirming email
# ------------------------------
@app.post("/confirm_email")
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
# Auth + 2FA
# ------------------------------
@app.post("/login")
def login(username: str = Form(...), password: str = Form(...)):
    user = get_user_by_username(username)
    if not user:
        raise HTTPException(400, "User not found")
    user_id, pass_hash, email_confirmed, email = user

    if not bcrypt_sha256.verify(password, pass_hash):
        raise HTTPException(400, "Invalid password")
    # if not email_confirmed:
    #    raise HTTPException(400, "Email is not confirmed")

    # email operations are in development now
    # code = generate_code()
    # add_code(user_id, code, minutes_valid=5)
    # send_email(email, "2FA", f"Your code: {code}")

    return {"status": "ok", "message": "You have been logged in"}

@app.post("/verify_2fa")
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

    return {"status": "ok", "message": "You have been logged in"}

