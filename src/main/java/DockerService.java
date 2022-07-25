import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;

public class DockerService {
    private final String CONTAINER_FOLDER = "/root/.ansible/ConnectServer/containers/";
    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final String DOCKERSETTINGS = "/root/.ansible/ConnectServer/vars/default.yml";
    private final String TAR_FOLDER = "/root/.ansible/ConnectServer/tarballs/";
    private final ShellService shellService;
    private LinkedHashMap<String, Server> serverMap = new LinkedHashMap<>();
    private boolean demo = false;

    public DockerService(ShellService shellService) {
        this.shellService = shellService;
    }

    public String parseImageInfo(String html, Server server) throws IOException {
        server.clearDocker();

        Path path = Paths.get(CONTAINER_FOLDER + server.getName()
                + "/"
                + server.getIp()
                + "/root/.ansible/tmp/Images");
        File f = new File(path.toString());
        if (!f.exists()) {
            return html;
        }

        String fileString = Files.readString(path, CHARSET);

        String[] lines = fileString.split("\\\\n");

        StringBuilder append = new StringBuilder();/* String for setup the table on each server */
        for (int i = lines.length - 1; i > 0; i--) {
            String[] items = lines[i].split("  \\s+");
            server.addDocker(items[0] + ":" + items[1]);
            if (i == lines.length - 1) {
                items[5] = items[5].split("\"")[0];
            }
        }
        append.append("<!--Images_start-->\n");

        for (String image: server.getDockerSet()) {
            append.append("      \t\t<option value=").append(image).append("> ").append(image).append("</option>\n");
        }

        append.append("\t\t<!--Images_end-->                  ");

        html = html.replaceAll("(<!--Images_start-->)[^&]*(<!--Images_end-->)", append.toString());

        return html;
    }

    public String parseDockerInfo(String html, Server server) throws IOException {

        if(server.getIp() == null){
            return getAllDockerContainerInfo(html);
        }

        HashSet<String> dockerImages = new HashSet<>();



            server.clearDocker();

            Path path = Paths.get(CONTAINER_FOLDER + server.getName()
                                  + "/"
                                  + server.getIp()
                                  + "/root/.ansible/tmp/containers");
            File f = new File(path.toString());
            if (!f.exists()) {
                return html;
            }

            String fileString = Files.readString(path, CHARSET);

            String[] lines = fileString.split("\\\\n");

            StringBuilder append = new StringBuilder();/* String for setup the table on each server */
            for (int i = lines.length - 1; i > 0; i--) {
                String[] items = lines[i].split("  \\s+");
                server.addDocker(items[1]);
                dockerImages.add(items[1]);
                if (i == lines.length - 1) {
                    items[5] = items[5].split("\"")[0];
                }
                String docker = "";
                String button = "";
                if (items[4].contains("Up")) {
                    docker = "Stop";
                    button = "/stopContainer?host=" + server.getIp() + "&container=" + items[5];
                } else {
                    docker = "Start";
                    button = "/startContainer?host=" + server.getIp() + "&container=" + items[5];
                }
                String name = validateCorrectName(items);
                append.append("  \t\t\t<tr>\n" + "    \t\t\t<td><a href=\"")
                      .append("/page/")
                      .append(server.getIp())
                      .append("/")
                      .append(name)
                      .append("\">")
                      .append(name)
                      .append("</a></td>\n")
                      .append("    \t\t\t<td>")
                      .append(items[1])
                      .append("</td>\n")
                      .append("    \t\t\t<td>")
                      .append(items[4])
                      .append("</td>\n")
                      .append("    \t\t\t<td><button onclick=\"window.location.href = '")
                      .append(button)
                      .append("';\">")
                      .append(docker)
                      .append("</td>\n")
                      .append("    \t\t\t<td><button onclick=\"window.location.href ='/removeContainer?ip=" + server.getIp() + "&name=" + name + "';\">X")
                      .append("</td>\n")
                      .append("  \t\t\t</tr>\n");
            }

            int size = lines.length - 1;

            StringBuilder s = new StringBuilder("            <!--Docker_start-->\n");
            s.append("            <summary>Server Installed docker Container: ")
             .append(size)
             .append("</summary>\n")
             .append("            <table style=\"width:100%\">\n")
             .append("  \t\t\t<tr>\n")
             .append("    \t\t\t<th>Name</th>\n")
             .append("    \t\t\t<th>Image</th>\n")
             .append("    \t\t\t<th>Status</th>\n")
             .append("    \t\t\t<th>Start/Stop</th>\n")
             .append("    \t\t\t<th>Remove</th>\n")
             .append("  \t\t\t</tr>\n")
             .append(append)
             .append("\t\t\t</table>\n")
             .append("<br><br>\n")
             .append("            <center>\n")
             .append("            <button onclick=\"window.location.href = '/startContainer?host=" + server.getIp() + "';\">Start All</button>\n")
             .append("            <button onclick=\"window.location.href = '/stopContainer?host=" + server.getIp() + "';\">Stop All</button>\n")
             .append("            </center>\n")
             .append("            <br><br>\n")
             .append("            <!--Docker_end-->\n");

            String pageString = html;

            pageString = pageString.replaceAll("(<!--Docker_start-->)[^&]*(<!--Docker_end-->)", s.toString());


            if (server.getIp() != null && server.getIp().contains(server.getIp())) {
                return pageString;
            }

        return html;

    }

