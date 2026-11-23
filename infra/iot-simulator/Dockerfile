# Simulador dos monitores dos leitos (IoT): publica sinais vitais via MQTT.
FROM python:3.12-alpine
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY simulador.py .
# -u: mostra os logs na hora (docker compose logs iot-simulator)
CMD ["python", "-u", "simulador.py"]
