import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlaybookManager {

    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final String PAGE_FOLDER = "/root/.ansible/ConnectServer/pages/";
    private final String PLAYBOOK_FOLDER = "/root/.ansible/ConnectServer/playbooks/";
    private final String CONTAINER_PLAYBOOK = "/root/.ansible/ConnectServer/playbook_create_container.yml";
    private final ShellService shellService;
    private final ServerManager serverManager;

    public PlaybookManager(ShellService shellService, ServerManager serverManager) {

        this.shellService = shellService;
        this.serverManager = serverManager;
    }

    public void listPlaybooks() throws IOException {

        StringBuilder table = new StringBuilder("<!--playbooks_start-->\n");
        File folder = new File(PLAYBOOK_FOLDER);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    table.append("    <tr>\n" + "    \t\t\t<td><a href=\"/playbook/")
                            .append(file.getName())
                            .append("\">")
                            .append(file.getName())
                            .append("</a></td>\n")
                            .append("    \t\t\t<td><button onclick=\"window.location.href = '/startPlaybook?playbook_name=")
                            .append(file.getName())
                            .append("';\">Apply</button></td>\n")
                            .append("  \t\t\t</tr>\n");
                }
            }

        }

        table.append("            <!--playbooks_end-->\n");

        Path path = Paths.get(PAGE_FOLDER + "server.html");

        String content = Files.readString(path, CHARSET);
        content = content.replaceAll("(<!--playbooks_start-->)[^&]*(<!--playbooks_end-->)", table.toString());
        Files.writeString(path, content, CHARSET);

    }

    public String applyPlaybook(String playbook) throws IOException {
        String command = "ansible-playbook -i inventory.ini " + PLAYBOOK_FOLDER + playbook;
        shellService.executeCommand(command);
        return serverManager.updateServerList(new Server());
    }

    public void convertPlaybook(String portsInput, String envInput) throws IOException {
        Path path = Paths.get(CONTAINER_PLAYBOOK);
        String content = Files.readString(path, CHARSET);

        String s = "NotEmpty";
        String t = "NotEmpty";

        if(portsInput.isEmpty()){
            s = "";
        }
        if(envInput.isEmpty()){
            t = "";
        }

        String[] portSplit = portsInput.split(";");
        String[] envSplit = envInput.split(";");

        envInput = "";
        portsInput = "";
        for (int i = 0; i < envSplit.length; i++) {
            envSplit[i] = envSplit[i].replace("=",": ");
            envSplit[i] = "            " + envSplit[i] + System.lineSeparator();
            envInput = envInput + envSplit[i];
        }

        for (int i = 0; i < portSplit.length; i++) {
            portSplit[i] = "         - \"" + portSplit[i] + "\"" + System.lineSeparator();
            portsInput = portsInput + portSplit[i];
        }

        if(s.contains("NotEmpty")){
            s = "        ports:" + System.lineSeparator() + portsInput;
        }

        if(t.contains("NotEmpty")){
            t = "        env:" + System.lineSeparator() + envInput;
        }

        String replacement ="#->" + System.lineSeparator() + s + t ;

        content = content.substring(0,content.indexOf("#"));
        content = content + replacement;
        Files.writeString(path, content, CHARSET);
    }
}
