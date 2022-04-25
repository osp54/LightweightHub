package inside;

import arc.func.Cons;
import mindustry.net.Host;

import static mindustry.Vars.*;

public class HostData {

    public String ip = "darkdustry.ml";
    public int port;

    public int size;

    public int teleportX;
    public int teleportY;

    public int titleX() {
        return teleportX * tilesize + 4;
    }

    public int titleY() {
        return teleportY * tilesize + 32;
    }

    public int labelX() {
        return teleportX * tilesize + 4;
    }

    public int labelY() {
        return teleportY * tilesize - 20;
    }

    public boolean inDiapason(int x, int y) {
        return x <= teleportX + size && x >= teleportX && y >= teleportY && y <= teleportY + size;
    }

    public void pingHost(Cons<Host> valid, Cons<Exception> failed) {
        net.pingHost(ip, port, valid, failed);
    }
}
