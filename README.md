# Mock Certificate Server

This service exposes an endpoint at `/cert/${domain}` that returns the provided domain appended with a UUID string to mock a certificate service.

You can run this locally with Docker by cloning the repo and then running `docker build -t certserver .` in the project's root directory. After the image has been built, you can start up the service itself by running `docker run -it -p 8080:8080 --name=certserver --rm certserver`.

## Context/Background Information

From January of 2018 until October 2018 I worked solely in Kotlin for both front-end and back-end development. In October, the Kotlin team released the 1.0 version of Ktor, the official Kotlin multi-platform server/client. I decided to try using Ktor for this project since it was developed specifically for the Kotlin language by the Kotlin team. 

Ktor is powered by Kotlin coroutines, a language feature that graduated from experimental status the same month that Ktor was released. As I was working on this project I quickly discovered that the documentation for both coroutines and Ktor itself was fragmented, usually out of date, and unreliable. I stuck to using Ktor because my Node.js skills have been too long neglected and while I was learning Golang in 2017, that dropped off once I started working full time with Kotlin.

I'm sure the Kotlin team will eventually turn Ktor into an easy to use framework that competes with Spring Boot or Javalin.io but in its current form, it was too much trial and error for me to get working and I would likely not use it again until its a bit more mature.

### App Overview
The app consists of a single Ktor application module: `Application.kt` and a separate class `CertApp` that contains most of the application's logic, split out from the main application file to make testing easier.

#### Concurrency
The mocked certificates are stored in memory in a ConcurrentHashMap. In order to properly handle concurrent requests for a new certificate I attempted to use just suspending coroutines but I wasn't able to compose them in a way that actually worked. Instead, I wrapped most of the logic around fetching certificates in synchronization calls. One of the drawbacks of this implementation is that the sync function puts a lock on the entire map of cached certificates when writing to the map. This is serviceable enough if traffic is infrequent but could slow the app considerably under significant load.

#### Containerization
This was not technically required as part of the challenge but I wanted to make it easier to get the app up and going for inspection.

Running the Test Gradle task within a Docker container doesn't work at all here. The Ktor app will start up but then fails to run the test code. There are issues on Ktor's Github repo reporting this same issue in a few different tickets but I'm unable to say whether the issue lies in the Ktor code, Gradle, or due to something in the container environment. It could also be something funky going on when compiled with the Linux Subsystem for Windows!

The app compiles to a single executable jar using shadowjar. I gave thought to trying out GraalVM to try compiling the app down to a single, native binary but I know that GraalVM has trouble working with apps that make use of Kotlin's reflection library and Ktor uses that library quite a bit internally so an executable jar file running on a system with the JVM will have to suffice.

### Final Thoughts
Most of the problems I encountered while working on this project centered around properly handling concurrency, and the build process. There are a number of things I dislike about Go but after this project, learning Golang is back on my 2019 goals list. The simple and effective build tool and its concurrency story make it easy to see why Go is popular.