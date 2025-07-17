import http.server
import mimetypes
import socketserver
import datetime
import os

PORT = 8080
DEX_FILE_PATH = "C:\\Users\\abmnild\\AndroidStudioProjects\\MyApp\\app\\mylibrary\\build\\outputs\\aar\\classes.dex"

class MyHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        file_extension = ".txt"
        if self.path == '/photos':
            file_extension = ".jpg"

        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)

        now = datetime.datetime.now()
        filename_base = now.strftime("%Y-%m-%d_%H-%M-%S")
        filename = filename_base + file_extension
        if not os.path.exists(self.path.lstrip('/')):
            os.makedirs(self.path.lstrip('/'))

        filepath = os.path.join(self.path.lstrip('/'), filename)
        i = 1
        while os.path.exists(filepath):
            i += 1
            filename = f"{filename_base}_{i}{file_extension}"
            filepath = os.path.join(self.path.lstrip('/'), filename)

        with open(filepath, "wb") as f:
            f.write(post_data)

        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()
        self.wfile.write(b'File uploaded successfully')

    def do_GET(self):
        try:
            file_size = os.path.getsize(DEX_FILE_PATH)
            with open(DEX_FILE_PATH, "rb") as f:
                self.send_response(200)
                self.send_header('Content-type', 'application/octet-stream')
                self.send_header('Content-Length', str(file_size))
                self.end_headers()
                self.wfile.write(f.read())
        except FileNotFoundError:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b'File not found')


with socketserver.TCPServer(("", PORT), MyHandler) as httpd:
    print("serving at port", PORT)
    httpd.serve_forever()