    private String validateCorrectName(String[] items) {
        if(items[5].matches("\\d.*")){
            return items[6];
        }
        return items[5];
    }

    private String getAllDockerContainerInfo(String html) throws IOException {
        HashSet<String> dockerImages = new HashSet<>();

        if(demo){
            dockerImages.add("FancyLight 2.8");
            dockerImages.add("Intelligent Shutters 1.1");
            dockerImages.add("Intelligent Shutters 1.8");
            dockerImages.add("SweatLodge Enhanced 2.0");
            dockerImages.add("Dolby Radio 1.0");
            dockerImages.add("FancyLight 1.9");

            serverMap.get("192.168.2.100").addDocker("FancyLight 2.8");
            serverMap.get("192.168.2.101").addDocker("Intelligent Shutters 1.1");
            serverMap.get("192.168.2.102").addDocker("");
            serverMap.get("192.168.2.103").addDocker("FancyLight 2.8");
            serverMap.get("192.168.2.104").addDocker("");
            serverMap.get("192.168.2.105").addDocker("FancyLight 2.8");
            serverMap.get("192.168.2.106").addDocker("Intelligent Shutters 1.1");
            serverMap.get("192.168.2.107").addDocker("Dolby Radio 1.0");
            serverMap.get("192.168.2.108").addDocker("FancyLight 2.8");
            serverMap.get("192.168.2.109").addDocker("Intelligent Shutters 1.8");
            serverMap.get("192.168.2.110").addDocker("Dolby Radio 1.0");
            serverMap.get("192.168.2.111").addDocker("FancyLight 2.8");
            serverMap.get("192.168.2.112").addDocker("SweatLodge Enhanced 2.0");
        }

        for (Server server: serverMap.values()) {
            if(server.getName().contains("DEMO")){
                continue;
            }
            server.clearDocker();

            Path path = Paths.get(CONTAINER_FOLDER + server.getName()
                    + "/"
                    + server.getIp()
                    + "/root/.ansible/tmp/containers");
            File f = new File(path.toString());
            if (!f.exists()) {
                return html;
            }

            String fileString = Files.readString(path, CHARSET);

            String[] lines = fileString.split("\\\\n");

            for (int i = lines.length - 1; i > 0; i--) {
                String[] items = lines[i].split("  \\s+");
                server.addDocker(items[1]);
                dockerImages.add(items[1]);
                if (i == lines.length - 1) {
                    items[5] = items[5].split("\"")[0];
                }

            }
        }


            StringBuilder tmp = new StringBuilder("<!--Docker_start-->\n");/*
             * String for overview of images and Server
             * in Main Server info
             */
            StringBuilder serverList = new StringBuilder();

            for (String str : dockerImages) {
                for (Server serv : serverMap.values()) {
                    HashSet<String> dockerSet = serv.getDockerSet();
                    if (dockerSet.contains(str)) {
                        serverList.append("            <p><a href=\"/page/")
                                .append(serv.getIp())
                                .append("\">")
                                .append(serv.getName())
                                .append("</a></p>\n");
                    }
                }

                tmp.append("            <details>\n" + "      \t\t<summary>")
                        .append(str)
                        .append("</summary>\n")
                        .append("            <ul>\n")
                        .append(serverList)
                        .append("          </ul>\n")
                        .append("          </details>\n");
                serverList = new StringBuilder();
            }

            tmp.append("<!--Docker_end-->\n");

            String pageString = html;
            pageString = pageString.replaceAll("(<!--Docker_start-->)[^&]*(<!--Docker_end-->)", tmp.toString());
            return pageString;

    }


    public void setServerlist(LinkedHashMap<String, Server> serverMap) {
        this.serverMap = serverMap;
    }

