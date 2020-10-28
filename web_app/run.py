from flask import Flask, render_template, send_file
app = Flask(__name__)

@app.route('/')
@app.route('/qumrax')
def index():
    return render_template('index.html')
   

@app.route("/download")
def download():
   return send_file("static\\images.zip", add_etags=True, mimetype="application/zip")

if __name__ == '__main__':
  app.run(host='127.0.0.1', port=8000, debug=True)
 