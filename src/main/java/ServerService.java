import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;

import spark.Request;
import spark.Response;
import spark.Service;

import static spark.Spark.exception;
import static spark.Spark.staticFiles;

public class ServerService {
    public int port = 42000;
    private final String INVENTORY = "/root/.ansible/ConnectServer/inventory.ini";
    private final String DOCKERSETTINGS = "/root/.ansible/ConnectServer/vars/default.yml";
    private final String PLAYBOOK_FOLDER = "/root/.ansible/ConnectServer/playbooks/";
    private final String CONTAINER_FOLDER = "/root/.ansible/ConnectServer/containers/";
    private final String TEXTAREA_FILE = "/root/.ansible/ConnectServer/pages/textAreaPage.html/";
    private final String SHOW_CONTAINER_FILE = "/root/.ansible/ConnectServer/pages/containerPage.html/";
    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final DockerService dockerService;
    private final GroupManager groupManager;
    private final ServerManager serverManager;
    private final PlaybookManager playbookManager;

    public ServerService(DockerService dockerService, GroupManager groupManager, ServerManager serverManager, PlaybookManager playbookManager) {
        this.dockerService = dockerService;
        this.groupManager = groupManager;
        this.serverManager = serverManager;
        this.playbookManager = playbookManager;
    }

    public static void main(String[] args) {
        ShellService shellService = new ShellService();
        DockerService dockerService = new DockerService(shellService);
        GroupManager groupManager = new GroupManager();
        ServerManager serverManager = new ServerManager(dockerService,shellService,groupManager);
        PlaybookManager playbookManager = new PlaybookManager(shellService,serverManager);
        new ServerService(dockerService, groupManager, serverManager, playbookManager).start();
    }

    public void start() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Service spark = Service.ignite();
        spark.port(port);

        spark.get("/", (request, response) -> executor.submit(() -> this.mainControl(request, response)).get());
        spark.get("/subscribe",
                  (request, response) -> executor.submit(() -> this.postSubscribe(request, response)).get());
        spark.get("/saveDocker", (request, response) -> executor.submit(() -> this.setDocker(request, response)).get());
        spark.get("/page/:ip", ((request, response) -> executor.submit(() -> this.getPage(request, response)).get()));
        spark.get("/playbook/:play",
                  ((request, response) -> executor.submit(() -> this.getPlaybook(request, response)).get()));
        spark.get("/startContainer",
                  (request, response) -> executor.submit(() -> this.startContainer(request, response)).get());
        spark.get("/stopContainer",
                  (request, response) -> executor.submit(() -> this.stopContainer(request, response)).get());
        spark.get("/removeContainer",
                  (request, response) -> executor.submit(() -> this.removeContainer(request, response)).get());
        spark.get("/savePlaybook",
                  (request, response) -> executor.submit(() -> this.writePlaybook(request, response)).get());
        spark.get("/startPlaybook",
                  (request, response) -> executor.submit(() -> this.startPlaybook(request, response)).get());
        spark.get("/createGroup",
                  (request, response) -> executor.submit(() -> this.createGroup(request, response)).get());
        spark.get("/manageGroups",
                  (request, response) -> executor.submit(() -> this.applyGroups(request, response)).get());
        spark.get("/addContainer",
                  (request, response) -> executor.submit(() -> this.addContainer(request, response)).get());
        spark.get("/page/:ip/:container",
                  (request, response) -> executor.submit(() -> this.getContainer(request, response)).get());
        spark.get("/page/inventory",
                  (request, response) -> executor.submit(() -> this.getInventory(request, response)).get());
        spark.get("/demo",
                (request, response) -> executor.submit(() -> this.setDemoEnvironment(request, response)).get());
        spark.get("/installDocker",
                (request, response) -> executor.submit(() -> this.applyDockerPlaybook(request, response)).get());
        spark.get("/pullImage",
                (request, response) -> executor.submit(() -> this.pullingImage(request, response)).get());
        spark.post("/uploadFile",
                   (request, response) -> executor.submit(() -> this.uploadFile(request, response)).get());
        spark.get("/tarball",
                (request, response) -> executor.submit(() -> this.applyTarball(request, response)).get());
        exception(Exception.class, (e,request,response) -> {e.printStackTrace();});

