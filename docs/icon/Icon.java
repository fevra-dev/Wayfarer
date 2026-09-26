// Draws icon.png (48x72, the Plugin Hub maximum). Regenerate from the repo root with:
//   java docs/icon/Icon.java icon.png /tmp/icon-preview
import java.awt.*; import java.awt.geom.*; import java.awt.image.*; import javax.imageio.*; import java.io.*;
public class Icon {
  static final Color PAPER = new Color(0xD4,0xCC,0xBC), PAPER_DK = new Color(0x9C,0x94,0x86),
    AMBER = new Color(0xE8,0xA0,0x20), AMBER_DK = new Color(0xB0,0x74,0x10),
    INK = new Color(0x0A,0x09,0x07);
  public static void main(String[] a) throws Exception {
    int W = 48, H = 72, K = 8;                       // draw at 8x, downsample
    BufferedImage big = new BufferedImage(W*K, H*K, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = big.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    double cx = W*K/2.0, cy = H*K/2.0, R = 23*K, r = 6.5*K, Ri = 14*K, ri = 4.2*K;
    // intercardinals first (behind), then cardinals
    for (int i = 0; i < 8; i++) {
      boolean card = i % 2 == 0; if (card) continue;
      point(g, cx, cy, i*45, Ri, ri, PAPER_DK, new Color(0x6E,0x68,0x5E));
    }
    for (int i = 0; i < 8; i += 2) {
      boolean north = i == 0;
      point(g, cx, cy, i*45, R, r, north ? AMBER : PAPER, north ? AMBER_DK : PAPER_DK);
    }
    // warm-black hub with a paper ring
    double hr = 3.2*K;
    g.setColor(PAPER); g.fill(new Ellipse2D.Double(cx-hr-K*0.9, cy-hr-K*0.9, 2*(hr+K*0.9), 2*(hr+K*0.9)));
    g.setColor(INK); g.fill(new Ellipse2D.Double(cx-hr, cy-hr, 2*hr, 2*hr));
    g.dispose();
    BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    Graphics2D o = out.createGraphics();
    o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    o.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    o.drawImage(big.getScaledInstance(W, H, Image.SCALE_AREA_AVERAGING), 0, 0, null); o.dispose();
    ImageIO.write(out, "png", new File(a[0]));
    // previews at 4x on dark and light
    for (String[] bg : new String[][]{{"dark","1E1E1E"},{"light","E6E6E6"}}) {
      BufferedImage p = new BufferedImage(W*4, H*4, BufferedImage.TYPE_INT_RGB);
      Graphics2D pg = p.createGraphics(); pg.setColor(new Color(Integer.parseInt(bg[1],16))); pg.fillRect(0,0,W*4,H*4);
      pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      pg.drawImage(out, 0, 0, W*4, H*4, null); pg.dispose();
      ImageIO.write(p, "png", new File(a[1] + "-" + bg[0] + ".png"));
    }
  }
  // a compass point as two triangles: lit left half, shaded right half
  static void point(Graphics2D g, double cx, double cy, double deg, double R, double r, Color lit, Color shade) {
    double t = Math.toRadians(deg - 90);
    double tx = cx + R*Math.cos(t), ty = cy + R*Math.sin(t);
    double lx = cx + r*Math.cos(t - Math.PI/2), ly = cy + r*Math.sin(t - Math.PI/2);
    double rx = cx + r*Math.cos(t + Math.PI/2), ry = cy + r*Math.sin(t + Math.PI/2);
    Path2D a = new Path2D.Double(); a.moveTo(cx,cy); a.lineTo(tx,ty); a.lineTo(lx,ly); a.closePath();
    Path2D b = new Path2D.Double(); b.moveTo(cx,cy); b.lineTo(tx,ty); b.lineTo(rx,ry); b.closePath();
    g.setColor(lit); g.fill(a); g.setColor(shade); g.fill(b);
  }
}
