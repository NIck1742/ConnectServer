import java.util.HashSet;

public class Server {
    private String ip;
    private String vpnIp = "";
    private String name;
    private String ssh_Username;
    private String group;
    private HashSet<String> dockerSet = new HashSet<>();
    private String available = "on";
    private boolean dockerInstalled = false;

    public boolean isDockerInstalled() {
        return dockerInstalled;
    }

    public void setDockerInstalled(boolean dockerInstalled) {
        this.dockerInstalled = dockerInstalled;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSsh_Username() {
        return ssh_Username;
    }

    public void setSsh_Username(String ssh_Username) {
        this.ssh_Username = ssh_Username;
    }

    public String getVpnIp() {
        return vpnIp;
    }

    public void setVpnIp(String vpnIp) {
        this.vpnIp = vpnIp;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public HashSet<String> getDockerSet() {
        return dockerSet;
    }

    public void addDocker(String docker) {
        this.dockerSet.add(docker);
    }

    @Override
    public String toString() {
        return  ip + " " + vpnIp + " " + name + " " + ssh_Username + " " + group;
    }

    public void clearDocker() {
        dockerSet = new HashSet<>();
    }
}
