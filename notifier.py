"""
Notificador WhatsApp
Suporta dois provedores:
  - CallMeBot (gratuito, sem cadastro de cartão)
  - Twilio (pago, mais confiável para produção)

Configure as variáveis de ambiente antes de usar.
"""

import os
import requests
import logging
from urllib.parse import quote

logger = logging.getLogger(__name__)

# ---- Configuração -------------------------------------------------------
# Escolha o provedor: "callmebot" ou "twilio"
PROVEDOR = os.getenv("WHATSAPP_PROVIDER", "callmebot")

# CallMeBot — gratuito
# Siga: https://www.callmebot.com/blog/free-api-whatsapp-messages/
CALLMEBOT_PHONE   = os.getenv("CALLMEBOT_PHONE", "")     # ex: "5531999990001"
CALLMEBOT_API_KEY = os.getenv("CALLMEBOT_API_KEY", "")   # chave gerada pelo bot

# Twilio — produção
TWILIO_SID        = os.getenv("TWILIO_ACCOUNT_SID", "")
TWILIO_TOKEN      = os.getenv("TWILIO_AUTH_TOKEN", "")
TWILIO_FROM       = os.getenv("TWILIO_FROM", "whatsapp:+14155238886")
TWILIO_TO         = os.getenv("TWILIO_TO", "")           # ex: "whatsapp:+5531999990001"

# Controla quais eventos notificar
NOTIFICAR_LIBERADO = os.getenv("NOTIFICAR_LIBERADO", "true").lower() == "true"
NOTIFICAR_NEGADO   = os.getenv("NOTIFICAR_NEGADO", "true").lower() == "true"
# -------------------------------------------------------------------------


class WhatsAppNotifier:
    def __init__(self):
        self.provedor = PROVEDOR
        self._ativo = self._verificar_config()
        if self._ativo:
            logger.info(f"Notificador WhatsApp ativo (provedor: {PROVEDOR})")
        else:
            logger.warning("Notificador WhatsApp desabilitado — configure as variáveis de ambiente.")

    def _verificar_config(self) -> bool:
        if PROVEDOR == "callmebot":
            return bool(CALLMEBOT_PHONE and CALLMEBOT_API_KEY)
        elif PROVEDOR == "twilio":
            return bool(TWILIO_SID and TWILIO_TOKEN and TWILIO_TO)
        return False

    def notificar(self, evento: dict, imagem_path: str = ""):
        """Envia notificação para o número configurado."""
        if not self._ativo:
            return

        permitido = evento.get("permitido", False)
        if permitido and not NOTIFICAR_LIBERADO:
            return
        if not permitido and not NOTIFICAR_NEGADO:
            return

        emoji = "✅" if permitido else "🚫"
        status = "LIBERADO" if permitido else "NEGADO"
        placa = evento.get("placa", "???")
        proprietario = evento.get("proprietario", "Desconhecido")
        apto = evento.get("apartamento", "-")
        camera = evento.get("camera_nome", "-")
        horario = evento.get("horario", "")

        mensagem = (
            f"{emoji} *Acesso {status}*\n"
            f"🚗 Placa: *{placa}*\n"
            f"👤 Proprietário: {proprietario}\n"
            f"🏠 Apartamento: {apto}\n"
            f"📷 Câmera: {camera}\n"
            f"🕐 Horário: {horario}"
        )

        try:
            if self.provedor == "callmebot":
                self._enviar_callmebot(mensagem)
            elif self.provedor == "twilio":
                self._enviar_twilio(mensagem)
        except Exception as e:
            logger.error(f"Erro ao enviar WhatsApp: {e}")

    def _enviar_callmebot(self, mensagem: str):
        url = (
            f"https://api.callmebot.com/whatsapp.php"
            f"?phone={CALLMEBOT_PHONE}"
            f"&text={quote(mensagem)}"
            f"&apikey={CALLMEBOT_API_KEY}"
        )
        resp = requests.get(url, timeout=10)
        if resp.status_code == 200:
            logger.info("WhatsApp enviado via CallMeBot.")
        else:
            logger.warning(f"CallMeBot respondeu {resp.status_code}: {resp.text[:200]}")

    def _enviar_twilio(self, mensagem: str):
        from twilio.rest import Client
        client = Client(TWILIO_SID, TWILIO_TOKEN)
        msg = client.messages.create(
            body=mensagem,
            from_=TWILIO_FROM,
            to=TWILIO_TO,
        )
        logger.info(f"WhatsApp enviado via Twilio. SID: {msg.sid}")
