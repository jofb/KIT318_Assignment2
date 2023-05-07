# KIT318_Assignment2

## Setup
1. Clone the repo either using a git GUI (Github Desktop, etc) or `git clone https://github.com/jofb/KIT318_Assignment2.git`
</br>If cloning through command line you will likely have to verify your account with username and a [temporary access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
2. In Eclipse go File > Import Projects from File System
3. Select the directory you cloned the repo to and complete the import.

### Notes

There are 6 files in the project. </br>
`WeatherServer.java` is the main server file. It waits for a connection from a client, and upon recieving one, opens up a thread to handle it. You can force exit this through eclipse, or in command line with ctrl-c and it should handle closing of the server properly. </br>

`ClientConnectionThread.java` handles client connections. The weather server opens these to deal with users. Inside here we need to add user authentication and command input. </br>

`UserConnection.java` attempts to connect to the server, and then takes in user input. This will be useful for connecting to the server as a single user and testing out commands. </br>

`UserConnectionCreator.java` creates a specified number of threads to mock user connections. </br>

`UserConnectionThread.java` is the thread that the creator spawns, rather than taking in user input it does automatic input into the server. This will be useful when we need to test how the server handles multiple users at once. </br>

`WorkerThread.java` is empty and unused but is the thread will manage the worker nodes on the cloud.
