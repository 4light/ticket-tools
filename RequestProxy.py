import mitmproxy.http
from mitmproxy import ctx, http
import time
class Action1:
    def request(self, flow: mitmproxy.http.HTTPFlow):
        #flow.request.host='www.google.com'
        #t=1/0
        #print("请求将被kill，后续将不会有response")
        #flow.kill()

        if 'action' in flow.request.url:
            print("触发了拦截,会阻塞后面的请求，即使请求不进入本逻辑")
            flow.intercept()
            a = input("输入1 放行: ")
            if a=='1':
                #time.sleep(20)
                flow.resume()#继续流动 - 在一个intercept（）之后调用
            else:
                print("输入不是1，此请求不再放行")
        else:
            print("不拦截")

        #pretty_host #类似于host，但使用主机头作为附加的首选数据源。这在透明模式下很有用，host只有IP地址，但可能不会反映实际的目的地，因为主机头可能被欺骗。
        flow.request.method #请求方式。POST、GET等
        flow.request.scheme #什么请求 ，应为“http”或“https”
        #flow.response = http.Response.make(200,"111",)
        flow.request.query #返回MultiDictView类型的数据，url直接带的键值参数
        flow.request.query.keys()#取得所有请求参数(不包含参数对应的值)
        print(list(flow.request.query.keys()))
        print(" ")
        act1 =flow.request.query.get('action')#取得请求参数wd的值
        print(f"act1={act1}")

        #flow.request.query.set_all(key,[value])#修改请求参数
        flow.request.query.set_all('action',['edit-action'])
        act2 =flow.request.query.get('action')
        print(f"act2={act2}")

        flow.request.cookies["log_id"] = "007"#修改cookie
        flow.request.get_content()#bytes,结果如flow.request.get_text()
        flow.request.raw_content #bytes,结果如flow.request.get_content()
        flow.request.urlencoded_form #MultiDictView，content-type：application/x-www-form-urlencoded时的请求参数，不包含url直接带的键值参数
        flow.request.urlencoded_form["code"] = "123456"#修改或赋值
        flow.request.urlencoded_form = [("code", "123456"),("name","lucy")]
        flow.request.multipart_form #MultiDictView，content-type：multipart/form-data
        return

    def response(self, flow):
        #t =1/0
        #flow.response = http.Response.make(200,"111+111",)
        #flow.response = flow.response.make(404)#返回404
        #print(flow.response.headers)
        for (k,v) in flow.response.headers.items():
            print(f"{k}:{v}")
        print(" ")
        #print(flow.response.get_text())
        flow.response.status_code #状态码
        flow.response.text#返回内容，已解码
        #print(flow.response.text)#返回内容，已解码
        flow.response.content #返回内容，二进制
        #flow.response.setText()#修改返回内容，不需要转码
        flow.response.set_text(flow.response.get_text().replace('<title>', '<title>返回title——'))
        flow.response.headers["isMitmproxy"]='yes'#给返回添加返回头
        #读取文件，在当前文件路径下执行脚本，否则需要写文件的绝对路径；不然会找不到该json文件
        with open('1.json','rb') as f:
            #从json文件中读取数据成python对象
            res = json.load(f)
        #将读取的python对象转成json字符串发送给客户端
        flow.response.set_text(json.dumps(res))
        return