from fastapi import FastAPI
from reg import router as reg_router
from messages import router as msg_router

from __init__ import SERVER_PUBLIC_KEY_PEM
from db import init_db

init_db()

app = FastAPI()

app.include_router(reg_router)
app.include_router(msg_router)

@app.post("/ping-pong")
def ping_pong():
    return {"status": "ok", "message": "pong-ping!", "token": "null", "public_key": SERVER_PUBLIC_KEY_PEM}
