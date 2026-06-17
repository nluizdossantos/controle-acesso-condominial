"""
Controlador de Acesso — v2
"""
import logging
from datetime import datetime

logger = logging.getLogger(__name__)

class AccessController:
    def __init__(self, db):
        self.db = db

    def verificar_acesso(self, placa: str) -> dict:
        veiculo = self.db.buscar_veiculo(placa)
        horario = datetime.now().strftime("%d/%m/%Y %H:%M:%S")
        if veiculo:
            resultado = {
                "permitido": True,
                "placa": placa,
                "proprietario": veiculo["proprietario"] or "Sem proprietário",
                "apartamento": veiculo["apartamento"] or "-",
                "tipo": veiculo["tipo"],
                "motivo": f"Veículo cadastrado ({veiculo['tipo']})",
                "horario": horario,
            }
            self._acionar_cancela(True)
        else:
            resultado = {
                "permitido": False,
                "placa": placa,
                "proprietario": "Desconhecido",
                "apartamento": "-",
                "tipo": "desconhecido",
                "motivo": "Placa não cadastrada",
                "horario": horario,
            }
            self._acionar_cancela(False)
        return resultado

    def _acionar_cancela(self, abrir: bool):
        """
        Integração com hardware:
        
        Raspberry Pi (GPIO relay):
            import RPi.GPIO as GPIO, time
            GPIO.setmode(GPIO.BCM)
            GPIO.setup(17, GPIO.OUT)
            if abrir:
                GPIO.output(17, GPIO.HIGH)
                time.sleep(3)
                GPIO.output(17, GPIO.LOW)
        """
        logger.info(f"CANCELA: {'ABRINDO' if abrir else 'FECHADA'}")
