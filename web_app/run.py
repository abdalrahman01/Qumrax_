from flask import Flask, render_template, send_file

app = Flask(__name__)

@app.route('/')
def index():
   
  with open("static/label.txt", "r") as labels:
    
    return render_template('index.html', labels = labels)


@app.route("/download")
def download():
   return send_file("app-release.apk", as_attachment=True)

if __name__ == '__main__':
  app.run(debug=True)
