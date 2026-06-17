"""
Gerenciador de Múltiplas Câmeras
Cada câmera roda em thread própria e publica eventos via callback.
"""

import cv2
import threading
import time
import logging
import json
import os
from datetime import datetime
from plate_detector import PlateDetector
from access_controller import AccessController

logger = logging.getLogger(__name__)

# Configuração das câmeras — edite conforme seu ambiente
CAMERAS_CONFIG = [
    {
        "id": "entrada_principal",
        "nome": "Entrada Principal",
        "fonte": 0,          # 0 = webcam, ou "rtsp://ip/stream"
        "ativa": True,
    },
    {
        "id": "entrada_garagem",
        "nome": "Garagem",
        "fonte": "rtsp://192.168.1.101:554/stream",
        "ativa": False,      # False = desabilitada por padrão
    },
    {
        "id": "saida",
        "nome": "Saída",
        "fonte": "rtsp://192.168.1.102:554/stream",
        "ativa": False,
    },
]


class Camera:
    INTERVALO_REPROCESSAMENTO = 6  # segundos

    def __init__(self, config: dict, db, notifier):
        self.id = config["id"]
        self.nome = config["nome"]
        self.fonte = config["fonte"]
        self.ativa = config["ativa"]
        self.db = db
        self.notifier = notifier

        self.detector = PlateDetector()
        self.controller = AccessController(db)

        self._frame = None
        self._lock = threading.Lock()
        self._thread: threading.Thread = None
        self._rodando = False
        self._ultima_placa = None
        self._ultimo_tempo = 0
        self._ultimo_evento: dict = None
        self._on_evento = None  # callback async

    def set_callback(self, fn):
        self._on_evento = fn

    def iniciar(self):
        if not self.ativa:
            return
        self._rodando = True
        self._thread = threading.Thread(target=self._loop, daemon=True, name=f"cam-{self.id}")
        self._thread.start()
        logger.info(f"Câmera iniciada: {self.nome} ({self.fonte})")

    def parar(self):
        self._rodando = False

    def get_frame_jpeg(self) -> bytes | None:
        with self._lock:
            if self._frame is None:
                return None
            _, buf = cv2.imencode(".jpg", self._frame, [cv2.IMWRITE_JPEG_QUALITY, 70])
            return buf.tobytes()

    def status(self) -> dict:
        return {
            "id": self.id,
            "nome": self.nome,
            "fonte": str(self.fonte),
            "ativa": self.ativa,
            "rodando": self._rodando and self._thread is not None and self._thread.is_alive(),
            "ultimo_evento": self._ultimo_evento,
        }

    def _loop(self):
        cap = None
        while self._rodando:
            if cap is None or not cap.isOpened():
                cap = cv2.VideoCapture(self.fonte)
                if not cap.isOpened():
                    logger.warning(f"[{self.nome}] Não foi possível abrir a câmera. Tentando em 5s...")
                    time.sleep(5)
                    continue

            ret, frame = cap.read()
            if not ret:
                logger.warning(f"[{self.nome}] Falha ao capturar frame. Reconectando...")
                cap.release()
                cap = None
                time.sleep(2)
                continue

            # Atualiza frame para stream MJPEG
            with self._lock:
                self._frame = frame.copy()

            # Detecta placas
            placas = self.detector.detectar(frame)
            for placa, roi in placas:
                agora = time.time()
                if placa == self._ultima_placa and (agora - self._ultimo_tempo) < self.INTERVALO_REPROCESSAMENTO:
                    continue

                self._ultima_placa = placa
                self._ultimo_tempo = agora

                resultado = self.controller.verificar_acesso(placa)
                resultado["camera_id"] = self.id
                resultado["camera_nome"] = self.nome
                self._ultimo_evento = resultado

                logger.info(f"[{self.nome}] Placa: {placa} → {'✅ LIBERADO' if resultado['permitido'] else '❌ NEGADO'}")

                # Salva captura
                ts = datetime.now().strftime("%Y%m%d_%H%M%S")
                img_path = f"static/captures/{placa}_{ts}.jpg"
                x1, y1 = roi[0]
                x2, y2 = roi[1]
                cv2.imwrite(img_path, frame)
                self.db.registrar_acesso(
                    placa=placa,
                    resultado="LIBERADO" if resultado["permitido"] else "NEGADO",
                    motivo=resultado["motivo"],
                    imagem_path=img_path,
                    camera_id=self.id,
                )

                # WhatsApp
                if self.notifier:
                    self.notifier.notificar(resultado, img_path)

                # Callback async (WebSocket broadcast)
                if self._on_evento:
                    import asyncio
                    try:
                        loop = asyncio.get_event_loop()
                        if loop.is_running():
                            asyncio.run_coroutine_threadsafe(self._on_evento(resultado), loop)
                    except Exception as e:
                        logger.error(f"Erro no broadcast WebSocket: {e}")

                # Desenha overlay no frame
                cor = (0, 255, 0) if resultado["permitido"] else (0, 0, 255)
                cv2.rectangle(frame, roi[0], roi[1], cor, 2)
                cv2.putText(frame, placa, (roi[0][0], roi[0][1] - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.9, cor, 2)
                with self._lock:
                    self._frame = frame

        if cap:
            cap.release()
        logger.info(f"Câmera encerrada: {self.nome}")


class CameraManager:
    def __init__(self, db, notifier):
        self.db = db
        self.notifier = notifier
        self.cameras: dict[str, Camera] = {}
        self._broadcast_callback = None

        for cfg in CAMERAS_CONFIG:
            cam = Camera(cfg, db, notifier)
            self.cameras[cfg["id"]] = cam

    def set_broadcast(self, fn):
        self._broadcast_callback = fn
        for cam in self.cameras.values():
            cam.set_callback(fn)

    def iniciar_todas(self):
        for cam in self.cameras.values():
            cam.iniciar()

    def parar_todas(self):
        for cam in self.cameras.values():
            cam.parar()

    def get_frame_jpeg(self, cam_id: str) -> bytes | None:
        cam = self.cameras.get(cam_id)
        return cam.get_frame_jpeg() if cam else None

    def status_cameras(self) -> list[dict]:
        return [cam.status() for cam in self.cameras.values()]

    def toggle(self, cam_id: str) -> bool:
        cam = self.cameras.get(cam_id)
        if not cam:
            return False
        cam.ativa = not cam.ativa
        if cam.ativa:
            cam.iniciar()
        else:
            cam.parar()
        return cam.ativa
