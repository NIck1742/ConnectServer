# ConnectServer
SE-Project

# Server-Side:
Install openSSH\
Install java-16-jre\
Install Ansible\
Install WireGuard

Copy all files from resources-folder to\
~/.ansible/ConnectServer/\
Generate SSH Key\
Generate WireGuard public and private-key\
Insert public and private WireGuard keys to playbook.yml and /group_vars/all.yml

# Client-Side
Make sure SSH-Manager is installed.

# On both sides 
Make sure SSH PermitRootLogin is set to: yes\
and root passwd is set\
# Start ConnectServer with:
java -jar ConnectServer.jar\
In directory ~/.ansible/ConnectServer\
Webservice is running on Port 42000

To use self-generated docker-tarfiles you have to use a sftp manager to ship tarfile to the ConnectServer.\
Save Tarfiles in the tarballs directory. 
