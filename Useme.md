# Usage and Operation

A user can interact with our service using the client package.


## Running the commands

We can run the service by doing the following steps:



* Servers and Orchestrator: In the base folderpath run the following command
* Client: to run the client you will need to run a jar file
    * Navigate to the client folder
    * Run the command java -jar client.jar &lt;userName>
        * &lt;userName> : Name of the current user.


## Supported Commands



* Get-ChildItem - Lists all the files in a location
* Set-Location &lt;path> -  Changes directory to a particular location
* New-Directory &lt;path> - Creates a new director
* Remove-Directory &lt;path> - Deletes a directory
* Remove-File &lt;path> - Deletes a file
* Get-Location - Gets the current working directory
* Get-File &lt;path>- Gets a file from the server
* Push-File &lt;localpath>- Uploads a file to the server
* Exit - exits the application
