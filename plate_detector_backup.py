"""
Detector de Placas — igual à v1, compatível com CameraManager
"""
import cv2
import numpy as np
import easyocr
import re
import logging
import time

logger = logging.getLogger(__name__)

class PlateDetector:
    PADRAO_PLACA = re.compile(r'^[A-Z]{3}\d[A-Z0-9]\d{2}$')

    def __init__(self):
        logger.info("Carregando EasyOCR...")
        self.reader = easyocr.Reader(['pt', 'en'], gpu=False)
        self.ultima_leitura = 0

    def detectar(self, frame):
        # Executa OCR apenas uma vez por segundo
        if time.time() - self.ultima_leitura < 1:
            return []

        self.ultima_leitura = time.time()
        resultados = []

        for roi, (x, y, w, h) in self._localizar_regioes(frame):
            texto = self._ler_placa(roi)
            if texto:
                resultados.append((texto, ((x, y), (x+w, y+h))))

        return resultados

    def _localizar_regioes(self, frame):
        regioes = []
        cinza = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        bordas = cv2.Canny(cv2.bilateralFilter(cinza, 11, 17, 17), 30, 200)
        contornos, _ = cv2.findContours(bordas, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        for c in sorted(contornos, key=cv2.contourArea, reverse=True)[:20]:
            approx = cv2.approxPolyDP(c, 0.018 * cv2.arcLength(c, True), True)
            if len(approx) == 4:
                x, y, w, h = cv2.boundingRect(approx)
                if 2.5 <= w/float(h) <= 6.5 and w > 80:
                    regioes.append((frame[y:y+h, x:x+w], (x, y, w, h)))
        if not regioes:
            h, w = frame.shape[:2]
            regioes.append((frame, (0, 0, w, h)))
        return regioes

    def _ler_placa(self, roi):
        try:
            cinza = cv2.cvtColor(roi, cv2.COLOR_BGR2GRAY)
            redim = cv2.resize(cinza, None, fx=2, fy=2, interpolation=cv2.INTER_CUBIC)
            _, bin_ = cv2.threshold(redim, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            for (_, texto, conf) in self.reader.readtext(bin_):
                if conf < 0.3:
                    continue
                p = re.sub(r'[^A-Z0-9]', '', texto.upper())
                if p and self.PADRAO_PLACA.match(p):
                    return p
        except Exception as e:
            logger.error(f"OCR error: {e}")
        return None
