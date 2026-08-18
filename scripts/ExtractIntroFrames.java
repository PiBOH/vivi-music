// Extracts the desktop intro video into a sequence of JPEG frames that
// `IntroSplash.kt` plays at startup.
//
// Why: the original GIF was limited to 256 colors and showed visible banding.
// Playing MP4 directly is not viable on the JVM without a native decoder, and
// jcodec's pure-Java H.264 decoder is too slow (~19 fps on a dev machine) for
// smooth 30 fps playback. So we decode the MP4 once, at build time, into
// full-color JPEG frames and play those instead (fast, cross-platform, works
// on old CPUs).
//
// Usage (requires the jcodec jar already used by the project):
//   javac -cp <jcodec-0.2.5.jar> ExtractIntroFrames.java
//   java  -cp .;<jcodec-0.2.5.jar> ExtractIntroFrames \
//       "desktop/icons/Vivi DE intro.mp4" \
//       "desktop/src/main/resources/images/intro" \
//       0.88 1280
//
// Args: <input.mp4> <outputDir> [jpegQuality=0.88] [maxWidth=-1]
// Output: frame_000.jpg .. frame_NNN.jpg (30 fps, matching the source video).
// After running, update INTRO_FRAME_COUNT in IntroSplash.kt to the new count.
import org.jcodec.api.FrameGrab;
import org.jcodec.common.model.Picture;
import org.jcodec.common.io.NIOUtils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageOutputStream;

public class ExtractIntroFrames {
    public static void main(String[] args) throws Exception {
        String mp4 = args[0];
        String outDir = args[1];
        float quality = args.length > 2 ? Float.parseFloat(args[2]) : 0.88f;
        int maxW = args.length > 3 ? Integer.parseInt(args[3]) : -1;
        new File(outDir).mkdirs();

        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(new File(mp4)));
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        Picture p;
        int n = 0;
        long totalBytes = 0;
        while ((p = grab.getNativeFrame()) != null) {
            BufferedImage img = toRgb(p);
            if (maxW > 0 && img.getWidth() > maxW) {
                int h = (int) Math.round(img.getHeight() * (double) maxW / img.getWidth());
                BufferedImage scaled = new BufferedImage(maxW, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, maxW, h, null);
                g.dispose();
                img = scaled;
            }
            File out = new File(outDir, String.format("frame_%03d.jpg", n));
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
            ios.close();
            totalBytes += out.length();
            n++;
        }
        writer.dispose();
        System.out.println("frames=" + n + " totalBytes=" + totalBytes + " ("
                + (totalBytes / 1024 / 1024) + " MB)");
    }

    /** Converts a jcodec YUV420J frame to an RGB BufferedImage (BT.601 limited range). */
    private static BufferedImage toRgb(Picture p) {
        int w = p.getPlaneWidth(0), h = p.getPlaneHeight(0);
        byte[] y = p.getPlaneData(0);
        byte[] u = p.getPlaneData(1);
        byte[] v = p.getPlaneData(2);
        int uw = p.getPlaneWidth(1), uh = p.getPlaneHeight(1);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] rgb = new int[w * h];
        for (int row = 0; row < h; row++) {
            int uy = (row * uh) / h;
            for (int col = 0; col < w; col++) {
                int ux = (col * uw) / w;
                int yy = y[row * w + col] & 0xff;
                int uu = u[uy * uw + ux] & 0xff;
                int vv = v[uy * uw + ux] & 0xff;
                int c = yy - 16;
                int d = uu - 128;
                int e = vv - 128;
                int r = clamp((298 * c + 409 * e + 128) >> 8);
                int g = clamp((298 * c - 100 * d - 208 * e + 128) >> 8);
                int b = clamp((298 * c + 516 * d + 128) >> 8);
                rgb[row * w + col] = (r << 16) | (g << 8) | b;
            }
        }
        img.setRGB(0, 0, w, h, rgb, 0, w);
        return img;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
