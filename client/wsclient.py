import threading
import time
import websocket


class WSClient:
    def __init__(
        self,
        host="localhost",
        port=58115,
        token="your_secret_token",
        timeout=15,
        on_message=None,
        on_error=None,
        on_open=None,
        on_close=None,
    ):
        self.host = host
        self.port = port
        self.token = token
        self.timeout = timeout
        self.on_message = on_message
        self.on_error = on_error
        self.on_open = on_open
        self.on_close = on_close
        self._ws = None
        self._thread = None
        self._timeout_thread = None
        self.running = False
        self.connecting = False

    @property
    def url(self):
        url = f"ws://{self.host}:{self.port}"
        if self.token:
            url += f"?token={self.token}"
        return url

    def connect(self):
        if self.running:
            return
        self.running = True
        self.connecting = True
        self._ws = websocket.WebSocketApp(
            self.url,
            on_open=self._on_open,
            on_message=self._on_message,
            on_error=self._on_error,
            on_close=self._on_close,
        )
        self._thread = threading.Thread(target=self._ws.run_forever, daemon=True)
        self._thread.start()
        self._start_timeout()

    def _start_timeout(self):
        def _wait():
            time.sleep(self.timeout)
            if self.connecting:
                self.close()
                if self.on_error:
                    self.on_error("Connection timeout ({0}s)".format(self.timeout))

        self._timeout_thread = threading.Thread(target=_wait, daemon=True)
        self._timeout_thread.start()

    def cancel_timeout(self):
        self.connecting = False

    def send(self, message):
        if self._ws and self.running:
            try:
                self._ws.send(message)
            except Exception:
                pass

    def close(self):
        self.running = False
        self.connecting = False
        if self._ws:
            try:
                self._ws.close()
            except Exception:
                pass
        self._ws = None

    def _on_open(self, ws):
        self.cancel_timeout()
        if self.on_open:
            self.on_open()

    def _on_message(self, ws, message):
        if self.on_message:
            self.on_message(message)

    def _on_error(self, ws, error):
        self.cancel_timeout()
        if self.on_error:
            self.on_error(str(error))

    def _on_close(self, ws, close_status_code, close_msg):
        self.running = False
        self.connecting = False
        if self.on_close:
            self.on_close()