        serverManager.setServerIp();

    }

    private String pullingImage(Request request, Response response) throws IOException {
        String host = request.queryParams("ip");
        String image = request.queryParams("image");
        dockerService.pullImage(host,image);
        return serverManager.updateServerList(serverManager.getServerMap().get(host));
    }

    private String setDemoEnvironment(Request request, Response response) throws IOException {
        System.out.println("Demo Started");
        serverManager.getDemoVersion();
        dockerService.isDemo();
        return serverManager.updateServerList(new Server());
    }

    private String getInventory(Request request, Response response) {
        Path invPath = Paths.get(INVENTORY);
        Path pagePath = Paths.get(TEXTAREA_FILE);

        try {
            String content = Files.readString(invPath, CHARSET);
            String page = Files.readString(pagePath, CHARSET);
            page = page.replaceAll("(<textarea id=\"playbook\" name=\"playbook\" rows=\"40\" cols=\"100\">)[^&]*(</textarea>)",
                                   content);
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            return "ups";
        }
    }

    private String addContainer(Request request, Response response) {
        String name = request.queryParams("name");
        String image = request.queryParams("image");
        String ip = request.queryParams("ip");
        try {
            playbookManager.convertPlaybook(request.queryParams("ports"),request.queryParams("envs"));
            dockerService.applyAddContainer(ip, image, name);
            dockerService.setServerlist(serverManager.getServerMap());
            dockerService.applyShowContainer();
            } catch (IOException e) {
                    e.printStackTrace();
            }

        try {
            return serverManager.updateServerList(serverManager.getServerMap().get(ip));
        } catch (IOException e) {
            e.printStackTrace();
            return "ups";
        }

    }

    private String getContainer(Request request, Response response) {

        Path pagePath = Paths.get(SHOW_CONTAINER_FILE);

        String id = request.params("ip");

        Server server = new Server();

        for (Server s : serverManager.getServerMap().values()) {
            if (s.getIp().contains(id)) {
                server = s;
                break;
            }
        }

        String container = request.params("container");

        String inspect = "";
        String log = "";
        try {
            dockerService.applyGetContainerInfos(id, container);
            Path logPath = Paths.get(CONTAINER_FOLDER + server.getName()
                                     + "/"
                                     + server.getIp()
                                     + "/root/.ansible/tmp/"
                                     + container
                                     + "Logs");
            Path inspectPath = Paths.get(CONTAINER_FOLDER + server.getName()
                                         + "/"
                                         + server.getIp()
                                         + "/root/.ansible/tmp/"
                                         + container
                                         + "Inspect");
            inspect = dockerService.parseDockerLog(inspectPath);
            log = dockerService.parseDockerLog(logPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String page = null;
        try {
            page = Files.readString(pagePath, CHARSET);
            page = page.replace("<textarea id=\"inspect\" name=\"inspect\" rows=\"40\" cols=\"100\" readonly></textarea>",
                                inspect);
            page = page.replace("<textarea id=\"log\" name=\"log\" rows=\"40\" cols=\"100\" readonly></textarea>", log);
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            return "ups";
        }

    }

    private String getPlaybook(Request request, Response response) throws IOException {

        String name = request.params("play");

        Path playbookPath = Paths.get(PLAYBOOK_FOLDER + name);
        Path pagePath = Paths.get(TEXTAREA_FILE);

        try {
            String content = Files.readString(playbookPath, CHARSET);
            content = "<textarea id=\"playbook\" name=\"playbook\" rows=\"40\" cols=\"100\">" + content + "</textarea>";
            String page = Files.readString(pagePath, CHARSET);
            page = page.replaceAll("(<textarea id=\"playbook\" name=\"playbook\" rows=\"40\" cols=\"100\">)[^&]*(</textarea>)",
                                   content);
            page = page.replaceAll("\\babsenden\\b",name);
            return page;
        } catch (IOException e) {
            e.printStackTrace();
            return "ups";
        }
    }

    private String uploadFile(Request request, Response response) throws IOException {

        File uploadDir = new File("upload");
        uploadDir.mkdir(); // create the upload directory if it doesn't exist

        staticFiles.externalLocation("upload");

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(uploadDir.toPath(), "", "");
        } catch (IOException exception) {
            exception.printStackTrace();
        }


        request.attribute("org.eclipse.jetty.multipartConfig",
                          new MultipartConfigElement("/root/.ansible/ConnectServer"));

        try (InputStream input = request.raw().getPart("dockerfile").getInputStream()) { // getPart needs to use same "name" as input field in form
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);

            return "<p style=\"text-align: center;\"><button onclick=\"window.location.href='/'\">Continue</button></p>\n";
        } catch (IOException | ServletException e) {
            e.printStackTrace();
            return "ups";
        }

    }


    private String applyGroups(Request request, Response response) throws IOException {
        String[] groups = request.queryParamsValues("group_select");
        int i = 0;
        for (Server s : serverManager.getServerMap().values()) {
            s.setGroup(groups[i++]);
            groupManager.updateServerGroup(s);
        }

        return serverManager.updateServerList(new Server());

    }


    private String createGroup(Request request, Response response) throws IOException {
        String group = request.queryParams("group");
        groupManager.getGroupList().add(group);
        groupManager.manageGroups(serverManager.getServerMap());
        return serverManager.updateServerList(new Server());
    }

    private String startPlaybook(Request request, Response response) throws IOException {
        playbookManager.applyPlaybook(request.queryParams("playbook_name"));
        return serverManager.updateServerList(new Server());
    }

    private String removeContainer(Request request, Response response) throws IOException {
        if (request.queryParams().size() == 2) {
            try {
                dockerService.applyRemoveContainer(request.queryParams("ip"), request.queryParams("name"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (request.queryParams().size() == 1) {
            try {
                dockerService.applyRemoveAllContainerOnHost(request.queryParams("ip"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return serverManager.updateServerList(serverManager.getServerMap().get(request.queryParams("ip")));

    }

    private String stopContainer(Request request, Response response) throws IOException {
        if (request.queryParams().size() == 2) {

                dockerService.applyStopContainer(request.queryParams("host"), request.queryParams("container"));

        } else if (request.queryParams().size() == 1) {

                dockerService.applyStopAllContainerOnHost(request.queryParams("host"));

        }

        return serverManager.updateServerList(serverManager.getServerMap().get(request.queryParams("host")));

    }

    private String startContainer(Request request, Response response) throws IOException {
        if (request.queryParams().size() == 2) {

            dockerService.applyStartContainer(request.queryParams("host"), request.queryParams("container"));

        } else if (request.queryParams().size() == 1) {

            dockerService.applyStartAllContainerOnHost(request.queryParams("host"));

        }

        return serverManager.updateServerList(serverManager.getServerMap().get(request.queryParams("host")));

    }

    private String getPage(Request request, Response response) {
        String id = request.params("ip");
        try {
            return serverManager.updateServerList(serverManager.getServerMap().get(id));
        } catch (IOException e) {
            e.printStackTrace();
            return "ups in getPage";
        }
    }


    private String mainControl(Request request, Response response) throws IOException {
        return serverManager.updateServerList(new Server());
    }

    private String postSubscribe(Request request, Response response) throws IOException, InterruptedException {
        if (request.queryParams("sshUser").length() > 0 && request.queryParams("ip").length() > 0
            && request.queryParams("name").length() > 0
            && request.queryParams("group").length() > 0) {
            if (serverManager.getServerMap().containsKey(request.queryParams("ip"))) {
                return "ip exists";
            }
            Server newServer = new Server();
            newServer.setIp(request.queryParams("ip"));
            newServer.setSsh_Username(request.queryParams("sshUser"));
            newServer.setName(request.queryParams("name"));
            newServer.setGroup(request.queryParams("group"));

            boolean installDocker = Boolean.parseBoolean(request.queryParams("docker"));

            if (!serverManager.getServerMap().containsValue(request.ip()) && serverManager.changeSshKeys(newServer,installDocker)) {
                serverManager.getServerMap().put(newServer.getIp(), newServer);
                dockerService.setServerlist(serverManager.getServerMap());
            }
        }

        return serverManager.updateServerList(new Server());
    }



    private String setDocker(Request request, Response response) throws IOException {
        Path path = Paths.get(DOCKERSETTINGS);

        String image = request.queryParams("image");
        String containers = request.queryParams("container");

        String content = Files.readString(path, CHARSET);
        content = content.replaceFirst("image(.*)", "image: " + image);
        content = content.replaceFirst("create(.*)", "create_containers: " + containers);
        Files.writeString(path, content, CHARSET);

        return serverManager.updateServerList(new Server());
    }

    private String applyDockerPlaybook(Request request, Response response) throws IOException {
        String host = request.queryParams("ip");
        dockerService.installDocker(host);
        dockerService.setServerlist(serverManager.getServerMap());
        dockerService.applyShowContainer();
        return serverManager.updateServerList(serverManager.getServerMap().get(host));
    }


    private String writePlaybook(Request request, Response response) throws IOException {
        String playbook_name = request.queryParams("playbook_name") + ".yml";
        String playbook = request.queryParams("playbook");
        if(playbook_name.equals("null.yml")){
            playbook_name = request.queryParams("id");
        }

        Path path = Paths.get(PLAYBOOK_FOLDER + playbook_name);

        try {
            Files.writeString(path, playbook, CHARSET, StandardOpenOption.CREATE);
            playbookManager.listPlaybooks();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serverManager.updateServerList(new Server());
    }

    private String applyTarball(Request request, Response response) throws IOException {
        dockerService.setServerlist(serverManager.getServerMap());
        dockerService.createImageFromTarball(request.queryParams("ip"),request.queryParams("tar"));
        return serverManager.updateServerList(serverManager.getServerMap().get(request.queryParams("ip")));
    }

}
