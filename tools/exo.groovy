#!/usr/bin/env groovy

config = new ConfigSlurper("development").parse(new File('app.conf').toURL())
last_push_file_date = [:]

/*
 * The list of extension we sync
 */
accepted_extension = [
        ".gadget.xml": "application/x-google-gadget",
        ".xml": "text/xml",
        ".groovy": "script/groovy",
        ".js": "application/javascript",
        ".html": "text/html",
        ".png": "image/png",
        ".jpg": "image/jpeg",
        ".gif": "image/gif",
]

/*
 * Check every second if the code has changed
 * if it has changed, update it.
 */
def autoUpdateCode() {
  while (1) {
    updateCode()
    sleep(1000)
  }
}

/*
 * Scan the current directory for files that need to be synchronized
 */
def updateCode() {
  def f = new File('.')
  f.list().each {filename ->
    gF = new File(filename)
    if (gF.lastModified() == last_push_file_date[filename]) {
      return
    }
    for (extension in accepted_extension.keySet()) {
      if (filename.endsWith(extension)) {
        println "PUSHING: $filename"
        _pushCode(gF, accepted_extension[extension])
        break
      }
    }
    last_push_file_date[filename] = gF.lastModified()
  }
}

def getConnection(action = null, filename = null, params = null, path = "script/groovy") {
  //we cannot split the code in project because the API don't know how to create directories
  def url
  if (filename) {
    url = "${config.server.url}/rest/private/$path/" + (action ? "$action/" : "") + "${config.server.repository}/${config.server.workspace}/${config.application.name}/" + filename
  } else {
    url = "${config.server.url}/rest/private/$path/" + (action ? "$action/" : "") + "${config.server.repository}/${config.server.workspace}"
  }
  if (params) {
    url += "?$params"
  }

  def conn = new URL(url).openConnection()
  conn.setRequestMethod("POST")
  def encoded = "${config.server.login}:${config.server.password}".getBytes().encodeBase64().toString()
  conn.setRequestProperty("Authorization", "Basic $encoded")
  return conn
}

def setAutoLoad(filename, value = true) {
  conn = getConnection("autoload", filename, "state=" + (value ? "true" : "false"))
  conn.doOutput = true

  try {
    println conn.content.text
  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
}

def listCode() {
  conn = getConnection("list")
  try {
    def res = conn.content.text

    def files = res.findAll("\".*?\"")
    //the first one is the key, we remove it
    files.remove(0)

    files.each {
      println it
    }

  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
}

def deleteCode(filename) {
  conn = getConnection("delete", filename)
  try {
    println conn.content.text
  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
}

def getCode(filename) {
  conn = getConnection("src", filename)
  try {
    println conn.content.text
  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
}

def _pushCode(f, mimetype, create = false) {
  def conn
  if (create && !_is_directory_exist(config.application.name)) {
    _createDirectory(config.application.name)
  }

  conn = getConnection(null, filename = f.name, null, path = "jcr")

  conn.setRequestProperty("Content-Type", mimetype)
  conn.setRequestProperty("X-HTTP-Method-Override", "PUT")
  conn.setRequestProperty("Content-NodeType", "nt:resource")

  conn.doOutput = true
  os = conn.getOutputStream()
  os.write(f.readBytes())

  try {
    if (conn.responseCode == 409) {
      if (!create) {
        _pushCode(f, mimetype, true)
        return
      }
      println "ERROR 409 pushing ${f.name}"
      return
      // Read response string, using response charset encoding sent by server
    } else if (conn.responseCode == 201) {
      print conn.content.text
    } else {
      println "ERROR ${conn.responseCode} pushing ${f.name}"
    }

  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
  if (config.server.autoload && mimetype == "script/groovy") {
    _load(f.name)
  }
}

def _load(filename, value = true) {
  conn = getConnection("load", filename, "state=" + (value ? "true" : "false"))
  conn.doOutput = true

  try {
    println conn.content.text
  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
}

def _is_directory_exist(directory) {
  def url = "${config.server.url}/rest/private/jcr/${config.server.repository}/${config.server.workspace}/$directory"
  def conn = new URL(url).openConnection()
  //conn.setRequestMethod("POST")
  def encoded = "${config.server.login}:${config.server.password}".getBytes().encodeBase64().toString()
  conn.setRequestProperty("Authorization", "Basic $encoded")
  //conn.setRequestProperty("X-HTTP-Method-Override", "MKCOL")

  if (conn.responseCode == 200)
     return true
  return false
}

def _createDirectory(directory) {
  def url = "${config.server.url}/rest/private/jcr/${config.server.repository}/${config.server.workspace}/$directory"
  def conn = new URL(url).openConnection()
  conn.setRequestMethod("POST")
  def encoded = "${config.server.login}:${config.server.password}".getBytes().encodeBase64().toString()
  conn.setRequestProperty("Authorization", "Basic $encoded")
  conn.setRequestProperty("X-HTTP-Method-Override", "MKCOL")
  
  try {
    println conn.content.text
  } catch (java.net.UnknownServiceException e) {
    //We ignore it
  }
}

def usage() {
  print """  usage: exo.groovy action [args]
  actions:                    
    update                       : Update the eXo server with the code
    autoupdate                   : Update the server everytime that a file is modified or added
    autoload <filename> [--off]  : Activate the autoload for the given file
    delete <filename>            : Delete the given file
    get <filename>               : Print on the standard output the content of the given file on the server
    lsgroovy                     : List the groovy files on the server
  """
}

if (args.length == 0) {
  usage()
  return
}


if (args[0] == "update") {
  updateCode()
} else if (args[0] == "autoupdate") {
  autoUpdateCode()
} else if (args[0] == "autoload") {
  if (args.length == 3 && args[2] == "--off")
    setAutoLoad(args[1], false)
  else
    setAutoLoad(args[1])
} else if (args[0] == "delete") {
  deleteCode(args[1])
} else if (args[0] == "get") {
  getCode(args[1])
} else if (args[0] == "lsgroovy") {
  listCode()
} else {
  usage()
}
