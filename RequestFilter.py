import json

import mitmproxy
from mitmproxy import http

httpRecord = {}


class HttpRecord:

    def request(self, flow: mitmproxy.http.HTTPFlow):
        if flow.request.pretty_url.startswith("https://lotswap.dpm.org.cn"):
            for key in flow.request.headers.keys():
                httpRecord[key] = flow.request.headers[key]
        with open('/Users/devin.zhang/Desktop/record', 'w+', encoding='utf-8') as f:
            f.write(httpRecord.__str__().replace("'", "\""))

addons = [
    HttpRecord()
]
