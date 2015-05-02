Java bomutils
========

*Java open source tools to create bill-of-materials files used in Mac OS X installers*

This is Java port of https://github.com/hogliux/bomutils

Java *bomutils* are a set of tools to create Mac OS X installer packages on foreign OSes (windows, linux, etc.) with Java. 
These tools can be used as part of the cross-compilation process. In particular, it includes an open source version of the mkbom tool which is distributed as a closed-source version on Mac OS X Developer Tools.

Usage
-----
To create a bom file, follow the following steps.

1. Put the installation payload into a directory. We assume the name of the directory is 'base'
2. Use mkbom to create the bom file by invoking 'java -jar bomutils.jar mkbom -u 0 -g 80 base Bom'

Original Documentation
-------------
For full documentation of original tool, it is best to follow the tutorial at http://hogliux.github.io/bomutils/tutorial.html

Acknowledgments
----------------
Joseph Coffland, Julian Devlin, Baron Roberts, Fabian Renn for making original bomutils.
