from flask import Flask, render_template, send_file

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('index.html')
   

@app.route("/download")
def download():
   return send_file("qumrax1-1.apk", as_attachment=True)

if __name__ == '__main__':
  app.run(host="127.0.0.1", port=5000, debug=True)
