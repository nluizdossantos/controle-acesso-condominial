"""
Banco de Dados SQLite — versão com suporte à interface web
"""

import sqlite3
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class DatabaseManager:
    def __init__(self, caminho_db: str = "condominio.db"):
        self.caminho_db = caminho_db
        self.conn = sqlite3.connect(caminho_db, check_same_thread=False)
        self.conn.row_factory = sqlite3.Row
        logger.info(f"Banco conectado: {caminho_db}")

    def inicializar(self):
        self.conn.executescript("""
            CREATE TABLE IF NOT EXISTS moradores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                apartamento TEXT NOT NULL,
                telefone TEXT DEFAULT '',
                whatsapp TEXT DEFAULT '',
                ativo INTEGER DEFAULT 1,
                criado_em TEXT DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS veiculos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                placa TEXT NOT NULL UNIQUE,
                modelo TEXT DEFAULT '',
                cor TEXT DEFAULT '',
                morador_id INTEGER,
                tipo TEXT DEFAULT 'morador',
                ativo INTEGER DEFAULT 1,
                criado_em TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (morador_id) REFERENCES moradores(id)
            );

            CREATE TABLE IF NOT EXISTS acessos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                placa TEXT NOT NULL,
                resultado TEXT NOT NULL,
                motivo TEXT DEFAULT '',
                imagem_path TEXT DEFAULT '',
                camera_id TEXT DEFAULT '',
                criado_em TEXT DEFAULT CURRENT_TIMESTAMP
            );
        """)
        self.conn.commit()
        self._inserir_exemplo()
        logger.info("Banco inicializado.")

    def _inserir_exemplo(self):
        cur = self.conn.cursor()
        cur.execute("SELECT COUNT(*) FROM moradores")
        if cur.fetchone()[0] > 0:
            return
        moradores = [
            ("João Silva", "101", "31999990001", "5531999990001"),
            ("Maria Souza", "202", "31999990002", "5531999990002"),
            ("Carlos Lima", "303", "31999990003", ""),
            ("Portaria", "000", "", ""),
        ]
        cur.executemany(
            "INSERT INTO moradores (nome, apartamento, telefone, whatsapp) VALUES (?,?,?,?)",
            moradores
        )
        veiculos = [
            ("ABC1D23", "Fiat Uno", "Branco", 1, "morador"),
            ("DEF2E34", "Honda Civic", "Prata", 2, "morador"),
            ("GHI3F45", "Toyota Corolla", "Preto", 3, "morador"),
            ("JKL4G56", "VW Gol", "Vermelho", 4, "servico"),
            ("MNO1234", "Ford Ka", "Azul", 1, "morador"),
        ]
        cur.executemany(
            "INSERT INTO veiculos (placa, modelo, cor, morador_id, tipo) VALUES (?,?,?,?,?)",
            veiculos
        )
        self.conn.commit()

    # --- Consultas ---

    def buscar_veiculo(self, placa: str):
        return self.conn.execute("""
            SELECT v.*, m.nome AS proprietario, m.apartamento, m.whatsapp
            FROM veiculos v
            LEFT JOIN moradores m ON v.morador_id = m.id
            WHERE v.placa = ? AND v.ativo = 1
        """, (placa.upper(),)).fetchone()

    def listar_veiculos(self):
        return self.conn.execute("""
            SELECT v.*, m.nome AS proprietario, m.apartamento
            FROM veiculos v
            LEFT JOIN moradores m ON v.morador_id = m.id
            WHERE v.ativo = 1
            ORDER BY v.placa
        """).fetchall()

    def listar_moradores(self):
        return self.conn.execute(
            "SELECT * FROM moradores WHERE ativo = 1 ORDER BY nome"
        ).fetchall()

    def listar_acessos(self, limite: int = 100):
        return self.conn.execute(
            "SELECT * FROM acessos ORDER BY criado_em DESC LIMIT ?", (limite,)
        ).fetchall()

    def get_stats(self) -> dict:
        cur = self.conn.cursor()
        hoje = datetime.now().strftime("%Y-%m-%d")
        total_veiculos = cur.execute("SELECT COUNT(*) FROM veiculos WHERE ativo=1").fetchone()[0]
        total_moradores = cur.execute("SELECT COUNT(*) FROM moradores WHERE ativo=1").fetchone()[0]
        acessos_hoje = cur.execute(
            "SELECT COUNT(*) FROM acessos WHERE DATE(criado_em)=?", (hoje,)
        ).fetchone()[0]
        negados_hoje = cur.execute(
            "SELECT COUNT(*) FROM acessos WHERE DATE(criado_em)=? AND resultado='NEGADO'", (hoje,)
        ).fetchone()[0]
        return {
            "veiculos": total_veiculos,
            "moradores": total_moradores,
            "acessos_hoje": acessos_hoje,
            "negados_hoje": negados_hoje,
        }

    # --- Mutações ---

    def registrar_acesso(self, placa, resultado, motivo="", imagem_path="", camera_id=""):
        self.conn.execute(
            "INSERT INTO acessos (placa, resultado, motivo, imagem_path, camera_id) VALUES (?,?,?,?,?)",
            (placa, resultado, motivo, imagem_path, camera_id)
        )
        self.conn.commit()

    def adicionar_veiculo(self, placa, modelo, cor, morador_id, tipo="morador") -> bool:
        try:
            self.conn.execute(
                "INSERT INTO veiculos (placa, modelo, cor, morador_id, tipo) VALUES (?,?,?,?,?)",
                (placa.upper(), modelo, cor, morador_id, tipo)
            )
            self.conn.commit()
            return True
        except sqlite3.IntegrityError:
            return False

    def desativar_veiculo(self, placa: str):
        self.conn.execute("UPDATE veiculos SET ativo=0 WHERE placa=?", (placa.upper(),))
        self.conn.commit()

    def adicionar_morador(self, nome, apartamento, telefone="", whatsapp="") -> int:
        cur = self.conn.execute(
            "INSERT INTO moradores (nome, apartamento, telefone, whatsapp) VALUES (?,?,?,?)",
            (nome, apartamento, telefone, whatsapp)
        )
        self.conn.commit()
        return cur.lastrowid

    def fechar(self):
        self.conn.close()
