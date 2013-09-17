package org.sikuli.basics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.sikuli.basics.proxies.Mat;
import org.sikuli.basics.proxies.Vision;

public class Image {

  private static String me = "Image";
  private static String mem = "";
  private static int lvl = 3;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "", me + ": " + message, args);
  }
  
  private static List<Image> images = Collections.synchronizedList(new ArrayList<Image>());
  private static Map<String, Image> imageFiles = Collections.synchronizedMap(new HashMap<String, Image>());
  private static int KB = 1024;
  private static int MB = KB * KB;
  private static int maxMemory = 64 * MB;
  private static int currentMemory;
  private static String imageFromJar = "__FROM_JAR__";
  public final static String isBImg = "__BufferedImage__";
  private String imageName;
  private boolean imageIsText = false;
  private String filepath = null;
  private URL fileURL = null;
  private BufferedImage bimg = null;
  private long bsize;
  private int bwidth;
  private int bheight;
  private Rectangle lastSeen = null;
  
  @Override
  public String toString() {
    return (imageName != null ? imageName : "__UNKNOWN__");
  }

  /**
   * Create Image from url, relative or absolute filename
   *
   * @param fname
   */
  public Image(String fname) {
    init(fname);
  }
  
  public Image(URL fURL) {
    if ("file".equals(fURL.getProtocol())) {
      init(fURL.getPath());
    } else if ("jar".equals(fURL.getProtocol())) {
      imageName = getNameFromURL(fURL);
      fileURL = fURL;
      filepath = imageFromJar;
      loadImage();
    } else if (fURL.getProtocol().startsWith("http")) {
      log(-1, "FatalError: Image from http(s) not supported: " + fURL);
      
    } else {
      log(-1, "FatalError: ImageURL not supported: " + fURL);
    }
    fileURL = fURL;
  }
  
  private static String getNameFromURL(URL fURL) {
//TODO add handling for http
    if ("jar".equals(fURL.getProtocol())) {
      int n = fURL.getPath().lastIndexOf(".jar!/");
      int k = fURL.getPath().substring(0, n).lastIndexOf("/");
      if (n > -1) {
        return "JAR:" + fURL.getPath().substring(k+1, n) + fURL.getPath().substring(n+5);
      }
    }
    return null;
  }
  
  private void init(String fname) {
    imageName = fname;
    if (!fname.endsWith(".png")) {
      fname += ".png";
    }
    fname = FileManager.slashify(fname, false);
    if (new File(fname).isAbsolute()) {
      if(new File(fname).exists()) {
        filepath = fname;
        fileURL = FileManager.makeURL(fname);
      } else {
        log(-1, "FatalError: not locatable: " + fname);
      }
    } else {
      for (ImagePath.PathEntry path : ImagePath.getPaths()) {
        if (path == null) {
          continue;
        }
        if ("jar".equals(path.pathURL.getProtocol())) {
          fileURL = FileManager.getURLForContentFromURL(path.pathURL, fname);
          if (fileURL != null) {
            filepath = imageFromJar;
            imageName = getNameFromURL(fileURL);
            break;
          }
        } else if ("file".equals(path.pathURL.getProtocol())) {
          fileURL = FileManager.makeURL(path.pathURL, fname);
          if (new File(fileURL.getPath()).exists()) {
            filepath = fileURL.getPath();
            break;
          }
        }
      }
      if (filepath == null) {
        log(-1, "not found on image path: " + fname);
        ImagePath.printPaths();
      }
    }
    loadImage();
  }
  
  private BufferedImage loadImage() {
    if (filepath != null) {
      try {
          bimg = ImageIO.read(this.fileURL);
      } catch (Exception e) {
        log(-1, "FatalError: image could not be loaded from " + filepath);
        return null;
      }
      if (imageName != null) {
        imageFiles.put(imageName, this);
        log(lvl, "added to image list: %s \nwith URL: %s", 
                imageName, fileURL);
        bwidth = bimg.getWidth();
        bheight = bimg.getHeight();
        bsize = bimg.getData().getDataBuffer().getSize();
        currentMemory += bsize;
        Image first;
        while (images.size() > 0 && currentMemory > maxMemory) {
          first = images.remove(0);
          currentMemory -= first.bsize;
          first.bimg = null;
        }
        images.add(this);
        log(lvl, "loaded %s (%d KB of %d MB (%d / %d %%))", imageName, (int) (bsize / KB),
                (int) (maxMemory / MB), images.size(), (int) (100 * currentMemory / maxMemory));
      } else {
        log(-1, "ImageName invalid! not cached!");
      }
    }
    return bimg;
  }
  
  public static Image getImageFromCache(URL imgURL) {
    return imageFiles.get(getNameFromURL(imgURL));
  }
  
  public static Image getImageFromCache(String imgName) {
    return imageFiles.get(imgName);
  }
  
  public static Image createImage(String imgName) {
    Image img = Image.getImageFromCache(imgName);
    if (img == null) {
      img = new Image(imgName);
    }
    return createImageValidate(img);
  }

  public static Image createImage(URL url) {
    Image img = Image.getImageFromCache(url);
    if (img == null) {
      img = new Image(url);
    }
    return createImageValidate(img);
  }
  
  private static Image createImageValidate(Image img) {
    if (!img.isValid()) {
      if (Settings.OcrTextSearch) {
        img.setIsText(true);
      } else {
        log(-1, "Image not valid, but TextSearch is switched off!");
      }
    }
    return img;
  }

  public Image(BufferedImage img) {
    imageName = isBImg;
    filepath = isBImg;
    bimg = img;
  }
  
  /**
   * check wether image is available
   *
   * @return true if located or is a buffered image
   */
  public boolean isValid() {
    return filepath != null;
  }

  public boolean isText() {
    return imageIsText;
  }

  public void setIsText(boolean isText) {
    imageIsText = isText;
  }

  public URL getURL() {
    return fileURL;
  }
  
  /**
   * Get the image's absolute filename or null if jar, http or in memory only
   *
   * @return
   */
  public String getFilename() {
    if (!"file".equals(fileURL) || isBImg.equals(imageName)) {
      return null;
    }
    return filepath;
  }

  /**
   * Get the image's absolute filename or null if in memory only
   *
   * @return
   */
  public String getName() {
    return imageName;
  }

  /**
   * return the image's BufferedImage (load it if not in cache)
   *
   * @return
   */
  public BufferedImage getImage() {
    if (bimg != null) {
      log(lvl, "getImage: %s taken from cache", imageName);
      return bimg;
    } else {
      return loadImage();
    }
  }
  
  /**
   * Simple getter for BufferedImage
   * 
   * @return 
   */
  public BufferedImage getBImage() {
    return bimg;
  }
  
  public Dimension getSize() {
    return new Dimension(bwidth, bheight);
  }

  public Rectangle getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(Rectangle lastSeen) {
    this.lastSeen = lastSeen;
  }  

  /**
   * return an OpenCV Mat version from the BufferedImage
   *
   * @return
   */
  public Mat getMat() {
    return convertBufferedImageToMat(getImage());
  }

  //<editor-fold defaultstate="collapsed" desc="create an OpenCV Mat from a BufferedImage">
  private static BufferedImage createBufferedImage(int w, int h) {
    ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
    int[] nBits = {8, 8, 8, 8};
    ColorModel cm = new ComponentColorModel(cs, nBits,
            true, false,
            Transparency.TRANSLUCENT,
            DataBuffer.TYPE_BYTE);
    SampleModel sm = cm.createCompatibleSampleModel(w, h);
    DataBufferByte db = new DataBufferByte(w * h * 4); //4 channels buffer
    WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0, 0));
    BufferedImage bm = new BufferedImage(cm, r, false, null);
    return bm;
  }

  private static byte[] convertBufferedImageToByteArray(BufferedImage img) {
    if (img != null) {
      BufferedImage cvImg = createBufferedImage(img.getWidth(), img.getHeight());
      Graphics2D g = cvImg.createGraphics();
      g.drawImage(img, 0, 0, null);
      g.dispose();
      return ((DataBufferByte) cvImg.getRaster().getDataBuffer()).getData();
    } else {
      return null;
    }
  }

  private static Mat convertBufferedImageToMat(BufferedImage img) {
    if (img != null) {
      byte[] data = convertBufferedImageToByteArray(img);
      return Vision.createMat(img.getHeight(), img.getWidth(), data);
    } else {
      return null;
    }
  }
  //</editor-fold>
}