    public void applyShowContainer() throws IOException {
        String[] cmdline = { "sh", "-c", "ansible-playbook -i inventory.ini playbook_show_all_container.yml" };
        shellService.executeCommand(cmdline,serverMap);
    }

    public void applyStopContainer(String host, String container) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_stop_container.yml --extra-vars \"host=" + host
                                   + " docker_container="
                                   + container
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void applyStopAllContainerOnHost(String host) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_stop_all_container.yml --extra-vars \"host="
                                   + host
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void applyAddContainer(String host, String image, String name) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_create_container.yml --extra-vars \"host="
                                   + host
                                   + " image="
                                   + image
                                   + " name="
                                   + name
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void applyStartContainer(String host, String container) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_start_container.yml --extra-vars \"host="
                                   + host
                                   + " docker_container="
                                   + container
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void applyStartAllContainerOnHost(String host) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_start_all_container.yml --extra-vars \"host="
                                   + host
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void applyRemoveContainer(String host, String container) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_remove_container.yml --extra-vars \"host="
                                   + host
                                   + " docker_container="
                                   + container
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }


    public void applyRemoveAllContainerOnHost(String host) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_remove_all_container.yml --extra-vars \"host="
                                   + host
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void applyGetContainerInfos(String host, String container) throws IOException {
        String[] cmdline = { "sh",
                             "-c",
                             "ansible-playbook -i inventory.ini playbook_get_container_infos.yml -e \"host=" + host
                                   + " docker_container="
                                   + container
                                   + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public String parseDockerImages(String content){
        return content;
    }

    public void getContainerInfo() {
        try {
            applyShowContainer();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String installDocker(String host) throws IOException {
        String[] cmdline = { "sh",
                "-c",
                "ansible-playbook -i inventory.ini install_docker_playbook.yml --extra-vars \"host="
                        + host
                        + "\"" };
        return shellService.executeCommand(cmdline,null);
    }

    public String getInstalledImages(String host) throws IOException {
        String[] cmdline = { "sh",
                "-c",
                "ansible-playbook -i inventory.ini playbook_get_Images.yml --extra-vars \"host="
                        + host
                        + "\"" };
        String s = shellService.executeCommand(cmdline,null);
        validateIfDockerInstalled(s,host);
        return s;
    }

    private void validateIfDockerInstalled(String s, String host) {
        serverMap.get(host).setDockerInstalled(!s.contains("docker: not found"));
    }

    public void pullImage(String host, String image) throws IOException {
        String[] cmdline = { "sh",
                "-c",
                "ansible-playbook -i inventory.ini pull_docker_playbook.yml -e \"host=" + host
                        + " image="
                        + image
                        + "\"" };
        shellService.executeCommand(cmdline,null);
    }

    public void isDemo() {
        demo = true;
    }

    public void createImageFromTarball(String host, String name) throws IOException {
        String[] cmdline = { "sh",
                "-c",
                "ansible-playbook -i inventory.ini build_docker.yml -e \"host=" + host
                        + " tar="
                        + name
                        + "\"" };
        shellService.executeCommand(cmdline,serverMap);
    }

    public String parseDockerLog(Path path) throws IOException {
        StringBuilder content = new StringBuilder(Files.readString(path, CHARSET));
        String[] lines = content.toString().split("\\\\n");
        content = new StringBuilder();

        if (path.toString().contains("Logs")) {
            content.append("<textarea id=\"log\" name=\"log\" rows=\"40\" cols=\"100\" readonly>");
        } else if (path.toString().contains("Inspect")) {
            content.append("<textarea id=\"inspect\" name=\"inspect\" rows=\"40\" cols=\"100\" readonly>");
        } else {
            System.out.println("Failed parsing Docker Log");
        }

        for (String line : lines) {
            content.append(line).append("\n");
        }

        content.append("</textarea>\n");
        return content.toString();
    }

    public String setTarNames(String html){

        StringBuilder append = new StringBuilder();
        append.append("<!--Tar_start-->\n");

        String[] pathnames;

        // Creates a new File instance by converting the given pathname string
        // into an abstract pathname
        File f = new File(TAR_FOLDER);

        // Populates the array with names of files and directories
        pathnames = f.list();

        // For each pathname in the pathnames array
        for (String pathname : Objects.requireNonNull(pathnames)) {
            // Print the names of files
            append.append("      \t\t<option value=").append(pathname).append("> ").append(pathname).append("</option>\n");
        }

        append.append("\t\t<!--Tar_end-->                  ");

        html = html.replaceAll("(<!--Tar_start-->)[^&]*(<!--Tar_end-->)", append.toString());

        return html;
    }
}

