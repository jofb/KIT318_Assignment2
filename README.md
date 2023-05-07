# KIT318_Assignment2

## Setup
1. Clone the repo either using a git GUI (Github Desktop, etc) or `git clone https://github.com/jofb/KIT318_Assignment2.git`

</br>1.1 If cloning through command line you will likely have to verify your account with username and a [temporary access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
2. In Eclipse go File > Import Projects from File System
3. Select the directory you cloned the repo to and complete the import.

### Notes for initial project
WeatherServer.java & MainWorker both using command line arguments. Make sure the thread count for both files is the same. </br>
`WeatherServer inputFile outputFile threadCount` </br>
`MainWorker threadCount` </br>

So you will have to set those arguments up in your run configurations accordingly.
