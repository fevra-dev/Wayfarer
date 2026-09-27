// Draws icon.png (48x72, the Plugin Hub maximum). Regenerate from the repo root with:
//   java docs/icon/Icon.java icon.png /tmp/icon-preview
//
// Dieter Rams-style badge: a flat dark tile, a one-line W, and one amber
// needle rising from the W's centre point (amber is the strip's caret
// colour, "where you are pointing"). No shading, no outline, one accent.
import java.awt.*; import java.awt.geom.*; import java.awt.image.*; import javax.imageio.*; import java.io.*;
public class Icon {
  static final Color TILE = new Color(0x35, 0x32, 0x2D);   // lifted from #2B2A28 so it reads on RuneLite's #1E1E1E
  static final Color WHITE = new Color(0xF2, 0xF0, 0xEB);  // Braun white
  static final Color AMBER = new Color(0xE8, 0xA0, 0x20);  // Palette.AMBER
  public static void main(String[] a) throws Exception {
    int W = 48, H = 72, K = 8;                            // draw at 8x, area-average down
    BufferedImage big = new BufferedImage(W*K, H*K, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = big.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g.scale(K, K);
    g.setColor(TILE); g.fill(new RoundRectangle2D.Double(4, 12, 40, 48, 16, 16));
    // W on whole-pixel vertices, stroke 3px, mitred joints
    Path2D w = new Path2D.Double(); w.moveTo(13, 31); w.lineTo(18.5, 51); w.lineTo(24, 38); w.lineTo(29.5, 51); w.lineTo(35, 31);
    g.setColor(WHITE); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f)); g.draw(w);
    // needle from the W's centre point up to the tile's top padding
    Path2D n = new Path2D.Double(); n.moveTo(24, 18); n.lineTo(26.2, 37); n.lineTo(21.8, 37); n.closePath();
    g.setColor(AMBER); g.fill(n);
    g.dispose();
    BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    Graphics2D o = out.createGraphics(); o.drawImage(big.getScaledInstance(W, H, Image.SCALE_AREA_AVERAGING), 0, 0, null); o.dispose();
    ImageIO.write(out, "png", new File(a[0]));
    // previews: 1x and 4x on RuneLite's dark panel and on light
    for (String[] bg : new String[][]{{"dark","1E1E1E"},{"light","E6E6E6"}}) {
      BufferedImage p = new BufferedImage(W*5 + 30, H*4 + 20, BufferedImage.TYPE_INT_RGB);
      Graphics2D pg = p.createGraphics(); pg.setColor(new Color(Integer.parseInt(bg[1],16))); pg.fillRect(0,0,p.getWidth(),p.getHeight());
      pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      pg.drawImage(out, 10, 10, W*4, H*4, null); pg.drawImage(out, W*4 + 20, 10 + H*3/2, W, H, null); pg.dispose();
      ImageIO.write(p, "png", new File(a[1] + "-" + bg[0] + ".png"));
    }
  }
}
