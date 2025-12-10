from typing import Dict, List

from fastapi import APIRouter, HTTPException, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from datetime import datetime
from db import (
    get_user_by_username, get_session_by_access, add_message,
    fetch_new_messages_for_user, mark_message_delivered, delete_message
)
import uuid

router = APIRouter()

class SendReq(BaseModel):
    recipient_username: str
    ciphertext: str
    access_token: str

def check_access_token(access_token: str):
    session = get_session_by_access(access_token)
    if not session:
        raise HTTPException(status_code=401, detail="Invalid access token")
    _, user_id, _, _, _, _ = session
    return user_id

@router.post("/messages/send")
def send_message(req: SendReq):
    sender_id = check_access_token(req.access_token)

    recipient = get_user_by_username(req.recipient_username)
    if not recipient:
        raise HTTPException(404, "Recipient not found")
    recipient_id = recipient[0]

    msg_id = add_message(recipient_id, sender_id, req.ciphertext)

    notify_user(int(req.recipient_username), {"messages": "new_msg"})

    return {"status": "ok", "message_id": msg_id}

@router.get("/messages/fetch")
def fetch_messages(access_token: str):
    user_id = check_access_token(access_token)
    msgs = fetch_new_messages_for_user(user_id)
    return {"messages": msgs}

class AckReq(BaseModel):
    message_id: str
    action: str = "delivered"  # "delivered" or "delete"
    access_token: str

@router.post("/messages/ack")
def ack_message(req: AckReq):
    user_id = check_access_token(req.access_token)
    if req.action == "delivered":
        mark_message_delivered(req.message_id)
    elif req.action == "delete":
        delete_message(req.message_id)
    else:
        raise HTTPException(400, "Unknown action")
    return {"status": "ok", "message_id": req.message_id}

@router.get("/keys/{username}")
def get_keys(username: str):
    user = get_user_by_username(username)
    if not user:
        raise HTTPException(404, "User not found")
    public_key = user[2]
    return {"username": username, "public_key": public_key}


# websocket (for reminding user that he's got new message)
active_connections: Dict[int, List[WebSocket]] = {}  # user_id -> list of websockets

@router.websocket("/ws/messages")
async def websocket_endpoint(websocket: WebSocket, access_token: str):
    user_id = check_access_token(access_token)
    await websocket.accept()
    if user_id not in active_connections:
        active_connections[user_id] = []
    active_connections[user_id].append(websocket)

    try:
        while True:
            data = await websocket.receive_text()
    except WebSocketDisconnect:
        active_connections[user_id].remove(websocket)


async def notify_user(recipient_id: int, msg):
    if recipient_id in active_connections:
        for ws in active_connections[recipient_id]:
            await ws.send_json(msg)
