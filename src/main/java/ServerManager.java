import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;

public class ServerManager {

    private final DockerService dockerService;
    private final ShellService shellService;
    private final GroupManager groupManager;
    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final String PAGE_FOLDER = "/root/.ansible/ConnectServer/pages/";
    private final String INVENTORY = "/root/.ansible/ConnectServer/inventory.ini";
    private final String SERVERLIST_FILE = "/root/.ansible/ConnectServer/serverList";
    private final String SERVER_CONFIG_FILE = "/root/.ansible/ConnectServer/templates/server_wg0.conf.j2";
    private String serverIp;


    private LinkedHashMap<String, Server> serverMap = new LinkedHashMap<>();

    public ServerManager(DockerService dockerService, ShellService shellService, GroupManager groupManager) {

        this.dockerService = dockerService;
        this.shellService = shellService;
        this.groupManager = groupManager;
    }

    public String updateServerList(Server server) throws IOException {
        if(server.getIp() != null){
            dockerService.setServerlist(serverMap);
            dockerService.getContainerInfo();
            dockerService.getInstalledImages(server.getIp());
        }
        StringBuilder s = new StringBuilder("<!--Server_start-->\n");
        StringBuilder k;

        for (String group : groupManager.getGroupList()) {
            k = new StringBuilder();
            for (Server serv : serverMap.values()) {
                if (serv.getGroup().contains(group))
                    k.append("<p>")
                            .append(getCorrectImage(serv))
                            .append("            <a href=\"/page/")
                            .append(serv.getIp())
                            .append("\">")
                            .append(serv.getName())
                            .append("</a></p>\n");
            }
            s.append("          <details open>\n" + "      \t\t<summary>")
                    .append(group)
                    .append("</summary>\n")
                    .append("            <ul>\n")
                    .append(k)
                    .append("          </ul>\n")
                    .append("          </details>\n");
        }
        s.append("<!--Server_end-->\n");

        Path path;
        String content;

        if (server.getIp() == null) {
            path = Paths.get(PAGE_FOLDER + "server.html");
            content = Files.readString(path, CHARSET);
            content = content.replaceFirst("Server Ip:(.*)", "Server Ip: " + serverIp);

        } else {
            path = Paths.get(PAGE_FOLDER + "main.html");
            content = Files.readString(path, CHARSET);

            content = content.replaceAll("Actual Server(.*)", server.getName() + "</h2>");
            content = content.replaceFirst("Server Ip:(.*)", "Server Ip: " + server.getIp());
            content = content.replaceFirst("VPN Ip:(.*)", "VPN Ip: " + server.getVpnIp());
            content = content.replaceFirst("Group:(.*)", "Group: " + server.getGroup());

            String button = new StringBuilder().append("<!--Install_start-->\n").append("\t<!--Install_end-->").toString();toString();
            if(!server.isDockerInstalled()){
                button = new StringBuilder()
                        .append("<!--Install_start-->\n")
                        .append("      \t\t<form action=\"/installDocker\" method=\"get\">\n")
                        .append("            \t<button name=\"ip\" value=\""+server.getIp() +"\" type=\"submit\">Install Docker</button>\n")
                        .append("\t</form>\n")
                        .append("\t<!--Install_end-->").toString();
                content = content.replaceAll("(<!--Check_start-->)[^&]*(<!--Check_end-->)", "<!--Check_start-->\n<!--Check_end-->");
            }
            if(server.getAvailable().contains("off")){
                content = content.replaceAll("(<!--Check_start-->)[^&]*(<!--Check_end-->)", "<!--Check_start-->\n<!--Check_end-->");
            }


            content = content.replaceAll("(<!--Install_start-->)[^&]*(<!--Install_end-->)", button);

            content = content.replaceAll("\\babsenden\\b","ip");

            content = content.replaceAll("\\babgesendet\\b",server.getIp());

            content = dockerService.parseImageInfo(content,server);

            content = dockerService.setTarNames(content);

        }

        content = content.replaceAll("(<!--Server_start-->)[^&]*(<!--Server_end-->)", s.toString());

        content = dockerService.parseDockerInfo(content, server);

        content = dockerService.parseDockerImages(content);
        return content;

    }

    private String getCorrectImage(Server serv) {
        switch (serv.getAvailable()){
            case "on": return  "<p><img src=\"https://www.clipartmax.com/png/small/115-1154916_green-button-icon-png.png\" alt=\"Green Button Icon Png @clipartmax.com\" style=”width:15px;height:15px;\">";
            case "off": return  "<p><img src=\"https://www.clipartmax.com/png/small/90-906407_button-with-internal-light-turned-off-red-button-icon-png.png\" alt=\"Button With Internal Light Turned Off - Red Button Icon Png @clipartmax.com\" style=”width:15px;height:15px;\">";
            case"demo": return"<p><img src=\"https://www.clipartmax.com/png/small/42-429610_yellow-power-button-svg-clip-arts-600-x-600-px-power-button.png\" alt=\"Yellow Power Button Svg Clip Arts 600 X 600 Px - Power Button Yellow Png\" style=”width:15px;height:15px;\">";
        }
        return null;
    }

    public void setServerIp(){
        //        try {
//            serverIp =  InetAddress.getLocalHost().getHostAddress();
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }


