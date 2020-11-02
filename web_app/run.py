from flask import Flask, render_template, send_file

app = Flask(__name__)

@app.route('/')
@app.route('/qumrax')
def index():
    return render_template('index.html')
   

@app.route("/download")
def download():
   return send_file("app-release.apk", as_attachment=True)

if __name__ == '__main__':
  app.run(host="0.0.0.0", port=5000, debug=True)
