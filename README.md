# Tirro

### Custom connectors to various technologies

The Tirro custom connectors is an open source project with different modules. Each module connects differents technologies between them. Implement stream-aware, reactive architecture. Its is built with [Akka streams](https://github.com/akka/akka) and it provides differents flags per module to establish the connection and move the data from one point to another. So far the available modules are:

* AWS S3 to FTP: Move data from an AWS S3 bucket to a FTP server.
* FTP to AWS S3: Move data from an FTP server to an AWS S3 bucket
* S3 Encryptor: Get data from an S3 bucket, encrypt it using GPG and move it to another S3 bucket.

## How to use this?

For run it locally [SBT](https://www.scala-sbt.org/) and [scala](https://scala-lang.org) are need to be installed. once you have everything installed, run the command `sbt`. Tirro will show the welcome page
```
[info]
[info] ** Welcome to the sbt build definition for Tirro **
[info]
[info] Useful sbt tasks:
[info] - project <name-of-the-module>
[info] - test
[info]
```

Go to the module you want using `project s3FTP` and then run `run --help` to see all flags available to establish the connections.