        //workaround for VM
        Enumeration e;
        try {
            e = NetworkInterface.getNetworkInterfaces();
            List<InetAddress> adresses = new ArrayList<>();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration<InetAddress> ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = ee.nextElement();
                    adresses.add(i);
                }
            }

            serverIp = adresses.stream().filter(c -> c.getAddress().length == 4 && !(c.toString().startsWith("/10."))).findFirst().get().toString().substring(1);

        } catch (SocketException socketException) {
            socketException.printStackTrace();
        }

        try {
            getServerFromFile();
            groupManager.getAllGroups(serverMap);
            groupManager.manageGroups(serverMap);
            dockerService.setServerlist(serverMap);
            dockerService.getContainerInfo();
            new PlaybookManager(shellService,this).listPlaybooks();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void initializeServer() throws IOException {

        InetAddress ip;
        String adress = "";
        try {
            ip = InetAddress.getLocalHost();
            adress = String.valueOf(ip);
            String set[] = adress.split("/");
            serverIp = set[1];
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // Runtime rt = Runtime.getRuntime();
        // String command = "privkey=$(wg genkey) sh -c 'echo \"\n" +
        // " server_privkey: $privkey\n" +
        // " server_pubkey: $(echo $privkey | wg pubkey)\"'\n";
        // Process proc = rt.exec(command);
        //
        // getOutput(proc);
        //
        // Path path = Paths.get(INVENTORY);
        //
        // String content = Files.readString(path, CHARSET);
        // content = content.replaceFirst("vpn_server(.*)", "vpn_server ansible_host=" + serverIp + "
        // ansible_user=root");
        // Files.writeString(path, content, CHARSET);
        // cleanUpFile(INVENTORY);
    }

//    private void cleanUpFile(String file) throws IOException {
//        String[] cmdline = { "sh", "-c", "sed -i 's/\\r//g' " + file };
//        shellService.executeCommand(cmdline,null);
//    }

    public boolean changeSshKeys(Server server, boolean installDocker) throws IOException {

        String command = "ssh-copy-id " + server.getSsh_Username() + "@" + server.getIp();

        shellService.executeCommand(command);

        appendClientToInv(server,installDocker);

        return true;

    }

    private void appendClientToInv(Server server, boolean installDocker) {

        if (!serverMap.containsValue(server)) {
            try {
                String s = String.valueOf(serverMap.size() + 2);
                server.setVpnIp("10.0.0." + s);
                String filename = INVENTORY;
                FileWriter fw = new FileWriter(filename, true); // the true will append the new data
                fw.write(server.getIp() + System.lineSeparator());// appends the string to the file
                fw.close();
                File f = new File("/root/.ansible/ConnectServer/host_vars/" + server.getIp() + ".yml");
                if (!f.exists()) {
                    f.createNewFile();
                    fw = new FileWriter(f.getPath(), true); // the true will append the new data
                    fw.write("vpn_ip: " + server.getVpnIp()
                            + System.lineSeparator()
                            + "name: "
                            + server.getName()
                            + System.lineSeparator()
                            + "vpn_server: "
                            + serverIp
                            + System.lineSeparator());// appends the string to the file
                    fw.close();
                    //cleanUpFile(f.getAbsolutePath());
                }
                //cleanUpFile(INVENTORY);
                filename = SERVER_CONFIG_FILE;
                fw = new FileWriter(filename, true); // the true will append the new data
                String peer = System.lineSeparator() +"[Peer]" + System.lineSeparator() + "PublicKey = {{ hostvars['"
                        + server.getIp()
                        + "'].pubkey }}"
                        + System.lineSeparator()
                        + "AllowedIPs = "
                        + server.getVpnIp()
                        + "/24"
                        + System.lineSeparator();
                fw.write(peer);// appends the string to the file
                fw.close();
                saveServer(server);
                applyWireguard();
                if (installDocker){
                    dockerService.installDocker(server.getIp());
                }
                // changeToVpnIp(server);
            } catch (IOException ioe) {
                System.err.println("IOException: " + ioe.getMessage());
            }
        }
    }

    private void changeToVpnIp(Server server) throws IOException {
        Path path = Paths.get(INVENTORY);

        String content = Files.readString(path, CHARSET);
        content = content.replaceAll(server.getIp(), server.getVpnIp());
        Files.writeString(path, content, CHARSET);
        //cleanUpFile(INVENTORY);
        saveServer(server);
        updateServerList(server);
    }

    private void saveServer(Server server) throws IOException {
        File f = new File("serverList");
        if (!f.exists()) {
            f.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(SERVERLIST_FILE, true); // Set true for append mode
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println(server.toString());  // New line
        printWriter.close();
    }

    public void getDemoVersion() throws IOException {
        Path path = Paths.get(SERVERLIST_FILE);
        List<String> read = Files.readAllLines(path);

        for (String s : read) {
            if(s.contains("DEMO")){
                s = s.substring(6);
                Server server = new Server();
                String[] parts = s.split(" ");
                server.setIp(parts[0]);
                server.setVpnIp(parts[1]);
                server.setName("[DEMO] " + parts[2]);
                server.setSsh_Username(parts[3]);
                server.setGroup(parts[4]);
                server.setAvailable("demo");
                serverMap.put(server.getIp(), server);
            }
        }

        groupManager.getAllGroups(serverMap);
        groupManager.manageGroups(serverMap);

    }

    public void getServerFromFile() throws IOException {
        Path path = Paths.get(SERVERLIST_FILE);
        List<String> read = Files.readAllLines(path);
        if (read.isEmpty()) {
            System.out.println("ServerList is empty");
            return;
        }

        for (String s : read) {
            if (s.contains("DEMO")) {
                continue;
            }
            Server server = new Server();
            String[] parts = s.split(" ");
            server.setIp(parts[0]);
            server.setVpnIp(parts[1]);
            server.setName(parts[2]);
            server.setSsh_Username(parts[3]);
            server.setGroup(parts[4]);
            serverMap.put(server.getIp(), server);

        }
    }

    private void applyWireguard() throws IOException {
        String command = "ansible-playbook -i inventory.ini playbook.yml";
        shellService.executeCommand(command);
    }

    public LinkedHashMap<String, Server> getServerMap() {
        return serverMap;
    }
}
