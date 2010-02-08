############################
 GateIn Groovy tool README
############################

This command line tool is a simple script to simplify the deployment of groovy REST service in the portal GateIn.
It is using the available REST API of GateIn.

Install
=======

* First you need to install Groovy if you don't have it yet. See http://groovy.codehaus.org/Installing+Groovy
* copy exo.groovy somewhere accessible from your PATH. On mac or ubuntu, you can copy it to your /usr/local/bin

Using it
========

Copy the app.conf of the demo in your project and adapt it to your need.

This are the command available::

  update                       : Update the GateIn server with the code
  autoupdate                   : Update the server every-time a file is modified or added
  autoload <filename> [--off]  : Activate (or deactivate) the autoload for the given file
  delete <filename>            : Delete the given file
  get <filename>               : Print on the standard output the content of the given file on the server
  lsgroovy                     : List the groovy files on the server

Known limitation
=================

* Only the root directory is synchronized
* If file are locally removed they are not removed on the server and need to be removed with the delete command
