import smtplib
from email.mime.text import MIMEText
import random

from my_secrets import EMAIL_ADDRESS, EMAIL_PASSWORD

def send_email(to_addr, subject, content):
    msg = MIMEText(content)
    msg['Subject'] = subject
    msg['From'] = EMAIL_ADDRESS
    msg['To'] = to_addr

    server = smtplib.SMTP_SSL("smtp.gmail.com", 465)
    server.login(EMAIL_ADDRESS, EMAIL_PASSWORD)  # Gmail App Password
    server.send_message(msg)
    server.quit()

# ------------------------------
# Генерация случайного кода
# ------------------------------
def generate_code():
    return str(random.randint(100000, 999999))
