"""
Servidor Web FastAPI — Interface de Gestão do Condomínio
Rotas: dashboard, veículos, moradores, logs de acesso, stream de câmeras
WebSocket: feed ao vivo + notificações em tempo real
"""

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, Request, Form
from fastapi.responses import HTMLResponse, StreamingResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from contextlib import asynccontextmanager
import asyncio
import json
import logging
import os
from datetime import datetime

from database import DatabaseManager
from camera_manager import CameraManager
from notifier import WhatsAppNotifier

logger = logging.getLogger(__name__)

# ---------- Inicialização ----------

db = DatabaseManager("condominio.db")
notifier = WhatsAppNotifier()
cam_manager: CameraManager = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    global cam_manager
    os.makedirs("logs", exist_ok=True)
    os.makedirs("static/captures", exist_ok=True)
    db.inicializar()
    cam_manager = CameraManager(db, notifier)
    cam_manager.iniciar_todas()
    yield
    cam_manager.parar_todas()
    db.fechar()

app = FastAPI(title="Controle de Acesso", lifespan=lifespan)
app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

# ---------- WebSocket Manager ----------

class ConnectionManager:
    def __init__(self):
        self.active: list[WebSocket] = []

    async def connect(self, ws: WebSocket):
        await ws.accept()
        self.active.append(ws)

    def disconnect(self, ws: WebSocket):
        self.active.remove(ws)

    async def broadcast(self, data: dict):
        msg = json.dumps(data, ensure_ascii=False)
        for ws in list(self.active):
            try:
                await ws.send_text(msg)
            except Exception:
                self.active.remove(ws)

ws_manager = ConnectionManager()


# ---------- Rotas HTML ----------

@app.get("/", response_class=HTMLResponse)
async def dashboard(request: Request):
    stats = db.get_stats()
    cameras = cam_manager.status_cameras()
    return templates.TemplateResponse(
    request=request,
    name="dashboard.html",
    context={
        "stats": stats,
        "cameras": cameras,
    }
    )

@app.get("/veiculos", response_class=HTMLResponse)
async def page_veiculos(request: Request):
    veiculos = db.listar_veiculos()
    moradores = db.listar_moradores()
    return templates.TemplateResponse("veiculos.html", {
        "request": request,
        "veiculos": [dict(v) for v in veiculos],
        "moradores": [dict(m) for m in moradores],
    })

@app.get("/veiculos", response_class=HTMLResponse)
async def page_veiculos(request: Request):
    veiculos = db.listar_veiculos()
    moradores = db.listar_moradores()
    return templates.TemplateResponse("veiculos.html", {
        "request": request,
        "veiculos": [dict(v) for v in veiculos],
        "moradores": [dict(m) for m in moradores],
    })

@app.get("/acessos", response_class=HTMLResponse)
async def page_acessos(request: Request):
    acessos = db.listar_acessos(100)
    return templates.TemplateResponse("acessos.html", {
        "request": request,
        "acessos": [dict(a) for a in acessos],
    })

@app.get("/cameras", response_class=HTMLResponse)
async def page_cameras(request: Request):
    cameras = cam_manager.status_cameras()
   return templates.TemplateResponse(
    request=request,
    name="cameras.html",
    context={
        "cameras": cameras,
    }
)


# ---------- API REST ----------

@app.get("/api/stats")
async def api_stats():
    return db.get_stats()

@app.get("/api/acessos")
async def api_acessos(limite: int = 50):
    return [dict(a) for a in db.listar_acessos(limite)]

@app.post("/api/veiculos")
async def criar_veiculo(
    placa: str = Form(...),
    modelo: str = Form(""),
    cor: str = Form(""),
    morador_id: int = Form(None),
    tipo: str = Form("morador"),
):
    ok = db.adicionar_veiculo(placa, modelo, cor, morador_id, tipo)
    if not ok:
        raise HTTPException(400, "Placa já cadastrada")
    return {"ok": True, "placa": placa.upper()}

@app.delete("/api/veiculos/{placa}")
async def remover_veiculo(placa: str):
    db.desativar_veiculo(placa)
    return {"ok": True}

@app.post("/api/moradores")
async def criar_morador(
    nome: str = Form(...),
    apartamento: str = Form(...),
    telefone: str = Form(""),
    whatsapp: str = Form(""),
):
    mid = db.adicionar_morador(nome, apartamento, telefone, whatsapp)
    return {"ok": True, "id": mid}

@app.get("/api/cameras")
async def api_cameras():
    return cam_manager.status_cameras()

@app.post("/api/cameras/{cam_id}/toggle")
async def toggle_camera(cam_id: str):
    resultado = cam_manager.toggle(cam_id)
    return {"ok": True, "ativo": resultado}

# ---------- Stream MJPEG por câmera ----------

@app.get("/stream/{cam_id}")
async def stream(cam_id: str):
    async def generate():
        while True:
            frame_bytes = cam_manager.get_frame_jpeg(cam_id)
            if frame_bytes:
                yield (
                    b"--frame\r\n"
                    b"Content-Type: image/jpeg\r\n\r\n" + frame_bytes + b"\r\n"
                )
            await asyncio.sleep(0.05)  # ~20fps
    return StreamingResponse(generate(), media_type="multipart/x-mixed-replace;boundary=frame")

# ---------- WebSocket notificações ----------

@app.websocket("/ws")
async def websocket_endpoint(ws: WebSocket):
    await ws_manager.connect(ws)
    try:
        while True:
            await ws.receive_text()  # mantém conexão viva
    except WebSocketDisconnect:
        ws_manager.disconnect(ws)


async def broadcast_acesso(evento: dict):
    """Chamado pelo CameraManager ao detectar placa."""
    await ws_manager.broadcast(evento)
