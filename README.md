# llt

[![Build Status](https://travis-ci.org/qiyi/llt.svg?branch=master)](https://travis-ci.org/qiyi/llt)

LLT is a low level test framework for spring apps.

It provides some embedded component while running junit4/5 test cases including:
  * embedded tomcat as the web container.
  * embedded MariaDB and auto migrate sql files with flyway.
  * embedded etcd for configurations.
  * embedded zookeeper for configuration.
  * embedded redis for cache usage.
  * embedded kafka for message queue.
  * embedded cassandra for no sql.
  
## Maven
The artifact is uploaded to : https://oss.sonatype.org/content/repositories/snapshots/org/isouth/llt/1.0-SNAPSHOT/ 
