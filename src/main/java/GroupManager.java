import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class GroupManager {

    private final String SERVERLIST_FILE = "/root/.ansible/ConnectServer/serverList";
    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final String PAGE_FOLDER = "/root/.ansible/ConnectServer/pages/";
    private List<String> groupList = new ArrayList<>();


    public void updateServerGroup(Server s){

        Path path = Paths.get(SERVERLIST_FILE);

        String content = null;
        try {
            content = Files.readString(path, CHARSET);
            content = content.replaceFirst("(?m)^" + s.getIp() + ".*", s.toString());
            Files.writeString(path, content, CHARSET);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void manageGroups(LinkedHashMap<String, Server> serverMap) {

        StringBuilder options = new StringBuilder("<!--group_select_start-->\n");
        StringBuilder options1 = new StringBuilder();
        StringBuilder s = new StringBuilder("<!--groups_start-->\n");
        for (String group : groupList) {
            options1.append("                \t<option value=")
                    .append(group)
                    .append("> ")
                    .append(group)
                    .append("</option>\n");
        }
        options.append(options1).append("    \t\t\t<!--group_select_end-->\n");

        for (Server server : serverMap.values()) {
            String group = server.getGroup();
            s.append("    \t\t\t<tr>\n" + "    \t\t\t<td>")
                    .append(server.getName())
                    .append("</td>\n")
                    .append("    \t\t\t<td><Select name = group_select id= group_select>\n")
                    .append(options1.insert(options1.indexOf(group) + group.length()," selected"))
                    .append("                    </Select></td>\n")
                    .append("  \t\t\t</tr>\n");
        }

        s.append("    \t\t\t<!--groups_end-->\n");

        Path path = Paths.get(PAGE_FOLDER + "server.html");

        String content = null;
        try {
            content = Files.readString(path, CHARSET);
            content = content.replaceFirst("(<!--group_select_start-->)[^&]*(<!--group_select_end-->)",
                    options.toString());
            content = content.replaceFirst("(<!--groups_start-->)[^&]*(<!--groups_end-->)", s.toString());
            Files.writeString(path, content, CHARSET);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getAllGroups(LinkedHashMap<String, Server> serverMap) {
        for (Server s : serverMap.values()) {
            if (!groupList.contains(s.getGroup())) {
                groupList.add(s.getGroup());
            }
        }
    }

    public List<String> getGroupList() {
        return groupList;
    }

}
