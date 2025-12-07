import secrets

def generate_access_token():
    return secrets.token_hex(32)

def generate_refresh_token():
    return secrets.token_hex(32)
