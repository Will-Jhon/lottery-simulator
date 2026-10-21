import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;

/**
 * 手机版静态服务器（零依赖，JDK8 自带 com.sun.net.httpserver）
 * 用法：java -cp . Serve [端口]   默认 8080
 * 启动后手机连同一 WiFi，浏览器访问打印出的 IP 地址即可
 */
public class Serve {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        final File root = new File(".").getCanonicalFile();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange ex) throws IOException {
                try {
                    String path = ex.getRequestURI().getPath();
                    if (path == null || path.isEmpty() || path.equals("/")) path = "/index.html";
                    File f = new File(root, URLDecoder.decode(path, "UTF-8")).getCanonicalFile();
                    // 防目录穿越
                    if (!f.getPath().startsWith(root.getPath())) {
                        sendText(ex, 403, "403 Forbidden");
                        return;
                    }
                    if (!f.isFile()) {
                        sendText(ex, 404, "404 Not Found");
                        return;
                    }
                    byte[] data = Files.readAllBytes(f.toPath());
                    ex.getResponseHeaders().set("Content-Type", mimeType(f.getName()));
                    ex.getResponseHeaders().set("Cache-Control", "no-cache");
                    ex.sendResponseHeaders(200, data.length);
                    OutputStream os = ex.getResponseBody();
                    os.write(data);
                    os.close();
                } catch (Exception e) {
                    sendText(ex, 500, "500 Internal Error");
                }
            }
        });
        server.setExecutor(null);
        server.start();
        System.out.println("========================================");
        System.out.println("  双色球模拟器 - 手机版已启动");
        System.out.println("========================================");
        System.out.println("  电脑访问 : http://localhost:" + port);
        System.out.println("  手机访问 : http://" + lanIp() + ":" + port + "  (需同一 WiFi)");
        System.out.println("  手机打开后，浏览器菜单可『添加到主屏幕』");
        System.out.println("  按 Ctrl+C 停止服务");
    }

    /** 取第一个可用的 IPv4 局域网地址 */
    private static String lanIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "<本机IP>";
    }

    private static String mimeType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private static void sendText(HttpExchange ex, int code, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        OutputStream os = ex.getResponseBody();
        os.write(data);
        os.close();
    }
}
