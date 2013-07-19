/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sikuli.basics;

import java.awt.Container;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author rhocke
 */
public class MultiFrame extends JFrame {

  private JLabel lbl, txt;
  private Container pane;
  private int proSize;
  private int fw, fh;

  public MultiFrame(String type) {
    setResizable(false);
    setUndecorated(true);
    pane = getContentPane();

    if ("download".equals(type)) {
      pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
      pane.add(new JLabel(" "));
      lbl = new JLabel("");
      lbl.setAlignmentX(CENTER_ALIGNMENT);
      pane.add(lbl);
      pane.add(new JLabel(" "));
      txt = new JLabel("... waiting");
      txt.setAlignmentX(CENTER_ALIGNMENT);
      pane.add(txt);
      fw = 250;
      fh = 80;
    }
    
    pack();
    setSize(fw, fh);
    setLocationRelativeTo(null);
    setVisible(true);
  }

  public void setProFile(String proFile) {
    lbl.setText("Downloading: " + proFile);
  }

  public void setProSize(int proSize) {
    this.proSize = proSize;
  }

  public void setProDone(int done) {
    if (done < 0) {
      txt.setText(" ..... failed !!!");
    } else if (proSize > 0) {
      txt.setText(done + " % out of " + proSize + " KB");
    } else {
      txt.setText(done + " KB out of ??? KB");
    }
    repaint();
  }
  
  public void closeAfter(int secs) {
    try {
      Thread.sleep(secs*1000);
    } catch (InterruptedException ex) {
    }
    setVisible(false);
  }
}
