# 🏢 Controle de Acesso Condominial v2
### Interface Web · Múltiplas Câmeras · Notificações WhatsApp

---

## 📁 Estrutura

```
alpr_v2/
├── app.py                # Servidor FastAPI (rotas, WebSocket, MJPEG)
├── camera_manager.py     # Gerenciamento de múltiplas câmeras em threads
├── plate_detector.py     # OpenCV + EasyOCR
├── access_controller.py  # Lógica de acesso + gancho cancela
├── database.py           # SQLite (moradores, veículos, acessos)
├── notifier.py           # Notificações WhatsApp (CallMeBot / Twilio)
├── requirements.txt
├── .env.example          # Variáveis de ambiente
├── templates/
│   ├── dashboard.html    # Dashboard em tempo real
│   ├── veiculos.html     # Cadastro de veículos
│   ├── acessos.html      # Log de acessos com filtros
│   └── cameras.html      # Monitoramento de câmeras
└── static/
    └── captures/         # Fotos das capturas (gerado automaticamente)
```

---

## 📱 App Android — Escala de Trabalho

A pasta [`android-escala/`](android-escala/README.md) contém um aplicativo Android para
montar e consultar a escala da equipe (12x36, 24x48, 6x1, 5x2…), com calendário mensal,
quem está em serviço agora, férias/atestados e compartilhamento da escala pelo WhatsApp.

---

## ✅ Pré-requisitos

- Python 3.9+
- pip
- Câmera USB ou câmera IP acessível via RTSP

---

## ⚙️ Instalação

```bash
# 1. Instalar dependências
pip install -r requirements.txt

# 2. Configurar variáveis de ambiente
# Linux/macOS:
cp .env.example .env
# Windows:
copy .env.example .env

# Edite .env com suas credenciais WhatsApp
```

---

## 🚀 Iniciar o Servidor

```bash
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

Acesse pelo **celular ou computador** na mesma rede:

```
http://IP_DO_SERVIDOR:8000
```

> Para descobrir o IP: `ip addr` (Linux) ou `ipconfig` (Windows)

---

## 📷 Configurar Câmeras

Edite `CAMERAS_CONFIG` em `camera_manager.py`:

```python
CAMERAS_CONFIG = [
    {
        "id": "entrada",
        "nome": "Entrada Principal",
        "fonte": 0,                              # webcam USB
        "ativa": True,
    },
    {
        "id": "garagem",
        "nome": "Garagem",
        "fonte": "rtsp://admin:senha@192.168.1.100:554/stream",  # câmera IP
        "ativa": True,
    },
    {
        "id": "saida",
        "nome": "Saída",
        "fonte": "rtsp://admin:senha@192.168.1.101:554/stream",
        "ativa": False,  # desabilitada na inicialização
    },
]
```

Câmeras podem ser **ligadas/desligadas pela interface** sem reiniciar o servidor.

---

## 📲 Configurar WhatsApp (CallMeBot — gratuito)

1. Salve o número `+34 644 59 21 95` na agenda como **CallMeBot**
2. Envie a mensagem `I allow callmebot to send me messages` via WhatsApp para esse contato
3. Você receberá sua **API key** em instantes
4. Preencha no `.env`:

```env
WHATSAPP_PROVIDER=callmebot
CALLMEBOT_PHONE=5531999990001
CALLMEBOT_API_KEY=SUA_CHAVE
NOTIFICAR_NEGADO=true
NOTIFICAR_LIBERADO=false
```

### Para produção (Twilio)

```env
WHATSAPP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=ACxxx
TWILIO_AUTH_TOKEN=xxx
TWILIO_FROM=whatsapp:+14155238886
TWILIO_TO=whatsapp:+5531999990001
```

---

## 🖥️ Páginas da Interface

| Página | URL | Função |
|---|---|---|
| Dashboard | `/` | Estatísticas + câmeras ao vivo + eventos em tempo real |
| Câmeras | `/cameras` | Gerenciar fontes de vídeo, ligar/desligar |
| Veículos | `/veiculos` | Cadastrar e remover placas autorizadas |
| Log de Acessos | `/acessos` | Histórico com filtros e visualização de capturas |

### API REST

| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/api/stats` | Estatísticas do dia |
| GET | `/api/acessos` | Últimos acessos (JSON) |
| POST | `/api/veiculos` | Cadastrar veículo |
| DELETE | `/api/veiculos/{placa}` | Remover veículo |
| POST | `/api/moradores` | Cadastrar morador |
| GET | `/api/cameras` | Status das câmeras |
| POST | `/api/cameras/{id}/toggle` | Ligar/desligar câmera |
| GET | `/stream/{cam_id}` | Stream MJPEG da câmera |
| WS | `/ws` | Eventos em tempo real |

---

## 🔌 Integração com Cancela

Edite `_acionar_cancela` em `access_controller.py`:

```python
import RPi.GPIO as GPIO
import time

PINO = 17
GPIO.setmode(GPIO.BCM)
GPIO.setup(PINO, GPIO.OUT)

def _acionar_cancela(self, abrir: bool):
    if abrir:
        GPIO.output(PINO, GPIO.HIGH)
        time.sleep(3)
        GPIO.output(PINO, GPIO.LOW)
```

---

## 🔒 Segurança em produção

Para expor o sistema na internet com segurança:

```bash
# Use HTTPS com nginx como proxy reverso
# /etc/nginx/sites-available/portaria:
server {
    listen 443 ssl;
    location / { proxy_pass http://localhost:8000; }
    location /ws { proxy_pass http://localhost:8000; proxy_http_version 1.1;
                   proxy_set_header Upgrade $http_upgrade;
                   proxy_set_header Connection "upgrade"; }
}
```

Adicione autenticação básica ou token para proteger o acesso externo.
