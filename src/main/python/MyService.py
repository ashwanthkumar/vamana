#!/usr/bin/python

from BaseHTTPServer import BaseHTTPRequestHandler
import urlparse
import random

class MyServiceHandler(BaseHTTPRequestHandler):
    
    def do_GET(self):
        parsed_path = urlparse.urlparse(self.path)
        if parsed_path.path == "/status":
        	self.send_response(200)
	        self.end_headers()
	        requests = random.randint(0, 1000)
	        self.wfile.write("{\"requests\": "+str(requests)+"}")
	        return
        else:
	        self.send_response(200)
	        self.end_headers()
	        self.wfile.write("Hey there, I'm alive n kicking!")
	        return

if __name__ == '__main__':
    from BaseHTTPServer import HTTPServer
    server = HTTPServer(('localhost', 8080), MyServiceHandler)
    print 'Starting server, use <Ctrl-C> to stop'
    server.serve_forever()