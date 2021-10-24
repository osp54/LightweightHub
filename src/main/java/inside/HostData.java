package inside;

public class HostData {
    /** Server IP address. */
    public String ip = "darkdustry.ml";
    /** Server port. */
    public int port;
    /** The teleport block size. */
    public int size;

    /** The latter <b>X</b> coordinate of the teleport border. */
    public int teleportX;
    /** The latter <b>Y</b> coordinate of the teleport border. */
    public int teleportY;

    /** The teleport title i.e. server display name. */
    public String title = "server";
    /** The latter <b>X</b> coordinate of the title signboard. */
    public int titleX;
    /** The latter <b>Y</b> coordinate of the title signboard. */
    public int titleY;

    /** The latter <b>X</b> coordinate of the status label. */
    public int labelX;
    /** The latter <b>Y</b> coordinate of the status label. */
    public int labelY;

    public HostData(int port, int size, int teleportX, int teleportY, int titleX, int titleY) {
        this.port = port;
        this.size = size;
        this.teleportX = teleportX;
        this.teleportY = teleportY;
        this.titleX = titleX;
        this.titleY = titleY;
        this.labelX = titleX;
        this.labelY = titleY - 36 - 16;
    }

    public boolean inDiapason(int x, int y) {
        return x <= teleportX + size && x >= teleportX && y >= teleportY && y <= teleportY + size;
    }
}
