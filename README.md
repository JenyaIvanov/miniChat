# miniChat
Java based text chat server with multiuser and PMs.
Status: halted.

# What is it used for?
I encourage everyone that needs a private place of conversation to use this. miniChat is good for emergency reason. Setting up is quick, "resource cheap" (can be executed on any Java IDE platforms) (*Note: a proper executeable client is something I'm considering*), and works well on high latency situations as its a TCP socket listening for bits. 

# Information:
This is an Java based TCP text server using built-in Java server sockets. The clients currently can connect through telnet panels as I have not finished working on the JFrame Java client. It takes roughly a minute to set up the server, just change few settings and hit RUN - and you got a working private chat server. 

# Running:
Grab the files and execute them on your favorite Java IDE, I have used Eclipse for testing and development. Make sure port 8878 is open on your machine/virtual machine and execute the ServerMain.java file. You can follow the IDE log in-order to figure out if the server was set up correcly.

# Commands:
(*Use !help on the server to be presented with the commands available.*)
Currently the server supports the following commands:
!pm(private message), !online, !credits, !help.
Administrator commands:
!rcon <password>, !motd <on/off>, !kick <username>.
  rcon - administrator login
  motd - message of the day

# Credits:
Testers: Dani (Denis) Kogel (@Kogwork)
