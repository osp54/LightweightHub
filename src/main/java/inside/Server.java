package inside;

import arc.func.Cons;
import mindustry.net.Host;

import static mindustry.Vars.*;

public class Server {

    public String ip = "darkdustry.ml";
    public int port;

    public int x;
    public int y;

    public float size;

    /** Это умножается на Vars.tilesize! */
    public float titleX;
    public float titleY;

    /** Это умножается на Vars.tilesize! */
    public float labelX;
    public float labelY;

    public boolean inDiapason(int x, int y) {
        return x <= this.x + size && x >= this.x - size && y <= this.y + size && y >= this.y - size;
    }

    public void pingHost(Cons<Host> valid, Cons<Exception> failed) {
        net.pingHost(ip, port, valid, failed);
    }
}