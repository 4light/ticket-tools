import json

import requests
import mitmproxy
from mitmproxy import http

headerRecord = {}
httpRecord = {}
headers = {
    'Content-Type': 'application/json'  # 设置 Content-Type 头
}


class HttpRecord:

    def request(self, flow: mitmproxy.http.HTTPFlow):
        if flow.request.pretty_url.startswith("https://lotswap"):
            httpRecord["url"] = flow.request.url.replace("'", "\"")
            for key in flow.request.headers.keys():
                headerRecord[key] = flow.request.headers[key]
            httpRecord["headers"] = headerRecord.__str__().replace("'", "\"")
            requests.post("http://192.168.10.124:8082/ticket/proxy/user/add",
                          json=httpRecord, headers=headers)


addons = [
    HttpRecord()
]
