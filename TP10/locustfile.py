"""Plan de carga del TP10 contra GET /productos.

Carga normal (50 usuarios, espera 1 a 2 s, 2 minutos):
  TP10_MODO=carga locust -f TP10/locustfile.py --headless -u 50 -r 10 -t 120s --host http://localhost:8090

Estres acotado (se corta a los 90 s y no pasa de 80 usuarios):
  TP10_MODO=estres locust -f TP10/locustfile.py --headless -t 90s --host http://localhost:8090

Interfaz para las capturas (http://localhost:8089):
  TP10_MODO=estres locust -f TP10/locustfile.py --host http://localhost:8090
"""

import os

from locust import HttpUser, LoadTestShape, between, task

MODO = os.environ.get("TP10_MODO", "carga")


class UsuarioCatalogo(HttpUser):
    if MODO == "estres":
        # Pide seguido. El timeout corto marca el punto de quiebre.
        wait_time = between(0.05, 0.15)
    else:
        wait_time = between(1, 2)

    @task
    def listar(self):
        timeout = 0.5 if MODO == "estres" else 5
        self.client.get("/productos", timeout=timeout, name="/productos")


if MODO == "estres":

    class RampaEstres(LoadTestShape):
        """Rampa corta y con techo. No sigue subiendo si la ventana queda abierta."""

        def tick(self):
            segundos = self.get_run_time()
            if segundos is None or segundos > 90:
                return None
            if segundos < 20:
                return (30, 5)
            avance = min(1.0, (segundos - 20) / 50.0)
            usuarios = 30 + int(avance * 50)
            return (min(usuarios, 80), 4)
