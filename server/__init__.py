import os
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

# Пути к файлам ключей
PRIVATE_KEY_FILE = "server_private_key.pem"
PUBLIC_KEY_FILE = "server_public_key.pem"

# Загрузка или генерация ключей
if os.path.exists(PRIVATE_KEY_FILE) and os.path.exists(PUBLIC_KEY_FILE):
    # Загружаем существующие ключи
    with open(PRIVATE_KEY_FILE, "rb") as f:
        SERVER_PRIVATE_KEY = serialization.load_pem_private_key(
            f.read(),
            password=None,
            backend=default_backend()
        )

    with open(PUBLIC_KEY_FILE, "rb") as f:
        SERVER_PUBLIC_KEY_PEM = f.read().decode("utf-8")
else:
    # Генерируем новую пару ключей
    SERVER_PRIVATE_KEY = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
        backend=default_backend()
    )

    SERVER_PUBLIC_KEY_PEM = SERVER_PRIVATE_KEY.public_key().public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    ).decode("utf-8")

    # Сохраняем ключи в файлы
    with open(PRIVATE_KEY_FILE, "wb") as f:
        f.write(SERVER_PRIVATE_KEY.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        ))

    with open(PUBLIC_KEY_FILE, "wb") as f:
        f.write(SERVER_PUBLIC_KEY_PEM.encode("utf-8"))
