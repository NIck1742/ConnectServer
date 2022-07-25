import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShellService {

    public String getOutput(Process process, Map<String,Server> serverMap) throws IOException {
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        String out = "";
        while ((s = stdInput.readLine()) != null) {
            out += s;
        }
        System.out.println(out);
        if(serverMap != null){
            checkIfReachable(out,serverMap);
        }

        // Read any errors from the attempted command
        if(stdError.readLine() == null){
            return out;
        }
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            out += s;
        }
        System.out.println(out);
        return out;
    }

    private void checkIfReachable(String out, Map<String,Server> serverMap) {
        Pattern pattern = Pattern.compile("\\b(?:[0-9]{1,3}\\.){2}[0-9]{1,3}]: UNREACHABLE!");
        List<String> res = new ArrayList<>();
        Matcher matcher = pattern.matcher(out);
        while (matcher.find()){
            res.add(matcher.group(0));
        }

        pattern = Pattern.compile("\\b(?:[0-9]{1,3}\\.){2}[0-9]{1,3}\\b");

        for (String s :res) {
            matcher = pattern.matcher(s);
            while (matcher.find()){
                res.add(matcher.group(0));
            }
            res.remove(s);
        }
        for (Server s: serverMap.values()) {
            if(res.isEmpty() && !s.getAvailable().contains("demo")){
                s.setAvailable("on");
            }
            for (String string :res) {
                if (s.getIp().contains(string) && !s.getAvailable().contains("demo")) {
                    s.setAvailable("off");
                }
            }
            if (s.getName().contains("DEMO")){
                s.setAvailable("demo");
            }
        }
    }

    public String executeCommand(String[] cmdline,Map<String,Server> serverMap) throws IOException {
        Process process = Runtime.getRuntime().exec(cmdline);
        return getOutput(process,serverMap);
    }

    public String executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        return getOutput(process,null);
    }

}
